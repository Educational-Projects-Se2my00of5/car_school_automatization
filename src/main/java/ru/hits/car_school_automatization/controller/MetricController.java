package ru.hits.car_school_automatization.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.hits.car_school_automatization.dto.CreateMetricDto;
import ru.hits.car_school_automatization.dto.MetricDto;
import ru.hits.car_school_automatization.service.MetricService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/metrics")
@RequiredArgsConstructor
@Tag(name = "Критерии", description = "Управление критериями оценивания")
public class MetricController {

    private final MetricService metricService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Создать критерий")
    public MetricDto createMetric(
            @Valid @RequestBody CreateMetricDto dto,
            @Parameter(hidden = true) @RequestHeader("Authorization") String authHeader) {
        return metricService.createMetric(dto, authHeader);
    }

    @DeleteMapping("/{metricId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Удалить критерий")
    public void deleteMetric(
            @PathVariable UUID metricId,
            @Parameter(hidden = true) @RequestHeader("Authorization") String authHeader) {
        metricService.deleteMetric(metricId, authHeader);
    }

    @GetMapping("/post/{postId}")
    @Operation(summary = "Получить критерии поста")
    public List<MetricDto> getPostMetrics(
            @PathVariable UUID postId,
            @Parameter(hidden = true) @RequestHeader("Authorization") String authHeader) {
        return metricService.getPostMetrics(postId, authHeader);
    }

    @GetMapping("/task/{taskId}")
    @Operation(summary = "Получить критерии командного задания")
    public List<MetricDto> getTaskMetrics(
            @PathVariable UUID taskId,
            @Parameter(hidden = true) @RequestHeader("Authorization") String authHeader) {
        return metricService.getTaskMetrics(taskId, authHeader);
    }

}
