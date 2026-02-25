package ru.hits.car_school_automatization.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.hits.car_school_automatization.dto.CreatePostDto;
import ru.hits.car_school_automatization.dto.PostDto;
import ru.hits.car_school_automatization.dto.ShortPostDto;
import ru.hits.car_school_automatization.entity.Channel;
import ru.hits.car_school_automatization.entity.Post;
import ru.hits.car_school_automatization.entity.User;
import ru.hits.car_school_automatization.enums.PostType;
import ru.hits.car_school_automatization.exception.BadRequestException;
import ru.hits.car_school_automatization.exception.NotFoundException;
import ru.hits.car_school_automatization.repository.ChannelRepository;
import ru.hits.car_school_automatization.repository.PostRepository;
import ru.hits.car_school_automatization.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class PostService {

    private final ChannelRepository channelRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final JwtTokenProvider tokenProvider;

    /**
     * Создание нового поста
     */
    public void createPost(CreatePostDto createPostDto, String authHeader) {
        log.info("Создание нового поста: {}", createPostDto.getLabel());

        Long authorId = extractUserIdFromHeader(authHeader);

        User author = userRepository.findById(authorId)
                .orElseThrow(() -> new NotFoundException("Пользователь с id " + authorId + " не найден"));

        UUID channelUuid;
        try {
            channelUuid = UUID.fromString(createPostDto.getChannelId());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Неверный формат ID канала");
        }

        Channel channel = channelRepository.findById(channelUuid)
                .orElseThrow(() -> new NotFoundException("Канал с id " + channelUuid + " не найден"));

        Post post = Post.builder()
                .label(createPostDto.getLabel())
                .text(createPostDto.getText())
                .type(createPostDto.getType())
                .deadline(createPostDto.getDeadline())
                .authorId(authorId)
                .channelId(channelUuid)
                .needMark(createPostDto.getNeedMark())
                .createdAt(LocalDateTime.now())
                .build();

        postRepository.save(post);
        log.info("Пост успешно создан с id: {}", post.getId());
    }

    /**
     * Удаление поста
     */
    public void deletePost(UUID postId, String authHeader) {
        log.info("Удаление поста с id: {}", postId);

        extractUserIdFromHeader(authHeader);

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new NotFoundException("Пост с id " + postId + " не найден"));

        postRepository.delete(post);
        log.info("Пост с id {} успешно удален", postId);
    }

    /**
     * Получение всех постов по ID канала
     */
    public List<ShortPostDto> getPostsByChannelId(UUID channelId) {
        log.info("Получение постов для канала с id: {}", channelId);

        return postRepository.findByChannelIdOrderByCreatedAtDesc(channelId)
                .stream()
                .map(this::mapToShortPostDto)
                .collect(Collectors.toList());
    }

    /**
     * Получение поста по ID
     */
    public PostDto getPostById(UUID postId) {
        log.info("Получение поста с id: {}", postId);

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new NotFoundException("Пост с id " + postId + " не найден"));

        return mapToPostDto(post);
    }

    /**
     * Получение задач пользователя
     */
    public List<ShortPostDto> getUserTasks(Long userId, String channelIdStr) {
        log.info("Получение задач для пользователя {} в канале {}", userId, channelIdStr);

        UUID channelId;
        try {
            channelId = UUID.fromString(channelIdStr);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Неверный формат ID канала");
        }

        return postRepository.findByChannelIdAndAuthorId(channelId, userId)
                .stream()
                .filter(post -> PostType.TASK.equals(post.getType()))
                .map(this::mapToShortPostDto)
                .collect(Collectors.toList());
    }

    /**
     * Извлечение ID пользователя из заголовка авторизации
     */
    private Long extractUserIdFromHeader(String authHeader) {
        String token = tokenProvider.extractTokenFromHeader(authHeader);

        if (!tokenProvider.validateToken(token)) {
            throw new BadRequestException("Невалидный или истёкший токен");
        }

        return tokenProvider.getUserIdFromToken(token);
    }

    /**
     * Маппинг Post в ShortPostDto
     */
    private ShortPostDto mapToShortPostDto(Post post) {
        return ShortPostDto.builder()
                .id(post.getId().toString())
                .label(post.getLabel())
                .type(post.getType())
                .build();
    }

    /**
     * Маппинг Post в PostDto
     */
    private PostDto mapToPostDto(Post post) {
        return PostDto.builder()
                .id(post.getId())
                .label(post.getLabel())
                .text(post.getText())
                .type(post.getType())
                .deadline(post.getDeadline())
                .authorId(post.getAuthorId())
                .build();
    }
}