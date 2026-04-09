package ru.hits.car_school_automatization.service;

import org.springframework.stereotype.Service;
import ru.hits.car_school_automatization.dto.CreateTaskDto;
import ru.hits.car_school_automatization.entity.Task;
import ru.hits.car_school_automatization.entity.Team;
import ru.hits.car_school_automatization.entity.User;
import ru.hits.car_school_automatization.enums.Role;
import ru.hits.car_school_automatization.exception.BadRequestException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class TeamFormationService {

    public List<Team> formByTeamType(Task task, List<User> channelUsers, CreateTaskDto dto) {
        List<User> students = channelUsers.stream()
                .filter(user -> user.getRole().contains(Role.STUDENT))
                .toList();

        Integer freeTeamCount = dto.getFreeTeamCount();
        if (freeTeamCount == null || freeTeamCount < 1) {
            throw new BadRequestException("freeTeamCount должен быть больше 0");
        }

        return switch (task.getTeamType()) {
            case RANDOM -> {
                validateAverageTeamSize(students.size(), freeTeamCount, task.getMinTeamSize(), "RANDOM");
                yield formRandom(task, students, freeTeamCount);
            }
            case DRAFT -> {
                validateAverageTeamSize(students.size(), freeTeamCount, task.getMinTeamSize(), "DRAFT");

                List<Long> captainIds = dto.getDraftCaptainIds();
                if (captainIds == null || captainIds.isEmpty()) {
                    throw new BadRequestException("Для DRAFT необходимо передать draftCaptainIds");
                }

                if (captainIds.size() != freeTeamCount) {
                    throw new BadRequestException("Для DRAFT число капитанов должно совпадать с freeTeamCount");
                }

                Set<Long> uniqueCaptainIds = new HashSet<>(captainIds);
                if (uniqueCaptainIds.size() != captainIds.size()) {
                    throw new BadRequestException("В draftCaptainIds не должно быть повторяющихся id");
                }

                Map<Long, User> studentsById = students.stream()
                        .collect(Collectors.toMap(User::getId, Function.identity()));

                List<User> captains = captainIds.stream()
                        .map(studentsById::get)
                        .toList();

                if (captains.stream().anyMatch(captain -> captain == null)) {
                    throw new BadRequestException("Все капитаны должны быть студентами данного предмета");
                }

                yield formDraft(task, captains);
            }
            case FREE -> {
                validateAverageTeamSize(students.size(), freeTeamCount, task.getMinTeamSize(), "FREE");
                yield formFree(task, freeTeamCount);
            }
        };
    }

    public List<Team> formRandom(Task task, List<User> users, int teamCount) {
        if (teamCount < 1) {
            throw new BadRequestException("teamCount должен быть больше 0");
        }

        List<User> shuffled = new ArrayList<>(users);
        Collections.shuffle(shuffled);

        List<List<User>> buckets = new ArrayList<>();
        for (int i = 0; i < teamCount; i++) {
            buckets.add(new ArrayList<>());
        }

        for (int i = 0; i < shuffled.size(); i++) {
            buckets.get(i % teamCount).add(shuffled.get(i));
        }

        List<Team> teams = new ArrayList<>();
        int teamIndex = 1;
        for (List<User> bucket : buckets) {
            Team team = Team.builder()
                    .name("Команда " + teamIndex++)
                    .task(task)
                    .users(new HashSet<>(bucket))
                    .build();
            teams.add(team);
        }

        return teams;
    }

    public List<Team> formDraft(Task task, List<User> captains) {
        List<Team> teams = new ArrayList<>();
        int teamIndex = 1;

        for (User captain : captains) {
            Team team = Team.builder()
                    .name("Драфт команда " + teamIndex++)
                    .task(task)
                    .captainId(captain.getId())
                    .users(new HashSet<>(Set.of(captain)))
                    .build();
            teams.add(team);
        }

        return teams;
    }

    public List<Team> formFree(Task task, int teamCount) {
        List<Team> teams = new ArrayList<>();
        for (int i = 1; i <= teamCount; i++) {
            Team team = Team.builder()
                    .name("Свободная команда " + i)
                    .task(task)
                    .users(new HashSet<>())
                    .build();
            teams.add(team);
        }

        return teams;
    }

    private void validateAverageTeamSize(int studentCount, int teamCount, int minTeamSize, String mode) {
        double averageSize = teamCount == 0 ? 0 : (double) studentCount / teamCount;
        if (averageSize < minTeamSize) {
            throw new BadRequestException("Для " + mode + " средний размер команды меньше minTeamSize");
        }
    }
}
