package ru.hits.car_school_automatization.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.hits.car_school_automatization.dto.MetricValueDto;
import ru.hits.car_school_automatization.dto.MetricValueHistoryDto;
import ru.hits.car_school_automatization.dto.MetricWithValuesDto;
import ru.hits.car_school_automatization.dto.SetMetricValueDto;
import ru.hits.car_school_automatization.dto.SetTeamMetricValueDto;
import ru.hits.car_school_automatization.entity.*;
import ru.hits.car_school_automatization.exception.BadRequestException;
import ru.hits.car_school_automatization.exception.ForbiddenException;
import ru.hits.car_school_automatization.exception.NotFoundException;
import ru.hits.car_school_automatization.mapper.MetricMapper;
import ru.hits.car_school_automatization.mapper.MetricValueMapper;
import ru.hits.car_school_automatization.repository.*;
import ru.hits.car_school_automatization.util.RoleUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class MetricValueService {

    private final MetricRepository metricRepository;
    private final MetricValueRepository metricValueRepository;
    private final MetricChangeRepository metricChangeRepository;
    private final PostRepository postRepository;
    private final TaskRepository taskRepository;
    private final TeamRepository teamRepository;
    private final UserRepository userRepository;
    private final MetricMapper metricMapper;
    private final MetricValueMapper metricValueMapper;
    private final JwtTokenProvider tokenProvider;
    private final P2PParamRepository p2pParamRepository;
    private final P2PPairPersonalRepository p2pPairPersonalRepository;
    private final P2PPairTeamRepository p2pPairTeamRepository;
    private final TeamP2PReviewGradeRepository teamP2PReviewGradeRepository;

    public List<MetricWithValuesDto> getPostMetricsWithValues(UUID postId, Long userId, String authHeader) {
        User requester = getUserFromHeader(authHeader);
        Long targetUserId = resolveTargetUserId(requester, userId);

        var post = postRepository.findById(postId)
                .orElseThrow(() -> new NotFoundException("Пост не найден"));

        boolean metricsVisibleToStudents = Boolean.TRUE.equals(post.getIsMetricsVisibleToStudents());
        boolean valuesVisibleToStudents = Boolean.TRUE.equals(post.getIsMetricValuesVisibleToStudents());

        List<Metric> metrics = metricRepository.findByPostId(postId);
        return toMetricsWithValues(metrics, requester, targetUserId, metricsVisibleToStudents, valuesVisibleToStudents);
    }

    public List<MetricWithValuesDto> getTaskMetricsWithValues(UUID taskId, Long userId, String authHeader) {
        User requester = getUserFromHeader(authHeader);
        Long targetUserId = resolveTargetUserId(requester, userId);

        var task = taskRepository.findById(taskId)
                .orElseThrow(() -> new NotFoundException("Задание не найдено"));

        boolean metricsVisibleToStudents = Boolean.TRUE.equals(task.getIsMetricsVisibleToStudents());
        boolean valuesVisibleToStudents = Boolean.TRUE.equals(task.getIsMetricValuesVisibleToStudents());

        List<Metric> metrics = metricRepository.findByTaskId(taskId);
        return toMetricsWithValues(metrics, requester, targetUserId, metricsVisibleToStudents, valuesVisibleToStudents);
    }

    public List<MetricValueHistoryDto> getMetricValueHistory(UUID metricId, Long userId, String authHeader) {
        User requester = getUserFromHeader(authHeader);
        Long targetUserId = resolveTargetUserId(requester, userId);

        Metric metric = metricRepository.findById(metricId)
                .orElseThrow(() -> new NotFoundException("Критерий не найден"));

        boolean metricsVisibleToStudents = true;
        boolean valuesVisibleToStudents = true;
        if (metric.getPostId() != null) {
            var post = postRepository.findById(metric.getPostId())
                    .orElseThrow(() -> new NotFoundException("Пост не найден"));
            metricsVisibleToStudents = Boolean.TRUE.equals(post.getIsMetricsVisibleToStudents());
            valuesVisibleToStudents = Boolean.TRUE.equals(post.getIsMetricValuesVisibleToStudents());
        } else if (metric.getTaskId() != null) {
            var task = taskRepository.findById(metric.getTaskId())
                    .orElseThrow(() -> new NotFoundException("Задание не найдено"));
            metricsVisibleToStudents = Boolean.TRUE.equals(task.getIsMetricsVisibleToStudents());
            valuesVisibleToStudents = Boolean.TRUE.equals(task.getIsMetricValuesVisibleToStudents());
        }

        boolean privileged = RoleUtils.isTeacherOrManager(requester);
        if (!privileged && !(metricsVisibleToStudents && valuesVisibleToStudents)) {
            return List.of();
        }

        MetricValue metricValue = metricValueRepository.findByMetricIdAndUserId(metricId, targetUserId)
                .orElse(null);
        if (metricValue == null) {
            return List.of();
        }

        List<MetricChange> changes = metricChangeRepository
                .findByMetricValueIdOrderByEditedAtDesc(metricValue.getId());
        if (changes.isEmpty()) {
            return List.of();
        }

        Map<Long, User> editors = new HashMap<>();
        List<Long> editorIds = changes.stream()
                .map(MetricChange::getEditorId)
                .distinct()
                .toList();
        userRepository.findAllById(editorIds)
                .forEach(user -> editors.put(user.getId(), user));

        return changes.stream()
                .map(change -> {
                    User editor = editors.get(change.getEditorId());
                    String editorName = editor != null
                            ? editor.getFirstName() + " " + editor.getLastName()
                            : null;
                    return MetricValueHistoryDto.builder()
                            .editorId(change.getEditorId())
                            .editorName(editorName)
                            .value(change.getEditValue())
                            .editedAt(change.getEditedAt())
                            .build();
                })
                .toList();
    }

    public List<MetricWithValuesDto> getTaskTeamMetricsWithValues(UUID taskId, UUID teamId, String authHeader) {
        User requester = getUserFromHeader(authHeader);

        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new NotFoundException("Команда не найдена"));

        if (team.getTask() == null || team.getTask().getId() == null) {
            throw new BadRequestException("Команда не привязана к заданию");
        }
        if (!team.getTask().getId().equals(taskId)) {
            throw new BadRequestException("Команда не принадлежит заданию");
        }

        Long targetUserId;
        if (RoleUtils.isTeacherOrManager(requester)) {
            if (team.getUsers() == null || team.getUsers().isEmpty()) {
                List<Metric> metrics = metricRepository.findByTaskId(taskId);
                return metrics.stream()
                        .map(metric -> MetricWithValuesDto.builder()
                                .metric(metricMapper.toDto(metric))
                                .values(List.of())
                                .build())
                        .toList();
            }
            targetUserId = team.getUsers().iterator().next().getId();
        } else {
            boolean isMember = team.getUsers() != null && team.getUsers().stream()
                    .anyMatch(u -> requester.getId().equals(u.getId()));
            if (!isMember) {
                throw new ForbiddenException("Недостаточно прав для просмотра оценок команды");
            }
            targetUserId = requester.getId();
        }

        var task = taskRepository.findById(taskId)
                .orElseThrow(() -> new NotFoundException("Задание не найдено"));

        boolean metricsVisibleToStudents = Boolean.TRUE.equals(task.getIsMetricsVisibleToStudents());
        boolean valuesVisibleToStudents = Boolean.TRUE.equals(task.getIsMetricValuesVisibleToStudents());

        List<Metric> metrics = metricRepository.findByTaskId(taskId);
        return toMetricsWithValues(metrics, requester, targetUserId, metricsVisibleToStudents, valuesVisibleToStudents);
    }

    public void setMetricValue(SetMetricValueDto dto, String authHeader) {
        User requester = getUserFromHeader(authHeader);
        Metric metric = metricRepository.findById(dto.getMetricId())
                .orElseThrow(() -> new NotFoundException("Критерий не найден"));
        userRepository.findById(dto.getUserId())
                .orElseThrow(() -> new NotFoundException("Пользователь не найден"));

        if (!RoleUtils.isTeacherOrManager(requester)) {
            // Check if it's a P2P reviewer
            UUID targetPostId = metric.getPostId();
            if (targetPostId == null) {
                throw new ForbiddenException("Только преподаватель может управлять критериями");
            }

            P2PParam p2pParam =
                    p2pParamRepository.findById(targetPostId).orElse(null);

            if (p2pParam == null) {
                throw new ForbiddenException("P2P не включено для этого задания");
            }

            if (p2pParam.getP2pDeadline() != null && java.time.Instant.now().isAfter(p2pParam.getP2pDeadline())) {
                throw new BadRequestException("Дедлайн P2P проверки истек");
            }

            boolean isReviewer = p2pPairPersonalRepository.findByPostId(targetPostId).stream()
                    .anyMatch(pair -> pair.getReviewerId().equals(requester.getId()) && pair.getOwnerId().equals(dto.getUserId()));

            if (!isReviewer) {
                throw new ForbiddenException("Вы не назначены проверяющим для этого пользователя");
            }
        }

        validateMetricValue(metric, dto.getValue());
        upsertMetricValue(metric.getId(), dto.getUserId(), dto.getValue(), requester.getId());
    }

    public void setTeamMetricValue(SetTeamMetricValueDto dto, String authHeader) {
        User requester = getUserFromHeader(authHeader);
        Metric metric = metricRepository.findById(dto.getMetricId())
                .orElseThrow(() -> new NotFoundException("Критерий не найден"));

        if (metric.getTaskId() == null) {
            throw new BadRequestException("Критерий не привязан к командному заданию");
        }

        Team targetTeam = teamRepository.findById(dto.getTeamId())
                .orElseThrow(() -> new NotFoundException("Команда не найдена"));

        if (!metric.getTaskId().equals(targetTeam.getTask().getId())) {
            throw new BadRequestException("Команда не принадлежит заданию критерия");
        }

        if (!RoleUtils.isTeacherOrManager(requester)) {
            P2PParam p2pParam =
                    p2pParamRepository.findById(metric.getTaskId()).orElse(null);

            if (p2pParam == null) {
                throw new ForbiddenException("P2P не включено для этого задания");
            }

            if (p2pParam.getP2pDeadline() != null && java.time.Instant.now().isAfter(p2pParam.getP2pDeadline())) {
                throw new BadRequestException("Дедлайн P2P проверки истек");
            }

            List<Team> userTeams = teamRepository.findByUsers_Id(requester.getId()).stream()
                    .filter(t -> t.getTask() != null && t.getTask().getId().equals(metric.getTaskId()))
                    .toList();

            if (userTeams.isEmpty()) {
                throw new ForbiddenException("Вы не состоите в команде для этого задания");
            }

            Team userTeam = userTeams.get(0);
            UUID userTeamId = userTeam.getId();

            P2PPairTeam p2pPair = p2pPairTeamRepository.findByTaskId(metric.getTaskId()).stream()
                    .filter(pair -> pair.getReviewerTeamId().equals(userTeamId) && pair.getOwnerTeamId().equals(dto.getTeamId()))
                    .findFirst()
                    .orElse(null);

            if (p2pPair == null) {
                throw new ForbiddenException("Ваша команда не назначена проверяющей для этой команды");
            }

            validateMetricValue(metric, dto.getValue());

            if (targetTeam.getUsers() == null || targetTeam.getUsers().isEmpty()) {
                return;
            }

            if (userTeam.getCaptainId() != null) {
                if (!userTeam.getCaptainId().equals(requester.getId())) {
                    throw new ForbiddenException("В команде есть капитан. Только капитан может выставлять оценку проверяемой команде.");
                }
                targetTeam.getUsers().forEach(user -> upsertMetricValue(metric.getId(), user.getId(), dto.getValue(), requester.getId()));
            } else {
                // Team without captain: Average the latest grades of all reviewing team members
                TeamP2PReviewGrade reviewGrade = teamP2PReviewGradeRepository
                        .findByP2pPairTeamIdAndReviewerIdAndMetricId(p2pPair.getId(), requester.getId(), metric.getId())
                        .map(existing -> {
                            existing.setValue(dto.getValue());
                            return existing;
                        })
                        .orElseGet(() -> TeamP2PReviewGrade.builder()
                                .p2pPairTeamId(p2pPair.getId())
                                .reviewerId(requester.getId())
                                .metricId(metric.getId())
                                .value(dto.getValue())
                                .build());
                teamP2PReviewGradeRepository.save(reviewGrade);

                List<TeamP2PReviewGrade> allGrades = teamP2PReviewGradeRepository
                        .findByP2pPairTeamIdAndMetricId(p2pPair.getId(), metric.getId());

                double sum = allGrades.stream().mapToDouble(TeamP2PReviewGrade::getValue).sum();
                double average = sum / allGrades.size();

                targetTeam.getUsers().forEach(user -> upsertMetricValue(metric.getId(), user.getId(), average, requester.getId()));
            }
        } else {
            validateMetricValue(metric, dto.getValue());
            if (targetTeam.getUsers() == null || targetTeam.getUsers().isEmpty()) {
                return;
            }
            targetTeam.getUsers().forEach(user -> upsertMetricValue(metric.getId(), user.getId(), dto.getValue(), requester.getId()));
        }
    }

    public void applyPenaltyForMissedP2P(UUID metricId, Long userId) {
        Metric metric = metricRepository.findById(metricId)
                .orElseThrow(() -> new NotFoundException("Критерий не найден"));

        Double penaltyValue = metric.getMinValue() != null ? metric.getMinValue() : 0.0;
        upsertMetricValue(metricId, userId, penaltyValue, userId);
    }

    private void upsertMetricValue(UUID metricId, Long userId, Double value, Long editorId) {
        User editor = userRepository.findById(editorId).orElse(null);
        boolean isEditorTeacher = editor != null && RoleUtils.isTeacherOrManager(editor);

        MetricValue metricValue = metricValueRepository.findByMetricIdAndUserId(metricId, userId).orElse(null);

        if (metricValue != null && !isEditorTeacher) {
            List<MetricChange> changes = metricChangeRepository.findByMetricValueIdOrderByEditedAtDesc(metricValue.getId());
            if (!changes.isEmpty()) {
                MetricChange latestChange = changes.get(0);
                User latestEditor = userRepository.findById(latestChange.getEditorId()).orElse(null);
                if (latestEditor != null && RoleUtils.isTeacherOrManager(latestEditor)) {
                    throw new ForbiddenException("Оценка уже переопределена преподавателем и не может быть изменена");
                }
            }
        }

        if (metricValue == null) {
            metricValue = MetricValue.builder()
                    .metricId(metricId)
                    .userId(userId)
                    .value(value)
                    .build();
        } else {
            metricValue.setValue(value);
        }

        MetricValue saved = metricValueRepository.save(metricValue);

        MetricChange change = MetricChange.builder()
                .metricValueId(saved.getId())
                .editorId(editorId)
                .editValue(value)
                .build();

        metricChangeRepository.save(change);
    }

    public void removeOverride(UUID metricId, Long studentId, String authHeader) {
        User requester = getUserFromHeader(authHeader);
        if (!RoleUtils.isTeacherOrManager(requester)) {
            throw new ForbiddenException("Только преподаватель может отменить оверрайд");
        }

        MetricValue metricValue = metricValueRepository.findByMetricIdAndUserId(metricId, studentId)
                .orElseThrow(() -> new NotFoundException("Оценка не найдена"));

        List<MetricChange> changes = metricChangeRepository.findByMetricValueIdOrderByEditedAtDesc(metricValue.getId());
        Double previousValue = null;

        for (MetricChange change : changes) {
            User editor = userRepository.findById(change.getEditorId()).orElse(null);
            if (editor == null || !RoleUtils.isTeacherOrManager(editor)) {
                previousValue = change.getEditValue();
                break;
            }
        }

        if (previousValue != null) {
            metricValue.setValue(previousValue);
            MetricValue saved = metricValueRepository.save(metricValue);
            
            MetricChange change = MetricChange.builder()
                .metricValueId(saved.getId())
                .editorId(requester.getId())
                .editValue(previousValue)
                .build();
            metricChangeRepository.save(change);
        } else {
            metricChangeRepository.deleteAll(changes);
            metricValueRepository.delete(metricValue);
        }
    }

    private List<MetricWithValuesDto> toMetricsWithValues(
            List<Metric> metrics,
            User requester,
            Long userId,
            boolean metricsVisibleToStudents,
            boolean valuesVisibleToStudents) {

        boolean privileged = RoleUtils.isTeacherOrManager(requester);
        if (!privileged && !metricsVisibleToStudents) {
            return List.of();
        }

        if (metrics == null || metrics.isEmpty()) {
            return List.of();
        }

        boolean canSeeValues = privileged || (metricsVisibleToStudents && valuesVisibleToStudents);
        List<UUID> metricIds = metrics.stream().map(Metric::getId).toList();
        Map<UUID, List<MetricValue>> valuesByMetric = (userId == null)
                ? metricValueRepository.findByMetricIdIn(metricIds).stream()
                  .collect(Collectors.groupingBy(MetricValue::getMetricId))
                : metricValueRepository.findByMetricIdInAndUserId(metricIds, userId).stream()
                  .collect(Collectors.groupingBy(MetricValue::getMetricId));

        List<MetricWithValuesDto> result = new ArrayList<>();

        for (Metric metric : metrics) {
            List<MetricValueDto> values;
            if (!canSeeValues) {
                values = List.of();
            } else if (userId != null) {
                MetricValue metricValue = valuesByMetric.getOrDefault(metric.getId(), List.of()).stream()
                        .findFirst()
                        .orElse(null);
                Double value = metricValue != null ? metricValue.getValue() : null;
                values = List.of(metricValueMapper.toDto(userId, value));
            } else {
                values = metricValueMapper.toDtoList(valuesByMetric.getOrDefault(metric.getId(), List.of()));
            }

            result.add(MetricWithValuesDto.builder()
                    .metric(metricMapper.toDto(metric))
                    .values(values)
                    .build());
        }

        return result;
    }

    private void validateMetricValue(Metric metric, Double value) {
        if (value == null) {
            throw new BadRequestException("Значение не может быть null");
        }
        if (value < metric.getMinValue() || value > metric.getMaxValue()) {
            throw new BadRequestException("Значение вне допустимого диапазона");
        }
    }

    private User getUserFromHeader(String authHeader) {
        Long userId = tokenProvider.extractUserIdFromHeader(authHeader);
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь не найден"));
    }

    private Long resolveTargetUserId(User requester, Long userId) {
        if (userId == null) {
            return RoleUtils.isTeacherOrManager(requester) ? null : requester.getId();
        }
//        if (!RoleUtils.isTeacherOrManager(requester) && !requester.getId().equals(userId)) {
//            throw new ForbiddenException("Недостаточно прав для просмотра значений");
//        }
        return userId;
    }
}
