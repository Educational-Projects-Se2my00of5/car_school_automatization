package ru.hits.car_school_automatization.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.hits.car_school_automatization.dto.ControlDto;
import ru.hits.car_school_automatization.dto.UpdateControlDto;
import ru.hits.car_school_automatization.service.ControlService;

import java.util.UUID;

@RestController
@RequestMapping("/api/controls")
@RequiredArgsConstructor
@Tag(name = "Контрольные", description = "Управление контрольными точками")
public class ControlController {

    private final ControlService controlService;

    @GetMapping("/{postId}")
    @Operation(summary = "Получить контрольную по postId")
    public ControlDto getControl(
            @PathVariable UUID postId,
            @Parameter(hidden = true) @RequestHeader("Authorization") String authHeader) {
        return controlService.getControl(postId, authHeader);
    }

    @PatchMapping("/{postId}")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Обновить список заданий контрольной")
    public ControlDto updateControl(
            @PathVariable UUID postId,
            @RequestBody UpdateControlDto dto,
            @Parameter(hidden = true) @RequestHeader("Authorization") String authHeader) {
        return controlService.updateControl(postId, dto, authHeader);
    }
}
