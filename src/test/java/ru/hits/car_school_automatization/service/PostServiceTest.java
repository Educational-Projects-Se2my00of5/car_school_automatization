package ru.hits.car_school_automatization.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;
import ru.hits.car_school_automatization.dto.CreatePostDto;
import ru.hits.car_school_automatization.dto.PostDto;
import ru.hits.car_school_automatization.dto.ShortPostDto;
import ru.hits.car_school_automatization.entity.Channel;
import ru.hits.car_school_automatization.entity.Post;
import ru.hits.car_school_automatization.entity.Solution;
import ru.hits.car_school_automatization.entity.User;
import ru.hits.car_school_automatization.enums.PostType;
import ru.hits.car_school_automatization.enums.Role;
import ru.hits.car_school_automatization.exception.BadRequestException;
import ru.hits.car_school_automatization.exception.NotFoundException;
import ru.hits.car_school_automatization.repository.ChannelRepository;
import ru.hits.car_school_automatization.repository.PostRepository;
import ru.hits.car_school_automatization.repository.SolutionRepository;
import ru.hits.car_school_automatization.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PostServiceTest {

    @Mock
    private SolutionRepository solutionRepository;

    @Mock
    private PostRepository postRepository;

    @Mock
    private ChannelRepository channelRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtTokenProvider tokenProvider;

    @Mock
    private FileStorageService fileStorageService;

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
                .role(List.of(Role.TEACHER))
                .email("john@example.com")
                .build();

        createPostDto = CreatePostDto.builder()
                .label("Test Post")
                .text("Test Content")
                .type(PostType.NEWS)
                .deadline(LocalDateTime.now().plusDays(7))
                .needMark(true)
                .channelId(channelId.toString())
                .build();

        post = Post.builder()
                .id(postId)
                .label("Test Post")
                .text("Test Content")
                .type(PostType.NEWS)
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
        when(channelRepository.findById(any())).thenReturn(Optional.of(new Channel(channelId, "label", "desc", null, Set.of(new User()), new User())));
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
        when(userRepository.findById(authorId)).thenReturn(Optional.of(author));

        List<ShortPostDto> result = postService.getPostsByChannelId(channelId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(postId.toString());
        assertThat(result.get(0).getLabel()).isEqualTo("Test Post");
        assertThat(result.get(0).getType()).isEqualTo(PostType.NEWS);
    }

    @Test
    @DisplayName("Получение поста по ID должно возвращать PostDto")
    void getPostById_ShouldReturnPostDto() {
        String authHeader = "Bearer token";

        when(tokenProvider.extractTokenFromHeader(authHeader)).thenReturn("token");
        when(tokenProvider.validateToken("token")).thenReturn(true);
        when(tokenProvider.getUserIdFromToken("token")).thenReturn(authorId);
        when(userRepository.findById(authorId)).thenReturn(Optional.of(author));
        when(postRepository.findById(postId)).thenReturn(Optional.of(post));

        PostDto result = postService.getPostById(postId, authHeader);

        assertThat(result.getId()).isEqualTo(postId);
        assertThat(result.getLabel()).isEqualTo("Test Post");
        assertThat(result.getText()).isEqualTo("Test Content");
        assertThat(result.getType()).isEqualTo(PostType.NEWS);
        assertThat(result.getAuthorName()).isEqualTo("John Doe");
        assertThat(result.getFileUrl()).isNull();
        assertThat(result.getFileName()).isNull();
    }

    @Test
    @DisplayName("Получение несуществующего поста должно выбрасывать исключение")
    void getPostById_WithNonExistingPost_ShouldThrowException() {
        String authHeader = "Bearer token";

        when(tokenProvider.extractTokenFromHeader(authHeader)).thenReturn("token");
        when(tokenProvider.validateToken("token")).thenReturn(true);
        when(tokenProvider.getUserIdFromToken("token")).thenReturn(authorId);
        when(userRepository.findById(authorId)).thenReturn(Optional.of(author));
        when(postRepository.findById(postId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> postService.getPostById(postId, authHeader))
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
                .label("News")
                .type(PostType.NEWS)
                .channelId(channelId)
                .authorId(userId)
                .build();

        List<Post> posts = List.of(task1, task2, lecture);
        when(postRepository.findByChannelIdAndAuthorId(channelId, userId)).thenReturn(posts);
        when(userRepository.findById(userId)).thenReturn(Optional.of(author));

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

    @Test
    @DisplayName("Добавление файла к посту должно сохранять файл и обновлять пост")
    void addFileToPost_ShouldSaveFileAndUpdatePost() {
        String authHeader = "Bearer token";
        MultipartFile file = mock(MultipartFile.class);
        String fileUrl = "http://localhost:8080/files/test.pdf";
        String fileName = "test.pdf";

        when(tokenProvider.extractTokenFromHeader(authHeader)).thenReturn("token");
        when(tokenProvider.validateToken("token")).thenReturn(true);
        when(tokenProvider.getUserIdFromToken("token")).thenReturn(authorId);
        when(userRepository.findById(authorId)).thenReturn(Optional.of(author));
        when(postRepository.findById(postId)).thenReturn(Optional.of(post));
        when(file.getOriginalFilename()).thenReturn(fileName);
        when(file.isEmpty()).thenReturn(false);
        when(fileStorageService.store(file)).thenReturn(fileUrl);

        postService.addFileToPost(postId, file, authHeader);

        verify(fileStorageService, times(1)).store(file);
        verify(postRepository, times(1)).save(post);
        assertThat(post.getFileUrl()).isEqualTo(fileUrl);
        assertThat(post.getFileName()).isEqualTo(fileName);
    }

    @Test
    @DisplayName("Добавление файла к посту с существующим файлом должно удалять старый и сохранять новый")
    void addFileToPost_WithExistingFile_ShouldDeleteOldAndSaveNew() {
        String authHeader = "Bearer token";
        MultipartFile newFile = mock(MultipartFile.class);
        String oldFileName = "old.pdf";
        String oldFileUrl = "http://localhost:8080/files/old.pdf";
        String newFileName = "new.pdf";
        String newFileUrl = "http://localhost:8080/files/new.pdf";

        post.setFileUrl(oldFileUrl);
        post.setFileName(oldFileName);

        when(tokenProvider.extractTokenFromHeader(authHeader)).thenReturn("token");
        when(tokenProvider.validateToken("token")).thenReturn(true);
        when(tokenProvider.getUserIdFromToken("token")).thenReturn(authorId);
        when(userRepository.findById(authorId)).thenReturn(Optional.of(author));
        when(postRepository.findById(postId)).thenReturn(Optional.of(post));
        when(newFile.getOriginalFilename()).thenReturn(newFileName);
        when(newFile.isEmpty()).thenReturn(false);
        when(fileStorageService.store(newFile)).thenReturn(newFileUrl);

        postService.addFileToPost(postId, newFile, authHeader);

        verify(fileStorageService, times(1)).delete(oldFileName);
        verify(fileStorageService, times(1)).store(newFile);
        verify(postRepository, times(1)).save(post);
        assertThat(post.getFileUrl()).isEqualTo(newFileUrl);
        assertThat(post.getFileName()).isEqualTo(newFileName);
    }

    @Test
    @DisplayName("Удаление файла из поста должно удалять файл и очищать поля")
    void deleteFileFromPost_ShouldDeleteFileAndClearFields() {
        String authHeader = "Bearer token";
        String fileName = "test.pdf";
        String fileUrl = "http://localhost:8080/files/test.pdf";
        post.setFileUrl(fileUrl);
        post.setFileName(fileName);

        when(tokenProvider.extractTokenFromHeader(authHeader)).thenReturn("token");
        when(tokenProvider.validateToken("token")).thenReturn(true);
        when(tokenProvider.getUserIdFromToken("token")).thenReturn(authorId);
        when(userRepository.findById(authorId)).thenReturn(Optional.of(author));
        when(postRepository.findById(postId)).thenReturn(Optional.of(post));

        postService.deleteFileFromPost(postId, authHeader);

        verify(fileStorageService, times(1)).delete(fileName);
        verify(postRepository, times(1)).save(post);
        assertThat(post.getFileUrl()).isNull();
        assertThat(post.getFileName()).isNull();
    }

    @Test
    @DisplayName("Получение задачи по ID для студента с решением должно возвращать PostDto с решением")
    void getPostById_ForStudentWithSolution_ShouldReturnPostDtoWithSolution() {
        String authHeader = "Bearer token";
        Long studentId = 3L;
        User student = User.builder()
                .id(studentId)
                .firstName("Student")
                .lastName("User")
                .role(List.of(Role.STUDENT))
                .build();

        Post task = Post.builder()
                .id(postId)
                .label("Test Task")
                .text("Task Content")
                .type(PostType.TASK)
                .authorId(authorId)
                .channelId(channelId)
                .needMark(true)
                .createdAt(LocalDateTime.now())
                .build();

        UUID solutionId = UUID.randomUUID();
        Solution solution = Solution.builder()
                .id(solutionId)
                .studentId(studentId)
                .taskId(postId)
                .text("My solution text")
                .fileUrl("http://localhost:8080/files/solution.pdf")
                .fileName("solution.pdf")
                .mark(null)
                .submittedAt(LocalDateTime.now())
                .build();

        when(tokenProvider.extractTokenFromHeader(authHeader)).thenReturn("token");
        when(tokenProvider.validateToken("token")).thenReturn(true);
        when(tokenProvider.getUserIdFromToken("token")).thenReturn(studentId);
        when(userRepository.findById(studentId)).thenReturn(Optional.of(student));
        when(postRepository.findById(postId)).thenReturn(Optional.of(task));
        when(userRepository.findById(authorId)).thenReturn(Optional.of(author));
        when(solutionRepository.findByTaskIdAndStudentId(postId, studentId)).thenReturn(Optional.of(solution));

        PostDto result = postService.getPostById(postId, authHeader);

        assertThat(result.getId()).isEqualTo(postId);
        assertThat(result.getLabel()).isEqualTo("Test Task");
        assertThat(result.getText()).isEqualTo("Task Content");
        assertThat(result.getType()).isEqualTo(PostType.TASK);
        assertThat(result.getAuthorName()).isEqualTo("John Doe");

        assertThat(result.getStudentSolution()).isNotNull();
        assertThat(result.getStudentSolution().getId()).isEqualTo(solutionId);
        assertThat(result.getStudentSolution().getText()).isEqualTo("My solution text");
        assertThat(result.getStudentSolution().getFileName()).isEqualTo("solution.pdf");
        assertThat(result.getStudentSolution().getFileUrl()).isEqualTo("http://localhost:8080/files/solution.pdf");
        assertThat(result.getStudentSolution().getStudentId()).isEqualTo(studentId);
        assertThat(result.getStudentSolution().getStudentName()).isEqualTo("Student User");
    }

    @Test
    @DisplayName("Получение задачи по ID для студента без решения должно возвращать PostDto с null решением")
    void getPostById_ForStudentWithoutSolution_ShouldReturnPostDtoWithNullSolution() {
        String authHeader = "Bearer token";
        Long studentId = 3L;
        User student = User.builder()
                .id(studentId)
                .firstName("Student")
                .lastName("User")
                .role(List.of(Role.STUDENT))
                .build();

        Post task = Post.builder()
                .id(postId)
                .label("Test Task")
                .text("Task Content")
                .type(PostType.TASK)
                .authorId(authorId)
                .channelId(channelId)
                .needMark(true)
                .createdAt(LocalDateTime.now())
                .build();

        when(tokenProvider.extractTokenFromHeader(authHeader)).thenReturn("token");
        when(tokenProvider.validateToken("token")).thenReturn(true);
        when(tokenProvider.getUserIdFromToken("token")).thenReturn(studentId);
        when(userRepository.findById(studentId)).thenReturn(Optional.of(student));
        when(postRepository.findById(postId)).thenReturn(Optional.of(task));
        when(userRepository.findById(authorId)).thenReturn(Optional.of(author));
        when(solutionRepository.findByTaskIdAndStudentId(postId, studentId)).thenReturn(Optional.empty());

        PostDto result = postService.getPostById(postId, authHeader);

        assertThat(result.getId()).isEqualTo(postId);
        assertThat(result.getLabel()).isEqualTo("Test Task");
        assertThat(result.getText()).isEqualTo("Task Content");
        assertThat(result.getType()).isEqualTo(PostType.TASK);
        assertThat(result.getAuthorName()).isEqualTo("John Doe");

        assertThat(result.getStudentSolution()).isNull();
    }

    @Test
    @DisplayName("Создание задачи должно сохранять пост с типом TASK")
    void createPost_WithTaskType_ShouldSaveTask() {
        String authHeader = "Bearer token";

        CreatePostDto taskDto = CreatePostDto.builder()
                .label("Test Task")
                .text("Task Description")
                .type(PostType.TASK)
                .deadline(LocalDateTime.now().plusDays(7))
                .needMark(true)
                .channelId(channelId.toString())
                .build();

        Post taskPost = Post.builder()
                .id(UUID.randomUUID())
                .label("Test Task")
                .text("Task Description")
                .type(PostType.TASK)
                .deadline(taskDto.getDeadline())
                .authorId(authorId)
                .channelId(channelId)
                .needMark(true)
                .createdAt(LocalDateTime.now())
                .build();

        when(channelRepository.findById(channelId)).thenReturn(Optional.of(new Channel(channelId, "label", "desc", null, Set.of(new User()), new User())));
        when(tokenProvider.extractTokenFromHeader(authHeader)).thenReturn("token");
        when(tokenProvider.validateToken("token")).thenReturn(true);
        when(tokenProvider.getUserIdFromToken("token")).thenReturn(authorId);
        when(userRepository.findById(authorId)).thenReturn(Optional.of(author));
        when(postRepository.save(any(Post.class))).thenReturn(taskPost);

        postService.createPost(taskDto, authHeader);

        verify(postRepository, times(1)).save(argThat(p ->
                p.getType() == PostType.TASK &&
                        p.getNeedMark() == true &&
                        p.getLabel().equals("Test Task")
        ));
    }
}
