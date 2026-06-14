package ru.hits.car_school_automatization.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.hits.car_school_automatization.dto.CreateMetricDto;
import ru.hits.car_school_automatization.dto.MetricDto;
import ru.hits.car_school_automatization.entity.Metric;
import ru.hits.car_school_automatization.entity.Post;
import ru.hits.car_school_automatization.entity.Task;
import ru.hits.car_school_automatization.entity.User;
import ru.hits.car_school_automatization.enums.MetricType;
import ru.hits.car_school_automatization.enums.PostType;
import ru.hits.car_school_automatization.exception.BadRequestException;
import ru.hits.car_school_automatization.exception.NotFoundException;
import ru.hits.car_school_automatization.mapper.MetricMapper;
import ru.hits.car_school_automatization.repository.MetricRepository;
import ru.hits.car_school_automatization.repository.PostRepository;
import ru.hits.car_school_automatization.repository.TaskRepository;
import ru.hits.car_school_automatization.repository.UserRepository;
import ru.hits.car_school_automatization.util.RoleUtils;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class MetricService {

    private final MetricRepository metricRepository;
    private final PostRepository postRepository;
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final MetricMapper metricMapper;
    private final JwtTokenProvider tokenProvider;

    public MetricDto createMetric(CreateMetricDto dto, String authHeader) {
        RoleUtils.requireTeacher(getUserFromHeader(authHeader), "Только преподаватель может управлять критериями");
        validateTarget(dto.getPostId(), dto.getTaskId());
        validateRange(dto.getMinValue(), dto.getMaxValue());

        if (dto.getPostId() != null) {
            Post post = postRepository.findById(dto.getPostId())
                    .orElseThrow(() -> new NotFoundException("Пост не найден"));
            if (post.getType() != PostType.TASK && post.getType() != PostType.CONTROL) {
                throw new BadRequestException("Критерии доступны только для TASK или CONTROL");
            }
            if (post.getType() == PostType.CONTROL) {
                if (dto.getType() != MetricType.COEFFICIENT) {
                    throw new BadRequestException("Для контрольной доступен только тип COEFFICIENT");
                }
                if (metricRepository.existsByPostId(post.getId())) {
                    throw new BadRequestException("Для контрольной уже существует критерий");
                }
            }
        }

        if (dto.getTaskId() != null) {
            taskRepository.findById(dto.getTaskId())
                    .orElseThrow(() -> new NotFoundException("Задание не найдено"));
        }

        Metric metric = Metric.builder()
                .name(dto.getName().trim())
                .comment(dto.getComment())
                .minValue(dto.getMinValue())
                .maxValue(dto.getMaxValue())
                .type(dto.getType())
                .postId(dto.getPostId())
                .taskId(dto.getTaskId())
                .build();

        return metricMapper.toDto(metricRepository.save(metric));
    }

    public void deleteMetric(UUID metricId, String authHeader) {
        RoleUtils.requireTeacher(getUserFromHeader(authHeader), "Только преподаватель может управлять критериями");
        Metric metric = metricRepository.findById(metricId)
                .orElseThrow(() -> new NotFoundException("Критерий не найден"));
        metricRepository.delete(metric);
    }

    public List<MetricDto> getPostMetrics(UUID postId, String authHeader) {
        User requester = getUserFromHeader(authHeader);
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new NotFoundException("Пост не найден"));

        if (!RoleUtils.isTeacherOrManager(requester) && !Boolean.TRUE.equals(post.getIsMetricsVisibleToStudents())) {
            return List.of();
        }

        return metricRepository.findByPostId(postId).stream()
                .map(metricMapper::toDto)
                .toList();
    }

    public List<MetricDto> getTaskMetrics(UUID taskId, String authHeader) {
        User requester = getUserFromHeader(authHeader);
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new NotFoundException("Задание не найдено"));

        if (!RoleUtils.isTeacherOrManager(requester) && !Boolean.TRUE.equals(task.getIsMetricsVisibleToStudents())) {
            return List.of();
        }

        return metricRepository.findByTaskId(taskId).stream()
                .map(metricMapper::toDto)
                .toList();
    }


    private void validateTarget(UUID postId, UUID taskId) {
        if ((postId == null && taskId == null) || (postId != null && taskId != null)) {
            throw new BadRequestException("Укажите либо postId, либо taskId");
        }
    }

    private void validateRange(Double minValue, Double maxValue) {
        if (minValue == null || maxValue == null) {
            throw new BadRequestException("Минимум и максимум обязательны");
        }
        if (minValue > maxValue) {
            throw new BadRequestException("minValue не может быть больше maxValue");
        }
    }

    private User getUserFromHeader(String authHeader) {
        Long userId = tokenProvider.extractUserIdFromHeader(authHeader);
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь не найден"));
    }

}
