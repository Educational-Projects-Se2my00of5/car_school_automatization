package ru.hits.car_school_automatization.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.hits.car_school_automatization.entity.Task;
import ru.hits.car_school_automatization.entity.Team;
import ru.hits.car_school_automatization.repository.TaskRepository;
import ru.hits.car_school_automatization.service.TaskSolutionService;

import java.time.Instant;
import java.util.List;

@Slf4j
@Component
@EnableScheduling
@RequiredArgsConstructor
public class TaskDeadlineScheduler {

    private final TaskRepository taskRepository;
    private final TaskSolutionService taskSolutionService;

    @Scheduled(cron = "0 * * * * *")
    @Transactional
    public void autoSelectSolutionsAfterDeadline() {
        List<Task> tasks = taskRepository.findByVotingDeadlineBefore(Instant.now());

        for (Task task : tasks) {
            for (Team team : task.getTeams()) {
                try {
                    taskSolutionService.autoSelectSolutionForTeam(task.getId(), team.getId());
                } catch (Exception e) {
                    log.error("Ошибка при автоматическом выборе решения для задания {} и команды {}",
                            task.getId(), team.getId(), e);
                }
            }
        }
    }
}