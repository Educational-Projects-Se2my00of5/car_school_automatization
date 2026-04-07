package ru.hits.car_school_automatization.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
public class CreateTaskDto {

    @NotBlank
    private String label;
    
    @NotBlank
    private String text;

    private List<String> documents;

    @NotNull
    private TeamType teamType;

    // Параметры формирования команд по режимам TeamType
    private Integer randomTeamSize;
    private List<Long> draftCaptainIds;
    private Integer freeTeamCount;

    @NotNull
    private TaskType type;

    @NotNull
    private Integer minTeamSize;

    private Instant votingDeadline;

    private Integer qualifiedMin;

    @NotNull
    private Boolean isCanRedistribute;
}
