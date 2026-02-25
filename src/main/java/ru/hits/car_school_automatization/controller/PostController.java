package ru.hits.car_school_automatization.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.hits.car_school_automatization.dto.CreatePostDto;
import ru.hits.car_school_automatization.dto.PostDto;
import ru.hits.car_school_automatization.dto.ShortPostDto;
import ru.hits.car_school_automatization.service.PostService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/posts")
@RequiredArgsConstructor
@Tag(name = "Посты", description = "Управление постами в каналах")
public class PostController {

    private final PostService postService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Создание нового поста")
    public void createPost(
            @RequestBody CreatePostDto createPostDto,
            @RequestHeader("Authorization") String authHeader) {
        postService.createPost(createPostDto, authHeader);
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
    public PostDto getPostById(@PathVariable UUID postId) {
        return postService.getPostById(postId);
    }

    @GetMapping("/tasks")
    @Operation(summary = "Получение задач пользователя")
    public List<ShortPostDto> getUserTasks(
            @RequestParam Long userId,
            @RequestParam String channelId) {
        return postService.getUserTasks(userId, channelId);
    }
}