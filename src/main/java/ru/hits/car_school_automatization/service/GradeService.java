package ru.hits.car_school_automatization.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.hits.car_school_automatization.entity.Channel;
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
import ru.hits.car_school_automatization.enums.GradeTargetType;
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
import ru.hits.car_school_automatization.util.RoleUtils;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import ru.hits.car_school_automatization.dto.GradeTableCellDto;
import ru.hits.car_school_automatization.dto.GradeTableDto;
import ru.hits.car_school_automatization.dto.GradeTableRowDto;
import ru.hits.car_school_automatization.dto.GradeTableTargetDto;
import ru.hits.car_school_automatization.dto.UserGradeDto;

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

        if (post.getType() != PostType.TASK && post.getType() != PostType.CONTROL) {
            throw new BadRequestException("Оценка доступна только для TASK или CONTROL");
        }

        validateUserInChannel(post.getChannelId(), targetUserId);

        double mark;
        if (post.getType() == PostType.CONTROL) {
            mark = calculateControlPostValue(postId, targetUserId);
        } else {
            mark = calculateMetrics(metricRepository.findByPostId(postId), targetUserId);
        }
        DeadlinePenalty postPenalty = post.getDeadlinePenalty();
        mark = applyDeadlinePenalty(mark, postPenalty != null ? postPenalty.getUnit() : null,
                postPenalty != null ? postPenalty.getStep() : null,
                postPenalty != null ? postPenalty.getValue() : null,
                post.getDeadline() != null ? post.getDeadline().toInstant(ZoneOffset.UTC) : null,
                getPostSubmissionTime(postId, targetUserId));

        return mark;
    }

    public List<UserGradeDto> getPostGrades(UUID postId, Long userId, String authHeader) {
        User requester = getUserFromHeader(authHeader);

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new NotFoundException("Пост не найден"));
        if (post.getType() != PostType.TASK && post.getType() != PostType.CONTROL) {
            throw new BadRequestException("Оценки доступны только для TASK или CONTROL");
        }

        if (userId != null) {
            double value = getPostGrade(postId, userId, authHeader);
            return List.of(UserGradeDto.builder().userId(userId).value(value).build());
        }

        if (!RoleUtils.isTeacherOrManager(requester)) {
            double value = getPostGrade(postId, requester.getId(), authHeader);
            return List.of(UserGradeDto.builder().userId(requester.getId()).value(value).build());
        }

        var channel = channelRepository.findById(post.getChannelId())
                .orElseThrow(() -> new NotFoundException("Предмет не найден"));

        return channel.getUsers().stream()
                .filter(u -> u.getRole() != null && u.getRole().contains(Role.STUDENT))
                .map(u -> UserGradeDto.builder()
                        .userId(u.getId())
                        .value(getPostGrade(postId, u.getId(), authHeader))
                        .build())
                .toList();
    }

    private double calculateControlPostValue(UUID postId, Long userId) {
        List<Metric> metrics = metricRepository.findByPostId(postId);
        if (metrics == null || metrics.isEmpty()) {
            return 1.0;
        }

        Metric metric = resolveSingleCoefficientMetric(metrics);
        if (metric == null) {
            return 1.0;
        }

        Map<UUID, MetricValue> values = metricValueRepository
                .findByMetricIdInAndUserId(List.of(metric.getId()), userId)
                .stream()
                .collect(Collectors.toMap(MetricValue::getMetricId, Function.identity()));

        MetricValue metricValue = values.get(metric.getId());
        return metricValue != null ? metricValue.getValue() : metric.getMinValue();
    }

    private Metric resolveSingleCoefficientMetric(List<Metric> metrics) {
        if (metrics == null || metrics.isEmpty()) {
            return null;
        }

        List<Metric> coefficientMetrics = metrics.stream()
                .filter(m -> m.getType() == MetricType.COEFFICIENT)
                .sorted(Comparator.comparing(Metric::getId))
                .toList();

        if (!coefficientMetrics.isEmpty()) {
            return coefficientMetrics.getFirst();
        }

        return metrics.size() == 1 ? metrics.getFirst() : null;
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

    public List<UserGradeDto> getTaskGrades(UUID taskId, Long userId, String authHeader) {
        User requester = getUserFromHeader(authHeader);

        if (userId != null) {
            double value = getTaskGrade(taskId, userId, authHeader);
            return List.of(UserGradeDto.builder().userId(userId).value(value).build());
        }

        if (!RoleUtils.isTeacherOrManager(requester)) {
            double value = getTaskGrade(taskId, requester.getId(), authHeader);
            return List.of(UserGradeDto.builder().userId(requester.getId()).value(value).build());
        }

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new NotFoundException("Задание не найдено"));

        List<Team> teams = teamRepository.findByTask_Id(taskId);
        if (teams == null || teams.isEmpty()) {
            return List.of();
        }

        return teams.stream()
                .filter(t -> t.getUsers() != null)
                .flatMap(t -> t.getUsers().stream())
                .filter(u -> u.getRole() != null && u.getRole().contains(Role.STUDENT))
                .collect(Collectors.toMap(User::getId, Function.identity(), (a, b) -> a))
                .values().stream()
                .map(u -> UserGradeDto.builder()
                        .userId(u.getId())
                        .value(getTaskGrade(task.getId(), u.getId(), authHeader))
                        .build())
                .toList();
    }

    public List<UserGradeDto> getTaskTeamGrades(UUID taskId, UUID teamId, String authHeader) {
        User requester = getUserFromHeader(authHeader);
        if (!RoleUtils.isTeacherOrManager(requester)) {
            throw new ForbiddenException("Недостаточно прав для просмотра оценок команды");
        }

        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new NotFoundException("Команда не найдена"));

        if (team.getTask() == null || team.getTask().getId() == null) {
            throw new BadRequestException("Команда не привязана к заданию");
        }
        if (!team.getTask().getId().equals(taskId)) {
            throw new BadRequestException("Команда не принадлежит заданию");
        }

        if (team.getUsers() == null || team.getUsers().isEmpty()) {
            return List.of();
        }

        return team.getUsers().stream()
                .filter(u -> u.getRole() != null && u.getRole().contains(Role.STUDENT))
                .map(u -> UserGradeDto.builder()
                        .userId(u.getId())
                        .value(getTaskGrade(taskId, u.getId(), authHeader))
                        .build())
                .toList();
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

    public GradeTableDto getChannelGradeTable(UUID channelId, Long userId, String authHeader) {
        User requester = getUserFromHeader(authHeader);
        validateUserInChannel(channelId, requester.getId());

        if (!RoleUtils.isTeacherOrManager(requester)) {
            if (userId != null && !requester.getId().equals(userId)) {
                throw new ForbiddenException("Недостаточно прав для просмотра таблицы");
            }
            userId = requester.getId();
        } else if (userId != null) {
            validateUserInChannel(channelId, userId);
        }

        Channel channel = channelRepository.findById(channelId)
                .orElseThrow(() -> new NotFoundException("Предмет не найден"));

        List<User> students = channel.getUsers().stream()
                .filter(u -> u.getRole() != null && u.getRole().contains(Role.STUDENT))
                .toList();

        if (userId != null) {
            Long finalUserId = userId;
            students = students.stream()
                    .filter(u -> u.getId().equals(finalUserId))
                    .toList();
        }

        List<Post> postTasks = postRepository.findByChannelIdAndType(channelId, PostType.TASK);
        List<Task> tasks = taskRepository.findByChannel_Id(channelId);
        List<Control> controls = controlRepository.findByChannelId(channelId);

        Map<UUID, List<UUID>> controlIdsByTarget = new HashMap<>();
        Map<UUID, Control> controlsById = controls.stream()
                .collect(Collectors.toMap(Control::getPostId, Function.identity(), (a, b) -> a));

        for (Control control : controls) {
            UUID controlId = control.getPostId();
            for (UUID postId : control.getPostTaskIds()) {
                controlIdsByTarget
                        .computeIfAbsent(postId, key -> new ArrayList<>())
                        .add(controlId);
            }
            for (UUID taskId : control.getTaskIds()) {
                controlIdsByTarget
                        .computeIfAbsent(taskId, key -> new ArrayList<>())
                        .add(controlId);
            }
        }

        Map<UUID, Post> controlPostsById = postRepository.findAllById(controlsById.keySet()).stream()
                .collect(Collectors.toMap(Post::getId, Function.identity(), (a, b) -> a));

        List<GradeTableTargetWithTime> targetEntries = new ArrayList<>();
        for (Post post : postTasks) {
            Instant createdAt = post.getCreatedAt() != null
                    ? post.getCreatedAt().toInstant(ZoneOffset.UTC)
                    : Instant.EPOCH;
            targetEntries.add(new GradeTableTargetWithTime(
                    GradeTableTargetDto.builder()
                            .targetId(post.getId())
                            .label(post.getLabel())
                            .type(GradeTargetType.POST_TASK)
                            .build(),
                    createdAt
            ));
        }

        for (Task task : tasks) {
            Instant startAt = task.getStartAt() != null ? task.getStartAt() : Instant.EPOCH;
            targetEntries.add(new GradeTableTargetWithTime(
                    GradeTableTargetDto.builder()
                            .targetId(task.getId())
                            .label(task.getLabel())
                            .type(GradeTargetType.TASK)
                            .build(),
                    startAt
            ));
        }

        for (Control control : controls) {
            Post controlPost = controlPostsById.get(control.getPostId());
            String label = controlPost != null ? controlPost.getLabel() : null;
            Instant createdAt = controlPost != null && controlPost.getCreatedAt() != null
                    ? controlPost.getCreatedAt().toInstant(ZoneOffset.UTC)
                    : Instant.EPOCH;
            targetEntries.add(new GradeTableTargetWithTime(
                    GradeTableTargetDto.builder()
                            .targetId(control.getPostId())
                            .label(label)
                            .type(GradeTargetType.CONTROL)
                            .build(),
                    createdAt
            ));
        }

        List<GradeTableTargetDto> targets = targetEntries.stream()
                .sorted(Comparator.comparing(GradeTableTargetWithTime::sortKey))
                .map(GradeTableTargetWithTime::target)
                .toList();

        List<GradeTableRowDto> rows = new ArrayList<>();
        for (User student : students) {
            List<GradeTableCellDto> grades = new ArrayList<>();
            double sum = 0.0;
            int count = 0;
            for (GradeTableTargetDto target : targets) {
                Double rawValue;
                if (target.getType() == GradeTargetType.POST_TASK) {
                    rawValue = getPostGrade(target.getTargetId(), student.getId(), authHeader);
                    double adjusted = applyControlCoefficients(rawValue, controls, target.getTargetId(), null, student.getId());
                    sum += adjusted;
                    count += 1;
                } else if (target.getType() == GradeTargetType.TASK) {
                    rawValue = getTaskGrade(target.getTargetId(), student.getId(), authHeader);
                    double adjusted = applyControlCoefficients(rawValue, controls, null, target.getTargetId(), student.getId());
                    sum += adjusted;
                    count += 1;
                } else {
                    Control control = controlsById.get(target.getTargetId());
                    rawValue = control != null ? calculateControlCoefficient(control, student.getId()) : null;
                }

                List<UUID> controlIds = controlIdsByTarget.getOrDefault(target.getTargetId(), List.of());
                grades.add(GradeTableCellDto.builder()
                        .targetId(target.getTargetId())
                        .rawValue(rawValue)
                        .controlIds(controlIds)
                        .build());
            }

            String userName = student.getFirstName() + " " + student.getLastName();
            rows.add(GradeTableRowDto.builder()
                    .userId(student.getId())
                    .userName(userName)
                    .channelGrade(count == 0 ? 0.0 : sum / count)
                    .grades(grades)
                    .build());
        }

        return GradeTableDto.builder()
                .targets(targets)
                .rows(rows)
                .build();
    }

    private record GradeTableTargetWithTime(GradeTableTargetDto target, Instant sortKey) {
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

        Metric metric = resolveSingleCoefficientMetric(metrics);
        if (metric == null) {
            return 1.0;
        }

        Map<UUID, MetricValue> values = metricValueRepository
                .findByMetricIdInAndUserId(List.of(metric.getId()), userId)
                .stream()
                .collect(Collectors.toMap(MetricValue::getMetricId, Function.identity()));

        MetricValue metricValue = values.get(metric.getId());
        return metricValue != null ? metricValue.getValue() : metric.getMinValue();
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
        if (!RoleUtils.isTeacherOrManager(requester) && !requester.getId().equals(userId)) {
            throw new ForbiddenException("Недостаточно прав для просмотра оценки");
        }
        return userId;
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
