package ru.hits.car_school_automatization.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.hits.car_school_automatization.enums.TaskType;
import ru.hits.car_school_automatization.enums.TeamType;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateTaskDto {
    private String label;
    private String text;
    private List<String> documents;
    private TeamType teamType;
    private TaskType type;
    private Boolean isCanRedistribute;
    private Integer qualifiedMin;
    private Integer minTeamSize;
    private Instant votingDeadline;
}
