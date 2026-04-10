package ru.hits.car_school_automatization.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.hits.car_school_automatization.dto.CreateTeamDto;
import ru.hits.car_school_automatization.dto.TeamDto;
import ru.hits.car_school_automatization.dto.UpdateTeamDto;
import ru.hits.car_school_automatization.entity.*;
import ru.hits.car_school_automatization.dto.*;
import ru.hits.car_school_automatization.entity.CaptainVote;
import ru.hits.car_school_automatization.entity.Task;
import ru.hits.car_school_automatization.entity.Team;
import ru.hits.car_school_automatization.entity.User;
import ru.hits.car_school_automatization.enums.Role;
import ru.hits.car_school_automatization.enums.TeamType;
import ru.hits.car_school_automatization.exception.BadRequestException;
import ru.hits.car_school_automatization.exception.ForbiddenException;
import ru.hits.car_school_automatization.exception.NotFoundException;
import ru.hits.car_school_automatization.mapper.TeamMapper;
import ru.hits.car_school_automatization.repository.InviteRepository;
import ru.hits.car_school_automatization.mapper.UserMapper;
import ru.hits.car_school_automatization.repository.CaptainVoteRepository;
import ru.hits.car_school_automatization.repository.TaskRepository;
import ru.hits.car_school_automatization.repository.TeamRepository;
import ru.hits.car_school_automatization.repository.UserRepository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class TeamService {

    private final TeamRepository teamRepository;
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final CaptainVoteRepository captainVoteRepository;
    private final JwtTokenProvider tokenProvider;
    private final InviteRepository inviteRepository;

    private final TeamMapper teamMapper;
    private final UserMapper userMapper;

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

    public TeamDto chooseCaptain(ChooseCaptainDto dto, String authHeader) {
        Long userId = tokenProvider.extractUserIdFromHeader(authHeader);
        User requester = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь не найден"));

        Team team = teamRepository.findById(dto.getTeamId())
                .orElseThrow(() -> new NotFoundException("Команда не найдена"));

        if (team.getTask().getTeamType() == TeamType.DRAFT) {
            throw new BadRequestException("Для DRAFT-заданий капитан назначается только при создании команды и не может быть изменён");
        }

        if (team.getDeadline() != null &&
                Instant.now().isAfter(team.getDeadline().minus(1, java.time.temporal.ChronoUnit.HOURS))) {
            if (team.getIsCaptainVotingActive()) {
                team.setIsCaptainVotingActive(false);
                teamRepository.save(team);
                autoSelectCaptain(team);
            }
            throw new BadRequestException("Выбор капитана недоступен: до дедлайна осталось менее часа");
        }

        boolean isTeacher = requester.getRole().contains(Role.TEACHER);
        boolean isStudent = requester.getRole().contains(Role.STUDENT);
        boolean isInTeam = team.getUsers().stream().anyMatch(u -> u.getId().equals(userId));

        if (isTeacher) {
            validateTeacherLeadsTaskChannel(authHeader, team.getTask());

            if (dto.getCaptainId() == null) {
                throw new BadRequestException("Укажите captainId для назначения капитана");
            }

            User newCaptain = userRepository.findById(dto.getCaptainId())
                    .orElseThrow(() -> new NotFoundException("Пользователь не найден"));

            boolean isCaptainInTeam = team.getUsers().stream()
                    .anyMatch(u -> u.getId().equals(dto.getCaptainId()));
            if (!isCaptainInTeam) {
                throw new BadRequestException("Капитан должен быть участником команды");
            }

            team.setCaptainId(dto.getCaptainId());
            team.setIsAvailableRevote(false);
            team.setIsCaptainVotingActive(false);

            captainVoteRepository.deleteByTeamId(team.getId());

            Team saved = teamRepository.save(team);
            log.info("Преподаватель {} назначил капитана {} для команды {}", userId, dto.getCaptainId(), team.getId());
            return teamMapper.toDto(saved);
        }

        if (isStudent && isInTeam) {
            if (team.getUsers().size() == 1 && team.getUsers().iterator().next().getId().equals(userId)) {
                team.setCaptainId(userId);
                team.setIsAvailableRevote(true);
                team.setIsCaptainVotingActive(false);
                Team saved = teamRepository.save(team);
                log.info("Студент {} автоматически стал капитаном созданной команды {}", userId, team.getId());
                return teamMapper.toDto(saved);
            }

            if (team.getCaptainId() != null && !team.getIsAvailableRevote()) {
                throw new BadRequestException("Перевыбор капитана запрещён");
            }

            if (dto.getCaptainId() != null) {
                boolean isCandidateInTeam = team.getUsers().stream()
                        .anyMatch(u -> u.getId().equals(dto.getCaptainId()));
                if (!isCandidateInTeam) {
                    throw new BadRequestException("Кандидат должен быть участником команды");
                }

                if (!team.getIsCaptainVotingActive()) {
                    team.setIsCaptainVotingActive(true);
                    teamRepository.save(team);
                }

                captainVoteRepository.findByTeamIdAndVoterId(team.getId(), userId)
                        .ifPresent(captainVoteRepository::delete);

                CaptainVote vote = CaptainVote.builder()
                        .team(team)
                        .voterId(userId)
                        .candidateId(dto.getCaptainId())
                        .votedAt(Instant.now())
                        .build();
                captainVoteRepository.save(vote);
                log.info("Студент {} проголосовал за кандидата {} в команде {}", userId, dto.getCaptainId(), team.getId());

                return teamMapper.toDto(team);
            }

            if (!team.getIsCaptainVotingActive()) {
                captainVoteRepository.deleteByTeamId(team.getId());
                team.setIsCaptainVotingActive(true);
                team.setIsAvailableRevote(true);
                team = teamRepository.save(team);
                log.info("Студент {} инициировал голосование за капитана в команде {}", userId, team.getId());
            }

            return teamMapper.toDto(team);
        }

        throw new ForbiddenException("У вас нет прав для выбора капитана");
    }

    private void autoSelectCaptain(Team team) {
        if (team.getCaptainId() != null) {
            return;
        }

        List<Object[]> voteResults = captainVoteRepository.countVotesByCandidate(team.getId());

        if (voteResults.isEmpty()) {
            team.getUsers().stream().findFirst().ifPresent(firstUser -> {
                team.setCaptainId(firstUser.getId());
            });
        } else {
            long maxVotes = 0;
            List<Long> topCandidates = new ArrayList<>();

            for (Object[] result : voteResults) {
                Long candidateId = (Long) result[0];
                Long voteCount = (Long) result[1];

                if (voteCount > maxVotes) {
                    maxVotes = voteCount;
                    topCandidates.clear();
                    topCandidates.add(candidateId);
                } else if (voteCount == maxVotes) {
                    topCandidates.add(candidateId);
                }
            }

            Long selectedCaptainId = topCandidates.size() == 1 ?
                    topCandidates.get(0) :
                    topCandidates.get(new Random().nextInt(topCandidates.size()));

            team.setCaptainId(selectedCaptainId);
        }

        team.setIsCaptainVotingActive(false);
        teamRepository.save(team);
    }

    public TeamDto initCaptainVoting(UUID teamId, String authHeader) {
        ChooseCaptainDto dto = ChooseCaptainDto.builder()
                .teamId(teamId)
                .captainId(null)
                .build();
        return chooseCaptain(dto, authHeader);
    }

    public UserDto.FullInfo getCaptain(UUID teamId) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new NotFoundException("Команда с id " + teamId + " не найдена"));

        if (team.getCaptainId() == null) {
            return null;
        }

        User captain = userRepository.findById(team.getCaptainId())
                .orElseThrow(() -> new NotFoundException("Капитан с id " + team.getCaptainId() + " не найден"));

        return userMapper.toDto(captain);
    }

    public List<CaptainVoteDto> getCaptainVotes(UUID teamId, String authHeader) {
        Long userId = tokenProvider.extractUserIdFromHeader(authHeader);
        User requester = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь не найден"));

        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new NotFoundException("Команда с id " + teamId + " не найдена"));

        boolean isInTeam = team.getUsers().stream().anyMatch(u -> u.getId().equals(userId));
        boolean isTeacher = requester.getRole().contains(Role.TEACHER);

        if (!isInTeam && !isTeacher) {
            throw new ForbiddenException("У вас нет прав на просмотр голосов");
        }

        List<CaptainVote> votes = captainVoteRepository.findByTeamId(teamId);

        return votes.stream().map(vote -> {
            User voter = userRepository.findById(vote.getVoterId()).orElse(null);
            User candidate = userRepository.findById(vote.getCandidateId()).orElse(null);

            return CaptainVoteDto.builder()
                    .id(vote.getId())
                    .teamId(vote.getTeam().getId())
                    .voterId(vote.getVoterId())
                    .voterName(voter != null ? voter.getFirstName() + " " + voter.getLastName() : null)
                    .candidateId(vote.getCandidateId())
                    .candidateName(candidate != null ? candidate.getFirstName() + " " + candidate.getLastName() : null)
                    .build();
        }).toList();
    }

    public void inviteToTeam(UUID teamId, Long inviteeId, String authHeader) {
        Long inviterId = tokenProvider.extractUserIdFromHeader(authHeader);

        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new NotFoundException("Команда не найдена"));

        if (team.getTask().getTeamType() != TeamType.FREE) {
            throw new BadRequestException("Приглашения доступны только для FREE-режима");
        }

        boolean isInviterInTeam = team.getUsers().stream()
                .anyMatch(u -> u.getId().equals(inviterId));
        if (!isInviterInTeam) {
            throw new ForbiddenException("Вы не состоите в этой команде");
        }

        User invitee = userRepository.findById(inviteeId)
                .orElseThrow(() -> new NotFoundException("Пользователь не найден"));
        if (!invitee.getRole().contains(Role.STUDENT)) {
            throw new BadRequestException("Приглашать можно только студентов");
        }

        boolean alreadyInTeam = team.getUsers().stream()
                .anyMatch(u -> u.getId().equals(inviteeId));
        if (alreadyInTeam) {
            throw new BadRequestException("Студент уже в этой команде");
        }

        boolean inAnotherTeam = teamRepository.existsByTask_IdAndUsers_IdAndIdNot(
                team.getTask().getId(), inviteeId, teamId);
        if (inAnotherTeam) {
            throw new BadRequestException("Студент уже в другой команде этого задания");
        }

        if (inviteRepository.existsByTeamIdAndInviteeId(teamId, inviteeId)) {
            throw new BadRequestException("Приглашение уже отправлено");
        }

        Invite invite = Invite.builder()
                .team(team)
                .inviterId(inviterId)
                .inviteeId(inviteeId)
                .build();

        inviteRepository.save(invite);
        log.info("Студент {} пригласил студента {} в команду {}", inviterId, inviteeId, teamId);
    }

    public TeamDto acceptInvite(UUID inviteId, String authHeader) {
        Long userId = tokenProvider.extractUserIdFromHeader(authHeader);

        Invite invite = inviteRepository.findById(inviteId)
                .orElseThrow(() -> new NotFoundException("Приглашение не найдено"));

        if (!invite.getInviteeId().equals(userId)) {
            throw new ForbiddenException("Это приглашение не для вас");
        }

        Team team = invite.getTeam();

        if (team.getTask().getTeamType() != TeamType.FREE) {
            throw new BadRequestException("Режим задания изменился");
        }

        boolean alreadyInTeam = team.getUsers().stream()
                .anyMatch(u -> u.getId().equals(userId));
        if (alreadyInTeam) {
            throw new BadRequestException("Вы уже в этой команде");
        }

        boolean inAnotherTeam = teamRepository.existsByTask_IdAndUsers_IdAndIdNot(
                team.getTask().getId(), userId, team.getId());
        if (inAnotherTeam) {
            throw new BadRequestException("Вы уже в другой команде этого задания");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь не найден"));
        team.getUsers().add(user);
        teamRepository.save(team);

        inviteRepository.delete(invite);

        log.info("Студент {} принял приглашение и вступил в команду {}", userId, team.getId());

        return teamMapper.toDto(team);
    }

    public void rejectInvite(UUID inviteId, String authHeader) {
        Long userId = tokenProvider.extractUserIdFromHeader(authHeader);

        Invite invite = inviteRepository.findById(inviteId)
                .orElseThrow(() -> new NotFoundException("Приглашение не найдено"));

        if (!invite.getInviteeId().equals(userId)) {
            throw new ForbiddenException("Это приглашение не для вас");
        }

        inviteRepository.delete(invite);
        log.info("Студент {} отклонил приглашение", userId);
    }

    public List<TeamDto> getMyInvites(String authHeader) {
        Long userId = tokenProvider.extractUserIdFromHeader(authHeader);

        List<Invite> invites = inviteRepository.findByInviteeId(userId);

        return invites.stream()
                .map(invite -> teamMapper.toDto(invite.getTeam()))
                .toList();
    }

    private void validateTaskFree(Task task) {
        if (task.getTeamType() != TeamType.FREE) {
            throw new BadRequestException("Самостоятельное вступление/выход доступны только для FREE-режима задания");
        }
    }
}
