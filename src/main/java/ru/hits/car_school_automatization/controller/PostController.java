package ru.hits.car_school_automatization.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import ru.hits.car_school_automatization.dto.CreatePostDto;
import ru.hits.car_school_automatization.dto.PostDto;
import ru.hits.car_school_automatization.dto.ShortPostDto;
import ru.hits.car_school_automatization.service.PostService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
@Tag(name = "Посты", description = "Управление постами в каналах")
public class PostController {

    private final PostService postService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Создание нового поста")
    public void createPost(
            @RequestPart("post") @Valid CreatePostDto createPostDto,
            @RequestPart(value = "file", required = false) MultipartFile file,
            @RequestHeader("Authorization") String authHeader) {
        postService.createPost(createPostDto, file, authHeader);
    }

    @DeleteMapping("/{postId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Удаление поста")
    public void deletePost(
            @PathVariable UUID postId,
            @RequestHeader("Authorization") String authHeader) {
        postService.deletePost(postId, authHeader);
    }

    @GetMapping("/channel/{channelId}")
    @Operation(summary = "Получение всех постов канала")
    public List<ShortPostDto> getPostsByChannelId(@PathVariable UUID channelId) {
        return postService.getPostsByChannelId(channelId);
    }

    @GetMapping("/{postId}")
    @Operation(summary = "Получение поста по ID")
    public PostDto getPostById(@PathVariable UUID postId, @RequestHeader("Authorization") String authHeader) {
        return postService.getPostById(postId, authHeader);
    }

    @PostMapping(value = "/{postId}/file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Добавление файла к посту")
    public void addFileToPost(
            @PathVariable UUID postId,
            @RequestPart("file") MultipartFile file,
            @RequestHeader("Authorization") String authHeader) {
        postService.addFileToPost(postId, file, authHeader);
    }

    @DeleteMapping("/{postId}/file")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Удаление файла из поста")
    public void deleteFileFromPost(
            @PathVariable UUID postId,
            @RequestHeader("Authorization") String authHeader) {
        postService.deleteFileFromPost(postId, authHeader);
    }

    @GetMapping("/tasks")
    @Operation(summary = "Получение задач пользователя")
    public List<ShortPostDto> getUserTasks(
            @RequestParam Long userId,
            @RequestParam String channelId) {
        return postService.getUserTasks(userId, channelId);
    }
}