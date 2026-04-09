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
import ru.hits.car_school_automatization.service.SolutionService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/solutions")
@RequiredArgsConstructor
@Tag(name = "Решения заданий", description = "Управление решениями студентов и их оценками")
public class SolutionController {

    private final SolutionService solutionService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Отправить решение на задание")
    public SolutionDto submitSolution(
            @Valid @ModelAttribute SubmitSolutionDto submitDto,
            @Parameter(hidden = true) @RequestHeader("Authorization") String authHeader) {
        return solutionService.submitSolution(submitDto, authHeader);
    }

    @PutMapping(value = "/{solutionId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Обновить своё решение (пока не оценено)")
    public SolutionDto updateSolution(
            @PathVariable UUID solutionId,
            @Valid @ModelAttribute UpdateSolutionDto updateDto,
            @Parameter(hidden = true) @RequestHeader("Authorization") String authHeader) {
        return solutionService.updateSolution(solutionId, updateDto, authHeader);
    }

    @PostMapping("/grade")
    @Operation(summary = "Оценить решение (для преподавателя)")
    public SolutionDto gradeSolution(
            @Valid @RequestBody GradeSolutionDto gradeDto,
            @Parameter(hidden = true) @RequestHeader("Authorization") String authHeader) {
        return solutionService.gradeSolution(gradeDto, authHeader);
    }

    @GetMapping("/{solutionId}")
    @Operation(summary = "Получить решение по ID")
    public SolutionDto getSolutionById(
            @PathVariable UUID solutionId,
            @Parameter(hidden = true) @RequestHeader("Authorization") String authHeader) {
        return solutionService.getSolutionById(solutionId, authHeader);
    }

    @GetMapping("/student/{studentId}")
    @Operation(summary = "Получить все решения студента")
    public List<SolutionDto> getStudentSolutions(
            @PathVariable Long studentId,
            @Parameter(hidden = true) @RequestHeader("Authorization") String authHeader) {
        return solutionService.getStudentSolutions(studentId, authHeader);
    }

    @GetMapping("/task/{taskId}")
    @Operation(summary = "Получить все решения по заданию (для преподавателя)")
    public List<SolutionDto> getTaskSolutions(
            @PathVariable UUID taskId,
            @Parameter(hidden = true) @RequestHeader("Authorization") String authHeader) {
        return solutionService.getTaskSolutions(taskId, authHeader);
    }

    @GetMapping("/task/{taskId}/ungraded")
    @Operation(summary = "Получить неоценённые решения по заданию (для преподавателя)")
    public List<SolutionDto> getUngradedSolutions(
            @PathVariable UUID taskId,
            @Parameter(hidden = true) @RequestHeader("Authorization") String authHeader) {
        return solutionService.getUngradedSolutions(taskId, authHeader);
    }

    @GetMapping("/student/{studentId}/channel/{channelId}/tasks")
    @Operation(summary = "Получить задачи студента с решениями в канале")
    public List<TaskWithSolutionDto> getStudentTasksWithSolutions(
            @PathVariable Long studentId,
            @PathVariable UUID channelId,
            @Parameter(hidden = true) @RequestHeader("Authorization") String authHeader) {
        return solutionService.getStudentTasksWithSolutions(studentId, channelId, authHeader);
    }

    @DeleteMapping("/{solutionId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Удалить решение (студент может удалить только неоценённое, админ - любое)")
    public void deleteSolution(
            @PathVariable UUID solutionId,
            @Parameter(hidden = true) @RequestHeader("Authorization") String authHeader) {
        solutionService.deleteSolution(solutionId, authHeader);
    }
}