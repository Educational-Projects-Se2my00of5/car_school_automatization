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
public class AssignP2PPersonalDto {
    @NotNull
    private UUID postId;
    
    @NotNull
    private Long reviewerId;
    
    @NotNull
    private Long ownerId;

    @NotNull
    private UUID targetSolutionId;
}
