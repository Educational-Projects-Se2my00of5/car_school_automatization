package ru.hits.car_school_automatization.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
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
}
