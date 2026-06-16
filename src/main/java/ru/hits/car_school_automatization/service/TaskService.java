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
import ru.hits.car_school_automatization.mapper.TaskMapper;
import ru.hits.car_school_automatization.mapper.UserMapperKt;
import ru.hits.car_school_automatization.repository.*;
import ru.hits.car_school_automatization.util.DeadlinePenaltyUtils;
import ru.hits.car_school_automatization.util.RoleUtils;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class TaskService {

    private final TaskRepository taskRepository;
    private final ChannelRepository channelRepository;
    private final UserRepository userRepository;
    private final JwtTokenProvider tokenProvider;
    private final TaskMapper taskMapper;
    private final TeamRepository teamRepository;
    private final TeamFormationService teamFormationService;
    private final FileStorageService fileStorageService;

    public TaskDto createTask(CreateTaskDto dto, UUID channelId, String authHeader) {
        log.info("start create");
        Channel channel = channelRepository.findById(channelId)
                .orElseThrow(() -> new NotFoundException("Предмет с id " + channelId + " не найден"));

        validateTeacherLeadsChannel(authHeader, channel);
        validateQualifiedMin(dto.getType(), dto.getQualifiedMin());
        validateMinTeamSize(dto.getMinTeamSize());

        if (Boolean.FALSE.equals(dto.getIsMetricsVisibleToStudents()) && Boolean.TRUE.equals(dto.getIsMetricValuesVisibleToStudents())) {
            throw new BadRequestException("isMetricValuesVisibleToStudents может быть true только если isMetricsVisibleToStudents тоже true");
        }

        Task task = Task.builder()
                .label(dto.getLabel().trim())
                .text(dto.getText())
                .channel(channel)
                .documents(storeDocumentsFromFiles(dto.getDocuments()))
                .teamType(dto.getTeamType())
                .type(dto.getType())
                .isCanRedistribute(dto.getIsCanRedistribute())
                .qualifiedMin(dto.getQualifiedMin())
                .minTeamSize(dto.getMinTeamSize())
                .deadlinePenalty(DeadlinePenaltyUtils.build(dto.getDeadlinePenalty()))
                .votingDeadline(dto.getVotingDeadline())
                .isAnonymousVoting(dto.getIsAnonymousVoting())
                .isP2pEnabled(dto.getIsP2pEnabled())
                .isMetricsVisibleToStudents(dto.getIsMetricsVisibleToStudents() != null ? dto.getIsMetricsVisibleToStudents() : false)
                .isMetricValuesVisibleToStudents(dto.getIsMetricValuesVisibleToStudents() != null ? dto.getIsMetricValuesVisibleToStudents() : false)
                .build();

        if (!Boolean.TRUE.equals(task.getIsMetricsVisibleToStudents())) {
            task.setIsMetricValuesVisibleToStudents(false);
        }

        Task savedTask = taskRepository.save(task);

        if (Boolean.TRUE.equals(dto.getIsP2pEnabled())) {
            if (dto.getP2pParam() == null) {
                throw new BadRequestException("Для задания с P2P требуется p2pParam");
            }
            P2PParam param = P2PParam.builder()
                    .id(savedTask.getId())
                    .type(dto.getP2pParam().getType())
                    .visibility(dto.getP2pParam().getVisibility())
                    .p2pDeadline(dto.getP2pParam().getP2pDeadline())
                    .build();
            param.setTask(savedTask);
            savedTask.setP2pParam(param);
            savedTask = taskRepository.save(savedTask);
        }

        List<Team> teams = teamFormationService.formByTeamType(savedTask, List.copyOf(channel.getUsers()), dto);
        if (!teams.isEmpty()) {
            List<Team> savedTeams = teamRepository.saveAll(teams);
            savedTask.setTeams(new HashSet<>(savedTeams));
        }
        return taskMapper.toDto(savedTask);
    }

    public TaskDto updateTask(UUID taskId, UpdateTaskDto dto, String authHeader) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new NotFoundException("Задание с id " + taskId + " не найдено"));

        validateTeacherLeadsChannel(authHeader, task.getChannel());

        if (Boolean.FALSE.equals(dto.getIsMetricsVisibleToStudents()) && Boolean.TRUE.equals(dto.getIsMetricValuesVisibleToStudents())) {
            throw new BadRequestException("isMetricValuesVisibleToStudents может быть true только если isMetricsVisibleToStudents тоже true");
        }

        taskMapper.updateTaskFromDto(dto, task);
        if (dto.getDocuments() != null) {
            replaceTaskDocuments(task, dto.getDocuments());
        }

        if (dto.getIsP2pEnabled() != null) {
            task.setIsP2pEnabled(dto.getIsP2pEnabled());
            if (Boolean.TRUE.equals(dto.getIsP2pEnabled())) {
                if (dto.getP2pParam() == null) {
                    throw new BadRequestException("Для задания с P2P требуется p2pParam");
                }
                P2PParam param = task.getP2pParam();
                if (param == null) {
                    param = new P2PParam();
                    param.setId(task.getId());
                    param.setTask(task);
                    task.setP2pParam(param);
                }
                param.setType(dto.getP2pParam().getType());
                param.setVisibility(dto.getP2pParam().getVisibility());
                param.setP2pDeadline(dto.getP2pParam().getP2pDeadline());
            } else {
                if (task.getP2pParam() != null) {
                    task.getP2pParam().setTask(null);
                    task.setP2pParam(null);
                }
            }
        }


        if (!Boolean.TRUE.equals(task.getIsMetricsVisibleToStudents())) {
            task.setIsMetricValuesVisibleToStudents(false);
        }

        validateQualifiedMin(task.getType(), task.getQualifiedMin());
        validateMinTeamSize(task.getMinTeamSize());
        applyDeadlinePenaltyUpdate(task, dto.getDeadlinePenalty());

        if (task.getTeams() != null && !task.getTeams().isEmpty()) {
            boolean updateDeadlines = dto.getDeadline() != null || dto.getSoftDeadline() != null;
            if (updateDeadlines) {
                task.getTeams().forEach(team -> {
                    if (dto.getDeadline() != null) {
                        team.setDeadline(dto.getDeadline());
                    }
                    if (dto.getSoftDeadline() != null) {
                        team.setSoftDeadline(dto.getSoftDeadline());
                    }
                });
            }
        }

        Task savedTask = taskRepository.save(task);
        return taskMapper.toDto(savedTask);
    }

    @Transactional
    public TaskDto getTask(UUID taskId, String authHeader) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new NotFoundException("Задание с id " + taskId + " не найдено"));

        validateUserInChannel(authHeader, task.getChannel());
        return taskMapper.toDto(task);
    }

    @Transactional
    public List<UserShortDto> getStudentsWithoutTeam(UUID taskId, String authHeader) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new NotFoundException("Задание с id " + taskId + " не найдено"));

        validateUserInChannel(authHeader, task.getChannel());

        Set<Long> usersInTeams = teamRepository.findByTask_Id(taskId).stream()
                .flatMap(team -> team.getUsers().stream())
                .map(User::getId)
                .collect(Collectors.toSet());

        return task.getChannel().getUsers().stream()
                .filter(user -> user.getRole().contains(Role.STUDENT))
                .filter(user -> !usersInTeams.contains(user.getId()))
                .sorted(Comparator
                        .comparing(User::getFirstName, Comparator.nullsFirst(String.CASE_INSENSITIVE_ORDER))
                        .thenComparing(User::getLastName, Comparator.nullsFirst(String.CASE_INSENSITIVE_ORDER)))
                .map(UserMapperKt::toShort)
                .toList();
    }

    @Transactional
    public List<TaskDto> getTasksByChannel(UUID channelId, String authHeader) {
        Channel channel = channelRepository.findById(channelId)
                .orElseThrow(() -> new NotFoundException("Предмет с id " + channelId + " не найден"));

        validateUserInChannel(authHeader, channel);
        return taskMapper.toDtoList(taskRepository.findByChannel_Id(channelId));
    }

    public TaskDto copyTask(UUID taskId, UUID targetChannelId, String authHeader) {
        Task source = taskRepository.findById(taskId)
                .orElseThrow(() -> new NotFoundException("Задание с id " + taskId + " не найдено"));

        Channel targetChannel = channelRepository.findById(targetChannelId)
                .orElseThrow(() -> new NotFoundException("Предмет с id " + targetChannelId + " не найден"));

        validateTeacherLeadsChannel(authHeader, targetChannel);

        Task copy = Task.builder()
                .label(source.getLabel())
                .text(source.getText())
                .channel(targetChannel)
                .documents(cloneDocuments(source.getDocuments()))
                .teamType(source.getTeamType())
                .type(source.getType())
                .isCanRedistribute(source.getIsCanRedistribute())
                .qualifiedMin(source.getQualifiedMin())
                .minTeamSize(source.getMinTeamSize())
                .deadlinePenalty(DeadlinePenaltyUtils.clonePenalty(source.getDeadlinePenalty()))
                .votingDeadline(source.getVotingDeadline())
                .build();

        if (source.getIsMetricsVisibleToStudents() != null) {
            copy.setIsMetricsVisibleToStudents(source.getIsMetricsVisibleToStudents());
        }
        if (source.getIsMetricValuesVisibleToStudents() != null) {
            copy.setIsMetricValuesVisibleToStudents(source.getIsMetricValuesVisibleToStudents());
        }

        if (!Boolean.TRUE.equals(copy.getIsMetricsVisibleToStudents())) {
            copy.setIsMetricValuesVisibleToStudents(false);
        }

        return taskMapper.toDto(taskRepository.save(copy));
    }

    public void deleteTask(UUID taskId, String authHeader) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new NotFoundException("Задание с id " + taskId + " не найдено"));

        validateTeacherLeadsChannel(authHeader, task.getChannel());
        deleteTaskDocuments(task);
        taskRepository.delete(task);
    }

    public TaskDto addDocument(UUID taskId, MultipartFile file, String authHeader) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new NotFoundException("Задание с id " + taskId + " не найдено"));

        validateTeacherLeadsChannel(authHeader, task.getChannel());
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Файл не может быть пустым");
        }

        String fileUrl = fileStorageService.store(file);
        String fileName = file.getOriginalFilename() != null ? file.getOriginalFilename() : extractFilenameFromUrl(fileUrl);

        if (task.getDocuments() == null) {
            task.setDocuments(new ArrayList<>());
        }
        task.getDocuments().add(TaskDocument.builder().fileName(fileName).fileUrl(fileUrl).build());

        return taskMapper.toDto(taskRepository.save(task));
    }

    public TaskDto removeDocument(UUID taskId, String fileUrl, String authHeader) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new NotFoundException("Задание с id " + taskId + " не найдено"));

        validateTeacherLeadsChannel(authHeader, task.getChannel());
        if (fileUrl == null || fileUrl.isBlank()) {
            throw new BadRequestException("fileUrl обязателен");
        }

        boolean removed = task.getDocuments().removeIf(doc -> fileUrl.equals(doc.getFileUrl()));
        if (!removed) {
            throw new NotFoundException("Документ с таким fileUrl не найден");
        }

        String filename = extractFilenameFromUrl(fileUrl);
        if (filename != null) {
            fileStorageService.delete(filename);
        }

        return taskMapper.toDto(taskRepository.save(task));
    }

    private void validateQualifiedMin(TaskType type, Integer qualifiedMin) {
        if (type == TaskType.QUALIFIED && qualifiedMin == null) {
            throw new BadRequestException("Для QUALIFIED требуется qualifiedMin");
        }
        if (type != TaskType.QUALIFIED && qualifiedMin != null) {
            throw new BadRequestException("qualifiedMin задается только для QUALIFIED");
        }
    }

    private void validateMinTeamSize(Integer minTeamSize) {
        if (minTeamSize != null && minTeamSize < 1) {
            throw new BadRequestException("minTeamSize должен быть больше 0");
        }
    }

    private void applyDeadlinePenaltyUpdate(Task task, DeadlinePenaltyDto dto) {
        if (dto == null) {
            return;
        }
        task.setDeadlinePenalty(DeadlinePenaltyUtils.build(dto));
    }

    private void validateTeacherLeadsChannel(String authHeader, Channel channel) {
        Long requesterId = tokenProvider.extractUserIdFromHeader(authHeader);
        User requester = userRepository.findById(requesterId)
                .orElseThrow(() -> new NotFoundException("Пользователь с id " + requesterId + " не найден"));

        log.info(requester.getRole().toString());
        if (!RoleUtils.isTeacher(requester)) {
            throw new ForbiddenException("Только преподаватель может управлять заданиями");
        }

        boolean teachesChannel = channel.getUsers().stream()
                .anyMatch(user -> user.getId().equals(requesterId));

        if (!teachesChannel) {
            throw new ForbiddenException("Преподаватель не ведет этот предмет");
        }
    }

    private void validateUserInChannel(String authHeader, Channel channel) {
        Long userId = tokenProvider.extractUserIdFromHeader(authHeader);
        boolean inChannel = channel.getUsers().stream().anyMatch(user -> user.getId().equals(userId));
        if (!inChannel) {
            throw new ForbiddenException("У пользователя нет доступа к этому предмету");
        }
    }

    private List<TaskDocument> storeDocumentsFromFiles(List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            return new ArrayList<>();
        }
        return files.stream()
                .filter(file -> file != null && !file.isEmpty())
                .map(file -> {
                    String fileUrl = fileStorageService.store(file);
                    String fileName = file.getOriginalFilename() != null ? file.getOriginalFilename() : extractFilenameFromUrl(fileUrl);
                    return TaskDocument.builder()
                            .fileName(fileName)
                            .fileUrl(fileUrl)
                            .build();
                })
                .toList();
    }

    private List<TaskDocument> cloneDocuments(List<TaskDocument> documents) {
        if (documents == null || documents.isEmpty()) {
            return new ArrayList<>();
        }
        return documents.stream()
                .map(doc -> TaskDocument.builder().fileName(doc.getFileName()).fileUrl(doc.getFileUrl()).build())
                .toList();
    }

    private void deleteTaskDocuments(Task task) {
        if (task.getDocuments() == null || task.getDocuments().isEmpty()) {
            return;
        }
        task.getDocuments().forEach(doc -> {
            String filename = extractFilenameFromUrl(doc.getFileUrl());
            if (filename != null) {
                fileStorageService.delete(filename);
            }
        });
    }

    private void replaceTaskDocuments(Task task, List<MultipartFile> files) {
        deleteTaskDocuments(task);
        task.setDocuments(storeDocumentsFromFiles(files));
    }

    private String extractFilenameFromUrl(String fileUrl) {
        if (fileUrl == null || !fileUrl.contains("/file/")) {
            return null;
        }
        return fileUrl.substring(fileUrl.lastIndexOf("/") + 1);
    }
}
