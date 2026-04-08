package ru.hits.car_school_automatization.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.*;
import ru.hits.car_school_automatization.dto.CreateTaskDto;
import ru.hits.car_school_automatization.dto.TaskDto;
import ru.hits.car_school_automatization.dto.UpdateTaskDto;
import ru.hits.car_school_automatization.service.TaskService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
@Tag(name = "Задания", description = "Управление командными заданиями")
public class TaskController {

    private final TaskService taskService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Создать задание")
    public TaskDto createTask(@Valid @ModelAttribute CreateTaskDto dto,
                              @RequestParam UUID channelId,
                              @RequestHeader("Authorization") String authHeader) {
        return taskService.createTask(dto, channelId, authHeader);
    }

    @PatchMapping(value = "/{taskId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Обновить задание")
    public TaskDto updateTask(@PathVariable UUID taskId,
                              @ModelAttribute UpdateTaskDto dto,
                              @RequestHeader("Authorization") String authHeader) {
        return taskService.updateTask(taskId, dto, authHeader);
    }

    @GetMapping("/{taskId}")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Получить задание по id")
    public TaskDto getTask(@PathVariable UUID taskId,
                           @RequestHeader("Authorization") String authHeader) {
        return taskService.getTask(taskId, authHeader);
    }

    @GetMapping("/channel/{channelId}")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Получить задания предмета")
    public List<TaskDto> getTasksByChannel(@PathVariable UUID channelId,
                                           @RequestHeader("Authorization") String authHeader) {
        return taskService.getTasksByChannel(channelId, authHeader);
    }

    @PostMapping("/{taskId}/copy")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Скопировать задание в предмет")
    public TaskDto copyTask(@PathVariable UUID taskId,
                            @RequestParam UUID targetChannelId,
                            @RequestHeader("Authorization") String authHeader) {
        return taskService.copyTask(taskId, targetChannelId, authHeader);
    }

    @PostMapping("/{taskId}/documents")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Добавить документ к заданию")
    public TaskDto addDocument(@PathVariable UUID taskId,
                               @RequestParam("file") MultipartFile file,
                               @RequestHeader("Authorization") String authHeader) {
        return taskService.addDocument(taskId, file, authHeader);
    }

    @DeleteMapping("/{taskId}/documents")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Удалить документ из задания")
    public TaskDto removeDocument(@PathVariable UUID taskId,
                                  @RequestParam String fileUrl,
                                  @RequestHeader("Authorization") String authHeader) {
        return taskService.removeDocument(taskId, fileUrl, authHeader);
    }

    @DeleteMapping("/{taskId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Удалить задание")
    public void deleteTask(@PathVariable UUID taskId,
                           @RequestHeader("Authorization") String authHeader) {
        taskService.deleteTask(taskId, authHeader);
    }
}