package ru.hits.car_school_automatization.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.hits.car_school_automatization.dto.CreateTaskDto;
import ru.hits.car_school_automatization.entity.Task;
import ru.hits.car_school_automatization.entity.Team;
import ru.hits.car_school_automatization.entity.User;
import ru.hits.car_school_automatization.enums.Role;
import ru.hits.car_school_automatization.enums.TaskType;
import ru.hits.car_school_automatization.enums.TeamType;
import ru.hits.car_school_automatization.exception.BadRequestException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TeamFormationServiceTest {

    private final TeamFormationService service = new TeamFormationService();

    @Test
    @DisplayName("RANDOM: freeTeamCount задает количество команд")
    void formByTeamType_Random_UsesFreeTeamCount() {
        Task task = task(TeamType.RANDOM, 2);
        CreateTaskDto dto = baseDto(TeamType.RANDOM);
        dto.setFreeTeamCount(2);

        List<User> channelUsers = List.of(student(1L), student(2L), student(3L), student(4L));

        List<Team> teams = service.formByTeamType(task, channelUsers, dto);

        assertEquals(2, teams.size());
        assertTrue(teams.stream().allMatch(t -> t.getUsers().size() == 2));
    }

    @Test
    @DisplayName("RANDOM: freeTeamCount обязателен")
    void formByTeamType_Random_WithoutFreeTeamCount_Throws() {
        Task task = task(TeamType.RANDOM, 2);
        CreateTaskDto dto = baseDto(TeamType.RANDOM);
        dto.setFreeTeamCount(null);

        assertThrows(BadRequestException.class, () -> service.formByTeamType(task, List.of(student(1L)), dto));
    }

    @Test
    @DisplayName("DRAFT: пустой список капитанов вызывает BadRequest")
    void formByTeamType_Draft_EmptyCaptains_Throws() {
        Task task = task(TeamType.DRAFT, 2);
        CreateTaskDto dto = baseDto(TeamType.DRAFT);
        dto.setFreeTeamCount(1);
        dto.setDraftCaptainIds(List.of());

        assertThrows(BadRequestException.class, () -> service.formByTeamType(task, List.of(student(1L)), dto));
    }

    @Test
    @DisplayName("DRAFT: дубликаты капитанов вызывают BadRequest")
    void formByTeamType_Draft_Duplicates_Throws() {
        Task task = task(TeamType.DRAFT, 2);
        CreateTaskDto dto = baseDto(TeamType.DRAFT);
        dto.setFreeTeamCount(2);
        dto.setDraftCaptainIds(List.of(1L, 1L));

        assertThrows(BadRequestException.class, () -> service.formByTeamType(task, List.of(student(1L), student(2L)), dto));
    }

    @Test
    @DisplayName("DRAFT: капитаны должны быть студентами канала")
    void formByTeamType_Draft_CaptainNotStudent_Throws() {
        Task task = task(TeamType.DRAFT, 2);
        CreateTaskDto dto = baseDto(TeamType.DRAFT);
        dto.setFreeTeamCount(1);
        dto.setDraftCaptainIds(List.of(1L));

        List<User> channelUsers = List.of(teacher(1L), student(2L), student(3L));

        assertThrows(BadRequestException.class, () -> service.formByTeamType(task, channelUsers, dto));
    }

    @Test
    @DisplayName("DRAFT: проверка minTeamSize через среднее вызывает BadRequest")
    void formByTeamType_Draft_AverageBelowMinTeamSize_Throws() {
        Task task = task(TeamType.DRAFT, 3);
        CreateTaskDto dto = baseDto(TeamType.DRAFT);
        dto.setFreeTeamCount(2);
        dto.setDraftCaptainIds(List.of(1L, 2L));

        List<User> channelUsers = List.of(student(1L), student(2L), student(3L), student(4L), student(5L));

        assertThrows(BadRequestException.class, () -> service.formByTeamType(task, channelUsers, dto));
    }

    @Test
    @DisplayName("DRAFT: количество капитанов должно совпадать с freeTeamCount")
    void formByTeamType_Draft_CaptainsCountMustMatchFreeTeamCount() {
        Task task = task(TeamType.DRAFT, 2);
        CreateTaskDto dto = baseDto(TeamType.DRAFT);
        dto.setFreeTeamCount(3);
        dto.setDraftCaptainIds(List.of(1L, 2L));

        List<User> channelUsers = List.of(student(1L), student(2L), student(3L), student(4L), student(5L), student(6L));

        assertThrows(BadRequestException.class, () -> service.formByTeamType(task, channelUsers, dto));
    }

    @Test
    @DisplayName("DRAFT: создаются команды по числу капитанов с одним участником-капитаном")
    void formByTeamType_Draft_Success() {
        Task task = task(TeamType.DRAFT, 2);
        CreateTaskDto dto = baseDto(TeamType.DRAFT);
        dto.setFreeTeamCount(2);
        dto.setDraftCaptainIds(List.of(1L, 2L));

        List<User> channelUsers = List.of(student(1L), student(2L), student(3L), student(4L));

        List<Team> teams = service.formByTeamType(task, channelUsers, dto);

        assertEquals(2, teams.size());
        assertTrue(teams.stream().allMatch(t -> t.getUsers().size() == 1));
        assertTrue(teams.stream().allMatch(t -> t.getCaptainId() != null));
    }

    @Test
    @DisplayName("FREE: freeTeamCount определяет число команд")
    void formByTeamType_Free_UsesProvidedTeamCount() {
        Task task = task(TeamType.FREE, 2);
        CreateTaskDto dto = baseDto(TeamType.FREE);
        dto.setFreeTeamCount(2);

        List<Team> teams = service.formByTeamType(task, List.of(student(1L), student(2L), student(3L), student(4L)), dto);

        assertEquals(2, teams.size());
    }

    @Test
    @DisplayName("FREE: проверка minTeamSize через среднее вызывает BadRequest")
    void formByTeamType_Free_AverageBelowMinTeamSize_Throws() {
        Task task = task(TeamType.FREE, 3);
        CreateTaskDto dto = baseDto(TeamType.FREE);
        dto.setFreeTeamCount(2);

        assertThrows(BadRequestException.class, () -> service.formByTeamType(task, List.of(student(1L), student(2L), student(3L), student(4L), student(5L)), dto));
    }

    @Test
    @DisplayName("RANDOM: проверка minTeamSize через среднее вызывает BadRequest")
    void formByTeamType_Random_AverageBelowMinTeamSize_Throws() {
        Task task = task(TeamType.RANDOM, 3);
        CreateTaskDto dto = baseDto(TeamType.RANDOM);
        dto.setFreeTeamCount(2);

        assertThrows(BadRequestException.class, () -> service.formByTeamType(task, List.of(student(1L), student(2L), student(3L), student(4L), student(5L)), dto));
    }

    private static CreateTaskDto baseDto(TeamType teamType) {
        return CreateTaskDto.builder()
                .label("Task")
                .text("Text")
                .teamType(teamType)
                .type(TaskType.FREE)
                .minTeamSize(2)
                .freeTeamCount(1)
                .isCanRedistribute(false)
                .build();
    }

    private static Task task(TeamType teamType, int minTeamSize) {
        return Task.builder()
                .label("Task")
                .text("Text")
                .teamType(teamType)
                .type(TaskType.FREE)
                .minTeamSize(minTeamSize)
                .isCanRedistribute(false)
                .build();
    }

    private static User student(Long id) {
        return User.builder().id(id).role(List.of(Role.STUDENT)).build();
    }

    private static User teacher(Long id) {
        return User.builder().id(id).role(List.of(Role.TEACHER)).build();
    }
}
