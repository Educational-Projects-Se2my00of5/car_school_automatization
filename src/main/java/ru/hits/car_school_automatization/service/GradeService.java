package ru.hits.car_school_automatization.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.hits.car_school_automatization.entity.Control;
import ru.hits.car_school_automatization.entity.DeadlinePenalty;
import ru.hits.car_school_automatization.entity.Metric;
import ru.hits.car_school_automatization.entity.MetricValue;
import ru.hits.car_school_automatization.entity.Post;
import ru.hits.car_school_automatization.entity.Solution;
import ru.hits.car_school_automatization.entity.Task;
import ru.hits.car_school_automatization.entity.TaskSolution;
import ru.hits.car_school_automatization.entity.Team;
import ru.hits.car_school_automatization.entity.User;
import ru.hits.car_school_automatization.enums.DeadlinePenaltyUnit;
import ru.hits.car_school_automatization.enums.MetricType;
import ru.hits.car_school_automatization.enums.PostType;
import ru.hits.car_school_automatization.enums.Role;
import ru.hits.car_school_automatization.exception.BadRequestException;
import ru.hits.car_school_automatization.exception.ForbiddenException;
import ru.hits.car_school_automatization.exception.NotFoundException;
import ru.hits.car_school_automatization.repository.ChannelRepository;
import ru.hits.car_school_automatization.repository.ControlRepository;
import ru.hits.car_school_automatization.repository.MetricRepository;
import ru.hits.car_school_automatization.repository.MetricValueRepository;
import ru.hits.car_school_automatization.repository.PostRepository;
import ru.hits.car_school_automatization.repository.SolutionRepository;
import ru.hits.car_school_automatization.repository.TaskRepository;
import ru.hits.car_school_automatization.repository.TaskSolutionRepository;
import ru.hits.car_school_automatization.repository.TeamRepository;
import ru.hits.car_school_automatization.repository.UserRepository;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class GradeService {

    private final MetricRepository metricRepository;
    private final MetricValueRepository metricValueRepository;
    private final ControlRepository controlRepository;
    private final PostRepository postRepository;
    private final TaskRepository taskRepository;
    private final TeamRepository teamRepository;
    private final SolutionRepository solutionRepository;
    private final TaskSolutionRepository taskSolutionRepository;
    private final ChannelRepository channelRepository;
    private final UserRepository userRepository;
    private final JwtTokenProvider tokenProvider;

    public double getPostGrade(UUID postId, Long userId, String authHeader) {
        User requester = getUserFromHeader(authHeader);
        Long targetUserId = resolveTargetUserId(requester, userId);

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new NotFoundException("Пост не найден"));
        if (post.getType() != PostType.TASK) {
            throw new BadRequestException("Оценка доступна только для TASK");
        }

        validateUserInChannel(post.getChannelId(), targetUserId);

        double mark = calculateMetrics(metricRepository.findByPostId(postId), targetUserId);
        DeadlinePenalty postPenalty = post.getDeadlinePenalty();
        mark = applyDeadlinePenalty(mark, postPenalty != null ? postPenalty.getUnit() : null,
            postPenalty != null ? postPenalty.getStep() : null,
            postPenalty != null ? postPenalty.getValue() : null,
                post.getDeadline() != null ? post.getDeadline().toInstant(ZoneOffset.UTC) : null,
                getPostSubmissionTime(postId, targetUserId));

        return mark;
    }

    public double getTaskGrade(UUID taskId, Long userId, String authHeader) {
        User requester = getUserFromHeader(authHeader);
        Long targetUserId = resolveTargetUserId(requester, userId);

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new NotFoundException("Задание не найдено"));

        validateUserInChannel(task.getChannel().getId(), targetUserId);

        Team team = teamRepository.findByTask_IdAndUsers_Id(taskId, targetUserId).orElse(null);
        if (team == null) {
            return 0.0;
        }

        double mark = calculateMetrics(metricRepository.findByTaskId(taskId), targetUserId);
        DeadlinePenalty taskPenalty = task.getDeadlinePenalty();
        mark = applyDeadlinePenalty(mark, taskPenalty != null ? taskPenalty.getUnit() : null,
            taskPenalty != null ? taskPenalty.getStep() : null,
            taskPenalty != null ? taskPenalty.getValue() : null,
                team.getDeadline(),
                getTeamSubmissionTime(taskId, team.getId()));

        return mark;
    }

    public double getChannelGrade(UUID channelId, Long userId, String authHeader) {
        User requester = getUserFromHeader(authHeader);
        Long targetUserId = resolveTargetUserId(requester, userId);

        validateUserInChannel(channelId, targetUserId);

        List<Post> taskPosts = postRepository.findByChannelIdAndType(channelId, PostType.TASK);
        List<Task> tasks = taskRepository.findByChannel_Id(channelId);
        List<Control> controls = controlRepository.findByChannelId(channelId);

        double sum = 0.0;
        int count = 0;

        for (Post post : taskPosts) {
            double mark = getPostGrade(post.getId(), targetUserId, authHeader);
            mark = applyControlCoefficients(mark, controls, post.getId(), null, targetUserId);
            sum += mark;
            count += 1;
        }

        for (Task task : tasks) {
            double mark = getTaskGrade(task.getId(), targetUserId, authHeader);
            mark = applyControlCoefficients(mark, controls, null, task.getId(), targetUserId);
            sum += mark;
            count += 1;
        }

        return count == 0 ? 0.0 : sum / count;
    }

    private double calculateMetrics(List<Metric> metrics, Long userId) {
        if (metrics == null || metrics.isEmpty()) {
            return 0.0;
        }

        List<UUID> metricIds = metrics.stream().map(Metric::getId).toList();
        Map<UUID, MetricValue> values = metricValueRepository.findByMetricIdInAndUserId(metricIds, userId).stream()
                .collect(Collectors.toMap(MetricValue::getMetricId, Function.identity()));

        List<Double> markValues = new ArrayList<>();
        List<Double> constraints = new ArrayList<>();
        double coefficientProduct = 1.0;

        for (Metric metric : metrics) {
            double value = values.get(metric.getId()) != null ? values.get(metric.getId()).getValue() : metric.getMinValue();

            if (metric.getType() == MetricType.MARK) {
                markValues.add(value);
            } else if (metric.getType() == MetricType.COEFFICIENT) {
                coefficientProduct *= value;
            } else if (metric.getType() == MetricType.CONSTRAINT) {
                constraints.add(value);
            }
        }

        double base = markValues.isEmpty() ? 0.0 : markValues.stream().mapToDouble(Double::doubleValue).sum() / markValues.size();
        double result = base * coefficientProduct;

        for (Double limit : constraints) {
            if (limit != null && result > limit) {
                result = limit;
            }
        }

        return result;
    }

    private double applyControlCoefficients(double mark, List<Control> controls, UUID postId, UUID taskId, Long userId) {
        if (controls == null || controls.isEmpty()) {
            return mark;
        }

        double result = mark;

        for (Control control : controls) {
            boolean applies = (postId != null && control.getPostTaskIds().contains(postId))
                    || (taskId != null && control.getTaskIds().contains(taskId));

            if (!applies) {
                continue;
            }

            double coef = calculateControlCoefficient(control, userId);
            result *= coef;
        }

        return result;
    }

    private double calculateControlCoefficient(Control control, Long userId) {
        List<Metric> metrics = metricRepository.findByPostId(control.getPostId());
        if (metrics == null || metrics.isEmpty()) {
            return 1.0;
        }

        List<UUID> metricIds = metrics.stream().map(Metric::getId).toList();
        Map<UUID, MetricValue> values = metricValueRepository.findByMetricIdInAndUserId(metricIds, userId).stream()
                .collect(Collectors.toMap(MetricValue::getMetricId, Function.identity()));

        double product = 1.0;
        for (Metric metric : metrics) {
            double value = values.get(metric.getId()) != null ? values.get(metric.getId()).getValue() : metric.getMinValue();
            product *= value;
        }

        return product;
    }

    private Instant getPostSubmissionTime(UUID postId, Long userId) {
        Optional<Solution> solution = solutionRepository.findByTaskIdAndStudentId(postId, userId);
        return solution.map(s -> s.getSubmittedAt().toInstant(ZoneOffset.UTC)).orElse(null);
    }

    private Instant getTeamSubmissionTime(UUID taskId, UUID teamId) {
        Optional<TaskSolution> selected = taskSolutionRepository.findByTaskIdAndTeamIdAndIsSelectedTrue(taskId, teamId);
        if (selected.isPresent()) {
            return selected.get().getCreatedAt();
        }

        List<TaskSolution> solutions = taskSolutionRepository.findByTaskIdAndTeamId(taskId, teamId);
        return solutions.stream()
                .map(TaskSolution::getCreatedAt)
                .min(Instant::compareTo)
                .orElse(null);
    }

    private double applyDeadlinePenalty(double mark, DeadlinePenaltyUnit unit, Integer step, Double penaltyValue,
                                        Instant deadline, Instant submittedAt) {
        if (unit == null || step == null || step <= 0 || penaltyValue == null || deadline == null) {
            return mark;
        }

        Instant actualTime = submittedAt != null ? submittedAt : Instant.now();
        if (!actualTime.isAfter(deadline)) {
            return mark;
        }

        Duration duration = Duration.between(deadline, actualTime);
        long delta;

        switch (unit) {
            case MINUTE -> delta = duration.toMinutes();
            case HOUR -> delta = duration.toHours();
            case DAY -> delta = duration.toDays();
            default -> delta = 0;
        }

        if (delta <= 0) {
            return mark;
        }

        long steps = delta / step;
        return mark - steps * penaltyValue;
    }

    private Long resolveTargetUserId(User requester, Long userId) {
        if (userId == null) {
            return requester.getId();
        }
        if (!isTeacher(requester) && !requester.getId().equals(userId)) {
            throw new ForbiddenException("Недостаточно прав для просмотра оценки");
        }
        return userId;
    }

    private boolean isTeacher(User user) {
        return user.getRole().contains(Role.TEACHER);
    }

    private User getUserFromHeader(String authHeader) {
        Long userId = tokenProvider.extractUserIdFromHeader(authHeader);
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь не найден"));
    }

    private void validateUserInChannel(UUID channelId, Long userId) {
        boolean inChannel = channelRepository.getUsersChannelByUserId(userId).stream()
                .anyMatch(channel -> channel.getId().equals(channelId));
        if (!inChannel) {
            throw new ForbiddenException("У пользователя нет доступа к этому предмету");
        }
    }
}
