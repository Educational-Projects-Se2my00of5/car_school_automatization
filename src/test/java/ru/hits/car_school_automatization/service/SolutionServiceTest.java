package ru.hits.car_school_automatization.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;
import ru.hits.car_school_automatization.dto.*;
import ru.hits.car_school_automatization.entity.Post;
import ru.hits.car_school_automatization.entity.Solution;
import ru.hits.car_school_automatization.entity.User;
import ru.hits.car_school_automatization.enums.PostType;
import ru.hits.car_school_automatization.enums.Role;
import ru.hits.car_school_automatization.exception.BadRequestException;
import ru.hits.car_school_automatization.exception.ForbiddenException;
import ru.hits.car_school_automatization.exception.NotFoundException;
import ru.hits.car_school_automatization.repository.PostRepository;
import ru.hits.car_school_automatization.repository.SolutionRepository;
import ru.hits.car_school_automatization.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SolutionServiceTest {

    @Mock
    private SolutionRepository solutionRepository;

    @Mock
    private PostRepository postRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtTokenProvider tokenProvider;

    @Mock
    private FileStorageService fileStorageService;

    @InjectMocks
    private SolutionService solutionService;

    private UUID taskId;
    private UUID solutionId;
    private Long studentId;
    private Long teacherId;
    private String authHeader;
    private String token;
    private User student;
    private User teacher;
    private Post task;
    private Solution solution;
    private SubmitSolutionDto submitSolutionDto;
    private GradeSolutionDto gradeSolutionDto;
    private UpdateSolutionDto updateSolutionDto;

    @BeforeEach
    void setUp() {
        taskId = UUID.randomUUID();
        solutionId = UUID.randomUUID();
        studentId = 1L;
        teacherId = 2L;
        authHeader = "Bearer test-token";
        token = "test-token";

        student = User.builder()
                .id(studentId)
                .firstName("Иван")
                .lastName("Иванов")
                .email("ivan@test.com")
                .role(List.of(Role.STUDENT))
                .build();

        teacher = User.builder()
                .id(teacherId)
                .firstName("Петр")
                .lastName("Петров")
                .email("petr@test.com")
                .role(List.of(Role.TEACHER))
                .build();

        task = Post.builder()
                .id(taskId)
                .label("Тестовое задание")
                .text("Описание задания")
                .type(PostType.TASK)
                .deadline(LocalDateTime.now().plusDays(7))
                .channelId(UUID.randomUUID())
                .authorId(teacherId)
                .build();

        solution = Solution.builder()
                .id(solutionId)
                .studentId(studentId)
                .taskId(taskId)
                .text("Мое решение")
                .fileUrl("http://localhost:8080/file/test.txt")
                .fileName("test.txt")
                .submittedAt(LocalDateTime.now())
                .build();

        submitSolutionDto = SubmitSolutionDto.builder()
                .taskId(taskId)
                .text("Мое решение")
                .build();

        gradeSolutionDto = GradeSolutionDto.builder()
                .solutionId(solutionId)
                .mark(5)
                .build();

        updateSolutionDto = new UpdateSolutionDto();
    }

    @Test
    void submitSolution_Success() {
        when(tokenProvider.extractTokenFromHeader(authHeader)).thenReturn(token);
        when(tokenProvider.validateToken(token)).thenReturn(true);
        when(tokenProvider.getUserIdFromToken(token)).thenReturn(studentId);
        when(userRepository.findById(studentId)).thenReturn(Optional.of(student));
        when(postRepository.findById(taskId)).thenReturn(Optional.of(task));
        when(solutionRepository.existsByTaskIdAndStudentId(taskId, studentId)).thenReturn(false);
        when(solutionRepository.save(any(Solution.class))).thenAnswer(i -> i.getArgument(0));

        SolutionDto result = solutionService.submitSolution(submitSolutionDto, authHeader);

        assertNotNull(result);
        assertEquals(studentId, result.getStudentId());
        assertEquals(taskId, result.getTaskId());
        assertEquals("Мое решение", result.getText());
        assertNull(result.getMark());

        verify(solutionRepository, times(1)).save(any(Solution.class));
    }

    @Test
    void submitSolution_TaskNotFound_ThrowsException() {
        when(tokenProvider.extractTokenFromHeader(authHeader)).thenReturn(token);
        when(tokenProvider.validateToken(token)).thenReturn(true);
        when(tokenProvider.getUserIdFromToken(token)).thenReturn(studentId);
        when(userRepository.findById(studentId)).thenReturn(Optional.of(student));
        when(postRepository.findById(taskId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () ->
                solutionService.submitSolution(submitSolutionDto, authHeader)
        );
    }

    @Test
    void submitSolution_NotATask_ThrowsException() {
        task.setType(PostType.NEWS);

        when(tokenProvider.extractTokenFromHeader(authHeader)).thenReturn(token);
        when(tokenProvider.validateToken(token)).thenReturn(true);
        when(tokenProvider.getUserIdFromToken(token)).thenReturn(studentId);
        when(userRepository.findById(studentId)).thenReturn(Optional.of(student));
        when(postRepository.findById(taskId)).thenReturn(Optional.of(task));

        assertThrows(BadRequestException.class, () ->
                solutionService.submitSolution(submitSolutionDto, authHeader)
        );
    }

    @Test
    void submitSolution_AlreadySubmitted_ThrowsException() {
        when(tokenProvider.extractTokenFromHeader(authHeader)).thenReturn(token);
        when(tokenProvider.validateToken(token)).thenReturn(true);
        when(tokenProvider.getUserIdFromToken(token)).thenReturn(studentId);
        when(userRepository.findById(studentId)).thenReturn(Optional.of(student));
        when(postRepository.findById(taskId)).thenReturn(Optional.of(task));
        when(solutionRepository.existsByTaskIdAndStudentId(taskId, studentId)).thenReturn(true);

        assertThrows(BadRequestException.class, () ->
                solutionService.submitSolution(submitSolutionDto, authHeader)
        );
    }

    @Test
    void submitSolution_EmptyTextAndNoFile_ThrowsException() {
        submitSolutionDto.setText("");

        when(tokenProvider.extractTokenFromHeader(authHeader)).thenReturn(token);
        when(tokenProvider.validateToken(token)).thenReturn(true);
        when(tokenProvider.getUserIdFromToken(token)).thenReturn(studentId);
        when(userRepository.findById(studentId)).thenReturn(Optional.of(student));
        when(postRepository.findById(taskId)).thenReturn(Optional.of(task));
        when(solutionRepository.existsByTaskIdAndStudentId(taskId, studentId)).thenReturn(false);

        assertThrows(BadRequestException.class, () ->
                solutionService.submitSolution(submitSolutionDto, authHeader)
        );
    }

    @Test
    void submitSolution_WithFile_Success() {
        MultipartFile mockFile = mock(MultipartFile.class);
        submitSolutionDto.setFile(mockFile);

        when(mockFile.isEmpty()).thenReturn(false);
        when(mockFile.getOriginalFilename()).thenReturn("solution.pdf");
        when(fileStorageService.store(mockFile)).thenReturn("http://localhost:8080/file/uuid.pdf");

        when(tokenProvider.extractTokenFromHeader(authHeader)).thenReturn(token);
        when(tokenProvider.validateToken(token)).thenReturn(true);
        when(tokenProvider.getUserIdFromToken(token)).thenReturn(studentId);
        when(userRepository.findById(studentId)).thenReturn(Optional.of(student));
        when(postRepository.findById(taskId)).thenReturn(Optional.of(task));
        when(solutionRepository.existsByTaskIdAndStudentId(taskId, studentId)).thenReturn(false);
        when(solutionRepository.save(any(Solution.class))).thenAnswer(i -> i.getArgument(0));

        SolutionDto result = solutionService.submitSolution(submitSolutionDto, authHeader);

        assertNotNull(result);
        assertEquals("http://localhost:8080/file/uuid.pdf", result.getFileUrl());
        assertEquals("solution.pdf", result.getFileName());

        verify(fileStorageService, times(1)).store(mockFile);
    }

    @Test
    void updateSolution_Success() {
        updateSolutionDto.setText("Обновленное решение");

        when(tokenProvider.extractTokenFromHeader(authHeader)).thenReturn(token);
        when(tokenProvider.validateToken(token)).thenReturn(true);
        when(tokenProvider.getUserIdFromToken(token)).thenReturn(studentId);
        when(solutionRepository.findById(solutionId)).thenReturn(Optional.of(solution));
        when(userRepository.findById(studentId)).thenReturn(Optional.of(student));
        when(postRepository.findById(taskId)).thenReturn(Optional.of(task));
        when(solutionRepository.save(any(Solution.class))).thenAnswer(i -> i.getArgument(0));

        SolutionDto result = solutionService.updateSolution(solutionId, updateSolutionDto, authHeader);

        assertNotNull(result);
        assertEquals("Обновленное решение", result.getText());
    }

    @Test
    void updateSolution_NotOwner_ThrowsException() {
        Long otherStudentId = 999L;

        when(tokenProvider.extractTokenFromHeader(authHeader)).thenReturn(token);
        when(tokenProvider.validateToken(token)).thenReturn(true);
        when(tokenProvider.getUserIdFromToken(token)).thenReturn(otherStudentId);
        when(solutionRepository.findById(solutionId)).thenReturn(Optional.of(solution));

        assertThrows(ForbiddenException.class, () ->
                solutionService.updateSolution(solutionId, updateSolutionDto, authHeader)
        );
    }

    @Test
    void gradeSolution_ShouldUpdateMark() {
        String authHeader = "Bearer token";
        Long teacherId = 2L;

        UUID solutionId = UUID.randomUUID();
        Solution solution = Solution.builder()
                .id(solutionId)
                .studentId(1L)
                .mark(3)
                .text("Solution text")
                .build();

        when(tokenProvider.extractTokenFromHeader(authHeader)).thenReturn("token");
        when(tokenProvider.validateToken("token")).thenReturn(true);
        when(tokenProvider.getUserIdFromToken("token")).thenReturn(teacherId);
        when(userRepository.findById(teacherId)).thenReturn(Optional.of(teacher));
        when(solutionRepository.findById(any())).thenReturn(Optional.of(solution));
        when(postRepository.findById(any())).thenReturn(Optional.of(Post.builder().label("Task").build()));
        when(userRepository.findById(1L)).thenReturn(Optional.of(User.builder().firstName("Student").lastName("User").build()));
        when(solutionRepository.save(any(Solution.class))).thenAnswer(i -> i.getArgument(0));

        SolutionDto result = solutionService.gradeSolution(gradeSolutionDto, authHeader);

        assertEquals(5, result.getMark());
        verify(solutionRepository, times(1)).save(argThat(s ->
                s.getMark() == 5 && s.getTeacherId().equals(teacherId)
        ));
    }

    @Test
    void gradeSolution_Success() {
        when(tokenProvider.extractTokenFromHeader(authHeader)).thenReturn(token);
        when(tokenProvider.validateToken(token)).thenReturn(true);
        when(tokenProvider.getUserIdFromToken(token)).thenReturn(teacherId);
        when(userRepository.findById(teacherId)).thenReturn(Optional.of(teacher));
        when(solutionRepository.findById(solutionId)).thenReturn(Optional.of(solution));
        when(userRepository.findById(studentId)).thenReturn(Optional.of(student));
        when(postRepository.findById(taskId)).thenReturn(Optional.of(task));
        when(solutionRepository.save(any(Solution.class))).thenAnswer(i -> i.getArgument(0));

        SolutionDto result = solutionService.gradeSolution(gradeSolutionDto, authHeader);

        assertNotNull(result);
        assertEquals(5, result.getMark());
        assertEquals(teacherId, result.getTeacherId());
        assertNotNull(result.getMarkedAt());
    }

    @Test
    void gradeSolution_NotTeacher_ThrowsException() {
        User studentUser = User.builder()
                .id(studentId)
                .role(List.of(Role.STUDENT))
                .build();

        when(tokenProvider.extractTokenFromHeader(authHeader)).thenReturn(token);
        when(tokenProvider.validateToken(token)).thenReturn(true);
        when(tokenProvider.getUserIdFromToken(token)).thenReturn(studentId);
        when(userRepository.findById(studentId)).thenReturn(Optional.of(studentUser));

        assertThrows(ForbiddenException.class, () ->
                solutionService.gradeSolution(gradeSolutionDto, authHeader)
        );
    }

    @Test
    void gradeSolution_InvalidMark_ThrowsException() {
        gradeSolutionDto.setMark(10);

        when(tokenProvider.extractTokenFromHeader(authHeader)).thenReturn(token);
        when(tokenProvider.validateToken(token)).thenReturn(true);
        when(tokenProvider.getUserIdFromToken(token)).thenReturn(teacherId);
        when(userRepository.findById(teacherId)).thenReturn(Optional.of(teacher));
        when(solutionRepository.findById(solutionId)).thenReturn(Optional.of(solution));

        assertThrows(BadRequestException.class, () ->
                solutionService.gradeSolution(gradeSolutionDto, authHeader)
        );
    }

    @Test
    void getSolutionById_AsOwner_Success() {
        when(tokenProvider.extractTokenFromHeader(authHeader)).thenReturn(token);
        when(tokenProvider.validateToken(token)).thenReturn(true);
        when(tokenProvider.getUserIdFromToken(token)).thenReturn(studentId);
        when(userRepository.findById(studentId)).thenReturn(Optional.of(student));
        when(solutionRepository.findById(solutionId)).thenReturn(Optional.of(solution));
        when(postRepository.findById(taskId)).thenReturn(Optional.of(task));

        SolutionDto result = solutionService.getSolutionById(solutionId, authHeader);

        assertNotNull(result);
        assertEquals(solutionId, result.getId());
        assertEquals(studentId, result.getStudentId());
    }

    @Test
    void getSolutionById_AsTeacher_Success() {
        when(tokenProvider.extractTokenFromHeader(authHeader)).thenReturn(token);
        when(tokenProvider.validateToken(token)).thenReturn(true);
        when(tokenProvider.getUserIdFromToken(token)).thenReturn(teacherId);
        when(userRepository.findById(teacherId)).thenReturn(Optional.of(teacher));
        when(solutionRepository.findById(solutionId)).thenReturn(Optional.of(solution));
        when(postRepository.findById(taskId)).thenReturn(Optional.of(task));
        when(userRepository.findById(studentId)).thenReturn(Optional.of(student));

        SolutionDto result = solutionService.getSolutionById(solutionId, authHeader);

        assertNotNull(result);
        assertEquals(solutionId, result.getId());
    }

    @Test
    void getSolutionById_Unauthorized_ThrowsException() {
        User otherStudent = User.builder()
                .id(999L)
                .role(List.of(Role.STUDENT))
                .build();

        when(tokenProvider.extractTokenFromHeader(authHeader)).thenReturn(token);
        when(tokenProvider.validateToken(token)).thenReturn(true);
        when(tokenProvider.getUserIdFromToken(token)).thenReturn(999L);
        when(userRepository.findById(999L)).thenReturn(Optional.of(otherStudent));
        when(solutionRepository.findById(solutionId)).thenReturn(Optional.of(solution));

        assertThrows(ForbiddenException.class, () ->
                solutionService.getSolutionById(solutionId, authHeader)
        );
    }

    @Test
    void getStudentSolutions_AsOwner_Success() {
        when(tokenProvider.extractTokenFromHeader(authHeader)).thenReturn(token);
        when(tokenProvider.validateToken(token)).thenReturn(true);
        when(tokenProvider.getUserIdFromToken(token)).thenReturn(studentId);
        when(userRepository.findById(studentId)).thenReturn(Optional.of(student));
        when(solutionRepository.findStudentSolutions(studentId)).thenReturn(List.of(solution));
        when(postRepository.findById(taskId)).thenReturn(Optional.of(task));

        List<SolutionDto> results = solutionService.getStudentSolutions(studentId, authHeader);

        assertFalse(results.isEmpty());
        assertEquals(1, results.size());
        assertEquals(solutionId, results.get(0).getId());
    }

    @Test
    void getStudentSolutions_AsTeacher_Success() {
        when(tokenProvider.extractTokenFromHeader(authHeader)).thenReturn(token);
        when(tokenProvider.validateToken(token)).thenReturn(true);
        when(tokenProvider.getUserIdFromToken(token)).thenReturn(teacherId);
        when(userRepository.findById(teacherId)).thenReturn(Optional.of(teacher));
        when(userRepository.findById(studentId)).thenReturn(Optional.of(student));
        when(solutionRepository.findStudentSolutions(studentId)).thenReturn(List.of(solution));
        when(postRepository.findById(taskId)).thenReturn(Optional.of(task));

        List<SolutionDto> results = solutionService.getStudentSolutions(studentId, authHeader);

        assertFalse(results.isEmpty());
        assertEquals(1, results.size());
    }

    @Test
    void getStudentSolutions_Unauthorized_ThrowsException() {
        User otherStudent = User.builder()
                .id(999L)
                .role(List.of(Role.STUDENT))
                .build();

        when(tokenProvider.extractTokenFromHeader(authHeader)).thenReturn(token);
        when(tokenProvider.validateToken(token)).thenReturn(true);
        when(tokenProvider.getUserIdFromToken(token)).thenReturn(999L);
        when(userRepository.findById(999L)).thenReturn(Optional.of(otherStudent));

        assertThrows(ForbiddenException.class, () ->
                solutionService.getStudentSolutions(studentId, authHeader)
        );
    }

    @Test
    void getTaskSolutions_AsTeacher_Success() {
        when(tokenProvider.extractTokenFromHeader(authHeader)).thenReturn(token);
        when(tokenProvider.validateToken(token)).thenReturn(true);
        when(tokenProvider.getUserIdFromToken(token)).thenReturn(teacherId);
        when(userRepository.findById(teacherId)).thenReturn(Optional.of(teacher));
        when(postRepository.findById(taskId)).thenReturn(Optional.of(task));
        when(solutionRepository.findTaskSolutions(taskId)).thenReturn(List.of(solution));
        when(userRepository.findById(studentId)).thenReturn(Optional.of(student));

        List<SolutionDto> results = solutionService.getTaskSolutions(taskId, authHeader);

        assertFalse(results.isEmpty());
        assertEquals(1, results.size());
    }

    @Test
    void getTaskSolutions_AsStudent_ThrowsException() {
        when(tokenProvider.extractTokenFromHeader(authHeader)).thenReturn(token);
        when(tokenProvider.validateToken(token)).thenReturn(true);
        when(tokenProvider.getUserIdFromToken(token)).thenReturn(studentId);
        when(userRepository.findById(studentId)).thenReturn(Optional.of(student));

        assertThrows(ForbiddenException.class, () ->
                solutionService.getTaskSolutions(taskId, authHeader)
        );
    }

    @Test
    void getUngradedSolutions_Success() {
        when(tokenProvider.extractTokenFromHeader(authHeader)).thenReturn(token);
        when(tokenProvider.validateToken(token)).thenReturn(true);
        when(tokenProvider.getUserIdFromToken(token)).thenReturn(teacherId);
        when(userRepository.findById(teacherId)).thenReturn(Optional.of(teacher));
        when(postRepository.findById(taskId)).thenReturn(Optional.of(task));
        when(solutionRepository.findUngradedByTaskId(taskId)).thenReturn(List.of(solution));
        when(userRepository.findById(studentId)).thenReturn(Optional.of(student));

        List<SolutionDto> results = solutionService.getUngradedSolutions(taskId, authHeader);

        assertFalse(results.isEmpty());
        assertEquals(1, results.size());
        assertNull(results.get(0).getMark());
    }

    @Test
    void getStudentTasksWithSolutions_Success() {
        UUID channelId = UUID.randomUUID();
        task.setChannelId(channelId);

        when(tokenProvider.extractTokenFromHeader(authHeader)).thenReturn(token);
        when(tokenProvider.validateToken(token)).thenReturn(true);
        when(tokenProvider.getUserIdFromToken(token)).thenReturn(studentId);
        when(userRepository.findById(studentId)).thenReturn(Optional.of(student));
        when(postRepository.findByChannelIdAndType(channelId, PostType.TASK)).thenReturn(List.of(task));
        when(userRepository.findById(studentId)).thenReturn(Optional.of(student));
        when(solutionRepository.findByTaskIdAndStudentId(taskId, studentId)).thenReturn(Optional.of(solution));

        List<TaskWithSolutionDto> results = solutionService.getStudentTasksWithSolutions(studentId, channelId, authHeader);

        assertFalse(results.isEmpty());
        assertEquals(1, results.size());
        assertEquals(taskId, results.get(0).getTaskId());
        assertNotNull(results.get(0).getSolution());
        assertEquals(solutionId, results.get(0).getSolution().getId());
    }

    @Test
    void deleteSolution_AsOwnerNotGraded_Success() {
        when(tokenProvider.extractTokenFromHeader(authHeader)).thenReturn(token);
        when(tokenProvider.validateToken(token)).thenReturn(true);
        when(tokenProvider.getUserIdFromToken(token)).thenReturn(studentId);
        when(userRepository.findById(studentId)).thenReturn(Optional.of(student));
        when(solutionRepository.findById(solutionId)).thenReturn(Optional.of(solution));
        doNothing().when(fileStorageService).delete(anyString());

        solutionService.deleteSolution(solutionId, authHeader);

        verify(solutionRepository, times(1)).delete(solution);
        verify(fileStorageService, times(1)).delete(anyString());
    }

    @Test
    void deleteSolution_AsOwnerButGraded_ThrowsException() {
        solution.setMark(5);

        when(tokenProvider.extractTokenFromHeader(authHeader)).thenReturn(token);
        when(tokenProvider.validateToken(token)).thenReturn(true);
        when(tokenProvider.getUserIdFromToken(token)).thenReturn(studentId);
        when(userRepository.findById(studentId)).thenReturn(Optional.of(student));
        when(solutionRepository.findById(solutionId)).thenReturn(Optional.of(solution));

        assertThrows(BadRequestException.class, () ->
                solutionService.deleteSolution(solutionId, authHeader)
        );
    }

    @Test
    void deleteSolution_AsManager_Success() {
        User admin = User.builder()
                .id(999L)
                .role(List.of(Role.MANAGER))
                .build();

        when(tokenProvider.extractTokenFromHeader(authHeader)).thenReturn(token);
        when(tokenProvider.validateToken(token)).thenReturn(true);
        when(tokenProvider.getUserIdFromToken(token)).thenReturn(999L);
        when(userRepository.findById(999L)).thenReturn(Optional.of(admin));
        when(solutionRepository.findById(solutionId)).thenReturn(Optional.of(solution));
        doNothing().when(fileStorageService).delete(anyString());

        solutionService.deleteSolution(solutionId, authHeader);

        verify(solutionRepository, times(1)).delete(solution);
    }

    @Test
    void deleteSolution_Unauthorized_ThrowsException() {
        User otherStudent = User.builder()
                .id(999L)
                .role(List.of(Role.STUDENT))
                .build();

        when(tokenProvider.extractTokenFromHeader(authHeader)).thenReturn(token);
        when(tokenProvider.validateToken(token)).thenReturn(true);
        when(tokenProvider.getUserIdFromToken(token)).thenReturn(999L);
        when(userRepository.findById(999L)).thenReturn(Optional.of(otherStudent));
        when(solutionRepository.findById(solutionId)).thenReturn(Optional.of(solution));

        assertThrows(ForbiddenException.class, () ->
                solutionService.deleteSolution(solutionId, authHeader)
        );
    }

    @Test
    void extractUserIdFromHeader_InvalidToken_ThrowsException() {
        when(tokenProvider.extractTokenFromHeader(authHeader)).thenReturn(token);
        when(tokenProvider.validateToken(token)).thenReturn(false);

        assertThrows(BadRequestException.class, () ->
                solutionService.submitSolution(submitSolutionDto, authHeader)
        );
    }

    @Test
    void validateMark_InvalidMark_ThrowsException() {
        gradeSolutionDto.setMark(0);

        when(tokenProvider.extractTokenFromHeader(authHeader)).thenReturn(token);
        when(tokenProvider.validateToken(token)).thenReturn(true);
        when(tokenProvider.getUserIdFromToken(token)).thenReturn(teacherId);
        when(userRepository.findById(teacherId)).thenReturn(Optional.of(teacher));
        when(solutionRepository.findById(solutionId)).thenReturn(Optional.of(solution));

        assertThrows(BadRequestException.class, () ->
                solutionService.gradeSolution(gradeSolutionDto, authHeader)
        );
    }
}