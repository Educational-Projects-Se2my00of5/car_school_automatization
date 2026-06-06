package ru.hits.car_school_automatization.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssignP2PTeamDto {
    @NotNull
    private UUID taskId;
    
    @NotNull
    private UUID reviewerTeamId;
    
    @NotNull
    private UUID ownerTeamId;

    @NotNull
    private UUID targetTaskSolutionId;
}
