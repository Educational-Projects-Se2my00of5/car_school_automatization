package ru.hits.car_school_automatization.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import ru.hits.car_school_automatization.dto.*;
import ru.hits.car_school_automatization.entity.*;
import ru.hits.car_school_automatization.enums.Role;
import ru.hits.car_school_automatization.enums.TaskType;
import ru.hits.car_school_automatization.exception.BadRequestException;
import ru.hits.car_school_automatization.exception.ForbiddenException;
import ru.hits.car_school_automatization.exception.NotFoundException;
import ru.hits.car_school_automatization.repository.*;

import java.time.Instant;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class TaskSolutionService {

    private final TeamRepository teamRepository;
    private final SolutionVoteRepository solutionVoteRepository;
    private final TaskSolutionRepository taskSolutionRepository;
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final JwtTokenProvider tokenProvider;
    private final FileStorageService fileStorageService;

    public TaskSolutionDto create(UUID taskId, CreateTaskSolutionDto dto, String authHeader) {
        Long userId = tokenProvider.extractUserIdFromHeader(authHeader);
        User user = getUserById(userId);

        Task task = getTaskById(taskId);
        validateUserInTaskChannel(userId, task);

        if (!user.getRole().contains(Role.STUDENT)) {
            throw new ForbiddenException("Только студент может отправить решение");
        }

        Team team = teamRepository.findByTask_IdAndUsers_Id(taskId, userId)
                .orElseThrow(() -> new ForbiddenException("Вы не состоите в команде этого задания"));

        if (taskSolutionRepository.existsByTaskIdAndStudentId(task.getId(), userId)) {
            throw new BadRequestException("Вы уже отправили решение на это задание");
        }

        List<TaskDocument> documents = storeDocumentsFromFiles(dto.getDocuments());
        if (documents.isEmpty()) {
            throw new BadRequestException("Добавьте хотя бы один файл");
        }

        TaskSolution solution = TaskSolution.builder()
                .taskId(task.getId())
                .teamId(team.getId())
                .studentId(userId)
                .documents(documents)
                .build();

        return toDto(taskSolutionRepository.save(solution));
    }

    @Transactional
    public TaskSolutionDto getById(UUID solutionId, String authHeader) {
        Long requesterId = tokenProvider.extractUserIdFromHeader(authHeader);
        User requester = getUserById(requesterId);

        TaskSolution solution = getSolutionById(solutionId);
        Task task = getTaskById(solution.getTaskId());
        validateCanReadSolution(requester, requesterId, solution, task);

        return toDto(solution);
    }

    @Transactional
    public List<TaskSolutionDto> getByTask(UUID taskId, String authHeader) {
        Long requesterId = tokenProvider.extractUserIdFromHeader(authHeader);
        Task task = getTaskById(taskId);
        validateUserInTaskChannel(requesterId, task);

        return taskSolutionRepository.findByTaskIdOrderByCreatedAtDesc(taskId)
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public List<TaskSolutionDto> getMySolutions(String authHeader) {
        Long requesterId = tokenProvider.extractUserIdFromHeader(authHeader);

        return taskSolutionRepository.findByStudentIdOrderByCreatedAtDesc(requesterId)
                .stream()
                .map(this::toDto)
                .toList();
    }

    public TaskSolutionDto update(UUID solutionId, UpdateTaskSolutionDto dto, String authHeader) {
        Long requesterId = tokenProvider.extractUserIdFromHeader(authHeader);
        TaskSolution solution = getSolutionById(solutionId);

        if (!solution.getStudentId().equals(requesterId)) {
            throw new ForbiddenException("Вы можете редактировать только свои решения");
        }

        Task task = getTaskById(solution.getTaskId());
        validateUserInTaskChannel(requesterId, task);

        if (dto.getDocuments() != null) {
            replaceDocuments(solution, dto.getDocuments());
        }

        if (solution.getDocuments() == null || solution.getDocuments().isEmpty()) {
            throw new BadRequestException("Добавьте хотя бы один файл");
        }

        return toDto(taskSolutionRepository.save(solution));
    }

    public void delete(UUID solutionId, String authHeader) {
        Long requesterId = tokenProvider.extractUserIdFromHeader(authHeader);
        User requester = getUserById(requesterId);

        TaskSolution solution = getSolutionById(solutionId);
        Task task = getTaskById(solution.getTaskId());
        validateCanDeleteSolution(requester, requesterId, solution, task);

        deleteSolutionDocuments(solution);
        taskSolutionRepository.delete(solution);
    }

    public SolutionVoteDto vote(CreateSolutionVoteDto dto, String authHeader) {
        Long voterId = tokenProvider.extractUserIdFromHeader(authHeader);
        User voter = getUserById(voterId);

        Task task = getTaskById(dto.getTaskId());

        validateUserInTaskChannel(voterId, task);

        Team team = teamRepository.findByTask_IdAndUsers_Id(task.getId(), voterId)
                .orElseThrow(() -> new ForbiddenException("Вы не состоите в команде этого задания"));

        if (task.getVotingDeadline() != null && Instant.now().isAfter(task.getVotingDeadline())) {
            throw new BadRequestException("Голосование закончилось");
        }

        if (task.getType() != TaskType.DEMOCRATIC && task.getType() != TaskType.QUALIFIED) {
            throw new BadRequestException("Для этого задания голосование не предусмотрено");
        }

        TaskSolution solution = getSolutionById(dto.getSolutionId());
        if (!solution.getTaskId().equals(task.getId())) {
            throw new BadRequestException("Решение не принадлежит этому заданию");
        }

        if (solution.getStudentId().equals(voterId)) {
            throw new BadRequestException("Нельзя голосовать за своё решение");
        }

        if (solutionVoteRepository.existsByTaskIdAndVoterId(task.getId(), voterId)) {
            throw new BadRequestException("Вы уже проголосовали");
        }

        SolutionVote vote = SolutionVote.builder()
                .taskId(task.getId())
                .solutionId(dto.getSolutionId())
                .voterId(voterId)
                .build();

        SolutionVote saved = solutionVoteRepository.save(vote);
        log.info("Студент {} проголосовал за решение {} в задании {}", voterId, dto.getSolutionId(), task.getId());

        return toVoteDto(saved);
    }

    public VotingResultsDto getVotingResults(UUID taskId, String authHeader) {
        Long requesterId = tokenProvider.extractUserIdFromHeader(authHeader);
        User requester = getUserById(requesterId);

        Task task = getTaskById(taskId);
        validateUserInTaskChannel(requesterId, task);

        Team team = teamRepository.findByTask_IdAndUsers_Id(taskId, requesterId).orElse(null);
        boolean isTeacher = isTeacherOrManager(requester);

        if (!isTeacher && team == null) {
            throw new ForbiddenException("У вас нет прав на просмотр результатов голосования");
        }

        List<Object[]> voteResults = solutionVoteRepository.countVotesBySolution(taskId);
        List<TaskSolution> solutions = taskSolutionRepository.findByTaskId(taskId);

        int totalVotes = voteResults.stream().mapToInt(r -> ((Long) r[1]).intValue()).sum();

        List<VoteResultDto> results = new ArrayList<>();

        for (Object[] result : voteResults) {
            UUID solutionId = (UUID) result[0];
            Long votesCount = (Long) result[1];

            TaskSolution solution = solutions.stream()
                    .filter(s -> s.getId().equals(solutionId))
                    .findFirst()
                    .orElse(null);

            if (solution == null) continue;

            double percentage = totalVotes > 0 ? (votesCount * 100.0 / totalVotes) : 0;

            List<VoteResultDto.VoterInfoDto> voters = null;
            if (!task.getIsAnonymousVoting() && (isTeacher || team != null)) {
                List<SolutionVote> votes = solutionVoteRepository.findBySolutionId(solutionId);
                voters = votes.stream().map(vote -> {
                    User voter = userRepository.findById(vote.getVoterId()).orElse(null);
                    return VoteResultDto.VoterInfoDto.builder()
                            .voterId(vote.getVoterId())
                            .voterName(voter != null ? voter.getFirstName() + " " + voter.getLastName() : null)
                            .build();
                }).toList();
            }

            results.add(VoteResultDto.builder()
                    .solutionId(solutionId)
                    .solutionLabel(solution.getTaskId().toString())
                    .votesCount(votesCount.intValue())
                    .percentage(percentage)
                    .voters(voters)
                    .build());
        }

        return VotingResultsDto.builder()
                .taskId(task.getId())
                .taskLabel(task.getLabel())
                .isAnonymous(task.getIsAnonymousVoting())
                .totalVotes(totalVotes)
                .results(results)
                .build();
    }

    public SolutionVoteDto getMyVote(UUID taskId, String authHeader) {
        Long voterId = tokenProvider.extractUserIdFromHeader(authHeader);

        Task task = getTaskById(taskId);
        validateUserInTaskChannel(voterId, task);

        SolutionVote vote = solutionVoteRepository.findByTaskIdAndVoterId(taskId, voterId)
                .orElse(null);

        if (vote == null) {
            return null;
        }

        return toVoteDto(vote);
    }

    public void cancelVote(UUID taskId, String authHeader) {
        Long voterId = tokenProvider.extractUserIdFromHeader(authHeader);

        Task task = getTaskById(taskId);
        validateUserInTaskChannel(voterId, task);

        if (task.getVotingDeadline() != null && Instant.now().isAfter(task.getVotingDeadline())) {
            throw new BadRequestException("Голосование закончилось, отменить голос нельзя");
        }

        SolutionVote vote = solutionVoteRepository.findByTaskIdAndVoterId(taskId, voterId)
                .orElseThrow(() -> new NotFoundException("Вы не голосовали в этом задании"));

        solutionVoteRepository.delete(vote);
        log.info("Студент {} отменил свой голос в задании {}", voterId, taskId);
    }

    public TaskSolutionDto selectAcceptedSolution(UUID taskId, UUID teamId, String authHeader) {
        Long requesterId = tokenProvider.extractUserIdFromHeader(authHeader);

        Task task = getTaskById(taskId);
        validateUserInTaskChannel(requesterId, task);

        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new NotFoundException("Команда не найдена"));

        boolean inTeam = team.getUsers().stream().anyMatch(u -> u.getId().equals(requesterId));
        if (!inTeam) {
            throw new ForbiddenException("Вы не состоите в этой команде");
        }

        if (task.getVotingDeadline() != null && Instant.now().isAfter(task.getVotingDeadline())) {
            throw new BadRequestException("Дедлайн задания прошёл");
        }

        List<TaskSolution> solutions = taskSolutionRepository.findByTaskIdAndTeamId(taskId, teamId);

        if (solutions.isEmpty()) {
            throw new BadRequestException("Нет решений для выбора");
        }

        TaskSolution selectedSolution = switch (task.getType()) {
            case FIRST -> selectFirstSolution(solutions);
            case LAST -> selectLastSolution(solutions);
            case CAPITAN -> selectCapitanSolution(solutions, team);
            case DEMOCRATIC -> selectDemocraticSolution(taskId, solutions, team);
            case QUALIFIED -> selectQualifiedSolution(taskId, solutions, team, task);
            default -> throw new BadRequestException("Для этого типа задания автоматический выбор не предусмотрен");
        };

        taskSolutionRepository.unselectAllByTaskIdAndTeamId(taskId, teamId);
        selectedSolution.setIsSelected(true);
        TaskSolution saved = taskSolutionRepository.save(selectedSolution);

        return toDto(saved);
    }

    private TaskSolution selectFirstSolution(List<TaskSolution> solutions) {
        return solutions.stream()
                .min(Comparator.comparing(TaskSolution::getCreatedAt))
                .orElseThrow(() -> new BadRequestException("Нет решений"));
    }

    private TaskSolution selectLastSolution(List<TaskSolution> solutions) {
        return solutions.stream()
                .max(Comparator.comparing(TaskSolution::getCreatedAt))
                .orElseThrow(() -> new BadRequestException("Нет решений"));
    }

    private TaskSolution selectCapitanSolution(List<TaskSolution> solutions, Team team) {
        if (team.getCaptainId() == null) {
            throw new BadRequestException("В команде не выбран капитан");
        }

        return solutions.stream()
                .filter(s -> s.getStudentId().equals(team.getCaptainId()))
                .findFirst()
                .orElseThrow(() -> new BadRequestException("Капитан не загрузил решение"));
    }

    private TaskSolution selectDemocraticSolution(UUID taskId, List<TaskSolution> solutions, Team team) {
        List<Object[]> voteResults = solutionVoteRepository.countVotesBySolution(taskId);

        if (voteResults.isEmpty()) {
            throw new BadRequestException("Нет голосов для выбора решения");
        }

        long maxVotes = 0;
        List<UUID> topSolutions = new ArrayList<>();

        for (Object[] result : voteResults) {
            UUID solutionId = (UUID) result[0];
            Long votesCount = (Long) result[1];

            if (votesCount > maxVotes) {
                maxVotes = votesCount;
                topSolutions.clear();
                topSolutions.add(solutionId);
            } else if (votesCount == maxVotes) {
                topSolutions.add(solutionId);
            }
        }

        UUID selectedSolutionId = topSolutions.size() == 1
                ? topSolutions.get(0)
                : topSolutions.get(new Random().nextInt(topSolutions.size()));

        return solutions.stream()
                .filter(s -> s.getId().equals(selectedSolutionId))
                .findFirst()
                .orElseThrow(() -> new BadRequestException("Выбранное решение не найдено"));
    }

    private TaskSolution selectQualifiedSolution(UUID taskId, List<TaskSolution> solutions, Team team, Task task) {
        Integer qualifiedMin = task.getQualifiedMin();
        if (qualifiedMin == null) {
            throw new BadRequestException("Для квалифицированного голосования не задан порог");
        }

        List<Object[]> voteResults = solutionVoteRepository.countVotesBySolution(taskId);

        if (voteResults.isEmpty()) {
            throw new BadRequestException("Нет голосов для выбора решения");
        }

        int totalTeamMembers = team.getUsers().size();
        int requiredVotes = (int) Math.ceil(totalTeamMembers * qualifiedMin / 100.0);

        List<UUID> qualifiedSolutions = new ArrayList<>();

        for (Object[] result : voteResults) {
            UUID solutionId = (UUID) result[0];
            Long votesCount = (Long) result[1];

            if (votesCount >= requiredVotes) {
                qualifiedSolutions.add(solutionId);
            }
        }

        if (qualifiedSolutions.isEmpty()) {
            throw new BadRequestException("Ни одно решение не набрало необходимый порог голосов (" + requiredVotes + " из " + totalTeamMembers + ")");
        }

        UUID selectedSolutionId = qualifiedSolutions.getFirst();

        return solutions.stream()
                .filter(s -> s.getId().equals(selectedSolutionId))
                .findFirst()
                .orElseThrow(() -> new BadRequestException("Выбранное решение не найдено"));
    }

    private SolutionVoteDto toVoteDto(SolutionVote vote) {
        User voter = userRepository.findById(vote.getVoterId()).orElse(null);

        return SolutionVoteDto.builder()
                .id(vote.getId())
                .taskId(vote.getTaskId())
                .solutionId(vote.getSolutionId())
                .voterId(vote.getVoterId())
                .voterName(voter != null ? voter.getFirstName() + " " + voter.getLastName() : null)
                .build();
    }

    public TaskSolutionDto getSelectedSolution(UUID taskId, UUID teamId, String authHeader) {
        Long requesterId = tokenProvider.extractUserIdFromHeader(authHeader);

        Task task = getTaskById(taskId);
        validateUserInTaskChannel(requesterId, task);

        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new NotFoundException("Команда не найдена"));

        boolean inTeam = team.getUsers().stream().anyMatch(u -> u.getId().equals(requesterId));
        boolean isTeacher = isTeacherOrManager(getUserById(requesterId));

        if (!inTeam && !isTeacher) {
            throw new ForbiddenException("У вас нет доступа к этой команде");
        }

        TaskSolution selected = taskSolutionRepository.findByTaskIdAndTeamIdAndIsSelectedTrue(taskId, teamId)
                .orElse(null);

        return selected != null ? toDto(selected) : null;
    }

    private void replaceDocuments(TaskSolution solution, List<MultipartFile> files) {
        deleteSolutionDocuments(solution);
        solution.setDocuments(storeDocumentsFromFiles(files));
    }

    private List<TaskDocument> storeDocumentsFromFiles(List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            return new ArrayList<>();
        }

        return files.stream()
                .filter(file -> file != null && !file.isEmpty())
                .map(file -> {
                    String fileUrl = fileStorageService.store(file);
                    String fileName = file.getOriginalFilename() != null
                            ? file.getOriginalFilename()
                            : extractFilenameFromUrl(fileUrl);
                    return TaskDocument.builder()
                            .fileName(fileName)
                            .fileUrl(fileUrl)
                            .build();
                })
                .toList();
    }

    private void deleteSolutionDocuments(TaskSolution solution) {
        if (solution.getDocuments() == null || solution.getDocuments().isEmpty()) {
            return;
        }
        solution.getDocuments().forEach(document -> {
            String filename = extractFilenameFromUrl(document.getFileUrl());
            if (filename != null) {
                fileStorageService.delete(filename);
            }
        });
    }

    private TaskSolution getSolutionById(UUID solutionId) {
        return taskSolutionRepository.findById(solutionId)
                .orElseThrow(() -> new NotFoundException("TaskSolution с id " + solutionId + " не найден"));
    }

    private Task getTaskById(UUID taskId) {
        return taskRepository.findById(taskId)
                .orElseThrow(() -> new NotFoundException("Задание с id " + taskId + " не найдено"));
    }

    private User getUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь с id " + userId + " не найден"));
    }

    private void validateCanReadSolution(User requester, Long requesterId, TaskSolution solution, Task task) {
        if (isTeacherOrManager(requester)) {
            validateUserInTaskChannel(requesterId, task);
            return;
        }

        if (!solution.getStudentId().equals(requesterId)) {
            throw new ForbiddenException("У вас нет прав на просмотр этого решения");
        }

        validateUserInTaskChannel(requesterId, task);
    }

    private void validateCanDeleteSolution(User requester, Long requesterId, TaskSolution solution, Task task) {
        validateUserInTaskChannel(requesterId, task);

        if (solution.getStudentId().equals(requesterId)) {
            return;
        }

        if (!isTeacherOrManager(requester)) {
            throw new ForbiddenException("Можно удалить только свое решение");
        }
    }

    private boolean isTeacherOrManager(User user) {
        return user.getRole().contains(Role.TEACHER) || user.getRole().contains(Role.MANAGER);
    }

    private void validateUserInTaskChannel(Long userId, Task task) {
        boolean inChannel = task.getChannel().getUsers().stream()
                .anyMatch(channelUser -> channelUser.getId().equals(userId));

        if (!inChannel) {
            throw new ForbiddenException("У пользователя нет доступа к этому предмету");
        }
    }

    private String extractFilenameFromUrl(String fileUrl) {
        if (fileUrl == null || !fileUrl.contains("/file/")) {
            return null;
        }
        return fileUrl.substring(fileUrl.lastIndexOf("/") + 1);
    }

    private TaskSolutionDto toDto(TaskSolution solution) {
        return TaskSolutionDto.builder()
                .id(solution.getId())
                .taskId(solution.getTaskId())
                .studentId(solution.getStudentId())
                .documents(solution.getDocuments() == null ? new ArrayList<>() : solution.getDocuments().stream()
                        .map(document -> TaskDocumentDto.builder()
                                .fileName(document.getFileName())
                                .fileUrl(document.getFileUrl())
                                .build())
                        .toList())
                .mark(solution.getMark())
                .createdAt(solution.getCreatedAt())
                .updatedAt(solution.getUpdatedAt())
                .build();
    }
}
