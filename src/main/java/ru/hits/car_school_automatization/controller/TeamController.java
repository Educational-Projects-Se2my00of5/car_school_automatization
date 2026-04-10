package ru.hits.car_school_automatization.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.hits.car_school_automatization.dto.*;
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

    @PostMapping("/choose-captain")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Выбор/голосование за капитана (TEACHER назначает, STUDENT голосует или инициирует)")
    public TeamDto chooseCaptain(
            @RequestBody ChooseCaptainDto dto,
            @Parameter(hidden = true) @RequestHeader("Authorization") String authHeader) {
        return teamService.chooseCaptain(dto, authHeader);
    }

    @PostMapping("/{teamId}/init-captain-voting")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Инициировать голосование за капитана (STUDENT)")
    public TeamDto initCaptainVoting(
            @PathVariable UUID teamId,
            @Parameter(hidden = true) @RequestHeader("Authorization") String authHeader) {
        return teamService.initCaptainVoting(teamId, authHeader);
    }

    @GetMapping("/{teamId}/captain")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Получить текущего капитана команды")
    public UserDto.FullInfo getCaptain(@PathVariable UUID teamId) {
        return teamService.getCaptain(teamId);
    }

    @GetMapping("/{teamId}/captain-votes")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Получить результаты голосования за капитана")
    public List<CaptainVoteDto> getCaptainVotes(
            @PathVariable UUID teamId,
            @Parameter(hidden = true) @RequestHeader("Authorization") String authHeader) {
        return teamService.getCaptainVotes(teamId, authHeader);
    }
}
