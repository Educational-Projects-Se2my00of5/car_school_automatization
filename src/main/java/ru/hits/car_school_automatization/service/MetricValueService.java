package ru.hits.car_school_automatization.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.hits.car_school_automatization.dto.MetricValueDto;
import ru.hits.car_school_automatization.dto.MetricWithValuesDto;
import ru.hits.car_school_automatization.dto.SetMetricValueDto;
import ru.hits.car_school_automatization.dto.SetTeamMetricValueDto;
import ru.hits.car_school_automatization.entity.Metric;
import ru.hits.car_school_automatization.entity.MetricChange;
import ru.hits.car_school_automatization.entity.MetricValue;
import ru.hits.car_school_automatization.entity.Team;
import ru.hits.car_school_automatization.entity.User;
import ru.hits.car_school_automatization.exception.BadRequestException;
import ru.hits.car_school_automatization.exception.ForbiddenException;
import ru.hits.car_school_automatization.exception.NotFoundException;
import ru.hits.car_school_automatization.mapper.MetricMapper;
import ru.hits.car_school_automatization.mapper.MetricValueMapper;
import ru.hits.car_school_automatization.repository.MetricChangeRepository;
import ru.hits.car_school_automatization.repository.MetricRepository;
import ru.hits.car_school_automatization.repository.MetricValueRepository;
import ru.hits.car_school_automatization.repository.PostRepository;
import ru.hits.car_school_automatization.repository.TaskRepository;
import ru.hits.car_school_automatization.repository.TeamRepository;
import ru.hits.car_school_automatization.repository.UserRepository;
import ru.hits.car_school_automatization.util.RoleUtils;

import java.util.ArrayList;
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

    public void setMetricValue(SetMetricValueDto dto, String authHeader) {
        User teacher = getUserFromHeader(authHeader);
        RoleUtils.requireTeacher(teacher, "Только преподаватель может управлять критериями");
        Metric metric = metricRepository.findById(dto.getMetricId())
                .orElseThrow(() -> new NotFoundException("Критерий не найден"));
        userRepository.findById(dto.getUserId())
                .orElseThrow(() -> new NotFoundException("Пользователь не найден"));

        validateMetricValue(metric, dto.getValue());
        upsertMetricValue(metric.getId(), dto.getUserId(), dto.getValue(), teacher.getId());
    }

    public void setTeamMetricValue(SetTeamMetricValueDto dto, String authHeader) {
        User teacher = getUserFromHeader(authHeader);
        RoleUtils.requireTeacher(teacher, "Только преподаватель может управлять критериями");
        Metric metric = metricRepository.findById(dto.getMetricId())
                .orElseThrow(() -> new NotFoundException("Критерий не найден"));

        if (metric.getTaskId() == null) {
            throw new BadRequestException("Критерий не привязан к командному заданию");
        }

        Team team = teamRepository.findById(dto.getTeamId())
                .orElseThrow(() -> new NotFoundException("Команда не найдена"));

        if (!metric.getTaskId().equals(team.getTask().getId())) {
            throw new BadRequestException("Команда не принадлежит заданию критерия");
        }

        validateMetricValue(metric, dto.getValue());

        if (team.getUsers() == null || team.getUsers().isEmpty()) {
            return;
        }

        team.getUsers().forEach(user -> upsertMetricValue(metric.getId(), user.getId(), dto.getValue(), teacher.getId()));
    }

    private void upsertMetricValue(UUID metricId, Long userId, Double value, Long editorId) {
        MetricValue metricValue = metricValueRepository.findByMetricIdAndUserId(metricId, userId)
                .map(existing -> {
                    existing.setValue(value);
                    return existing;
                })
                .orElseGet(() -> MetricValue.builder()
                        .metricId(metricId)
                        .userId(userId)
                        .value(value)
                        .build());

        MetricValue saved = metricValueRepository.save(metricValue);

        MetricChange change = MetricChange.builder()
                .metricValueId(saved.getId())
                .editorId(editorId)
                .editValue(value)
                .build();

        metricChangeRepository.save(change);
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
        if (!RoleUtils.isTeacherOrManager(requester) && !requester.getId().equals(userId)) {
            throw new ForbiddenException("Недостаточно прав для просмотра значений");
        }
        return userId;
    }
}
