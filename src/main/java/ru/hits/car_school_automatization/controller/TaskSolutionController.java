package ru.hits.car_school_automatization.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import ru.hits.car_school_automatization.dto.*;
import ru.hits.car_school_automatization.service.TaskSolutionService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/task-solutions")
@RequiredArgsConstructor
@Tag(name = "TaskSolution", description = "CRUD для решений командных заданий")
public class TaskSolutionController {

    private final TaskSolutionService taskSolutionService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Создать решение по заданию")
    public TaskSolutionDto create(
            @RequestParam UUID taskId,
            @Valid @ModelAttribute CreateTaskSolutionDto dto,
            @Parameter(hidden = true) @RequestHeader("Authorization") String authHeader
    ) {
        return taskSolutionService.create(taskId, dto, authHeader);
    }

    @GetMapping("/{solutionId}")
    @Operation(summary = "Получить решение по id")
    public TaskSolutionDto getById(
            @PathVariable UUID solutionId,
            @Parameter(hidden = true) @RequestHeader("Authorization") String authHeader
    ) {
        return taskSolutionService.getById(solutionId, authHeader);
    }

    @GetMapping("/task/{taskId}")
    @Operation(summary = "Получить все решения по taskId")
    public List<TaskSolutionDto> getByTask(
            @PathVariable UUID taskId,
            @Parameter(hidden = true) @RequestHeader("Authorization") String authHeader
    ) {
        return taskSolutionService.getByTask(taskId, authHeader);
    }

    @GetMapping("/my")
    @Operation(summary = "Получить мои решения")
    public List<TaskSolutionDto> getMySolutions(
            @Parameter(hidden = true) @RequestHeader("Authorization") String authHeader
    ) {
        return taskSolutionService.getMySolutions(authHeader);
    }

    @PatchMapping(value = "/{solutionId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Обновить решение")
    public TaskSolutionDto update(
            @PathVariable UUID solutionId,
            @Valid @ModelAttribute UpdateTaskSolutionDto dto,
            @Parameter(hidden = true) @RequestHeader("Authorization") String authHeader
    ) {
        return taskSolutionService.update(solutionId, dto, authHeader);
    }

    @DeleteMapping("/{solutionId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Удалить решение")
    public void delete(
            @PathVariable UUID solutionId,
            @Parameter(hidden = true) @RequestHeader("Authorization") String authHeader
    ) {
        taskSolutionService.delete(solutionId, authHeader);
    }

    @PostMapping("/vote")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Проголосовать за решение")
    public SolutionVoteDto vote(
            @RequestBody @Valid CreateSolutionVoteDto dto,
            @Parameter(hidden = true) @RequestHeader("Authorization") String authHeader) {
        return taskSolutionService.vote(dto, authHeader);
    }

    @GetMapping("/vote/my/{taskId}")
    @Operation(summary = "Получить мой голос по заданию")
    public SolutionVoteDto getMyVote(
            @PathVariable UUID taskId,
            @Parameter(hidden = true) @RequestHeader("Authorization") String authHeader) {
        return taskSolutionService.getMyVote(taskId, authHeader);
    }

    @DeleteMapping("/vote/{taskId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Отменить мой голос")
    public void cancelVote(
            @PathVariable UUID taskId,
            @Parameter(hidden = true) @RequestHeader("Authorization") String authHeader) {
        taskSolutionService.cancelVote(taskId, authHeader);
    }

    @GetMapping("/{taskId}/team/{teamId}/voting-results")
    @Operation(summary = "Получить результаты голосования для команды")
    public VotingResultsDto getVotingResults(
            @PathVariable UUID taskId,
            @PathVariable UUID teamId,
            @Parameter(hidden = true) @RequestHeader("Authorization") String authHeader) {
        return taskSolutionService.getVotingResults(taskId, teamId, authHeader);
    }

    @GetMapping("/{taskId}/team/{teamId}/selected-solution")
    @Operation(summary = "Получить выбранное решение для команды")
    public TaskSolutionDto getSelectedSolution(
            @PathVariable UUID taskId,
            @PathVariable UUID teamId,
            @Parameter(hidden = true) @RequestHeader("Authorization") String authHeader) {
        return taskSolutionService.getSelectedSolution(taskId, teamId, authHeader);
    }

    @GetMapping("/{taskId}/selected-solutions")
    @Operation(summary = "Получить выбранные решения для всех команд задания (TEACHER)")
    public List<TaskSolutionDto> getAllSelectedSolutions(
            @PathVariable UUID taskId,
            @Parameter(hidden = true) @RequestHeader("Authorization") String authHeader) {
        return taskSolutionService.getAllSelectedSolutions(taskId, authHeader);
    }
}
