package ru.hits.car_school_automatization.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReassignP2PPersonalDto {
    @NotNull
    private Long newReviewerId;
}
