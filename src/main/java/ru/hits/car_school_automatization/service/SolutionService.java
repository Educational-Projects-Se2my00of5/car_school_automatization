package ru.hits.car_school_automatization.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
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
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class SolutionService {

    private final SolutionRepository solutionRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final JwtTokenProvider tokenProvider;
    private final FileStorageService fileStorageService;

    private static final int MIN_MARK = 1;
    private static final int MAX_MARK = 5;

    /**
     * Студент отправляет решение
     */
    public SolutionDto submitSolution(SubmitSolutionDto submitDto, String authHeader) {
        Long studentId = extractUserIdFromHeader(authHeader);
        User student = getUserById(studentId);

        // Проверяем задание
        Post task = postRepository.findById(submitDto.getTaskId())
                .orElseThrow(() -> new NotFoundException("Задание с id " + submitDto.getTaskId() + " не найдено"));

        if (task.getType() != PostType.TASK) {
            throw new BadRequestException("Решения можно отправлять только для заданий (TASK)");
        }

        // Проверяем, не отправлял ли уже
        if (solutionRepository.existsByTaskIdAndStudentId(submitDto.getTaskId(), studentId)) {
            throw new BadRequestException("Вы уже отправили решение на это задание");
        }

        // Сохраняем файл
        String fileUrl = null;
        String fileName = null;
        if (submitDto.getFile() != null && !submitDto.getFile().isEmpty()) {
            fileUrl = fileStorageService.store(submitDto.getFile());
            fileName = submitDto.getFile().getOriginalFilename();
        }

        // Проверяем, что есть хоть что-то
        if ((submitDto.getText() == null || submitDto.getText().trim().isEmpty()) && fileUrl == null) {
            throw new BadRequestException("Добавьте текст решения или файл");
        }

        Solution solution = Solution.builder()
                .studentId(studentId)
                .taskId(submitDto.getTaskId())
                .text(submitDto.getText())
                .fileUrl(fileUrl)
                .fileName(fileName)
                .build();

        Solution savedSolution = solutionRepository.save(solution);
        log.info("Студент {} отправил решение на задание {}", studentId, submitDto.getTaskId());

        return mapToDto(savedSolution, task.getLabel(),
                student.getFirstName() + " " + student.getLastName(), null);
    }

    /**
     * Студент обновляет своё решение (пока не оценено)
     */
    public SolutionDto updateSolution(UUID solutionId, UpdateSolutionDto updateDto, String authHeader) {
        Long studentId = extractUserIdFromHeader(authHeader);

        Solution solution = solutionRepository.findById(solutionId)
                .orElseThrow(() -> new NotFoundException("Решение с id " + solutionId + " не найдено"));

        // Проверяем, что это решение принадлежит студенту
        if (!solution.getStudentId().equals(studentId)) {
            throw new ForbiddenException("Вы можете редактировать только свои решения");
        }

        // Проверяем, не оценено ли уже
        if (solution.getMark() != null) {
            throw new BadRequestException("Нельзя редактировать уже оценённое решение");
        }

        Post task = postRepository.findById(solution.getTaskId())
                .orElseThrow(() -> new NotFoundException("Задание не найдено"));
        User student = getUserById(studentId);

        // Обновляем текст
        if (updateDto.getText() != null) {
            solution.setText(updateDto.getText());
        }

        if (solution.getFileUrl() != null) {
            String oldFilename = extractFilenameFromUrl(solution.getFileUrl());
            if (oldFilename != null) {
                fileStorageService.delete(oldFilename);
            }
            solution.setFileUrl(null);
            solution.setFileName(null);
        }

        MultipartFile newFile = updateDto.getFile();
        if (newFile != null && !newFile.isEmpty()) {
            String fileUrl = fileStorageService.store(newFile);
            solution.setFileUrl(fileUrl);
            solution.setFileName(newFile.getOriginalFilename());
        }

        Solution updatedSolution = solutionRepository.save(solution);
        log.info("Студент {} обновил решение {}", studentId, solutionId);

        return mapToDto(updatedSolution, task.getLabel(),
                student.getFirstName() + " " + student.getLastName(), null);
    }

    /**
     * Преподаватель или менеджер оценивает решение
     */
    public SolutionDto gradeSolution(GradeSolutionDto gradeDto, String authHeader) {
        Long teacherId = extractUserIdFromHeader(authHeader);
        User teacher = getUserById(teacherId);

        // Проверяем, что пользователь преподаватель или менеджер
        if (!isTeacherOrManager(teacher)) {
            throw new ForbiddenException("Только преподаватели могут оценивать решения");
        }

        Solution solution = solutionRepository.findById(gradeDto.getSolutionId())
                .orElseThrow(() -> new NotFoundException("Решение с id " + gradeDto.getSolutionId() + " не найдено"));

        // Валидация оценки
        validateMark(gradeDto.getMark());

        solution.setMark(gradeDto.getMark());
        solution.setTeacherId(teacherId);
        solution.setMarkedAt(LocalDateTime.now());

        Solution gradedSolution = solutionRepository.save(solution);
        log.info("Преподаватель {} оценил решение {} на {}", teacherId, gradeDto.getSolutionId(), gradeDto.getMark());

        Post task = postRepository.findById(solution.getTaskId())
                .orElseThrow(() -> new NotFoundException("Задание не найдено"));
        User student = getUserById(solution.getStudentId());

        return mapToDto(gradedSolution, task.getLabel(),
                student.getFirstName() + " " + student.getLastName(),
                teacher.getFirstName() + " " + teacher.getLastName());
    }

    /**
     * Получить решение по ID
     */
    public SolutionDto getSolutionById(UUID solutionId, String authHeader) {
        Long userId = extractUserIdFromHeader(authHeader);
        User user = getUserById(userId);

        Solution solution = solutionRepository.findById(solutionId)
                .orElseThrow(() -> new NotFoundException("Решение с id " + solutionId + " не найдено"));

        // Проверка прав
        if (!solution.getStudentId().equals(userId) && !isTeacherOrManager(user)) {
            throw new ForbiddenException("У вас нет прав на просмотр этого решения");
        }

        Post task = postRepository.findById(solution.getTaskId())
                .orElseThrow(() -> new NotFoundException("Задание не найдено"));
        User student = getUserById(solution.getStudentId());
        User teacher = solution.getTeacherId() != null ? getUserById(solution.getTeacherId()) : null;

        return mapToDto(solution, task.getLabel(),
                student.getFirstName() + " " + student.getLastName(),
                teacher != null ? teacher.getFirstName() + " " + teacher.getLastName() : null);
    }

    /**
     * Получить все решения студента
     */
    public List<SolutionDto> getStudentSolutions(Long studentId, String authHeader) {
        Long userId = extractUserIdFromHeader(authHeader);
        User user = getUserById(userId);

        if (!userId.equals(studentId) && !isTeacherOrManager(user)) {
            throw new ForbiddenException("У вас нет прав на просмотр решений этого студента");
        }

        User student = getUserById(studentId);

        return solutionRepository.findStudentSolutions(studentId).stream()
                .map(solution -> {
                    Post task = postRepository.findById(solution.getTaskId()).orElse(null);
                    User teacher = solution.getTeacherId() != null ? getUserById(solution.getTeacherId()) : null;

                    return mapToDto(solution,
                            task != null ? task.getLabel() : null,
                            student.getFirstName() + " " + student.getLastName(),
                            teacher != null ? teacher.getFirstName() + " " + teacher.getLastName() : null);
                })
                .collect(Collectors.toList());
    }

    /**
     * Получить все решения по заданию (для преподавателя или менеджера)
     */
    public List<SolutionDto> getTaskSolutions(UUID taskId, String authHeader) {
        Long userId = extractUserIdFromHeader(authHeader);
        User user = getUserById(userId);

        if (!isTeacherOrManager(user)) {
            throw new ForbiddenException("Только преподаватели и менеджеры могут просматривать все решения");
        }

        Post task = postRepository.findById(taskId)
                .orElseThrow(() -> new NotFoundException("Задание с id " + taskId + " не найдено"));

        return solutionRepository.findTaskSolutions(taskId).stream()
                .map(solution -> {
                    User student = getUserById(solution.getStudentId());
                    User teacher = solution.getTeacherId() != null ? getUserById(solution.getTeacherId()) : null;

                    return mapToDto(solution,
                            task.getLabel(),
                            student.getFirstName() + " " + student.getLastName(),
                            teacher != null ? teacher.getFirstName() + " " + teacher.getLastName() : null);
                })
                .collect(Collectors.toList());
    }

    /**
     * Получить неоценённые решения по заданию
     */
    public List<SolutionDto> getUngradedSolutions(UUID taskId, String authHeader) {
        Long userId = extractUserIdFromHeader(authHeader);
        User user = getUserById(userId);

        if (!isTeacherOrManager(user)) {
            throw new ForbiddenException("Только преподаватели и менеджеры могут просматривать неоценённые решения");
        }

        Post task = postRepository.findById(taskId)
                .orElseThrow(() -> new NotFoundException("Задание с id " + taskId + " не найдено"));

        return solutionRepository.findUngradedByTaskId(taskId).stream()
                .map(solution -> {
                    User student = getUserById(solution.getStudentId());

                    return mapToDto(solution,
                            task.getLabel(),
                            student.getFirstName() + " " + student.getLastName(),
                            null);
                })
                .collect(Collectors.toList());
    }

    /**
     * Получить задачи с решениями для студента в канале
     */
    public List<TaskWithSolutionDto> getStudentTasksWithSolutions(Long studentId, UUID channelId, String authHeader) {
        Long userId = extractUserIdFromHeader(authHeader);
        User user = getUserById(userId);

        if (!userId.equals(studentId) && !isTeacherOrManager(user)) {
            throw new ForbiddenException("У вас нет прав на просмотр");
        }

        // Получаем все задания в канале
        List<Post> tasks = postRepository.findByChannelIdAndType(channelId, PostType.TASK);
        User student = getUserById(studentId);

        return tasks.stream()
                .map(task -> {
                    Solution solution = solutionRepository.findByTaskIdAndStudentId(task.getId(), studentId).orElse(null);

                    SolutionDto solutionDto = null;
                    if (solution != null) {
                        User teacher = solution.getTeacherId() != null ? getUserById(solution.getTeacherId()) : null;
                        solutionDto = mapToDto(solution, task.getLabel(),
                                student.getFirstName() + " " + student.getLastName(),
                                teacher != null ? teacher.getFirstName() + " " + teacher.getLastName() : null);
                    }

                    return TaskWithSolutionDto.builder()
                            .taskId(task.getId())
                            .taskLabel(task.getLabel())
                            .taskText(task.getText())
                            .deadline(task.getDeadline())
                            .solution(solutionDto)
                            .build();
                })
                .collect(Collectors.toList());
    }

    /**
     * Удаление решения
     */
    public void deleteSolution(UUID solutionId, String authHeader) {
        Long userId = extractUserIdFromHeader(authHeader);
        User user = getUserById(userId);

        Solution solution = solutionRepository.findById(solutionId)
                .orElseThrow(() -> new NotFoundException("Решение с id " + solutionId + " не найдено"));

        // Проверка прав: студент может удалить своё неоценённое, админ - любое
        if (solution.getStudentId().equals(userId)) {
            if (solution.getMark() != null) {
                throw new BadRequestException("Нельзя удалить уже оценённое решение");
            }
        } else if (!user.getRole().contains(Role.MANAGER)) {
            throw new ForbiddenException("Вы можете удалять только свои неоценённые решения");
        }

        // Удаляем файл
        if (solution.getFileUrl() != null) {
            String filename = extractFilenameFromUrl(solution.getFileUrl());
            if (filename != null) {
                fileStorageService.delete(filename);
            }
        }

        solutionRepository.delete(solution);
        log.info("Решение с id {} удалено", solutionId);
    }

    private boolean isTeacherOrManager(User user) {
        return user.getRole().contains(Role.TEACHER) || user.getRole().contains(Role.MANAGER);
    }

    private void validateMark(Integer mark) {
        if (mark == null) {
            throw new BadRequestException("Оценка не может быть null");
        }
        if (mark < MIN_MARK || mark > MAX_MARK) {
            throw new BadRequestException("Оценка должна быть от " + MIN_MARK + " до " + MAX_MARK);
        }
    }

    private Long extractUserIdFromHeader(String authHeader) {
        String token = tokenProvider.extractTokenFromHeader(authHeader);
        if (!tokenProvider.validateToken(token)) {
            throw new BadRequestException("Невалидный или истёкший токен");
        }
        return tokenProvider.getUserIdFromToken(token);
    }

    private User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Пользователь с id " + id + " не найден"));
    }

    private String extractFilenameFromUrl(String fileUrl) {
        if (fileUrl == null || !fileUrl.contains("/file/")) {
            return null;
        }
        return fileUrl.substring(fileUrl.lastIndexOf("/") + 1);
    }

    private SolutionDto mapToDto(Solution solution, String taskLabel, String studentName, String teacherName) {
        return SolutionDto.builder()
                .id(solution.getId())
                .studentId(solution.getStudentId())
                .studentName(studentName)
                .taskId(solution.getTaskId())
                .taskLabel(taskLabel)
                .teacherId(solution.getTeacherId())
                .teacherName(teacherName)
                .text(solution.getText())
                .fileUrl(solution.getFileUrl())
                .fileName(solution.getFileName())
                .mark(solution.getMark())
                .submittedAt(solution.getSubmittedAt())
                .updatedAt(solution.getUpdatedAt())
                .markedAt(solution.getMarkedAt())
                .build();
    }
}
