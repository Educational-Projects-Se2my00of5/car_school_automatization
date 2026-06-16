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
public class PersonalReviewTaskDto {
    private UUID id;
    private PostDto post;
    private UserShortDto owner;
    private SolutionDto targetSolution;
    private P2PPairStatus status;
}
