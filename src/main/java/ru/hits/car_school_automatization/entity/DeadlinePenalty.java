package ru.hits.car_school_automatization.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.hits.car_school_automatization.enums.DeadlinePenaltyUnit;

@Embeddable
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeadlinePenalty {

    @Enumerated(EnumType.STRING)
    @Column(name = "deadline_penalty_unit")
    private DeadlinePenaltyUnit unit;

    @Column(name = "deadline_penalty_step")
    private Integer step;

    @Column(name = "deadline_penalty_value")
    private Double value;
}
