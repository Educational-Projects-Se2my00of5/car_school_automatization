package ru.hits.car_school_automatization.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.hits.car_school_automatization.dto.CreateTaskDto;
import ru.hits.car_school_automatization.dto.TaskDto;
import ru.hits.car_school_automatization.dto.UpdateTaskDto;
import ru.hits.car_school_automatization.entity.Channel;
import ru.hits.car_school_automatization.entity.Task;
import ru.hits.car_school_automatization.entity.Team;
import ru.hits.car_school_automatization.entity.User;
import ru.hits.car_school_automatization.enums.Role;
import ru.hits.car_school_automatization.enums.TaskType;
import ru.hits.car_school_automatization.enums.TeamType;
import ru.hits.car_school_automatization.exception.BadRequestException;
import ru.hits.car_school_automatization.exception.ForbiddenException;
import ru.hits.car_school_automatization.exception.NotFoundException;
import ru.hits.car_school_automatization.mapper.TaskMapper;
import ru.hits.car_school_automatization.repository.ChannelRepository;
import ru.hits.car_school_automatization.repository.TaskRepository;
import ru.hits.car_school_automatization.repository.TeamRepository;
import ru.hits.car_school_automatization.repository.UserRepository;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private ChannelRepository channelRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtTokenProvider tokenProvider;

    @Mock
    private TaskMapper taskMapper;

    @Mock
    private TeamRepository teamRepository;

    @Mock
    private TeamFormationService teamFormationService;

    @InjectMocks
    private TaskService taskService;

    private UUID channelId;
    private UUID taskId;
    private Long teacherId;
    private String authHeader;
    private User teacher;
    private Channel channel;
    private Task existingTask;

    @BeforeEach
    void setUp() {
        channelId = UUID.randomUUID();
        taskId = UUID.randomUUID();
        teacherId = 10L;
        authHeader = "Bearer token";

        teacher = User.builder()
                .id(teacherId)
                .role(List.of(Role.TEACHER))
                .build();

        channel = new Channel(
                channelId,
                "Channel",
                "Desc",
                null,
                new HashSet<>(Set.of(teacher)),
                teacher
        );

        existingTask = Task.builder()
            .id(taskId)
            .label("Task")
            .text("Text")
            .channel(channel)
            .documents(new ArrayList<>())
            .teamType(TeamType.FREE)
            .type(TaskType.FREE)
            .minTeamSize(2)
            .isCanRedistribute(false)
            .build();
    }

    @Test
    @DisplayName("createTask: делегирует формирование команд в TeamFormationService")
    void createTask_ShouldDelegateTeamFormation() {
        CreateTaskDto dto = CreateTaskDto.builder()
                .label("Task")
                .text("Text")
                .teamType(TeamType.FREE)
                .type(TaskType.FREE)
                .minTeamSize(2)
                .isCanRedistribute(false)
                .build();

        when(channelRepository.findById(channelId)).thenReturn(Optional.of(channel));
        when(tokenProvider.extractUserIdFromHeader(authHeader)).thenReturn(teacherId);
        when(userRepository.findById(teacherId)).thenReturn(Optional.of(teacher));

        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> {
            Task saved = invocation.getArgument(0);
            saved.setId(taskId);
            return saved;
        });
        when(teamFormationService.formByTeamType(any(Task.class), anyList(), eq(dto)))
                .thenReturn(List.of(Team.builder().name("Команда").build()));
        when(taskMapper.toDto(any(Task.class))).thenReturn(TaskDto.builder().id(taskId).label("Task").build());

        TaskDto result = taskService.createTask(dto, channelId, authHeader);

        assertNotNull(result);
        verify(teamFormationService).formByTeamType(any(Task.class), anyList(), eq(dto));
        verify(teamRepository).saveAll(anyList());
    }

    @Test
    @DisplayName("createTask: несуществующий предмет возвращает NotFound")
    void createTask_WhenChannelNotFound_ShouldThrowNotFound() {
        CreateTaskDto dto = CreateTaskDto.builder()
                .label("Task")
                .text("Text")
                .teamType(TeamType.FREE)
                .type(TaskType.FREE)
                .minTeamSize(2)
                .isCanRedistribute(false)
                .build();

        when(channelRepository.findById(channelId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> taskService.createTask(dto, channelId, authHeader));
        verify(taskRepository, never()).save(any(Task.class));
    }

    @Test
    @DisplayName("createTask: преподаватель, не ведущий предмет, получает Forbidden")
    void createTask_WhenTeacherNotLeadingChannel_ShouldThrowForbidden() {
        CreateTaskDto dto = CreateTaskDto.builder()
                .label("Task")
                .text("Text")
                .teamType(TeamType.FREE)
                .type(TaskType.FREE)
                .minTeamSize(2)
                .isCanRedistribute(false)
                .build();

        User otherTeacher = User.builder().id(777L).role(List.of(Role.TEACHER)).build();
        Channel foreignChannel = new Channel(
                channelId,
                "Foreign",
                "Desc",
                null,
                new HashSet<>(Set.of(otherTeacher)),
                otherTeacher
        );

        when(channelRepository.findById(channelId)).thenReturn(Optional.of(foreignChannel));
        when(tokenProvider.extractUserIdFromHeader(authHeader)).thenReturn(teacherId);
        when(userRepository.findById(teacherId)).thenReturn(Optional.of(teacher));

        assertThrows(ForbiddenException.class, () -> taskService.createTask(dto, channelId, authHeader));
        verify(taskRepository, never()).save(any(Task.class));
    }

    @Test
    @DisplayName("createTask: для QUALIFIED без qualifiedMin возвращает BadRequest")
    void createTask_QualifiedWithoutMin_ShouldThrowBadRequest() {
        CreateTaskDto dto = CreateTaskDto.builder()
                .label("Task")
                .text("Text")
                .teamType(TeamType.FREE)
                .type(TaskType.QUALIFIED)
                .minTeamSize(2)
                .qualifiedMin(null)
                .isCanRedistribute(false)
                .build();

        when(channelRepository.findById(channelId)).thenReturn(Optional.of(channel));
        when(tokenProvider.extractUserIdFromHeader(authHeader)).thenReturn(teacherId);
        when(userRepository.findById(teacherId)).thenReturn(Optional.of(teacher));

        assertThrows(BadRequestException.class, () -> taskService.createTask(dto, channelId, authHeader));
        verify(taskRepository, never()).save(any(Task.class));
    }

    @Test
    @DisplayName("createTask: minTeamSize < 1 возвращает BadRequest")
    void createTask_WithInvalidMinTeamSize_ShouldThrowBadRequest() {
        CreateTaskDto dto = CreateTaskDto.builder()
                .label("Task")
                .text("Text")
                .teamType(TeamType.FREE)
                .type(TaskType.FREE)
                .minTeamSize(0)
                .isCanRedistribute(false)
                .build();

        when(channelRepository.findById(channelId)).thenReturn(Optional.of(channel));
        when(tokenProvider.extractUserIdFromHeader(authHeader)).thenReturn(teacherId);
        when(userRepository.findById(teacherId)).thenReturn(Optional.of(teacher));

        assertThrows(BadRequestException.class, () -> taskService.createTask(dto, channelId, authHeader));
        verify(taskRepository, never()).save(any(Task.class));
    }

    @Test
    @DisplayName("deleteTask: преподаватель ведущий предмет может удалить задачу")
    void deleteTask_WhenTeacherLeadsChannel_ShouldDelete() {
        when(taskRepository.findById(taskId)).thenReturn(Optional.of(existingTask));
        when(tokenProvider.extractUserIdFromHeader(authHeader)).thenReturn(teacherId);
        when(userRepository.findById(teacherId)).thenReturn(Optional.of(teacher));

        taskService.deleteTask(taskId, authHeader);

        verify(taskRepository).delete(existingTask);
    }

    @Test
    @DisplayName("getTask: возвращает задачу при доступе к предмету")
    void getTask_ShouldReturnTask() {
        when(taskRepository.findById(taskId)).thenReturn(Optional.of(existingTask));
        when(tokenProvider.extractUserIdFromHeader(authHeader)).thenReturn(teacherId);
        when(taskMapper.toDto(existingTask)).thenReturn(TaskDto.builder().id(taskId).label("Task").build());

        TaskDto result = taskService.getTask(taskId, authHeader);

        assertNotNull(result);
        assertEquals(taskId, result.getId());
        verify(taskMapper).toDto(existingTask);
    }

    @Test
    @DisplayName("getTask: пользователь без доступа к предмету получает Forbidden")
    void getTask_WhenUserNotInChannel_ShouldThrowForbidden() {
        when(taskRepository.findById(taskId)).thenReturn(Optional.of(existingTask));
        when(tokenProvider.extractUserIdFromHeader(authHeader)).thenReturn(999L);

        assertThrows(ForbiddenException.class, () -> taskService.getTask(taskId, authHeader));
    }

    @Test
    @DisplayName("getTasksByChannel: возвращает список задач предмета")
    void getTasksByChannel_ShouldReturnTasks() {
        Task anotherTask = Task.builder()
                .id(UUID.randomUUID())
                .label("Task 2")
                .text("Text 2")
                .channel(channel)
                .teamType(TeamType.FREE)
                .type(TaskType.FREE)
                .minTeamSize(2)
                .isCanRedistribute(false)
                .build();

        when(channelRepository.findById(channelId)).thenReturn(Optional.of(channel));
        when(tokenProvider.extractUserIdFromHeader(authHeader)).thenReturn(teacherId);
        when(taskRepository.findByChannel_Id(channelId)).thenReturn(List.of(existingTask, anotherTask));
        when(taskMapper.toDtoList(anyList())).thenReturn(List.of(
                TaskDto.builder().id(existingTask.getId()).label(existingTask.getLabel()).build(),
                TaskDto.builder().id(anotherTask.getId()).label(anotherTask.getLabel()).build()
        ));

        List<TaskDto> result = taskService.getTasksByChannel(channelId, authHeader);

        assertEquals(2, result.size());
        verify(taskMapper).toDtoList(anyList());
    }

    @Test
    @DisplayName("getTasksByChannel: несуществующий предмет возвращает NotFound")
    void getTasksByChannel_WhenChannelNotFound_ShouldThrowNotFound() {
        when(channelRepository.findById(channelId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> taskService.getTasksByChannel(channelId, authHeader));
    }

    @Test
    @DisplayName("updateTask: обновляет задачу и возвращает DTO")
    void updateTask_ShouldUpdateAndReturnDto() {
        UpdateTaskDto dto = UpdateTaskDto.builder()
                .label("Updated")
                .minTeamSize(3)
                .build();

        when(taskRepository.findById(taskId)).thenReturn(Optional.of(existingTask));
        when(tokenProvider.extractUserIdFromHeader(authHeader)).thenReturn(teacherId);
        when(userRepository.findById(teacherId)).thenReturn(Optional.of(teacher));
        when(taskRepository.save(existingTask)).thenReturn(existingTask);
        when(taskMapper.toDto(existingTask)).thenReturn(TaskDto.builder().id(taskId).label("Updated").build());

        TaskDto result = taskService.updateTask(taskId, dto, authHeader);

        assertNotNull(result);
        verify(taskMapper).updateTaskFromDto(dto, existingTask);
        verify(taskRepository).save(existingTask);
    }

    @Test
    @DisplayName("updateTask: minTeamSize < 1 вызывает BadRequest")
    void updateTask_WithInvalidMinTeamSize_ShouldThrowBadRequest() {
        UpdateTaskDto dto = UpdateTaskDto.builder().minTeamSize(0).build();

        when(taskRepository.findById(taskId)).thenReturn(Optional.of(existingTask));
        when(tokenProvider.extractUserIdFromHeader(authHeader)).thenReturn(teacherId);
        when(userRepository.findById(teacherId)).thenReturn(Optional.of(teacher));
        doAnswer(invocation -> {
            Task target = invocation.getArgument(1);
            target.setMinTeamSize(0);
            return null;
        }).when(taskMapper).updateTaskFromDto(eq(dto), eq(existingTask));

        assertThrows(BadRequestException.class, () -> taskService.updateTask(taskId, dto, authHeader));
        verify(taskRepository, never()).save(any(Task.class));
    }

    @Test
    @DisplayName("copyTask: копирует задачу в целевой предмет")
    void copyTask_ShouldCopyToTargetChannel() {
        UUID targetChannelId = UUID.randomUUID();
        Channel targetChannel = new Channel(
                targetChannelId,
                "Target",
                "Desc",
                null,
                new HashSet<>(Set.of(teacher)),
                teacher
        );

        when(taskRepository.findById(taskId)).thenReturn(Optional.of(existingTask));
        when(channelRepository.findById(targetChannelId)).thenReturn(Optional.of(targetChannel));
        when(tokenProvider.extractUserIdFromHeader(authHeader)).thenReturn(teacherId);
        when(userRepository.findById(teacherId)).thenReturn(Optional.of(teacher));
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> {
            Task saved = invocation.getArgument(0);
            saved.setId(UUID.randomUUID());
            return saved;
        });
        when(taskMapper.toDto(any(Task.class))).thenReturn(TaskDto.builder().label("Task").build());

        TaskDto result = taskService.copyTask(taskId, targetChannelId, authHeader);

        assertNotNull(result);
        verify(taskRepository).save(any(Task.class));
    }

    @Test
    @DisplayName("copyTask: несуществующий целевой предмет возвращает NotFound")
    void copyTask_WhenTargetChannelNotFound_ShouldThrowNotFound() {
        UUID targetChannelId = UUID.randomUUID();

        when(taskRepository.findById(taskId)).thenReturn(Optional.of(existingTask));
        when(channelRepository.findById(targetChannelId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> taskService.copyTask(taskId, targetChannelId, authHeader));
    }

    @Test
    @DisplayName("copyTask: преподаватель не ведущий целевой предмет получает Forbidden")
    void copyTask_WhenTeacherNotLeadingTargetChannel_ShouldThrowForbidden() {
        UUID targetChannelId = UUID.randomUUID();
        User otherTeacher = User.builder().id(777L).role(List.of(Role.TEACHER)).build();
        Channel targetChannel = new Channel(
                targetChannelId,
                "Target",
                "Desc",
                null,
                new HashSet<>(Set.of(otherTeacher)),
                otherTeacher
        );

        when(taskRepository.findById(taskId)).thenReturn(Optional.of(existingTask));
        when(channelRepository.findById(targetChannelId)).thenReturn(Optional.of(targetChannel));
        when(tokenProvider.extractUserIdFromHeader(authHeader)).thenReturn(teacherId);
        when(userRepository.findById(teacherId)).thenReturn(Optional.of(teacher));

        assertThrows(ForbiddenException.class, () -> taskService.copyTask(taskId, targetChannelId, authHeader));
    }
}
