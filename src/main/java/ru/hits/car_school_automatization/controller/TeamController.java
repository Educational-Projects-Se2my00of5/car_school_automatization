package ru.hits.car_school_automatization.controller;

import io.swagger.v3.oas.annotations.Operation;
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
    @Operation(summary = "Создание команды")
    public TeamDto createTeam(
            @Valid @RequestBody CreateTeamDto dto,
            @RequestHeader("Authorization") String authHeader) {
        return teamService.createTeam(dto, authHeader);
    }

    @PatchMapping("/{teamId}")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Обновление команды")
    public TeamDto updateTeam(
            @PathVariable UUID teamId,
            @RequestBody UpdateTeamDto dto,
            @RequestHeader("Authorization") String authHeader) {
        return teamService.updateTeam(teamId, dto, authHeader);
    }

    @GetMapping("/{teamId}")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Получение команды по id")
    public TeamDto getTeam(@PathVariable UUID teamId) {
        return teamService.getTeam(teamId);
    }

    @GetMapping("/task/{taskId}")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Получение всех команд задания")
    public List<TeamDto> getTaskTeams(@PathVariable UUID taskId) {
        return teamService.getTaskTeams(taskId);
    }

    @PostMapping("/{teamId}/members/{userId}")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Добавить участника в команду")
    public TeamDto addMember(
            @PathVariable UUID teamId,
            @PathVariable Long userId,
            @RequestHeader("Authorization") String authHeader) {
        return teamService.addMember(teamId, userId, authHeader);
    }

    @DeleteMapping("/{teamId}/members/{userId}")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Удалить участника из команды")
    public TeamDto removeMember(
            @PathVariable UUID teamId,
            @PathVariable Long userId,
            @RequestHeader("Authorization") String authHeader) {
        return teamService.removeMember(teamId, userId, authHeader);
    }

    @PostMapping("/{teamId}/join")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Студент вступает в команду")
    public TeamDto joinTeam(
            @PathVariable UUID teamId,
            @RequestHeader("Authorization") String authHeader) {
        return teamService.joinTeam(teamId, authHeader);
    }

    @DeleteMapping("/{teamId}/leave")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Студент выходит из команды")
    public TeamDto leaveTeam(
            @PathVariable UUID teamId,
            @RequestHeader("Authorization") String authHeader) {
        return teamService.leaveTeam(teamId, authHeader);
    }

    @DeleteMapping("/{teamId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Удаление команды")
    public void deleteTeam(
            @PathVariable UUID teamId,
            @RequestHeader("Authorization") String authHeader) {
        teamService.deleteTeam(teamId, authHeader);
    }
}
