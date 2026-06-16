package ru.hits.car_school_automatization.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.hits.car_school_automatization.enums.P2PPairStatus;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeamReviewTaskDto {
    private UUID id;
    private TaskDto task;
    private TeamDto ownerTeam;
    private TaskSolutionDto targetTaskSolution;
    private P2PPairStatus status;
}
