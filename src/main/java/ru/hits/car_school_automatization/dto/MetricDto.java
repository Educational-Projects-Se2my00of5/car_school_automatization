package ru.hits.car_school_automatization.dto;

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
public class MetricDto {
    private UUID id;
    private String name;
    private String comment;
    private Double minValue;
    private Double maxValue;
    private MetricType type;
    private Boolean isVisibleToStudents;
    private Boolean isValuesVisibleToStudents;
    private UUID postId;
    private UUID taskId;
}
