package ru.hits.car_school_automatization.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.hits.car_school_automatization.dto.CreateTeamDto;
import ru.hits.car_school_automatization.dto.TeamDto;
import ru.hits.car_school_automatization.dto.UpdateTeamDto;
import ru.hits.car_school_automatization.entity.Channel;
import ru.hits.car_school_automatization.entity.Task;
import ru.hits.car_school_automatization.entity.Team;
import ru.hits.car_school_automatization.entity.User;
import ru.hits.car_school_automatization.enums.Role;
import ru.hits.car_school_automatization.enums.TeamType;
import ru.hits.car_school_automatization.exception.BadRequestException;
import ru.hits.car_school_automatization.exception.ForbiddenException;
import ru.hits.car_school_automatization.exception.NotFoundException;
import ru.hits.car_school_automatization.mapper.TeamMapper;
import ru.hits.car_school_automatization.repository.TaskRepository;
import ru.hits.car_school_automatization.repository.TeamRepository;
import ru.hits.car_school_automatization.repository.UserRepository;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TeamServiceTest {

    @Mock
    private TeamRepository teamRepository;

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtTokenProvider tokenProvider;

    @Mock
    private TeamMapper teamMapper;

    @InjectMocks
    private TeamService teamService;

    private UUID taskId;
    private UUID teamId;
    private String authHeader;
    private Long teacherId;
    private Long studentId;

    private User teacher;
    private User student;
    private Task task;
    private Team team;

    @BeforeEach
    void setUp() {
        taskId = UUID.randomUUID();
        teamId = UUID.randomUUID();
        authHeader = "Bearer token";
        teacherId = 100L;
        studentId = 200L;

        teacher = User.builder()
                .id(teacherId)
                .firstName("Teacher")
                .lastName("One")
                .role(List.of(Role.TEACHER))
                .build();

        student = User.builder()
                .id(studentId)
                .firstName("Student")
                .lastName("One")
                .role(List.of(Role.STUDENT))
                .build();

        Channel channel = new Channel(taskId, "Channel", "Desc", null, new HashSet<>(Set.of(teacher)), teacher);

        task = Task.builder()
                .id(taskId)
                .channel(channel)
                .teamType(TeamType.FREE)
                .build();

        team = Team.builder()
                .id(teamId)
                .name("Команда 1")
                .task(task)
                .users(new HashSet<>())
                .build();
    }

    @Test
    @DisplayName("createTeam: подставляет имя по умолчанию, если передано пустое")
    void createTeam_ShouldCreateWithDefaultName_WhenNameIsBlank() {
        CreateTeamDto dto = CreateTeamDto.builder()
                .taskId(taskId)
                .name("   ")
                .deadline(Instant.now())
                .build();

        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));
        when(tokenProvider.extractUserIdFromHeader(authHeader)).thenReturn(teacherId);
        when(userRepository.findById(teacherId)).thenReturn(Optional.of(teacher));
        when(teamRepository.save(any(Team.class))).thenAnswer(invocation -> {
            Team saved = invocation.getArgument(0);
            saved.setId(teamId);
            return saved;
        });
        when(teamMapper.toDto(any(Team.class))).thenAnswer(invocation -> {
            Team saved = invocation.getArgument(0);
            return TeamDto.builder().id(saved.getId()).name(saved.getName()).taskId(saved.getTask().getId()).build();
        });

        TeamDto result = teamService.createTeam(dto, authHeader);

        assertEquals(teamId, result.getId());
        assertEquals("Новая команда", result.getName());
    }

    @Test
    @DisplayName("updateTeam: пустое имя вызывает BadRequest")
    void updateTeam_WithBlankName_ShouldThrowBadRequest() {
        UpdateTeamDto dto = UpdateTeamDto.builder().name("   ").build();

        when(teamRepository.findById(teamId)).thenReturn(Optional.of(team));
        when(tokenProvider.extractUserIdFromHeader(authHeader)).thenReturn(teacherId);
        when(userRepository.findById(teacherId)).thenReturn(Optional.of(teacher));

        assertThrows(BadRequestException.class, () -> teamService.updateTeam(teamId, dto, authHeader));
    }

    @Test
    @DisplayName("updateTeam: капитан вне состава вызывает BadRequest")
    void updateTeam_WithCaptainNotInTeam_ShouldThrowBadRequest() {
        UpdateTeamDto dto = UpdateTeamDto.builder().captainId(studentId).build();

        when(teamRepository.findById(teamId)).thenReturn(Optional.of(team));
        when(tokenProvider.extractUserIdFromHeader(authHeader)).thenReturn(teacherId);
        when(userRepository.findById(teacherId)).thenReturn(Optional.of(teacher));

        assertThrows(BadRequestException.class, () -> teamService.updateTeam(teamId, dto, authHeader));
    }

    @Test
    @DisplayName("getTeam: несуществующая команда вызывает NotFound")
    void getTeam_WithUnknownId_ShouldThrowNotFound() {
        when(teamRepository.findById(teamId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> teamService.getTeam(teamId));
    }

    @Test
    @DisplayName("getTaskTeams: несуществующее задание вызывает NotFound")
    void getTaskTeams_WithUnknownTask_ShouldThrowNotFound() {
        when(taskRepository.existsById(taskId)).thenReturn(false);

        assertThrows(NotFoundException.class, () -> teamService.getTaskTeams(taskId));
    }

    @Test
    @DisplayName("addMember: повторное добавление участника вызывает BadRequest")
    void addMember_WhenAlreadyInTeam_ShouldThrowBadRequest() {
        team.getUsers().add(student);

        when(teamRepository.findById(teamId)).thenReturn(Optional.of(team));
        when(tokenProvider.extractUserIdFromHeader(authHeader)).thenReturn(teacherId);
        when(userRepository.findById(teacherId)).thenReturn(Optional.of(teacher));
        when(userRepository.findById(studentId)).thenReturn(Optional.of(student));

        assertThrows(BadRequestException.class, () -> teamService.addMember(teamId, studentId, authHeader));
    }

    @Test
    @DisplayName("joinTeam: для не-FREE задания возвращает BadRequest")
    void joinTeam_WhenTaskNotFree_ShouldThrowBadRequest() {
        task.setTeamType(TeamType.DRAFT);

        when(tokenProvider.extractUserIdFromHeader(authHeader)).thenReturn(studentId);
        when(teamRepository.findById(teamId)).thenReturn(Optional.of(team));

        assertThrows(BadRequestException.class, () -> teamService.joinTeam(teamId, authHeader));
    }

    @Test
    @DisplayName("joinTeam: если студент уже в другой команде задания, возвращает BadRequest")
    void joinTeam_WhenAlreadyInAnotherTeam_ShouldThrowBadRequest() {
        when(tokenProvider.extractUserIdFromHeader(authHeader)).thenReturn(studentId);
        when(teamRepository.findById(teamId)).thenReturn(Optional.of(team));
        when(userRepository.findById(studentId)).thenReturn(Optional.of(student));
        when(teamRepository.existsByTask_IdAndUsers_IdAndIdNot(taskId, studentId, teamId)).thenReturn(true);

        assertThrows(BadRequestException.class, () -> teamService.joinTeam(teamId, authHeader));
    }

    @Test
    @DisplayName("joinTeam: успешно добавляет студента в команду")
    void joinTeam_ShouldAddStudentToTeam() {
        when(tokenProvider.extractUserIdFromHeader(authHeader)).thenReturn(studentId);
        when(teamRepository.findById(teamId)).thenReturn(Optional.of(team));
        when(userRepository.findById(studentId)).thenReturn(Optional.of(student));
        when(teamRepository.existsByTask_IdAndUsers_IdAndIdNot(taskId, studentId, teamId)).thenReturn(false);
        when(teamRepository.save(any(Team.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(teamMapper.toDto(any(Team.class))).thenReturn(TeamDto.builder().id(teamId).build());

        TeamDto result = teamService.joinTeam(teamId, authHeader);

        assertNotNull(result);
        assertTrue(team.getUsers().stream().anyMatch(u -> u.getId().equals(studentId)));
    }

    @Test
    @DisplayName("leaveTeam: при выходе капитана captainId сбрасывается")
    void leaveTeam_WhenCaptainLeaves_ShouldResetCaptain() {
        team.getUsers().add(student);
        team.setCaptainId(studentId);

        when(tokenProvider.extractUserIdFromHeader(authHeader)).thenReturn(studentId);
        when(teamRepository.findById(teamId)).thenReturn(Optional.of(team));
        when(teamRepository.save(any(Team.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(teamMapper.toDto(any(Team.class))).thenReturn(TeamDto.builder().id(teamId).captainId(null).build());

        TeamDto result = teamService.leaveTeam(teamId, authHeader);

        assertNotNull(result);
        assertNull(team.getCaptainId());
        assertFalse(team.getUsers().stream().anyMatch(u -> u.getId().equals(studentId)));
    }

    @Test
    @DisplayName("deleteTeam: преподаватель, не ведущий предмет, получает Forbidden")
    void deleteTeam_WhenTeacherNotLeadingChannel_ShouldThrowForbidden() {
        User otherTeacher = User.builder().id(999L).role(List.of(Role.TEACHER)).build();

        when(teamRepository.findById(teamId)).thenReturn(Optional.of(team));
        when(tokenProvider.extractUserIdFromHeader(authHeader)).thenReturn(999L);
        when(userRepository.findById(999L)).thenReturn(Optional.of(otherTeacher));

        assertThrows(ForbiddenException.class, () -> teamService.deleteTeam(teamId, authHeader));
        verify(teamRepository, never()).delete(any());
    }

    @Test
    @DisplayName("removeMember: удаление отсутствующего участника возвращает NotFound")
    void removeMember_WhenUserNotInTeam_ShouldThrowNotFound() {
        when(teamRepository.findById(teamId)).thenReturn(Optional.of(team));
        when(tokenProvider.extractUserIdFromHeader(authHeader)).thenReturn(teacherId);
        when(userRepository.findById(teacherId)).thenReturn(Optional.of(teacher));

        assertThrows(NotFoundException.class, () -> teamService.removeMember(teamId, studentId, authHeader));
    }
}
