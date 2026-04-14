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
import ru.hits.car_school_automatization.entity.*;
import ru.hits.car_school_automatization.dto.*;
import ru.hits.car_school_automatization.entity.*;
import ru.hits.car_school_automatization.enums.Role;
import ru.hits.car_school_automatization.enums.TeamType;
import ru.hits.car_school_automatization.exception.BadRequestException;
import ru.hits.car_school_automatization.exception.ForbiddenException;
import ru.hits.car_school_automatization.exception.NotFoundException;
import ru.hits.car_school_automatization.mapper.TeamMapper;
import ru.hits.car_school_automatization.repository.InviteRepository;
import ru.hits.car_school_automatization.mapper.UserMapper;
import ru.hits.car_school_automatization.repository.CaptainVoteRepository;
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

    @Mock
    private InviteRepository inviteRepository;

    @Mock
    private UserMapper userMapper;

    @Mock
    private CaptainVoteRepository captainVoteRepository;

    @InjectMocks
    private TeamService teamService;

    private UUID taskId;
    private UUID teamId;
    private String authHeader;
    private Long teacherId;
    private Long studentId;
    private Long anotherStudentId;

    private User anotherStudent;
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
        anotherStudentId = 300L;

        anotherStudent = User.builder()
                .id(anotherStudentId)
                .firstName("Student")
                .lastName("Two")
                .role(List.of(Role.STUDENT))
                .build();

        anotherStudentId = 300L;

        anotherStudent = User.builder()
                .id(anotherStudentId)
                .firstName("Student")
                .lastName("Two")
                .role(List.of(Role.STUDENT))
                .build();

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

    @Test
    @DisplayName("chooseCaptain: DRAFT задание выбрасывает ошибку")
    void chooseCaptain_ForDraftTask_ShouldThrowBadRequest() {
        task.setTeamType(TeamType.DRAFT);
        ChooseCaptainDto dto = ChooseCaptainDto.builder().teamId(teamId).captainId(studentId).build();

        when(tokenProvider.extractUserIdFromHeader(authHeader)).thenReturn(studentId);
        when(userRepository.findById(studentId)).thenReturn(Optional.of(student));
        when(teamRepository.findById(teamId)).thenReturn(Optional.of(team));

        assertThrows(BadRequestException.class, () -> teamService.chooseCaptain(dto, authHeader));
    }

    @Test
    @DisplayName("chooseCaptain: преподаватель назначает капитана")
    void chooseCaptain_TeacherAssignsCaptain_ShouldSuccess() {
        Team teamWithUsers = Team.builder()
                .id(teamId)
                .task(task)
                .users(new HashSet<>(Set.of(student, anotherStudent)))
                .isAvailableRevote(true)
                .isCaptainVotingActive(false)
                .build();

        ChooseCaptainDto dto = ChooseCaptainDto.builder().teamId(teamId).captainId(studentId).build();

        when(tokenProvider.extractUserIdFromHeader(authHeader)).thenReturn(teacherId);
        when(userRepository.findById(teacherId)).thenReturn(Optional.of(teacher));
        when(teamRepository.findById(teamId)).thenReturn(Optional.of(teamWithUsers));
        when(userRepository.findById(studentId)).thenReturn(Optional.of(student));
        when(teamRepository.save(any(Team.class))).thenReturn(teamWithUsers);
        when(teamMapper.toDto(any(Team.class))).thenReturn(TeamDto.builder().id(teamId).captainId(studentId).build());

        TeamDto result = teamService.chooseCaptain(dto, authHeader);

        assertNotNull(result);
        assertEquals(studentId, result.getCaptainId());
        verify(captainVoteRepository, times(1)).deleteByTeamId(teamId);
    }

    @Test
    @DisplayName("chooseCaptain: студент автоматически становится капитаном при создании команды (первый участник)")
    void chooseCaptain_FirstStudentInTeam_ShouldAutoBecomeCaptain() {
        Team emptyTeam = Team.builder()
                .id(teamId)
                .task(task)
                .users(new HashSet<>())
                .isAvailableRevote(true)
                .build();
        emptyTeam.getUsers().add(student);

        ChooseCaptainDto dto = ChooseCaptainDto.builder().teamId(teamId).captainId(null).build();

        when(tokenProvider.extractUserIdFromHeader(authHeader)).thenReturn(studentId);
        when(userRepository.findById(studentId)).thenReturn(Optional.of(student));
        when(teamRepository.findById(teamId)).thenReturn(Optional.of(emptyTeam));
        when(teamRepository.save(any(Team.class))).thenReturn(emptyTeam);
        when(teamMapper.toDto(any(Team.class))).thenReturn(TeamDto.builder().id(teamId).captainId(studentId).build());

        TeamDto result = teamService.chooseCaptain(dto, authHeader);

        assertNotNull(result);
        assertEquals(studentId, result.getCaptainId());
    }

    @Test
    @DisplayName("chooseCaptain: студент голосует за кандидата")
    void chooseCaptain_StudentVotesForCandidate_ShouldSaveVote() {
        Team teamWithUsers = Team.builder()
                .id(teamId)
                .task(task)
                .users(new HashSet<>(Set.of(student, anotherStudent)))
                .isAvailableRevote(true)
                .isCaptainVotingActive(false)
                .build();

        ChooseCaptainDto dto = ChooseCaptainDto.builder().teamId(teamId).captainId(anotherStudentId).build();

        when(tokenProvider.extractUserIdFromHeader(authHeader)).thenReturn(studentId);
        when(userRepository.findById(studentId)).thenReturn(Optional.of(student));
        when(teamRepository.findById(teamId)).thenReturn(Optional.of(teamWithUsers));
        when(captainVoteRepository.findByTeamIdAndVoterId(teamId, studentId)).thenReturn(Optional.empty());
        when(captainVoteRepository.save(any(CaptainVote.class))).thenReturn(new CaptainVote());
        when(teamMapper.toDto(any(Team.class))).thenReturn(TeamDto.builder().id(teamId).build());

        TeamDto result = teamService.chooseCaptain(dto, authHeader);

        assertNotNull(result);
        verify(captainVoteRepository, times(1)).save(any(CaptainVote.class));
    }

    @Test
    @DisplayName("chooseCaptain: студент инициирует голосование (без указания кандидата)")
    void chooseCaptain_StudentInitiatesVoting_ShouldActivateVoting() {
        Team teamWithUsers = Team.builder()
                .id(teamId)
                .task(task)
                .users(new HashSet<>(Set.of(student, anotherStudent)))
                .isAvailableRevote(true)
                .isCaptainVotingActive(false)
                .build();

        ChooseCaptainDto dto = ChooseCaptainDto.builder().teamId(teamId).captainId(null).build();

        when(tokenProvider.extractUserIdFromHeader(authHeader)).thenReturn(studentId);
        when(userRepository.findById(studentId)).thenReturn(Optional.of(student));
        when(teamRepository.findById(teamId)).thenReturn(Optional.of(teamWithUsers));
        when(teamRepository.save(any(Team.class))).thenReturn(teamWithUsers);
        when(teamMapper.toDto(any(Team.class))).thenReturn(TeamDto.builder().id(teamId).isCaptainVotingActive(true).build());

        TeamDto result = teamService.chooseCaptain(dto, authHeader);

        assertNotNull(result);
        assertTrue(result.getIsCaptainVotingActive());
        verify(captainVoteRepository, times(1)).deleteByTeamId(teamId);
    }

    @Test
    @DisplayName("chooseCaptain: перевыбор капитана запрещён")
    void chooseCaptain_RevoteNotAllowed_ShouldThrowBadRequest() {
        team.setCaptainId(anotherStudentId);
        team.setIsAvailableRevote(false);
        ChooseCaptainDto dto = ChooseCaptainDto.builder().teamId(teamId).captainId(studentId).build();

        when(tokenProvider.extractUserIdFromHeader(authHeader)).thenReturn(studentId);
        when(userRepository.findById(studentId)).thenReturn(Optional.of(student));
        when(teamRepository.findById(teamId)).thenReturn(Optional.of(team));

        assertThrows(ForbiddenException.class, () -> teamService.chooseCaptain(dto, authHeader));
    }

    @Test
    @DisplayName("getCaptain: возвращает капитана команды")
    void getCaptain_ShouldReturnCaptain() {
        team.setCaptainId(studentId);
        UserDto.FullInfo expectedDto = UserDto.FullInfo.builder().id(studentId).firstName("Student").lastName("One").build();

        when(teamRepository.findById(teamId)).thenReturn(Optional.of(team));
        when(userRepository.findById(studentId)).thenReturn(Optional.of(student));
        when(userMapper.toDto(student)).thenReturn(expectedDto);

        UserDto.FullInfo result = teamService.getCaptain(teamId);

        assertNotNull(result);
        assertEquals(studentId, result.getId());
    }

    @Test
    @DisplayName("getCaptain: капитана нет - возвращает null")
    void getCaptain_NoCaptain_ShouldReturnNull() {
        when(teamRepository.findById(teamId)).thenReturn(Optional.of(team));

        UserDto.FullInfo result = teamService.getCaptain(teamId);

        assertNull(result);
    }

    @Test
    @DisplayName("getCaptainVotes: студент - участник команды может видеть голоса")
    void getCaptainVotes_StudentInTeam_ShouldReturnVotes() {
        Team teamWithUsers = Team.builder()
                .id(teamId)
                .task(task)
                .users(new HashSet<>(Set.of(student, anotherStudent)))
                .isAvailableRevote(true)
                .isCaptainVotingActive(false)
                .build();

        CaptainVote vote = CaptainVote.builder()
                .id(UUID.randomUUID())
                .team(teamWithUsers)
                .voterId(studentId)
                .candidateId(anotherStudentId)
                .votedAt(Instant.now())
                .build();

        when(tokenProvider.extractUserIdFromHeader(authHeader)).thenReturn(studentId);
        when(userRepository.findById(studentId)).thenReturn(Optional.of(student));
        when(teamRepository.findById(teamId)).thenReturn(Optional.of(teamWithUsers));
        when(captainVoteRepository.findByTeamId(teamId)).thenReturn(List.of(vote));

        var result = teamService.getCaptainVotes(teamId, authHeader);

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("getCaptainVotes: посторонний пользователь не может видеть голоса")
    void getCaptainVotes_Outsider_ShouldThrowForbidden() {
        Long outsiderId = 999L;
        User outsider = User.builder().id(outsiderId).role(List.of(Role.STUDENT)).build();

        when(tokenProvider.extractUserIdFromHeader(authHeader)).thenReturn(outsiderId);
        when(userRepository.findById(outsiderId)).thenReturn(Optional.of(outsider));
        when(teamRepository.findById(teamId)).thenReturn(Optional.of(team));

        assertThrows(ForbiddenException.class, () -> teamService.getCaptainVotes(teamId, authHeader));
    }

    @Test
    @DisplayName("initCaptainVoting: студент инициирует голосование")
    void initCaptainVoting_ShouldActivateVoting() {
        Team teamWithUsers = Team.builder()
                .id(teamId)
                .task(task)
                .users(new HashSet<>(Set.of(student, anotherStudent)))
                .isAvailableRevote(true)
                .isCaptainVotingActive(false)
                .build();

        when(tokenProvider.extractUserIdFromHeader(authHeader)).thenReturn(studentId);
        when(userRepository.findById(studentId)).thenReturn(Optional.of(student));
        when(teamRepository.findById(teamId)).thenReturn(Optional.of(teamWithUsers));
        when(teamRepository.save(any(Team.class))).thenReturn(teamWithUsers);
        when(teamMapper.toDto(any(Team.class))).thenReturn(TeamDto.builder().id(teamId).isCaptainVotingActive(true).build());

        TeamDto result = teamService.initCaptainVoting(teamId, authHeader);

        assertNotNull(result);
        assertTrue(result.getIsCaptainVotingActive());
        verify(captainVoteRepository, times(1)).deleteByTeamId(teamId);
    }

    @Test
    @DisplayName("inviteToTeam: успешное приглашение")
    void inviteToTeam_ShouldSuccess() {
        Team teamWithUsers = Team.builder()
                .id(teamId)
                .task(task)
                .users(new HashSet<>(Set.of(student)))
                .build();
        task.setTeamType(TeamType.FREE);

        when(tokenProvider.extractUserIdFromHeader(authHeader)).thenReturn(studentId);
        when(teamRepository.findById(teamId)).thenReturn(Optional.of(teamWithUsers));
        when(userRepository.findById(anotherStudentId)).thenReturn(Optional.of(anotherStudent));
        when(teamRepository.existsByTask_IdAndUsers_IdAndIdNot(taskId, anotherStudentId, teamId))
                .thenReturn(false);
        when(inviteRepository.existsByTeamIdAndInviteeId(teamId, anotherStudentId))
                .thenReturn(false);
        when(inviteRepository.save(any(Invite.class))).thenReturn(new Invite());

        assertDoesNotThrow(() -> teamService.inviteToTeam(teamId, anotherStudentId, authHeader));
        verify(inviteRepository, times(1)).save(any(Invite.class));
    }

    @Test
    @DisplayName("inviteToTeam: не FREE-режим - ошибка")
    void inviteToTeam_NotFreeMode_ShouldThrowBadRequest() {
        task.setTeamType(TeamType.DRAFT);
        Team teamWithUsers = Team.builder().id(teamId).task(task).build();

        when(tokenProvider.extractUserIdFromHeader(authHeader)).thenReturn(studentId);
        when(teamRepository.findById(teamId)).thenReturn(Optional.of(teamWithUsers));

        assertThrows(BadRequestException.class,
                () -> teamService.inviteToTeam(teamId, anotherStudentId, authHeader));
    }

    @Test
    @DisplayName("inviteToTeam: приглашаемый уже в команде - ошибка")
    void inviteToTeam_InviteeAlreadyInTeam_ShouldThrowBadRequest() {
        Team teamWithUsers = Team.builder()
                .id(teamId)
                .task(task)
                .users(new HashSet<>(Set.of(student, anotherStudent)))
                .build();
        task.setTeamType(TeamType.FREE);

        when(tokenProvider.extractUserIdFromHeader(authHeader)).thenReturn(studentId);
        when(teamRepository.findById(teamId)).thenReturn(Optional.of(teamWithUsers));
        when(userRepository.findById(anotherStudentId)).thenReturn(Optional.of(anotherStudent));

        assertThrows(BadRequestException.class,
                () -> teamService.inviteToTeam(teamId, anotherStudentId, authHeader));
    }

    @Test
    @DisplayName("inviteToTeam: приглашаемый в другой команде - ошибка")
    void inviteToTeam_InviteeInAnotherTeam_ShouldThrowBadRequest() {
        Team teamWithUsers = Team.builder()
                .id(teamId)
                .task(task)
                .users(new HashSet<>(Set.of(student)))
                .build();
        task.setTeamType(TeamType.FREE);

        when(tokenProvider.extractUserIdFromHeader(authHeader)).thenReturn(studentId);
        when(teamRepository.findById(teamId)).thenReturn(Optional.of(teamWithUsers));
        when(userRepository.findById(anotherStudentId)).thenReturn(Optional.of(anotherStudent));
        when(teamRepository.existsByTask_IdAndUsers_IdAndIdNot(taskId, anotherStudentId, teamId))
                .thenReturn(true);

        assertThrows(BadRequestException.class,
                () -> teamService.inviteToTeam(teamId, anotherStudentId, authHeader));
    }

    @Test
    @DisplayName("inviteToTeam: приглашение уже отправлено - ошибка")
    void inviteToTeam_InviteAlreadyExists_ShouldThrowBadRequest() {
        Team teamWithUsers = Team.builder()
                .id(teamId)
                .task(task)
                .users(new HashSet<>(Set.of(student)))
                .build();
        task.setTeamType(TeamType.FREE);

        when(tokenProvider.extractUserIdFromHeader(authHeader)).thenReturn(studentId);
        when(teamRepository.findById(teamId)).thenReturn(Optional.of(teamWithUsers));
        when(userRepository.findById(anotherStudentId)).thenReturn(Optional.of(anotherStudent));
        when(teamRepository.existsByTask_IdAndUsers_IdAndIdNot(taskId, anotherStudentId, teamId))
                .thenReturn(false);
        when(inviteRepository.existsByTeamIdAndInviteeId(teamId, anotherStudentId))
                .thenReturn(true);

        assertThrows(BadRequestException.class,
                () -> teamService.inviteToTeam(teamId, anotherStudentId, authHeader));
    }

        @Test
        @DisplayName("getAvailableInvitees: возвращает отсортированный список доступных студентов")
        void getAvailableInvitees_ShouldReturnSortedAndFilteredStudents() {
        Long availableAId = 401L;
        Long availableBId = 402L;
        Long inAnotherTeamId = 403L;
        Long alreadyInvitedId = 404L;

        User availableA = User.builder()
            .id(availableAId)
            .firstName("Boris")
            .lastName("One")
            .email("boris@test.ru")
            .role(List.of(Role.STUDENT))
            .build();

        User availableB = User.builder()
            .id(availableBId)
            .firstName("Anna")
            .lastName("Two")
            .email("anna@test.ru")
            .role(List.of(Role.STUDENT))
            .build();

        User inAnotherTeam = User.builder()
            .id(inAnotherTeamId)
            .firstName("Chris")
            .lastName("Three")
            .email("chris@test.ru")
            .role(List.of(Role.STUDENT))
            .build();

        User alreadyInvited = User.builder()
            .id(alreadyInvitedId)
            .firstName("Den")
            .lastName("Four")
            .email("den@test.ru")
            .role(List.of(Role.STUDENT))
            .build();

        task.getChannel().getUsers().add(availableA);
        task.getChannel().getUsers().add(availableB);
        task.getChannel().getUsers().add(inAnotherTeam);
        task.getChannel().getUsers().add(alreadyInvited);

        Team teamWithUsers = Team.builder()
            .id(teamId)
            .task(task)
            .users(new HashSet<>(Set.of(student)))
            .build();
        task.setTeamType(TeamType.FREE);

        when(tokenProvider.extractUserIdFromHeader(authHeader)).thenReturn(studentId);
        when(teamRepository.findById(teamId)).thenReturn(Optional.of(teamWithUsers));

        when(teamRepository.existsByTask_IdAndUsers_IdAndIdNot(taskId, availableAId, teamId)).thenReturn(false);
        when(teamRepository.existsByTask_IdAndUsers_IdAndIdNot(taskId, availableBId, teamId)).thenReturn(false);
        when(teamRepository.existsByTask_IdAndUsers_IdAndIdNot(taskId, inAnotherTeamId, teamId)).thenReturn(true);
        when(teamRepository.existsByTask_IdAndUsers_IdAndIdNot(taskId, alreadyInvitedId, teamId)).thenReturn(false);

        when(inviteRepository.existsByTeamIdAndInviteeId(teamId, availableAId)).thenReturn(false);
        when(inviteRepository.existsByTeamIdAndInviteeId(teamId, availableBId)).thenReturn(false);
        when(inviteRepository.existsByTeamIdAndInviteeId(teamId, alreadyInvitedId)).thenReturn(true);

        List<UserShortDto> result = teamService.getAvailableInvitees(teamId, authHeader);

        assertEquals(2, result.size());
        assertEquals(availableBId, result.get(0).getId());
        assertEquals(availableAId, result.get(1).getId());
        }

    @Test
    @DisplayName("acceptInvite: успешное принятие приглашения")
    void acceptInvite_ShouldSuccess() {
        UUID inviteId = UUID.randomUUID();
        Team teamWithUsers = Team.builder()
                .id(teamId)
                .task(task)
                .users(new HashSet<>(Set.of(student)))
                .build();
        task.setTeamType(TeamType.FREE);

        Invite invite = Invite.builder()
                .id(inviteId)
                .team(teamWithUsers)
                .inviterId(studentId)
                .inviteeId(anotherStudentId)
                .build();

        when(tokenProvider.extractUserIdFromHeader(authHeader)).thenReturn(anotherStudentId);
        when(inviteRepository.findById(inviteId)).thenReturn(Optional.of(invite));
        when(teamRepository.existsByTask_IdAndUsers_IdAndIdNot(taskId, anotherStudentId, teamId))
                .thenReturn(false);
        when(userRepository.findById(anotherStudentId)).thenReturn(Optional.of(anotherStudent));
        when(teamRepository.save(any(Team.class))).thenReturn(teamWithUsers);
        when(teamMapper.toDto(any(Team.class))).thenReturn(TeamDto.builder().id(teamId).build());

        TeamDto result = teamService.acceptInvite(inviteId, authHeader);

        assertNotNull(result);
        verify(inviteRepository, times(1)).delete(invite);
    }

    @Test
    @DisplayName("acceptInvite: уже в команде - ошибка")
    void acceptInvite_AlreadyInTeam_ShouldThrowBadRequest() {
        UUID inviteId = UUID.randomUUID();
        Team teamWithUsers = Team.builder()
                .id(teamId)
                .task(task)
                .users(new HashSet<>(Set.of(anotherStudent)))
                .build();
        task.setTeamType(TeamType.FREE);

        Invite invite = Invite.builder()
                .id(inviteId)
                .team(teamWithUsers)
                .inviteeId(anotherStudentId)
                .build();

        when(tokenProvider.extractUserIdFromHeader(authHeader)).thenReturn(anotherStudentId);
        when(inviteRepository.findById(inviteId)).thenReturn(Optional.of(invite));

        assertThrows(BadRequestException.class,
                () -> teamService.acceptInvite(inviteId, authHeader));
    }

    @Test
    @DisplayName("rejectInvite: успешное отклонение")
    void rejectInvite_ShouldSuccess() {
        UUID inviteId = UUID.randomUUID();
        Invite invite = Invite.builder()
                .id(inviteId)
                .inviteeId(studentId)
                .build();

        when(tokenProvider.extractUserIdFromHeader(authHeader)).thenReturn(studentId);
        when(inviteRepository.findById(inviteId)).thenReturn(Optional.of(invite));

        assertDoesNotThrow(() -> teamService.rejectInvite(inviteId, authHeader));
        verify(inviteRepository, times(1)).delete(invite);
    }

    @Test
    @DisplayName("getMyInvites: получение моих приглашений")
    void getMyInvites_ShouldReturnMyInvites() {
        Team team1 = Team.builder().id(UUID.randomUUID()).name("Team 1").build();
        Long inviterId = 500L;
        User inviter = User.builder().id(inviterId).firstName("Inviter").lastName("User").build();

        Invite invite1 = Invite.builder()
                .id(UUID.randomUUID())
                .team(team1)
                .inviteeId(studentId)
                .inviterId(inviterId)
                .build();

        when(tokenProvider.extractUserIdFromHeader(authHeader)).thenReturn(studentId);
        when(inviteRepository.findByInviteeId(studentId)).thenReturn(List.of(invite1));
        when(userRepository.findById(inviterId)).thenReturn(Optional.of(inviter));

        List<InviteDto> result = teamService.getMyInvites(authHeader);

        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("getMyInvites: нет приглашений - пустой список")
    void getMyInvites_NoInvites_ShouldReturnEmptyList() {
        when(tokenProvider.extractUserIdFromHeader(authHeader)).thenReturn(studentId);
        when(inviteRepository.findByInviteeId(studentId)).thenReturn(List.of());

        List<InviteDto> result = teamService.getMyInvites(authHeader);

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("setTeamMark: преподаватель может выставить оценку")
    void setTeamMark_ShouldSuccess() {
        UUID teamId = UUID.randomUUID();
        Float mark = 4.5f;

        Team team = Team.builder()
                .id(teamId)
                .task(task)
                .users(new HashSet<>(Set.of(student)))
                .build();

        when(tokenProvider.extractUserIdFromHeader(authHeader)).thenReturn(teacherId);
        when(userRepository.findById(teacherId)).thenReturn(Optional.of(teacher));
        when(teamRepository.findById(teamId)).thenReturn(Optional.of(team));
        when(teamRepository.save(any(Team.class))).thenReturn(team);
        when(teamMapper.toDto(any(Team.class))).thenReturn(TeamDto.builder().id(teamId).mark(mark).build());

        TeamDto result = teamService.setTeamMark(teamId, mark, authHeader);

        assertNotNull(result);
        assertEquals(mark, result.getMark());
        verify(teamRepository).save(team);
    }

    @Test
    @DisplayName("setTeamMark: не преподаватель не может выставить оценку")
    void setTeamMark_NotTeacher_ShouldThrowForbidden() {
        UUID teamId = UUID.randomUUID();
        Float mark = 4.5f;

        Team team = Team.builder().id(teamId).task(task).build();

        when(tokenProvider.extractUserIdFromHeader(authHeader)).thenReturn(studentId);
        when(userRepository.findById(studentId)).thenReturn(Optional.of(student));
        when(teamRepository.findById(teamId)).thenReturn(Optional.of(team));

        assertThrows(ForbiddenException.class, () -> teamService.setTeamMark(teamId, mark, authHeader));
    }

    @Test
    @DisplayName("setTeamMark: оценка вне диапазона вызывает ошибку")
    void setTeamMark_InvalidMark_ShouldThrowBadRequest() {
        UUID teamId = UUID.randomUUID();

        when(tokenProvider.extractUserIdFromHeader(authHeader)).thenReturn(teacherId);
        when(userRepository.findById(teacherId)).thenReturn(Optional.of(teacher));
        when(teamRepository.findById(teamId)).thenReturn(Optional.of(Team.builder().id(teamId).task(task).build()));

        assertThrows(BadRequestException.class, () -> teamService.setTeamMark(teamId, 0f, authHeader));
        assertThrows(BadRequestException.class, () -> teamService.setTeamMark(teamId, 6f, authHeader));
    }

    @Test
    @DisplayName("getTeamMark: студент может получить оценку своей команды")
    void getTeamMark_ShouldReturnMark() {
        UUID teamId = UUID.randomUUID();
        Float expectedMark = 4.5f;

        Channel testChannel = new Channel(
                UUID.randomUUID(),
                "Channel",
                "Desc",
                null,
                new HashSet<>(Set.of(student, teacher)),
                teacher
        );

        Task testTask = Task.builder()
                .id(taskId)
                .channel(testChannel)
                .build();

        Team team = Team.builder()
                .id(teamId)
                .task(testTask)
                .mark(expectedMark)
                .users(new HashSet<>(Set.of(student)))
                .build();

        when(tokenProvider.extractUserIdFromHeader(authHeader)).thenReturn(studentId);
        when(teamRepository.findById(teamId)).thenReturn(Optional.of(team));

        Float result = teamService.getTeamMark(teamId, authHeader);

        assertEquals(expectedMark, result);
    }
}
