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
public class P2PPairTeamDto {
    private UUID id;
    private TaskDto task;
    private TeamDto reviewerTeam;
    private TeamDto ownerTeam;
    private UUID targetTaskSolutionId;
    private P2PPairStatus status;
}
