package ru.hits.car_school_automatization.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.hits.car_school_automatization.enums.MetricType;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateMetricDto {

    @NotBlank
    private String name;

    private String comment;

    @NotNull
    private Double minValue;

    @NotNull
    private Double maxValue;

    @NotNull
    private MetricType type;

    @NotNull
    private Boolean isVisibleToStudents;

    @NotNull
    private Boolean isValuesVisibleToStudents;

    private UUID postId;

    private UUID taskId;
}
