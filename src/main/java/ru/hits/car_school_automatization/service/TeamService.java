package ru.hits.car_school_automatization.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.hits.car_school_automatization.dto.CreateTeamDto;
import ru.hits.car_school_automatization.dto.TeamDto;
import ru.hits.car_school_automatization.dto.UpdateTeamDto;
import ru.hits.car_school_automatization.entity.Channel;
import ru.hits.car_school_automatization.entity.Task;
import ru.hits.car_school_automatization.entity.Team;
import ru.hits.car_school_automatization.entity.User;
import ru.hits.car_school_automatization.enums.Role;
import ru.hits.car_school_automatization.enums.TeamType;
import ru.hits.car_school_automatization.exception.BadRequestException;
import ru.hits.car_school_automatization.exception.ForbiddenException;
import ru.hits.car_school_automatization.exception.NotFoundException;
import ru.hits.car_school_automatization.mapper.TeamMapper;
import ru.hits.car_school_automatization.repository.TaskRepository;
import ru.hits.car_school_automatization.repository.TeamRepository;
import ru.hits.car_school_automatization.repository.UserRepository;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class TeamService {

    private final TeamRepository teamRepository;
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final JwtTokenProvider tokenProvider;
    private final TeamMapper teamMapper;

    public TeamDto createTeam(CreateTeamDto dto, String authHeader) {
        Task task = taskRepository.findById(dto.getTaskId())
                .orElseThrow(() -> new NotFoundException("Задание с id " + dto.getTaskId() + " не найдено"));

        Long requesterId = validateTeacherLeadsTaskChannel(authHeader, task);

        String teamName = dto.getName() == null || dto.getName().isBlank() ? "Новая команда" : dto.getName().trim();

        Team team = Team.builder()
                .name(teamName)
                .task(task)
                .deadline(dto.getDeadline())
                .softDeadline(dto.getSoftDeadline())
                .build();

        Team saved = teamRepository.save(team);
        log.info("Пользователь {} создал команду {} для задания {}", requesterId, saved.getId(), task.getId());

        return teamMapper.toDto(saved);
    }

    public TeamDto updateTeam(UUID teamId, UpdateTeamDto dto, String authHeader) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new NotFoundException("Команда с id " + teamId + " не найдена"));

        validateTeacherLeadsTaskChannel(authHeader, team.getTask());

        if (dto.getName() != null) {
            String trimmedName = dto.getName().trim();
            if (trimmedName.isEmpty()) {
                throw new BadRequestException("Название команды не может быть пустым");
            }
            dto.setName(trimmedName);
        }

        if (dto.getCaptainId() != null) {
            boolean isCaptainInTeam = team.getUsers().stream()
                    .anyMatch(user -> user.getId().equals(dto.getCaptainId()));

            if (!isCaptainInTeam) {
                throw new BadRequestException("Капитан должен быть участником команды");
            }

            team.setCaptainId(dto.getCaptainId());
        }

        teamMapper.updateTeamFromDto(dto, team);

        Team saved = teamRepository.save(team);
        return teamMapper.toDto(saved);
    }

    @Transactional
    public TeamDto getTeam(UUID teamId) {

        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new NotFoundException("Команда с id " + teamId + " не найдена"));

        return teamMapper.toDto(team);
    }

    @Transactional
    public List<TeamDto> getTaskTeams(UUID taskId) {

        if (!taskRepository.existsById(taskId)) {
            throw new NotFoundException("Задание с id " + taskId + " не найдено");
        }

        return teamMapper.toDtoList(teamRepository.findByTask_Id(taskId));
    }

    public TeamDto addMember(UUID teamId, Long userId, String authHeader) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new NotFoundException("Команда с id " + teamId + " не найдена"));

        validateTeacherLeadsTaskChannel(authHeader, team.getTask());

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь с id " + userId + " не найден"));

        boolean added = team.getUsers().add(user);
        if (!added) {
            throw new BadRequestException("Пользователь уже в команде");
        }

        Team saved = teamRepository.save(team);
        return teamMapper.toDto(saved);
    }

    public TeamDto joinTeam(UUID teamId, String authHeader) {
        Long requesterId = tokenProvider.extractUserIdFromHeader(authHeader);

        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new NotFoundException("Команда с id " + teamId + " не найдена"));

        Task task = team.getTask();
        validateTaskFree(task);

        //TODO: добавить проверку на максимальное количество участников в команде, если будет такая логика

        User student = userRepository.findById(requesterId)
                .orElseThrow(() -> new NotFoundException("Пользователь с id " + requesterId + " не найден"));

        if (team.getUsers().stream().anyMatch(user -> user.getId().equals(requesterId))) {
            throw new BadRequestException("Вы уже состоите в этой команде");
        }

        boolean inAnotherTeam = teamRepository.existsByTask_IdAndUsers_IdAndIdNot(task.getId(), requesterId, teamId);
        if (inAnotherTeam) {
            throw new BadRequestException("Вы уже состоите в другой команде этого задания");
        }

        team.getUsers().add(student);

        Team saved = teamRepository.save(team);
        return teamMapper.toDto(saved);
    }

    public TeamDto removeMember(UUID teamId, Long userId, String authHeader) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new NotFoundException("Команда с id " + teamId + " не найдена"));

        validateTeacherLeadsTaskChannel(authHeader, team.getTask());

        boolean removed = team.getUsers().removeIf(user -> user.getId().equals(userId));
        if (!removed) {
            throw new NotFoundException("Пользователь с id " + userId + " не состоит в команде");
        }

        if (team.getCaptainId() != null && team.getCaptainId().equals(userId)) {
            team.setCaptainId(null);
        }

        Team saved = teamRepository.save(team);
        return teamMapper.toDto(saved);
    }

    public TeamDto leaveTeam(UUID teamId, String authHeader) {
        Long requesterId = tokenProvider.extractUserIdFromHeader(authHeader);

        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new NotFoundException("Команда с id " + teamId + " не найдена"));

        Task task = team.getTask();
        validateTaskFree(task);

        boolean removed = team.getUsers().removeIf(user -> user.getId().equals(requesterId));
        if (!removed) {
            throw new BadRequestException("Вы не состоите в этой команде");
        }

        if (team.getCaptainId() != null && team.getCaptainId().equals(requesterId)) {
            team.setCaptainId(null);
        }

        Team saved = teamRepository.save(team);
        return teamMapper.toDto(saved);
    }

    public void deleteTeam(UUID teamId, String authHeader) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new NotFoundException("Команда с id " + teamId + " не найдена"));

        validateTeacherLeadsTaskChannel(authHeader, team.getTask());

        teamRepository.delete(team);
    }

    private Long validateTeacherLeadsTaskChannel(String authHeader, Task task) {
        Long requesterId = tokenProvider.extractUserIdFromHeader(authHeader);

        User requester = userRepository.findById(requesterId)
                .orElseThrow(() -> new NotFoundException("Пользователь с id " + requesterId + " не найден"));

        if (!requester.getRole().contains(Role.TEACHER)) {
            throw new ForbiddenException("Только преподаватель может управлять командами задания");
        }

        boolean teachesChannel = task.getChannel().getUsers().stream()
                .anyMatch(user -> user.getId().equals(requesterId));

        if (!teachesChannel) {
            throw new ForbiddenException("Преподаватель не ведет предмет, к которому привязано задание");
        }

        return requesterId;
    }

    private void validateTaskFree(Task task) {
        if (task.getTeamType() != TeamType.FREE) {
            throw new BadRequestException("Самостоятельное вступление/выход доступны только для FREE-режима задания");
        }
    }
}
