package ru.hits.car_school_automatization.bdd;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.hits.car_school_automatization.entity.*;
import ru.hits.car_school_automatization.enums.*;
import ru.hits.car_school_automatization.repository.*;
import ru.hits.car_school_automatization.service.JwtTokenProvider;
import ru.hits.car_school_automatization.service.P2PService;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
@Getter
public class BddTestHelper {

    private final UserRepository userRepository;
    private final ChannelRepository channelRepository;
    private final TaskRepository taskRepository;
    private final PostRepository postRepository;
    private final TeamRepository teamRepository;
    private final MetricRepository metricRepository;
    private final P2PParamRepository p2pParamRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final SolutionRepository solutionRepository;
    private final TaskSolutionRepository taskSolutionRepository;
    private final P2PPairPersonalRepository p2pPairPersonalRepository;
    private final P2PPairTeamRepository p2pPairTeamRepository;
    private final MetricValueRepository metricValueRepository;
    private final MetricChangeRepository metricChangeRepository;
    private final P2PService p2pService;

    @Transactional
    public void cleanDb() {
        metricChangeRepository.deleteAll();
        metricValueRepository.deleteAll();
        p2pPairPersonalRepository.deleteAll();
        p2pPairTeamRepository.deleteAll();
        taskSolutionRepository.deleteAll();
        solutionRepository.deleteAll();
        teamRepository.deleteAll();
        metricRepository.deleteAll();
        p2pParamRepository.deleteAll();
        taskRepository.deleteAll();
        postRepository.deleteAll();
        channelRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Transactional
    public User createTeacher() {
        User teacher = User.builder()
                .email("teacher@test.com")
                .passwordHash("pass")
                .firstName("Teacher")
                .lastName("Ivanov")
                .phone("1234567890")
                .age(30)
                .role(List.of(Role.TEACHER))
                .build();
        return userRepository.save(teacher);
    }

    @Transactional
    public User createStudent(String name) {
        User student = User.builder()
                .email(name + "@test.com")
                .passwordHash("pass")
                .firstName(name)
                .lastName("Student")
                .phone(name + "123")
                .age(20)
                .role(List.of(Role.STUDENT))
                .build();
        return userRepository.save(student);
    }

    @Transactional
    public Channel createChannel(User teacher, List<User> students) {
        java.util.HashSet<User> users = new java.util.HashSet<>();
        users.add(teacher);
        users.addAll(students);
        Channel channel = new Channel(null, "Test Channel", "desc", null, users, teacher);
        return channelRepository.save(channel);
    }

    @Transactional
    public Post createPostWithP2P(Channel channel, User teacher, String label) {
        Post post = Post.builder()
                .channelId(channel.getId())
                .authorId(teacher.getId())
                .label(label)
                .text("Text")
                .type(PostType.TASK)
                .createdAt(LocalDateTime.now())
                .isP2pEnabled(true)
                .build();
        post = postRepository.save(post);

        P2PParam param = P2PParam.builder()
                .id(post.getId())
                .type(P2PType.MANUAL)
                .visibility(P2PVisibility.PART)
                .p2pDeadline(Instant.now().plusSeconds(86400))
                .build();
        p2pParamRepository.save(param);
        return post;
    }

    @Transactional
    public Task createTaskWithP2P(Channel channel, String label) {
        Task task = Task.builder()
                .channel(channel)
                .label(label)
                .text("Task Text")
                .type(TaskType.QUALIFIED)
                .teamType(TeamType.RANDOM)
                .minTeamSize(1)
                .isCanRedistribute(false)
                .qualifiedMin(5)
                .isP2pEnabled(true)
                .startAt(Instant.now())
                .votingDeadline(Instant.now().plusSeconds(86400))
                .build();
        task = taskRepository.save(task);

        P2PParam param = P2PParam.builder()
                .id(task.getId())
                .type(P2PType.RANDOM)
                .visibility(P2PVisibility.ALL)
                .p2pDeadline(Instant.now().plusSeconds(86400))
                .build();
        p2pParamRepository.save(param);
        return task;
    }

    @Transactional
    public Team createTeam(String name, Task task, List<User> members, User captain) {
        Team team = Team.builder()
                .name(name)
                .task(task)
                .users(Set.copyOf(members))
                .captainId(captain != null ? captain.getId() : null)
                .build();
        return teamRepository.save(team);
    }

    @Transactional
    public Metric createMetricForPost(Post post, String label) {
        Metric metric = Metric.builder()
                .postId(post.getId())
                .name(label)
                .type(ru.hits.car_school_automatization.enums.MetricType.MARK)
                .minValue(0.0)
                .maxValue(10.0)
                .build();
        return metricRepository.save(metric);
    }

    @Transactional
    public Metric createMetricForTask(Task task, String label) {
        Metric metric = Metric.builder()
                .taskId(task.getId())
                .name(label)
                .type(ru.hits.car_school_automatization.enums.MetricType.MARK)
                .minValue(0.0)
                .maxValue(10.0)
                .build();
        return metricRepository.save(metric);
    }

    public String getToken(User user) {
        return "Bearer " + jwtTokenProvider.generateToken(user.getId());
    }
}
