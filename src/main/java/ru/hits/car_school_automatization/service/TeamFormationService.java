package ru.hits.car_school_automatization.service;

import org.springframework.stereotype.Service;
import ru.hits.car_school_automatization.entity.Task;
import ru.hits.car_school_automatization.entity.Team;
import ru.hits.car_school_automatization.entity.User;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class TeamFormationService {

    public List<Team> formRandom(Task task, List<User> users, int teamSize) {
        List<User> shuffled = new ArrayList<>(users);
        Collections.shuffle(shuffled);

        List<Team> teams = new ArrayList<>();
        int teamIndex = 1;
        for (int i = 0; i < shuffled.size(); i += teamSize) {
            Set<User> members = new HashSet<>(shuffled.subList(i, Math.min(i + teamSize, shuffled.size())));
            Team team = Team.builder()
                    .name("Команда " + teamIndex++)
                    .task(task)
                    .users(members)
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
}
