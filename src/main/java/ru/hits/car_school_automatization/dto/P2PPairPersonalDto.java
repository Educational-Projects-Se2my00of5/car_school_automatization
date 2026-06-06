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
public class P2PPairPersonalDto {
    private UUID id;
    private UUID postId;
    private Long reviewerId;
    private Long ownerId;
    private UUID targetSolutionId;
    private P2PPairStatus status;
}
