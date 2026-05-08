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
public class SetTeamMetricValueDto {

    @NotNull
    private UUID metricId;

    @NotNull
    private UUID teamId;

    @NotNull
    private Double value;
}
