package ru.hits.car_school_automatization.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import ru.hits.car_school_automatization.dto.CreateTaskDto;
import ru.hits.car_school_automatization.dto.TaskDto;
import ru.hits.car_school_automatization.dto.UpdateTaskDto;
import ru.hits.car_school_automatization.entity.Channel;
import ru.hits.car_school_automatization.entity.Task;
import ru.hits.car_school_automatization.entity.TaskDocument;
import ru.hits.car_school_automatization.entity.Team;
import ru.hits.car_school_automatization.entity.User;
import ru.hits.car_school_automatization.enums.Role;
import ru.hits.car_school_automatization.enums.TaskType;
import ru.hits.car_school_automatization.exception.BadRequestException;
import ru.hits.car_school_automatization.exception.ForbiddenException;
import ru.hits.car_school_automatization.exception.NotFoundException;
import ru.hits.car_school_automatization.mapper.TaskMapper;
import ru.hits.car_school_automatization.repository.ChannelRepository;
import ru.hits.car_school_automatization.repository.TaskRepository;
import ru.hits.car_school_automatization.repository.TeamRepository;
import ru.hits.car_school_automatization.repository.UserRepository;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class TaskService {

    private final TaskRepository taskRepository;
    private final ChannelRepository channelRepository;
    private final UserRepository userRepository;
    private final JwtTokenProvider tokenProvider;
    private final TaskMapper taskMapper;
    private final TeamRepository teamRepository;
    private final TeamFormationService teamFormationService;
    private final FileStorageService fileStorageService;

    public TaskDto createTask(CreateTaskDto dto, UUID channelId, String authHeader) {
        Channel channel = channelRepository.findById(channelId)
                .orElseThrow(() -> new NotFoundException("Предмет с id " + channelId + " не найден"));

        validateTeacherLeadsChannel(authHeader, channel);
        validateQualifiedMin(dto.getType(), dto.getQualifiedMin());
        validateMinTeamSize(dto.getMinTeamSize());

        Task task = Task.builder()
                .label(dto.getLabel().trim())
                .text(dto.getText())
                .channel(channel)
                .documents(storeDocumentsFromFiles(dto.getDocuments()))
                .teamType(dto.getTeamType())
                .type(dto.getType())
                .isCanRedistribute(dto.getIsCanRedistribute())
                .qualifiedMin(dto.getQualifiedMin())
                .minTeamSize(dto.getMinTeamSize())
                .votingDeadline(dto.getVotingDeadline())
                .build();

        Task savedTask = taskRepository.save(task);
        List<Team> teams = teamFormationService.formByTeamType(savedTask, List.copyOf(channel.getUsers()), dto);
        if (!teams.isEmpty()) {
            List<Team> savedTeams = teamRepository.saveAll(teams);
            savedTask.setTeams(new HashSet<>(savedTeams));
        }
        return taskMapper.toDto(savedTask);
    }

    public TaskDto updateTask(UUID taskId, UpdateTaskDto dto, String authHeader) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new NotFoundException("Задание с id " + taskId + " не найдено"));

        validateTeacherLeadsChannel(authHeader, task.getChannel());

        taskMapper.updateTaskFromDto(dto, task);
        if (dto.getDocuments() != null) {
            replaceTaskDocuments(task, dto.getDocuments());
        }
        validateQualifiedMin(task.getType(), task.getQualifiedMin());
        validateMinTeamSize(task.getMinTeamSize());

        return taskMapper.toDto(taskRepository.save(task));
    }

    @Transactional
    public TaskDto getTask(UUID taskId, String authHeader) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new NotFoundException("Задание с id " + taskId + " не найдено"));

        validateUserInChannel(authHeader, task.getChannel());
        return taskMapper.toDto(task);
    }

    @Transactional
    public List<TaskDto> getTasksByChannel(UUID channelId, String authHeader) {
        Channel channel = channelRepository.findById(channelId)
                .orElseThrow(() -> new NotFoundException("Предмет с id " + channelId + " не найден"));

        validateUserInChannel(authHeader, channel);
        return taskMapper.toDtoList(taskRepository.findByChannel_Id(channelId));
    }

    public TaskDto copyTask(UUID taskId, UUID targetChannelId, String authHeader) {
        Task source = taskRepository.findById(taskId)
                .orElseThrow(() -> new NotFoundException("Задание с id " + taskId + " не найдено"));

        Channel targetChannel = channelRepository.findById(targetChannelId)
                .orElseThrow(() -> new NotFoundException("Предмет с id " + targetChannelId + " не найден"));

        validateTeacherLeadsChannel(authHeader, targetChannel);

        Task copy = Task.builder()
                .label(source.getLabel())
                .text(source.getText())
                .channel(targetChannel)
                .documents(cloneDocuments(source.getDocuments()))
                .teamType(source.getTeamType())
                .type(source.getType())
                .isCanRedistribute(source.getIsCanRedistribute())
                .qualifiedMin(source.getQualifiedMin())
                .minTeamSize(source.getMinTeamSize())
                .votingDeadline(source.getVotingDeadline())
                .build();

        return taskMapper.toDto(taskRepository.save(copy));
    }

    public void deleteTask(UUID taskId, String authHeader) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new NotFoundException("Задание с id " + taskId + " не найдено"));

        validateTeacherLeadsChannel(authHeader, task.getChannel());
        deleteTaskDocuments(task);
        taskRepository.delete(task);
    }

    public TaskDto addDocument(UUID taskId, MultipartFile file, String authHeader) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new NotFoundException("Задание с id " + taskId + " не найдено"));

        validateTeacherLeadsChannel(authHeader, task.getChannel());
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Файл не может быть пустым");
        }

        String fileUrl = fileStorageService.store(file);
        String fileName = file.getOriginalFilename() != null ? file.getOriginalFilename() : extractFilenameFromUrl(fileUrl);

        if (task.getDocuments() == null) {
            task.setDocuments(new ArrayList<>());
        }
        task.getDocuments().add(TaskDocument.builder().fileName(fileName).fileUrl(fileUrl).build());

        return taskMapper.toDto(taskRepository.save(task));
    }

    public TaskDto removeDocument(UUID taskId, String fileUrl, String authHeader) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new NotFoundException("Задание с id " + taskId + " не найдено"));

        validateTeacherLeadsChannel(authHeader, task.getChannel());
        if (fileUrl == null || fileUrl.isBlank()) {
            throw new BadRequestException("fileUrl обязателен");
        }

        boolean removed = task.getDocuments().removeIf(doc -> fileUrl.equals(doc.getFileUrl()));
        if (!removed) {
            throw new NotFoundException("Документ с таким fileUrl не найден");
        }

        String filename = extractFilenameFromUrl(fileUrl);
        if (filename != null) {
            fileStorageService.delete(filename);
        }

        return taskMapper.toDto(taskRepository.save(task));
    }

    private void validateQualifiedMin(TaskType type, Integer qualifiedMin) {
        if (type == TaskType.QUALIFIED && qualifiedMin == null) {
            throw new BadRequestException("Для QUALIFIED требуется qualifiedMin");
        }
        if (type != TaskType.QUALIFIED && qualifiedMin != null) {
            throw new BadRequestException("qualifiedMin задается только для QUALIFIED");
        }
    }

    private void validateMinTeamSize(Integer minTeamSize) {
        if (minTeamSize != null && minTeamSize < 1) {
            throw new BadRequestException("minTeamSize должен быть больше 0");
        }
    }

    private void validateTeacherLeadsChannel(String authHeader, Channel channel) {
        Long requesterId = tokenProvider.extractUserIdFromHeader(authHeader);
        User requester = userRepository.findById(requesterId)
                .orElseThrow(() -> new NotFoundException("Пользователь с id " + requesterId + " не найден"));

        if (!requester.getRole().contains(Role.TEACHER)) {
            throw new ForbiddenException("Только преподаватель может управлять заданиями");
        }

        boolean teachesChannel = channel.getUsers().stream()
                .anyMatch(user -> user.getId().equals(requesterId));

        if (!teachesChannel) {
            throw new ForbiddenException("Преподаватель не ведет этот предмет");
        }
    }

    private void validateUserInChannel(String authHeader, Channel channel) {
        Long userId = tokenProvider.extractUserIdFromHeader(authHeader);
        boolean inChannel = channel.getUsers().stream().anyMatch(user -> user.getId().equals(userId));
        if (!inChannel) {
            throw new ForbiddenException("У пользователя нет доступа к этому предмету");
        }
    }

    private List<TaskDocument> storeDocumentsFromFiles(List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            return new ArrayList<>();
        }
        return files.stream()
                .filter(file -> file != null && !file.isEmpty())
                .map(file -> {
                    String fileUrl = fileStorageService.store(file);
                    String fileName = file.getOriginalFilename() != null ? file.getOriginalFilename() : extractFilenameFromUrl(fileUrl);
                    return TaskDocument.builder()
                            .fileName(fileName)
                            .fileUrl(fileUrl)
                            .build();
                })
                .toList();
    }

    private List<TaskDocument> cloneDocuments(List<TaskDocument> documents) {
        if (documents == null || documents.isEmpty()) {
            return new ArrayList<>();
        }
        return documents.stream()
                .map(doc -> TaskDocument.builder().fileName(doc.getFileName()).fileUrl(doc.getFileUrl()).build())
                .toList();
    }

    private void deleteTaskDocuments(Task task) {
        if (task.getDocuments() == null || task.getDocuments().isEmpty()) {
            return;
        }
        task.getDocuments().forEach(doc -> {
            String filename = extractFilenameFromUrl(doc.getFileUrl());
            if (filename != null) {
                fileStorageService.delete(filename);
            }
        });
    }

    private void replaceTaskDocuments(Task task, List<MultipartFile> files) {
        deleteTaskDocuments(task);
        task.setDocuments(storeDocumentsFromFiles(files));
    }

    private String extractFilenameFromUrl(String fileUrl) {
        if (fileUrl == null || !fileUrl.contains("/file/")) {
            return null;
        }
        return fileUrl.substring(fileUrl.lastIndexOf("/") + 1);
    }
}
