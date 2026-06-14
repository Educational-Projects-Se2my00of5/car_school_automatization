package ru.hits.car_school_automatization.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.hits.car_school_automatization.dto.MetricValueHistoryDto;
import ru.hits.car_school_automatization.dto.MetricWithValuesDto;
import ru.hits.car_school_automatization.dto.SetMetricValueDto;
import ru.hits.car_school_automatization.dto.SetTeamMetricValueDto;
import ru.hits.car_school_automatization.service.MetricValueService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/metrics")
@RequiredArgsConstructor
@Tag(name = "Значения критериев", description = "Управление значениями критериев")
public class MetricValueController {

    private final MetricValueService metricValueService;

    @GetMapping("/post/{postId}/values")
    @Operation(summary = "Получить критерии и значения по посту")
    public List<MetricWithValuesDto> getPostMetricsWithValues(
            @PathVariable UUID postId,
            @RequestParam(required = false) Long userId,
            @Parameter(hidden = true) @RequestHeader("Authorization") String authHeader) {
        return metricValueService.getPostMetricsWithValues(postId, userId, authHeader);
    }

    @GetMapping("/task/{taskId}/values")
    @Operation(summary = "Получить критерии и значения по командному заданию")
    public List<MetricWithValuesDto> getTaskMetricsWithValues(
            @PathVariable UUID taskId,
            @RequestParam(required = false) Long userId,
            @Parameter(hidden = true) @RequestHeader("Authorization") String authHeader) {
        return metricValueService.getTaskMetricsWithValues(taskId, userId, authHeader);
    }

    @GetMapping("/task/{taskId}/values/team/{teamId}")
    @Operation(summary = "Получить критерии и значения команды по командному заданию")
    public List<MetricWithValuesDto> getTaskTeamMetricsWithValues(
            @PathVariable UUID taskId,
            @PathVariable UUID teamId,
            @Parameter(hidden = true) @RequestHeader("Authorization") String authHeader) {
        return metricValueService.getTaskTeamMetricsWithValues(taskId, teamId, authHeader);
    }

    @GetMapping("/values/history")
    @Operation(summary = "Получить историю изменений значения критерия")
    public List<MetricValueHistoryDto> getMetricValueHistory(
            @RequestParam UUID metricId,
            @RequestParam Long userId,
            @Parameter(hidden = true) @RequestHeader("Authorization") String authHeader) {
        return metricValueService.getMetricValueHistory(metricId, userId, authHeader);
    }

    @PutMapping("/values")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Установить значение критерия пользователю")
    public void setMetricValue(
            @Valid @RequestBody SetMetricValueDto dto,
            @Parameter(hidden = true) @RequestHeader("Authorization") String authHeader) {
        metricValueService.setMetricValue(dto, authHeader);
    }

    @PutMapping("/values/team")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Установить значение критерия команде")
    public void setTeamMetricValue(
            @Valid @RequestBody SetTeamMetricValueDto dto,
            @Parameter(hidden = true) @RequestHeader("Authorization") String authHeader) {
        metricValueService.setTeamMetricValue(dto, authHeader);
    }

    @DeleteMapping("/values/override")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Отменить оверрайд оценки")
    public void removeOverride(
            @RequestParam UUID metricId,
            @RequestParam Long userId,
            @Parameter(hidden = true) @RequestHeader("Authorization") String authHeader) {
        metricValueService.removeOverride(metricId, userId, authHeader);
    }
}
