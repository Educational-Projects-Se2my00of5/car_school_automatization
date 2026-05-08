package ru.hits.car_school_automatization.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.hits.car_school_automatization.enums.DeadlinePenaltyUnit;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeadlinePenaltyDto {
    private DeadlinePenaltyUnit unit;
    private Integer step;
    private Double value;
}
