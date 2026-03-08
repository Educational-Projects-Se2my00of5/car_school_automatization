package ru.hits.car_school_automatization.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import ru.hits.car_school_automatization.dto.CreatePostDto;
import ru.hits.car_school_automatization.dto.PostDto;
import ru.hits.car_school_automatization.dto.ShortPostDto;
import ru.hits.car_school_automatization.dto.SolutionDto;
import ru.hits.car_school_automatization.entity.Channel;
import ru.hits.car_school_automatization.entity.Post;
import ru.hits.car_school_automatization.entity.Solution;
import ru.hits.car_school_automatization.entity.User;
import ru.hits.car_school_automatization.enums.PostType;
import ru.hits.car_school_automatization.enums.Role;
import ru.hits.car_school_automatization.exception.BadRequestException;
import ru.hits.car_school_automatization.exception.ForbiddenException;
import ru.hits.car_school_automatization.exception.NotFoundException;
import ru.hits.car_school_automatization.repository.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class PostService {

    private final CommentRepository commentRepository;
    private final ChannelRepository channelRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;
    private final SolutionRepository solutionRepository;
    private final JwtTokenProvider tokenProvider;

    /**
     * Создание нового поста
     */
    public void createPost(CreatePostDto createPostDto, String authHeader) {
        log.info("Создание нового поста: {}", createPostDto.getLabel());

        Long authorId = extractUserIdFromHeader(authHeader);

        User author = userRepository.findById(authorId)
                .orElseThrow(() -> new NotFoundException("Пользователь с id " + authorId + " не найден"));

        if (!isTeacherOrManager(author)) {
            throw new ForbiddenException("Только преподаватели и менеджеры могут создавать посты");
        }

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

        MultipartFile file = createPostDto.getFile();
        if (file != null && !file.isEmpty()) {
            String fileUrl = fileStorageService.store(file);
            post.setFileUrl(fileUrl);
            post.setFileName(file.getOriginalFilename());
            log.info("Файл {} добавлен к посту", file.getOriginalFilename());
        }

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
                .map(post -> mapToShortPostDto(post, getAuthorFullName(post.getAuthorId())))
                .collect(Collectors.toList());
    }

    /**
     * Получение поста по ID
     */
    public PostDto getPostById(UUID postId, String authHeader) {
        log.info("Получение поста с id: {}", postId);

        Long userId = extractUserIdFromHeader(authHeader);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь с id " + userId + " не найден"));

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new NotFoundException("Пост с id " + postId + " не найден"));

        SolutionDto studentSolution = null;
        if (PostType.TASK.equals(post.getType()) && isStudent(user)) {
            studentSolution = solutionRepository.findByTaskIdAndStudentId(postId, userId)
                    .map(this::mapToSolutionDto)
                    .orElse(null);
        }

        return mapToPostDto(post, getAuthorFullName(post.getAuthorId()), studentSolution);
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
                .map(post -> mapToShortPostDto(post, getAuthorFullName(post.getAuthorId())))
                .collect(Collectors.toList());
    }

    /**
     * Добавление файла к посту
     */
    public void addFileToPost(UUID postId, MultipartFile file, String authHeader) {
        log.info("Добавление файла к посту с id: {}", postId);

        Long authorId = extractUserIdFromHeader(authHeader);

        User user = userRepository.findById(authorId)
                .orElseThrow(() -> new NotFoundException("Пользователь с id " + authorId + " не найден"));

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new NotFoundException("Пост с id " + postId + " не найден"));

        if (!post.getAuthorId().equals(authorId) && !isTeacherOrManager(user)) {
            throw new ForbiddenException("У вас нет прав на добавление файла к этому посту");
        }

        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Файл не может быть пустым");
        }

        if (post.getFileUrl() != null) {
            String oldFilename = post.getFileName();
            if (oldFilename != null) {
                fileStorageService.delete(oldFilename);
            }
        }

        String fileUrl = fileStorageService.store(file);
        post.setFileUrl(fileUrl);
        post.setFileName(file.getOriginalFilename());

        postRepository.save(post);
        log.info("Файл {} добавлен к посту {}", file.getOriginalFilename(), postId);
    }

    /**
     * Удаление файла из поста
     */
    public void deleteFileFromPost(UUID postId, String authHeader) {
        log.info("Удаление файла из поста с id: {}", postId);

        Long authorId = extractUserIdFromHeader(authHeader);

        User user = userRepository.findById(authorId)
                .orElseThrow(() -> new NotFoundException("Пользователь с id " + authorId + " не найден"));

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new NotFoundException("Пост с id " + postId + " не найден"));

        if (!post.getAuthorId().equals(authorId) && !user.getRole().contains(Role.MANAGER)) {
            throw new ForbiddenException("У вас нет прав на удаление файла из этого поста");
        }

        if (post.getFileUrl() != null) {
            String filename = post.getFileName();
            if (filename != null) {
                fileStorageService.delete(filename);
            }
            post.setFileUrl(null);
            post.setFileName(null);
            postRepository.save(post);
            log.info("Файл удален из поста {}", postId);
        }
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

    private boolean isStudent(User user) {
        return user.getRole().contains(Role.STUDENT);
    }

    private boolean isTeacherOrManager(User user) {
        return user.getRole().contains(Role.TEACHER) || user.getRole().contains(Role.MANAGER);
    }

    /**
     * Маппинг Post в ShortPostDto
     */
    private ShortPostDto mapToShortPostDto(Post post, String fullName) {
        var commentsCount = commentRepository.countByPostId(post.getId());

        return ShortPostDto.builder()
                .id(post.getId().toString())
                .authorName(fullName)
                .label(post.getLabel())
                .type(post.getType())
                .totalComments(commentsCount)
                .build();
    }

    /**
     * Маппинг Post в PostDto
     */
    private PostDto mapToPostDto(Post post, String authorName, SolutionDto studentSolution) {
        return PostDto.builder()
                .id(post.getId())
                .label(post.getLabel())
                .text(post.getText())
                .type(post.getType())
                .deadline(post.getDeadline())
                .authorName(authorName)
                .fileUrl(post.getFileUrl())
                .fileName(post.getFileName())
                .studentSolution(studentSolution)
                .build();
    }

    private SolutionDto mapToSolutionDto(Solution solution) {
        User student = userRepository.findById(solution.getStudentId())
                .orElseThrow(() -> new NotFoundException("Пользователь с id " + solution.getStudentId() + " не найден"));
        User teacher = null;
        if (solution.getTeacherId() != null) {
            teacher = userRepository.findById(solution.getStudentId())
                    .orElseThrow(() -> new NotFoundException("Пользователь с id " + solution.getTeacherId() + " не найден"));
        }

        Post task = postRepository.findById(solution.getTaskId()).orElse(null);

        return SolutionDto.builder()
                .id(solution.getId())
                .studentId(solution.getStudentId())
                .studentName(student.getFirstName() + " " + student.getLastName())
                .taskId(solution.getTaskId())
                .taskLabel(task != null ? task.getLabel() : null)
                .teacherId(solution.getTeacherId())
                .teacherName(teacher != null ? teacher.getFirstName() + " " + teacher.getLastName() : null)
                .text(solution.getText())
                .fileUrl(solution.getFileUrl())
                .fileName(solution.getFileName())
                .mark(solution.getMark())
                .submittedAt(solution.getSubmittedAt())
                .updatedAt(solution.getUpdatedAt())
                .markedAt(solution.getMarkedAt())
                .build();
    }

    private String getAuthorFullName(Long id) {
        User author = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Пользователь с id " + id + " не найден"));
        return author.getFirstName() + " " +  author.getLastName();
    }
}