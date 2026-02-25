package ru.hits.car_school_automatization.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.hits.car_school_automatization.dto.CreatePostDto;
import ru.hits.car_school_automatization.dto.PostDto;
import ru.hits.car_school_automatization.dto.ShortPostDto;
import ru.hits.car_school_automatization.entity.Post;
import ru.hits.car_school_automatization.entity.User;
import ru.hits.car_school_automatization.enums.PostType;
import ru.hits.car_school_automatization.exception.BadRequestException;
import ru.hits.car_school_automatization.exception.NotFoundException;
import ru.hits.car_school_automatization.repository.PostRepository;
import ru.hits.car_school_automatization.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PostServiceTest {

    @Mock
    private PostRepository postRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtTokenProvider tokenProvider;

    @InjectMocks
    private PostService postService;

    private UUID channelId;
    private Long authorId;
    private UUID postId;
    private User author;
    private Post post;
    private CreatePostDto createPostDto;

    @BeforeEach
    void setUp() {
        channelId = UUID.randomUUID();
        authorId = 1L;
        postId = UUID.randomUUID();

        author = User.builder()
                .id(authorId)
                .firstName("John")
                .lastName("Doe")
                .email("john@example.com")
                .build();

        createPostDto = CreatePostDto.builder()
                .label("Test Post")
                .text("Test Content")
                .type(PostType.LECTURE)
                .deadline(LocalDateTime.now().plusDays(7))
                .authorId(authorId)
                .needMark(true)
                .channelId(channelId.toString())
                .build();

        post = Post.builder()
                .id(postId)
                .label("Test Post")
                .text("Test Content")
                .type(PostType.LECTURE)
                .deadline(LocalDateTime.now().plusDays(7))
                .authorId(authorId)
                .channelId(channelId)
                .needMark(true)
                .build();
    }

    @Test
    @DisplayName("Создание поста должно сохранять пост в БД")
    void createPost_ShouldSavePost() {
        String authHeader = "Bearer token";
        when(tokenProvider.extractTokenFromHeader(authHeader)).thenReturn("token");
        when(tokenProvider.validateToken("token")).thenReturn(true);
        when(tokenProvider.getUserIdFromToken("token")).thenReturn(authorId);
        when(userRepository.findById(authorId)).thenReturn(Optional.of(author));
        when(postRepository.save(any(Post.class))).thenReturn(post);

        postService.createPost(createPostDto, authHeader);

        verify(postRepository, times(1)).save(any(Post.class));
    }

    @Test
    @DisplayName("Создание поста с невалидным токеном должно выбрасывать исключение")
    void createPost_WithInvalidToken_ShouldThrowException() {
        String authHeader = "Bearer invalid";
        when(tokenProvider.extractTokenFromHeader(authHeader)).thenReturn("invalid");
        when(tokenProvider.validateToken("invalid")).thenReturn(false);

        assertThatThrownBy(() -> postService.createPost(createPostDto, authHeader))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Невалидный или истёкший токен");
    }

    @Test
    @DisplayName("Создание поста с несуществующим автором должно выбрасывать исключение")
    void createPost_WithNonExistingAuthor_ShouldThrowException() {
        String authHeader = "Bearer token";
        when(tokenProvider.extractTokenFromHeader(authHeader)).thenReturn("token");
        when(tokenProvider.validateToken("token")).thenReturn(true);
        when(tokenProvider.getUserIdFromToken("token")).thenReturn(authorId);
        when(userRepository.findById(authorId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> postService.createPost(createPostDto, authHeader))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Пользователь с id " + authorId + " не найден");
    }

    @Test
    @DisplayName("Удаление поста должно удалять пост из БД")
    void deletePost_ShouldDeletePost() {
        String authHeader = "Bearer token";
        when(tokenProvider.extractTokenFromHeader(authHeader)).thenReturn("token");
        when(tokenProvider.validateToken("token")).thenReturn(true);
        when(tokenProvider.getUserIdFromToken("token")).thenReturn(authorId);
        when(postRepository.findById(postId)).thenReturn(Optional.of(post));
        doNothing().when(postRepository).delete(post);

        postService.deletePost(postId, authHeader);

        verify(postRepository, times(1)).delete(post);
    }

    @Test
    @DisplayName("Удаление несуществующего поста должно выбрасывать исключение")
    void deletePost_WithNonExistingPost_ShouldThrowException() {
        String authHeader = "Bearer token";
        when(tokenProvider.extractTokenFromHeader(authHeader)).thenReturn("token");
        when(tokenProvider.validateToken("token")).thenReturn(true);
        when(tokenProvider.getUserIdFromToken("token")).thenReturn(authorId);
        when(postRepository.findById(postId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> postService.deletePost(postId, authHeader))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Пост с id " + postId + " не найден");
    }

    @Test
    @DisplayName("Получение постов по ID канала должно возвращать список ShortPostDto")
    void getPostsByChannelId_ShouldReturnListOfShortPostDto() {
        List<Post> posts = List.of(post);
        when(postRepository.findByChannelIdOrderByCreatedAtDesc(channelId)).thenReturn(posts);

        List<ShortPostDto> result = postService.getPostsByChannelId(channelId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(postId.toString());
        assertThat(result.get(0).getLabel()).isEqualTo("Test Post");
        assertThat(result.get(0).getType()).isEqualTo(PostType.LECTURE);
    }

    @Test
    @DisplayName("Получение поста по ID должно возвращать PostDto")
    void getPostById_ShouldReturnPostDto() {
        when(postRepository.findById(postId)).thenReturn(Optional.of(post));

        PostDto result = postService.getPostById(postId);

        assertThat(result.getId()).isEqualTo(postId);
        assertThat(result.getLabel()).isEqualTo("Test Post");
        assertThat(result.getText()).isEqualTo("Test Content");
        assertThat(result.getType()).isEqualTo(PostType.LECTURE);
    }

    @Test
    @DisplayName("Получение несуществующего поста должно выбрасывать исключение")
    void getPostById_WithNonExistingPost_ShouldThrowException() {
        when(postRepository.findById(postId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> postService.getPostById(postId))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Пост с id " + postId + " не найден");
    }

    @Test
    @DisplayName("Получение задач пользователя должно возвращать только задачи")
    void getUserTasks_ShouldReturnOnlyTasks() {
        Long userId = 1L;

        Post task1 = Post.builder()
                .id(UUID.randomUUID())
                .label("Task 1")
                .type(PostType.TASK)
                .channelId(channelId)
                .authorId(userId)
                .build();

        Post task2 = Post.builder()
                .id(UUID.randomUUID())
                .label("Task 2")
                .type(PostType.TASK)
                .channelId(channelId)
                .authorId(userId)
                .build();

        Post lecture = Post.builder()
                .id(UUID.randomUUID())
                .label("Lecture")
                .type(PostType.LECTURE)
                .channelId(channelId)
                .authorId(userId)
                .build();

        List<Post> posts = List.of(task1, task2, lecture);
        when(postRepository.findByChannelIdAndAuthorId(channelId, userId)).thenReturn(posts);

        List<ShortPostDto> result = postService.getUserTasks(userId, channelId.toString());

        assertThat(result).hasSize(2);
        assertThat(result).allMatch(dto -> dto.getType() == PostType.TASK);
    }

    @Test
    @DisplayName("Получение задач с невалидным ID канала должно выбрасывать исключение")
    void getUserTasks_WithInvalidChannelId_ShouldThrowException() {
        Long userId = 1L;
        String invalidChannelId = "not-a-uuid";

        assertThatThrownBy(() -> postService.getUserTasks(userId, invalidChannelId))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Неверный формат ID канала");
    }
}
