package ru.hits.car_school_automatization.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.hits.car_school_automatization.dto.CreateTeamDto;
import ru.hits.car_school_automatization.dto.TeamDto;
import ru.hits.car_school_automatization.dto.UpdateTeamDto;
import ru.hits.car_school_automatization.service.TeamService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/teams")
@RequiredArgsConstructor
@Tag(name = "Команды", description = "Управление командами для заданий")
public class TeamController {

    private final TeamService teamService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Создание команды (TEACHER)")
    public TeamDto createTeam(
            @Valid @RequestBody CreateTeamDto dto,
            @Parameter(hidden = true) @RequestHeader("Authorization") String authHeader) {
        return teamService.createTeam(dto, authHeader);
    }

    @PatchMapping("/{teamId}")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Обновление команды (TEACHER)")
    public TeamDto updateTeam(
            @PathVariable UUID teamId,
            @RequestBody UpdateTeamDto dto,
            @Parameter(hidden = true) @RequestHeader("Authorization") String authHeader) {
        return teamService.updateTeam(teamId, dto, authHeader);
    }

    @GetMapping("/{teamId}")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Получение команды по id (Any)")
    public TeamDto getTeam(@PathVariable UUID teamId) {
        return teamService.getTeam(teamId);
    }

    @GetMapping("/task/{taskId}")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Получение всех команд задания (Any)")
    public List<TeamDto> getTaskTeams(@PathVariable UUID taskId) {
        return teamService.getTaskTeams(taskId);
    }

    @PostMapping("/{teamId}/members/{userId}")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Добавить участника в команду (TEACHER)")
    public TeamDto addMember(
            @PathVariable UUID teamId,
            @PathVariable Long userId,
            @Parameter(hidden = true) @RequestHeader("Authorization") String authHeader) {
        return teamService.addMember(teamId, userId, authHeader);
    }

    @DeleteMapping("/{teamId}/members/{userId}")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Удалить участника из команды (TEACHER)")
    public TeamDto removeMember(
            @PathVariable UUID teamId,
            @PathVariable Long userId,
            @Parameter(hidden = true) @RequestHeader("Authorization") String authHeader) {
        return teamService.removeMember(teamId, userId, authHeader);
    }

    @PostMapping("/{teamId}/join")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Студент вступает в команду (STUDENT)")
    public TeamDto joinTeam(
            @PathVariable UUID teamId,
            @Parameter(hidden = true) @RequestHeader("Authorization") String authHeader) {
        return teamService.joinTeam(teamId, authHeader);
    }

    @DeleteMapping("/{teamId}/leave")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Студент выходит из команды (STUDENT)")
    public TeamDto leaveTeam(
            @PathVariable UUID teamId,
            @Parameter(hidden = true) @RequestHeader("Authorization") String authHeader) {
        return teamService.leaveTeam(teamId, authHeader);
    }

    @DeleteMapping("/{teamId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Удаление команды (TEACHER)")
    public void deleteTeam(
            @PathVariable UUID teamId,
            @Parameter(hidden = true) @RequestHeader("Authorization") String authHeader) {
        teamService.deleteTeam(teamId, authHeader);
    }

    @PostMapping("/{teamId}/invite/{userId}")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Пригласить студента в команду")
    public void inviteToTeam(
            @PathVariable UUID teamId,
            @PathVariable Long userId,
            @Parameter(hidden = true) @RequestHeader("Authorization") String authHeader) {
        teamService.inviteToTeam(teamId, userId, authHeader);
    }

    @PostMapping("/invites/{inviteId}/accept")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Принять приглашение")
    public TeamDto acceptInvite(
            @PathVariable UUID inviteId,
            @Parameter(hidden = true) @RequestHeader("Authorization") String authHeader) {
        return teamService.acceptInvite(inviteId, authHeader);
    }

    @DeleteMapping("/invites/{inviteId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Отклонить приглашение")
    public void rejectInvite(
            @PathVariable UUID inviteId,
            @Parameter(hidden = true) @RequestHeader("Authorization") String authHeader) {
        teamService.rejectInvite(inviteId, authHeader);
    }

    @GetMapping("/invites/my")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Получить мои приглашения")
    public List<TeamDto> getMyInvites(
            @Parameter(hidden = true) @RequestHeader("Authorization") String authHeader) {
        return teamService.getMyInvites(authHeader);
    }
}
