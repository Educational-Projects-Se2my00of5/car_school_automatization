package ru.hits.car_school_automatization.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.hits.car_school_automatization.enums.GradeTargetType;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GradeTableTargetDto {
    private UUID targetId;
    private String label;
    private GradeTargetType type;
}
