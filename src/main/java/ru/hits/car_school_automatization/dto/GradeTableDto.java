package ru.hits.car_school_automatization.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GradeTableDto {
    private List<GradeTableTargetDto> targets;
    private List<GradeTableRowDto> rows;
}
