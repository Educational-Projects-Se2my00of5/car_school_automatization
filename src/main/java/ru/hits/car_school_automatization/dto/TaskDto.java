package ru.hits.car_school_automatization.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.hits.car_school_automatization.enums.TaskType;
import ru.hits.car_school_automatization.enums.TeamType;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskDto {
    private UUID id;
    private String label;
    private String text;
    private UUID channelId;
    private Instant startAt;
    private List<TaskDocumentDto> documents;
    private TeamType teamType;
    private TaskType type;
    private Boolean isCanRedistribute;
    private Integer qualifiedMin;
    private Integer minTeamSize;
    private Instant votingDeadline;
}
