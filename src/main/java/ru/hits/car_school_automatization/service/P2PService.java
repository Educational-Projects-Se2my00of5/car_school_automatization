package ru.hits.car_school_automatization.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.hits.car_school_automatization.dto.*;
import ru.hits.car_school_automatization.entity.*;
import ru.hits.car_school_automatization.enums.P2PPairStatus;
import ru.hits.car_school_automatization.enums.P2PType;
import ru.hits.car_school_automatization.exception.ForbiddenException;
import ru.hits.car_school_automatization.exception.NotFoundException;
import ru.hits.car_school_automatization.mapper.P2PMapper;
import ru.hits.car_school_automatization.repository.*;
import ru.hits.car_school_automatization.util.RoleUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class P2PService {

    private final P2PParamRepository p2pParamRepository;
    private final P2PPairPersonalRepository p2pPairPersonalRepository;
    private final P2PPairTeamRepository p2pPairTeamRepository;
    private final PostRepository postRepository;
    private final TaskRepository taskRepository;
    private final TeamRepository teamRepository;
    private final SolutionRepository solutionRepository;
    private final TaskSolutionRepository taskSolutionRepository;
    private final MetricRepository metricRepository;
    private final MetricValueService metricValueService;
    private final P2PMapper p2PMapper;
    private final JwtTokenProvider tokenProvider;
    private final UserRepository userRepository;

    public void processExpiredP2PPairs() {
        Instant now = Instant.now();

        List<P2PPairPersonal> expiredPersonal = p2pPairPersonalRepository.findExpiredPendingPairs(now);
        for (P2PPairPersonal pair : expiredPersonal) {
            pair.setStatus(P2PPairStatus.EXPIRED);
            p2pPairPersonalRepository.save(pair);

            List<Metric> metrics = metricRepository.findByPostId(pair.getPostId());
            for (Metric metric : metrics) {
                metricValueService.applyPenaltyForMissedP2P(metric.getId(), pair.getReviewerId());
            }
            log.info("P2P Personal {} expired, penalty applied to reviewer {}", pair.getId(), pair.getReviewerId());
        }

        List<P2PPairTeam> expiredTeam = p2pPairTeamRepository.findExpiredPendingPairs(now);
        for (P2PPairTeam pair : expiredTeam) {
            pair.setStatus(P2PPairStatus.EXPIRED);
            p2pPairTeamRepository.save(pair);

            List<Metric> metrics = metricRepository.findByTaskId(pair.getTaskId());
            Team reviewerTeam = teamRepository.findById(pair.getReviewerTeamId()).orElse(null);
            if (reviewerTeam != null && reviewerTeam.getUsers() != null) {
                for (User user : reviewerTeam.getUsers()) {
                    for (Metric metric : metrics) {
                        metricValueService.applyPenaltyForMissedP2P(metric.getId(), user.getId());
                    }
                }
            }
            log.info("P2P Team {} expired, penalty applied to reviewer team {}", pair.getId(), pair.getReviewerTeamId());
        }
    }

    @Transactional(readOnly = true)
    public ReviewTasksDto getReviewTasks() {
        User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Long userId = currentUser.getId();

        List<PersonalReviewTaskDto> personalTasks = p2pPairPersonalRepository.findByReviewerId(userId)
                .stream()
                .map(pair -> {
                    PersonalReviewTaskDto dto = p2PMapper.toPersonalReviewTaskDto(pair);
                    P2PParam param = p2pParamRepository.findById(pair.getPostId()).orElse(null);
                    if (param != null && param.getVisibility() != ru.hits.car_school_automatization.enums.P2PVisibility.ALL) {
                        dto.setOwnerId(null);
                    }
                    return dto;
                })
                .collect(Collectors.toList());

        List<UUID> teamIds = teamRepository.findByUsers_Id(userId)
                .stream()
                .map(Team::getId)
                .collect(Collectors.toList());

        List<TeamReviewTaskDto> teamTasks = teamIds.isEmpty() ? List.of() :
                p2pPairTeamRepository.findByReviewerTeamIdIn(teamIds)
                .stream()
                .map(pair -> {
                    TeamReviewTaskDto dto = p2PMapper.toTeamReviewTaskDto(pair);
                    P2PParam param = p2pParamRepository.findById(pair.getTaskId()).orElse(null);
                    if (param != null && param.getVisibility() != ru.hits.car_school_automatization.enums.P2PVisibility.ALL) {
                        dto.setOwnerTeamId(null);
                    }
                    return dto;
                })
                .collect(Collectors.toList());

        return new ReviewTasksDto(personalTasks, teamTasks);
    }

    @Transactional(readOnly = true)
    public List<P2PPairPersonalDto> getP2PPairPersonal(UUID postId, String authHeader) {
        validateTeacherOrManager(authHeader);
        return p2pPairPersonalRepository.findByPostId(postId).stream()
                .map(p2PMapper::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<P2PPairTeamDto> getP2PPairTeam(UUID taskId, String authHeader) {
        validateTeacherOrManager(authHeader);
        return p2pPairTeamRepository.findByTaskId(taskId).stream()
                .map(p2PMapper::toDto)
                .toList();
    }

    public P2PPairPersonalDto assignP2PPersonal(AssignP2PPersonalDto dto, String authHeader) {
        Long requesterId = tokenProvider.extractUserIdFromHeader(authHeader);
        User requester = userRepository.findById(requesterId)
                .orElseThrow(() -> new NotFoundException("Пользователь не найден"));

        Post post = postRepository.findById(dto.getPostId())
                .orElseThrow(() -> new NotFoundException("Пост не найден"));

        P2PParam param = p2pParamRepository.findById(post.getId()).orElse(null);
        if (!RoleUtils.isTeacherOrManager(requester)) {
            if (param == null || param.getType() != P2PType.HIMSELF) {
                throw new ForbiddenException("Самостоятельное распределение недоступно");
            }
            if (!dto.getReviewerId().equals(requesterId)) {
                throw new ForbiddenException("Вы можете назначить проверяющим только себя");
            }
        }

        P2PPairPersonal pair = P2PPairPersonal.builder()
                .postId(dto.getPostId())
                .reviewerId(dto.getReviewerId())
                .ownerId(dto.getOwnerId())
                .targetSolutionId(dto.getTargetSolutionId())
                .status(P2PPairStatus.PENDING)
                .build();
        return p2PMapper.toDto(p2pPairPersonalRepository.save(pair));
    }

    public P2PPairTeamDto assignP2PTeam(AssignP2PTeamDto dto, String authHeader) {
        Long requesterId = tokenProvider.extractUserIdFromHeader(authHeader);
        User requester = userRepository.findById(requesterId)
                .orElseThrow(() -> new NotFoundException("Пользователь не найден"));

        Task task = taskRepository.findById(dto.getTaskId())
                .orElseThrow(() -> new NotFoundException("Задание не найдено"));

        P2PParam param = p2pParamRepository.findById(task.getId()).orElse(null);
        if (!RoleUtils.isTeacherOrManager(requester)) {
            if (param == null || param.getType() != P2PType.HIMSELF) {
                throw new ForbiddenException("Самостоятельное распределение недоступно");
            }
            // For teams, reviewer should be a team where requester is a member
            Team reviewerTeam = teamRepository.findById(dto.getReviewerTeamId())
                    .orElseThrow(() -> new NotFoundException("Команда проверяющих не найдена"));

            boolean isMember = reviewerTeam.getUsers().stream().anyMatch(u -> u.getId().equals(requesterId));
            if (!isMember) {
                throw new ForbiddenException("Вы можете назначить проверяющим только свою команду");
            }
        }

        P2PPairTeam pair = P2PPairTeam.builder()
                .taskId(dto.getTaskId())
                .reviewerTeamId(dto.getReviewerTeamId())
                .ownerTeamId(dto.getOwnerTeamId())
                .targetTaskSolutionId(dto.getTargetTaskSolutionId())
                .status(P2PPairStatus.PENDING)
                .build();
        return p2PMapper.toDto(p2pPairTeamRepository.save(pair));
    }

    public P2PPairPersonalDto reassignP2PPersonal(UUID pairId, ReassignP2PPersonalDto dto, String authHeader) {
        validateTeacherOrManager(authHeader);
        P2PPairPersonal pair = p2pPairPersonalRepository.findById(pairId)
                .orElseThrow(() -> new NotFoundException("Пара не найдена"));
        pair.setReviewerId(dto.getNewReviewerId());
        return p2PMapper.toDto(p2pPairPersonalRepository.save(pair));
    }

    public P2PPairTeamDto reassignP2PTeam(UUID pairId, ReassignP2PTeamDto dto, String authHeader) {
        validateTeacherOrManager(authHeader);
        P2PPairTeam pair = p2pPairTeamRepository.findById(pairId)
                .orElseThrow(() -> new NotFoundException("Пара не найдена"));
        pair.setReviewerTeamId(dto.getNewReviewerTeamId());
        return p2PMapper.toDto(p2pPairTeamRepository.save(pair));
    }

    public void removeP2PPersonal(UUID pairId, String authHeader) {
        validateTeacherOrManager(authHeader);
        p2pPairPersonalRepository.deleteById(pairId);
    }

    public void removeP2PTeam(UUID pairId, String authHeader) {
        validateTeacherOrManager(authHeader);
        p2pPairTeamRepository.deleteById(pairId);
    }

    private void validateTeacherOrManager(String authHeader) {
        Long requesterId = tokenProvider.extractUserIdFromHeader(authHeader);
        User requester = userRepository.findById(requesterId)
                .orElseThrow(() -> new NotFoundException("Пользователь не найден"));
        if (!RoleUtils.isTeacherOrManager(requester)) {
            throw new ForbiddenException("Нет прав для выполнения операции");
        }
    }

    public void generateP2PForPost(UUID postId) {
        P2PParam param = p2pParamRepository.findById(postId).orElse(null);
        if (param == null || param.getType() != P2PType.RANDOM) {
            return;
        }

        List<Solution> solutions = new ArrayList<>(solutionRepository.findByTaskId(postId));
        if (solutions.size() < 2) {
            log.warn("Недостаточно решений для P2P по посту: {}", postId);
            return;
        }

        Collections.shuffle(solutions);
        List<P2PPairPersonal> pairs = new ArrayList<>();

        for (int i = 0; i < solutions.size(); i++) {
            Solution reviewer = solutions.get(i);
            Solution target = solutions.get((i + 1) % solutions.size());

            P2PPairPersonal pair = P2PPairPersonal.builder()
                    .postId(postId)
                    .reviewerId(reviewer.getStudentId())
                    .ownerId(target.getStudentId())
                    .targetSolutionId(target.getId())
                    .status(P2PPairStatus.PENDING)
                    .build();
            pairs.add(pair);
        }

        p2pPairPersonalRepository.saveAll(pairs);
        log.info("Сгенерировано {} P2P-пар для поста {}", pairs.size(), postId);
    }

    public void generateP2PForTask(UUID taskId) {
        P2PParam param = p2pParamRepository.findById(taskId).orElse(null);
        if (param == null || param.getType() != P2PType.RANDOM) {
            return;
        }

        if (!p2pPairTeamRepository.findByTaskId(taskId).isEmpty()) {
            return; // Уже сгенерировано
        }

        List<Team> teams = teamRepository.findByTask_Id(taskId);
        List<TaskSolution> selectedSolutions = new ArrayList<>();

        for (Team team : teams) {
            taskSolutionRepository.findByTaskIdAndTeamIdAndIsSelectedTrue(taskId, team.getId())
                    .ifPresent(selectedSolutions::add);
        }

        if (selectedSolutions.size() < 2) {
            log.warn("Недостаточно командных решений для P2P по таске: {}", taskId);
            return;
        }

        Collections.shuffle(selectedSolutions);
        List<P2PPairTeam> pairs = new ArrayList<>();

        for (int i = 0; i < selectedSolutions.size(); i++) {
            TaskSolution reviewerSolution = selectedSolutions.get(i);
            TaskSolution targetSolution = selectedSolutions.get((i + 1) % selectedSolutions.size());

            P2PPairTeam pair = P2PPairTeam.builder()
                    .taskId(taskId)
                    .reviewerTeamId(reviewerSolution.getTeamId())
                    .ownerTeamId(targetSolution.getTeamId())
                    .targetTaskSolutionId(targetSolution.getId())
                    .status(P2PPairStatus.PENDING)
                    .build();
            pairs.add(pair);
        }

        p2pPairTeamRepository.saveAll(pairs);
        log.info("Сгенерировано {} P2P-пар команд для таски {}", pairs.size(), taskId);
    }
}
