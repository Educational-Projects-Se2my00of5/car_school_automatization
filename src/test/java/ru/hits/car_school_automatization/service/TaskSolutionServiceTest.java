package ru.hits.car_school_automatization.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;
import ru.hits.car_school_automatization.dto.CreateTaskSolutionDto;
import ru.hits.car_school_automatization.dto.TaskSolutionDto;
import ru.hits.car_school_automatization.dto.UpdateTaskSolutionDto;
import ru.hits.car_school_automatization.entity.Channel;
import ru.hits.car_school_automatization.entity.Task;
import ru.hits.car_school_automatization.entity.TaskDocument;
import ru.hits.car_school_automatization.entity.TaskSolution;
import ru.hits.car_school_automatization.entity.User;
import ru.hits.car_school_automatization.enums.Role;
import ru.hits.car_school_automatization.enums.TaskType;
import ru.hits.car_school_automatization.enums.TeamType;
import ru.hits.car_school_automatization.exception.BadRequestException;
import ru.hits.car_school_automatization.exception.ForbiddenException;
import ru.hits.car_school_automatization.repository.TaskRepository;
import ru.hits.car_school_automatization.repository.TaskSolutionRepository;
import ru.hits.car_school_automatization.repository.UserRepository;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskSolutionServiceTest {

    @Mock
    private TaskSolutionRepository taskSolutionRepository;

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtTokenProvider tokenProvider;

    @Mock
    private FileStorageService fileStorageService;

    @InjectMocks
    private TaskSolutionService taskSolutionService;

    private UUID taskId;
    private UUID solutionId;
    private Long studentId;
    private Long teacherId;
    private String authHeader;
    private User student;
    private User teacher;
    private Task task;

    @BeforeEach
    void setUp() {
        taskId = UUID.randomUUID();
        solutionId = UUID.randomUUID();
        studentId = 100L;
        teacherId = 200L;
        authHeader = "Bearer token";

        student = User.builder().id(studentId).role(List.of(Role.STUDENT)).build();
        teacher = User.builder().id(teacherId).role(List.of(Role.TEACHER)).build();

        Channel channel = new Channel(
                UUID.randomUUID(),
                "Channel",
                "Desc",
                null,
                new HashSet<>(Set.of(student, teacher)),
                teacher
        );

        task = Task.builder()
                .id(taskId)
                .label("Task")
                .text("Task text")
                .channel(channel)
                .documents(List.of())
                .teamType(TeamType.FREE)
                .type(TaskType.FREE)
                .minTeamSize(2)
                .isCanRedistribute(false)
                .build();
    }

    @Test
    @DisplayName("create: студент может создать решение с документами")
    void create_WhenStudentAndValidDocuments_ShouldCreate() {
        MultipartFile file = org.mockito.Mockito.mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getOriginalFilename()).thenReturn("answer.pdf");

        CreateTaskSolutionDto dto = CreateTaskSolutionDto.builder()
                .documents(List.of(file))
                .build();

        when(tokenProvider.extractUserIdFromHeader(authHeader)).thenReturn(studentId);
        when(userRepository.findById(studentId)).thenReturn(Optional.of(student));
        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));
        when(taskSolutionRepository.existsByTaskIdAndStudentId(taskId, studentId)).thenReturn(false);
        when(fileStorageService.store(file)).thenReturn("/file/stored.pdf");
        when(taskSolutionRepository.save(any(TaskSolution.class))).thenAnswer(invocation -> {
            TaskSolution saved = invocation.getArgument(0);
            saved.setId(solutionId);
            saved.setCreatedAt(Instant.now());
            return saved;
        });

        TaskSolutionDto result = taskSolutionService.create(taskId, dto, authHeader);

        assertNotNull(result);
        assertEquals(solutionId, result.getId());
        assertEquals(taskId, result.getTaskId());
        assertEquals(1, result.getDocuments().size());
        assertEquals("answer.pdf", result.getDocuments().get(0).getFileName());
        verify(taskSolutionRepository).save(any(TaskSolution.class));
    }

    @Test
    @DisplayName("create: без документов возвращает BadRequest")
    void create_WhenNoDocuments_ShouldThrowBadRequest() {
        CreateTaskSolutionDto dto = CreateTaskSolutionDto.builder()
                .documents(List.of())
                .build();

        when(tokenProvider.extractUserIdFromHeader(authHeader)).thenReturn(studentId);
        when(userRepository.findById(studentId)).thenReturn(Optional.of(student));
        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));
        when(taskSolutionRepository.existsByTaskIdAndStudentId(taskId, studentId)).thenReturn(false);

        assertThrows(BadRequestException.class, () -> taskSolutionService.create(taskId, dto, authHeader));
        verify(taskSolutionRepository, never()).save(any(TaskSolution.class));
    }

    @Test
    @DisplayName("create: преподаватель не может отправить решение")
    void create_WhenNotStudent_ShouldThrowForbidden() {
        MultipartFile file = org.mockito.Mockito.mock(MultipartFile.class);

        CreateTaskSolutionDto dto = CreateTaskSolutionDto.builder()
                .documents(List.of(file))
                .build();

        when(tokenProvider.extractUserIdFromHeader(authHeader)).thenReturn(teacherId);
        when(userRepository.findById(teacherId)).thenReturn(Optional.of(teacher));
        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));

        assertThrows(ForbiddenException.class, () -> taskSolutionService.create(taskId, dto, authHeader));
        verify(taskSolutionRepository, never()).save(any(TaskSolution.class));
    }

    @Test
    @DisplayName("getById: чужое решение для студента недоступно")
    void getById_WhenStudentReadsForeignSolution_ShouldThrowForbidden() {
        Long otherStudentId = 300L;
        User otherStudent = User.builder().id(otherStudentId).role(List.of(Role.STUDENT)).build();

        TaskSolution solution = TaskSolution.builder()
                .id(solutionId)
                .taskId(taskId)
                .studentId(otherStudentId)
                .documents(List.of())
                .build();

        when(tokenProvider.extractUserIdFromHeader(authHeader)).thenReturn(studentId);
        when(userRepository.findById(studentId)).thenReturn(Optional.of(student));
        when(taskSolutionRepository.findById(solutionId)).thenReturn(Optional.of(solution));
        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));

        assertThrows(ForbiddenException.class, () -> taskSolutionService.getById(solutionId, authHeader));
    }

    @Test
    @DisplayName("update: владелец может заменить документы")
    void update_WhenOwnerReplacesDocuments_ShouldUpdateAndDeleteOldFiles() {
        MultipartFile newFile = org.mockito.Mockito.mock(MultipartFile.class);
        when(newFile.isEmpty()).thenReturn(false);
        when(newFile.getOriginalFilename()).thenReturn("new.docx");

        TaskSolution existing = TaskSolution.builder()
                .id(solutionId)
                .taskId(taskId)
                .studentId(studentId)
                .documents(List.of(TaskDocument.builder().fileName("old.pdf").fileUrl("/file/old.pdf").build()))
                .build();

        UpdateTaskSolutionDto dto = UpdateTaskSolutionDto.builder()
                .documents(List.of(newFile))
                .build();

        when(tokenProvider.extractUserIdFromHeader(authHeader)).thenReturn(studentId);
        when(taskSolutionRepository.findById(solutionId)).thenReturn(Optional.of(existing));
        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));
        when(fileStorageService.store(newFile)).thenReturn("/file/new.docx");
        when(taskSolutionRepository.save(existing)).thenReturn(existing);

        TaskSolutionDto result = taskSolutionService.update(solutionId, dto, authHeader);

        assertEquals(1, result.getDocuments().size());
        assertEquals("new.docx", result.getDocuments().get(0).getFileName());
        verify(fileStorageService).delete("old.pdf");
        verify(fileStorageService).store(newFile);
        verify(taskSolutionRepository).save(existing);
    }

    @Test
    @DisplayName("update: если после замены список пустой, возвращает BadRequest")
    void update_WhenDocumentsBecomeEmpty_ShouldThrowBadRequest() {
        TaskSolution existing = TaskSolution.builder()
                .id(solutionId)
                .taskId(taskId)
                .studentId(studentId)
                .documents(List.of(TaskDocument.builder().fileName("old.pdf").fileUrl("/file/old.pdf").build()))
                .build();

        MultipartFile emptyFile = org.mockito.Mockito.mock(MultipartFile.class);
        when(emptyFile.isEmpty()).thenReturn(true);

        UpdateTaskSolutionDto dto = UpdateTaskSolutionDto.builder()
                .documents(List.of(emptyFile))
                .build();

        when(tokenProvider.extractUserIdFromHeader(authHeader)).thenReturn(studentId);
        when(taskSolutionRepository.findById(solutionId)).thenReturn(Optional.of(existing));
        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));

        assertThrows(BadRequestException.class, () -> taskSolutionService.update(solutionId, dto, authHeader));
        verify(taskSolutionRepository, never()).save(any(TaskSolution.class));
    }

    @Test
    @DisplayName("delete: преподаватель может удалить решение и файлы")
    void delete_WhenTeacher_ShouldDeleteSolutionAndFiles() {
        TaskSolution solution = TaskSolution.builder()
                .id(solutionId)
                .taskId(taskId)
                .studentId(studentId)
                .documents(List.of(
                        TaskDocument.builder().fileName("a.txt").fileUrl("/file/a.txt").build(),
                        TaskDocument.builder().fileName("b.txt").fileUrl("/file/b.txt").build()
                ))
                .build();

        when(tokenProvider.extractUserIdFromHeader(authHeader)).thenReturn(teacherId);
        when(userRepository.findById(teacherId)).thenReturn(Optional.of(teacher));
        when(taskSolutionRepository.findById(solutionId)).thenReturn(Optional.of(solution));
        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));

        taskSolutionService.delete(solutionId, authHeader);

        verify(fileStorageService).delete("a.txt");
        verify(fileStorageService).delete("b.txt");
        verify(taskSolutionRepository).delete(solution);
    }

    @Test
    @DisplayName("getMySolutions: возвращает решения текущего пользователя")
    void getMySolutions_ShouldReturnCurrentUserSolutions() {
        TaskSolution s1 = TaskSolution.builder().id(UUID.randomUUID()).taskId(taskId).studentId(studentId).documents(List.of()).build();
        TaskSolution s2 = TaskSolution.builder().id(UUID.randomUUID()).taskId(taskId).studentId(studentId).documents(List.of()).build();

        when(tokenProvider.extractUserIdFromHeader(authHeader)).thenReturn(studentId);
        when(taskSolutionRepository.findByStudentIdOrderByCreatedAtDesc(studentId)).thenReturn(List.of(s1, s2));

        List<TaskSolutionDto> result = taskSolutionService.getMySolutions(authHeader);

        assertEquals(2, result.size());
        verify(taskSolutionRepository).findByStudentIdOrderByCreatedAtDesc(studentId);
    }
}
