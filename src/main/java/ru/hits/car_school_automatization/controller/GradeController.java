package ru.hits.car_school_automatization.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import ru.hits.car_school_automatization.dto.GradeDto;
import ru.hits.car_school_automatization.service.GradeService;

import java.util.UUID;

@RestController
@RequestMapping("/api/grades")
@RequiredArgsConstructor
@Tag(name = "Оценки", description = "Расчет оценок по критериям")
public class GradeController {

    private final GradeService gradeService;

    @GetMapping("/post/{postId}")
    @Operation(summary = "Получить оценку за пост (для текущего пользователя)")
    public GradeDto getPostGrade(
            @PathVariable UUID postId,
            @Parameter(hidden = true) @RequestHeader("Authorization") String authHeader) {
        return GradeDto.builder()
                .targetId(postId)
                .value(gradeService.getPostGrade(postId, null, authHeader))
                .build();
    }

    @GetMapping("/post/{postId}/user/{userId}")
    @Operation(summary = "Получить оценку за пост для пользователя")
    public GradeDto getPostGradeForUser(
            @PathVariable UUID postId,
            @PathVariable Long userId,
            @Parameter(hidden = true) @RequestHeader("Authorization") String authHeader) {
        return GradeDto.builder()
                .targetId(postId)
                .value(gradeService.getPostGrade(postId, userId, authHeader))
                .build();
    }

    @GetMapping("/task/{taskId}")
    @Operation(summary = "Получить оценку за командное задание (для текущего пользователя)")
    public GradeDto getTaskGrade(
            @PathVariable UUID taskId,
            @Parameter(hidden = true) @RequestHeader("Authorization") String authHeader) {
        return GradeDto.builder()
                .targetId(taskId)
                .value(gradeService.getTaskGrade(taskId, null, authHeader))
                .build();
    }

    @GetMapping("/task/{taskId}/user/{userId}")
    @Operation(summary = "Получить оценку за командное задание для пользователя")
    public GradeDto getTaskGradeForUser(
            @PathVariable UUID taskId,
            @PathVariable Long userId,
            @Parameter(hidden = true) @RequestHeader("Authorization") String authHeader) {
        return GradeDto.builder()
                .targetId(taskId)
                .value(gradeService.getTaskGrade(taskId, userId, authHeader))
                .build();
    }

    @GetMapping("/channel/{channelId}")
    @Operation(summary = "Получить оценку за предмет (для текущего пользователя)")
    public GradeDto getChannelGrade(
            @PathVariable UUID channelId,
            @Parameter(hidden = true) @RequestHeader("Authorization") String authHeader) {
        return GradeDto.builder()
                .targetId(channelId)
                .value(gradeService.getChannelGrade(channelId, null, authHeader))
                .build();
    }

    @GetMapping("/channel/{channelId}/user/{userId}")
    @Operation(summary = "Получить оценку за предмет для пользователя")
    public GradeDto getChannelGradeForUser(
            @PathVariable UUID channelId,
            @PathVariable Long userId,
            @Parameter(hidden = true) @RequestHeader("Authorization") String authHeader) {
        return GradeDto.builder()
                .targetId(channelId)
                .value(gradeService.getChannelGrade(channelId, userId, authHeader))
                .build();
    }
}
