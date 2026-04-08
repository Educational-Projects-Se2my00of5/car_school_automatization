package ru.hits.car_school_automatization.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import ru.hits.car_school_automatization.dto.CreateTaskSolutionDto;
import ru.hits.car_school_automatization.dto.TaskDocumentDto;
import ru.hits.car_school_automatization.dto.TaskSolutionDto;
import ru.hits.car_school_automatization.dto.UpdateTaskSolutionDto;
import ru.hits.car_school_automatization.entity.Task;
import ru.hits.car_school_automatization.entity.TaskDocument;
import ru.hits.car_school_automatization.entity.TaskSolution;
import ru.hits.car_school_automatization.entity.User;
import ru.hits.car_school_automatization.enums.Role;
import ru.hits.car_school_automatization.exception.BadRequestException;
import ru.hits.car_school_automatization.exception.ForbiddenException;
import ru.hits.car_school_automatization.exception.NotFoundException;
import ru.hits.car_school_automatization.repository.TaskRepository;
import ru.hits.car_school_automatization.repository.TaskSolutionRepository;
import ru.hits.car_school_automatization.repository.UserRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class TaskSolutionService {

    private final TaskSolutionRepository taskSolutionRepository;
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final JwtTokenProvider tokenProvider;
    private final FileStorageService fileStorageService;

    public TaskSolutionDto create(UUID taskId, CreateTaskSolutionDto dto, String authHeader) {
        Long userId = tokenProvider.extractUserIdFromHeader(authHeader);
        User user = getUserById(userId);

        Task task = getTaskById(taskId);
        validateUserInTaskChannel(userId, task);

        if (!user.getRole().contains(Role.STUDENT)) {
            throw new ForbiddenException("Только студент может отправить решение");
        }

        if (taskSolutionRepository.existsByTaskIdAndStudentId(task.getId(), userId)) {
            throw new BadRequestException("Вы уже отправили решение на это задание");
        }

        List<TaskDocument> documents = storeDocumentsFromFiles(dto.getDocuments());
        if (documents.isEmpty()) {
            throw new BadRequestException("Добавьте хотя бы один файл");
        }

        TaskSolution solution = TaskSolution.builder()
                .taskId(task.getId())
                .studentId(userId)
                .documents(documents)
                .build();

        return toDto(taskSolutionRepository.save(solution));
    }

    @Transactional
    public TaskSolutionDto getById(UUID solutionId, String authHeader) {
        Long requesterId = tokenProvider.extractUserIdFromHeader(authHeader);
        User requester = getUserById(requesterId);

        TaskSolution solution = getSolutionById(solutionId);
        Task task = getTaskById(solution.getTaskId());
        validateCanReadSolution(requester, requesterId, solution, task);

        return toDto(solution);
    }

    @Transactional
    public List<TaskSolutionDto> getByTask(UUID taskId, String authHeader) {
        Long requesterId = tokenProvider.extractUserIdFromHeader(authHeader);
        Task task = getTaskById(taskId);
        validateUserInTaskChannel(requesterId, task);

        return taskSolutionRepository.findByTaskIdOrderByCreatedAtDesc(taskId)
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public List<TaskSolutionDto> getMySolutions(String authHeader) {
        Long requesterId = tokenProvider.extractUserIdFromHeader(authHeader);

        return taskSolutionRepository.findByStudentIdOrderByCreatedAtDesc(requesterId)
                .stream()
                .map(this::toDto)
                .toList();
    }

    public TaskSolutionDto update(UUID solutionId, UpdateTaskSolutionDto dto, String authHeader) {
        Long requesterId = tokenProvider.extractUserIdFromHeader(authHeader);
        TaskSolution solution = getSolutionById(solutionId);

        if (!solution.getStudentId().equals(requesterId)) {
            throw new ForbiddenException("Вы можете редактировать только свои решения");
        }

        Task task = getTaskById(solution.getTaskId());
        validateUserInTaskChannel(requesterId, task);

        if (dto.getDocuments() != null) {
            replaceDocuments(solution, dto.getDocuments());
        }

        if (solution.getDocuments() == null || solution.getDocuments().isEmpty()) {
            throw new BadRequestException("Добавьте хотя бы один файл");
        }

        return toDto(taskSolutionRepository.save(solution));
    }

    public void delete(UUID solutionId, String authHeader) {
        Long requesterId = tokenProvider.extractUserIdFromHeader(authHeader);
        User requester = getUserById(requesterId);

        TaskSolution solution = getSolutionById(solutionId);
        Task task = getTaskById(solution.getTaskId());
        validateCanDeleteSolution(requester, requesterId, solution, task);

        deleteSolutionDocuments(solution);
        taskSolutionRepository.delete(solution);
    }

    private void replaceDocuments(TaskSolution solution, List<MultipartFile> files) {
        deleteSolutionDocuments(solution);
        solution.setDocuments(storeDocumentsFromFiles(files));
    }

    private List<TaskDocument> storeDocumentsFromFiles(List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            return new ArrayList<>();
        }

        return files.stream()
                .filter(file -> file != null && !file.isEmpty())
                .map(file -> {
                    String fileUrl = fileStorageService.store(file);
                    String fileName = file.getOriginalFilename() != null
                            ? file.getOriginalFilename()
                            : extractFilenameFromUrl(fileUrl);
                    return TaskDocument.builder()
                            .fileName(fileName)
                            .fileUrl(fileUrl)
                            .build();
                })
                .toList();
    }

    private void deleteSolutionDocuments(TaskSolution solution) {
        if (solution.getDocuments() == null || solution.getDocuments().isEmpty()) {
            return;
        }
        solution.getDocuments().forEach(document -> {
            String filename = extractFilenameFromUrl(document.getFileUrl());
            if (filename != null) {
                fileStorageService.delete(filename);
            }
        });
    }

    private TaskSolution getSolutionById(UUID solutionId) {
        return taskSolutionRepository.findById(solutionId)
                .orElseThrow(() -> new NotFoundException("TaskSolution с id " + solutionId + " не найден"));
    }

    private Task getTaskById(UUID taskId) {
        return taskRepository.findById(taskId)
                .orElseThrow(() -> new NotFoundException("Задание с id " + taskId + " не найдено"));
    }

    private User getUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь с id " + userId + " не найден"));
    }

    private void validateCanReadSolution(User requester, Long requesterId, TaskSolution solution, Task task) {
        if (isTeacherOrManager(requester)) {
            validateUserInTaskChannel(requesterId, task);
            return;
        }

        if (!solution.getStudentId().equals(requesterId)) {
            throw new ForbiddenException("У вас нет прав на просмотр этого решения");
        }

        validateUserInTaskChannel(requesterId, task);
    }

    private void validateCanDeleteSolution(User requester, Long requesterId, TaskSolution solution, Task task) {
        validateUserInTaskChannel(requesterId, task);

        if (solution.getStudentId().equals(requesterId)) {
            return;
        }

        if (!isTeacherOrManager(requester)) {
            throw new ForbiddenException("Можно удалить только свое решение");
        }
    }

    private boolean isTeacherOrManager(User user) {
        return user.getRole().contains(Role.TEACHER) || user.getRole().contains(Role.MANAGER);
    }

    private void validateUserInTaskChannel(Long userId, Task task) {
        boolean inChannel = task.getChannel().getUsers().stream()
                .anyMatch(channelUser -> channelUser.getId().equals(userId));

        if (!inChannel) {
            throw new ForbiddenException("У пользователя нет доступа к этому предмету");
        }
    }

    private String extractFilenameFromUrl(String fileUrl) {
        if (fileUrl == null || !fileUrl.contains("/file/")) {
            return null;
        }
        return fileUrl.substring(fileUrl.lastIndexOf("/") + 1);
    }

    private TaskSolutionDto toDto(TaskSolution solution) {
        return TaskSolutionDto.builder()
                .id(solution.getId())
                .taskId(solution.getTaskId())
                .studentId(solution.getStudentId())
                .documents(solution.getDocuments() == null ? new ArrayList<>() : solution.getDocuments().stream()
                        .map(document -> TaskDocumentDto.builder()
                                .fileName(document.getFileName())
                                .fileUrl(document.getFileUrl())
                                .build())
                        .toList())
                .mark(solution.getMark())
                .createdAt(solution.getCreatedAt())
                .updatedAt(solution.getUpdatedAt())
                .build();
    }
}
