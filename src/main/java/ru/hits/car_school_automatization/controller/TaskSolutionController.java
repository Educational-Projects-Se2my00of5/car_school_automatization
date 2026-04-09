package ru.hits.car_school_automatization.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import ru.hits.car_school_automatization.dto.CreateTaskSolutionDto;
import ru.hits.car_school_automatization.dto.TaskSolutionDto;
import ru.hits.car_school_automatization.dto.UpdateTaskSolutionDto;
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
}
