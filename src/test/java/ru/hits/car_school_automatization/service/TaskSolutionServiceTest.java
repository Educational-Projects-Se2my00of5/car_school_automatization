package ru.hits.car_school_automatization.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;
import ru.hits.car_school_automatization.dto.*;
import ru.hits.car_school_automatization.entity.*;
import ru.hits.car_school_automatization.enums.Role;
import ru.hits.car_school_automatization.enums.TaskType;
import ru.hits.car_school_automatization.enums.TeamType;
import ru.hits.car_school_automatization.exception.BadRequestException;
import ru.hits.car_school_automatization.exception.ForbiddenException;
import ru.hits.car_school_automatization.repository.*;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

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

    @Mock
    private TeamRepository teamRepository;

    @Mock
    private SolutionVoteRepository solutionVoteRepository;

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

    @Test
    @DisplayName("vote: студент может проголосовать за решение")
    void vote_ShouldSuccess() {
        UUID voteTaskId = taskId;
        UUID voteSolutionId = UUID.randomUUID();
        Long voterId = studentId;

        task.setType(TaskType.DEMOCRATIC);

        TaskSolution solution = TaskSolution.builder()
                .id(voteSolutionId)
                .taskId(voteTaskId)
                .studentId(300L)
                .build();

        Team team = Team.builder()
                .id(UUID.randomUUID())
                .users(new HashSet<>(Set.of(student)))
                .build();

        CreateSolutionVoteDto dto = CreateSolutionVoteDto.builder()
                .taskId(voteTaskId)
                .solutionId(voteSolutionId)
                .build();

        when(tokenProvider.extractUserIdFromHeader(authHeader)).thenReturn(voterId);
        when(userRepository.findById(voterId)).thenReturn(Optional.of(student));
        when(taskRepository.findById(voteTaskId)).thenReturn(Optional.of(task));
        when(teamRepository.findByTask_IdAndUsers_Id(voteTaskId, voterId)).thenReturn(Optional.of(team));
        when(taskSolutionRepository.findById(voteSolutionId)).thenReturn(Optional.of(solution));
        when(solutionVoteRepository.existsByTaskIdAndVoterId(voteTaskId, voterId)).thenReturn(false);
        when(solutionVoteRepository.save(any(SolutionVote.class))).thenAnswer(i -> i.getArgument(0));

        SolutionVoteDto result = taskSolutionService.vote(dto, authHeader);

        assertNotNull(result);
        assertEquals(voteTaskId, result.getTaskId());
        assertEquals(voteSolutionId, result.getSolutionId());
        assertEquals(voterId, result.getVoterId());
        verify(solutionVoteRepository).save(any(SolutionVote.class));
    }

    @Test
    @DisplayName("vote: нельзя голосовать за своё решение")
    void vote_ForOwnSolution_ShouldThrowBadRequest() {
        UUID voteTaskId = taskId;
        UUID voteSolutionId = UUID.randomUUID();
        Long voterId = studentId;

        task.setType(TaskType.DEMOCRATIC);

        TaskSolution solution = TaskSolution.builder()
                .id(voteSolutionId)
                .taskId(voteTaskId)
                .studentId(voterId)
                .build();

        Team team = Team.builder()
                .id(UUID.randomUUID())
                .users(new HashSet<>(Set.of(student)))
                .build();

        CreateSolutionVoteDto dto = CreateSolutionVoteDto.builder()
                .taskId(voteTaskId)
                .solutionId(voteSolutionId)
                .build();

        when(tokenProvider.extractUserIdFromHeader(authHeader)).thenReturn(voterId);
        when(userRepository.findById(voterId)).thenReturn(Optional.of(student));
        when(taskRepository.findById(voteTaskId)).thenReturn(Optional.of(task));
        when(teamRepository.findByTask_IdAndUsers_Id(voteTaskId, voterId)).thenReturn(Optional.of(team));
        when(taskSolutionRepository.findById(voteSolutionId)).thenReturn(Optional.of(solution));

        assertThrows(BadRequestException.class, () -> taskSolutionService.vote(dto, authHeader));
        verify(solutionVoteRepository, never()).save(any(SolutionVote.class));
    }

    @Test
    @DisplayName("vote: повторное голосование запрещено")
    void vote_AlreadyVoted_ShouldThrowBadRequest() {
        UUID voteTaskId = taskId;
        UUID voteSolutionId = UUID.randomUUID();
        Long voterId = studentId;

        TaskSolution solution = TaskSolution.builder()
                .id(voteSolutionId)
                .taskId(voteTaskId)
                .studentId(300L)
                .build();

        task.setType(TaskType.DEMOCRATIC);

        Team team = Team.builder()
                .id(UUID.randomUUID())
                .users(new HashSet<>(Set.of(student)))
                .build();

        CreateSolutionVoteDto dto = CreateSolutionVoteDto.builder()
                .taskId(voteTaskId)
                .solutionId(voteSolutionId)
                .build();

        when(tokenProvider.extractUserIdFromHeader(authHeader)).thenReturn(voterId);
        when(userRepository.findById(voterId)).thenReturn(Optional.of(student));
        when(taskRepository.findById(voteTaskId)).thenReturn(Optional.of(task));
        when(teamRepository.findByTask_IdAndUsers_Id(voteTaskId, voterId)).thenReturn(Optional.of(team));
        when(taskSolutionRepository.findById(voteSolutionId)).thenReturn(Optional.of(solution));
        when(solutionVoteRepository.existsByTaskIdAndVoterId(voteTaskId, voterId)).thenReturn(true);

        assertThrows(BadRequestException.class, () -> taskSolutionService.vote(dto, authHeader));
        verify(solutionVoteRepository, never()).save(any(SolutionVote.class));
    }

    @Test
    @DisplayName("cancelVote: студент может отменить свой голос")
    void cancelVote_ShouldSuccess() {
        UUID voteTaskId = taskId;
        Long voterId = studentId;

        SolutionVote vote = SolutionVote.builder()
                .id(UUID.randomUUID())
                .taskId(voteTaskId)
                .voterId(voterId)
                .build();

        when(tokenProvider.extractUserIdFromHeader(authHeader)).thenReturn(voterId);
        when(taskRepository.findById(voteTaskId)).thenReturn(Optional.of(task));
        when(solutionVoteRepository.findByTaskIdAndVoterId(voteTaskId, voterId)).thenReturn(Optional.of(vote));

        taskSolutionService.cancelVote(voteTaskId, authHeader);

        verify(solutionVoteRepository).delete(vote);
    }
}
