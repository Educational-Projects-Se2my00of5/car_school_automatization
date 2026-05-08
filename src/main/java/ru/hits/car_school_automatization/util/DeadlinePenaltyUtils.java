package ru.hits.car_school_automatization.util;

import ru.hits.car_school_automatization.dto.DeadlinePenaltyDto;
import ru.hits.car_school_automatization.entity.DeadlinePenalty;
import ru.hits.car_school_automatization.exception.BadRequestException;

public final class DeadlinePenaltyUtils {

    private DeadlinePenaltyUtils() {
    }

    public static void validate(DeadlinePenalty penalty) {
        if (penalty == null) {
            return;
        }
        if (penalty.getUnit() == null || penalty.getStep() == null || penalty.getValue() == null) {
            throw new BadRequestException("Параметры дедлайна должны быть заполнены полностью");
        }
        if (penalty.getStep() <= 0) {
            throw new BadRequestException("Шаг дедлайна должен быть больше 0");
        }
        if (penalty.getValue() < 0) {
            throw new BadRequestException("Значение штрафа не может быть отрицательным");
        }
    }

    public static DeadlinePenalty build(DeadlinePenaltyDto dto) {
        if (dto == null) {
            return null;
        }
        DeadlinePenalty penalty = DeadlinePenalty.builder()
                .unit(dto.getUnit())
                .step(dto.getStep())
                .value(dto.getValue())
                .build();
        validate(penalty);
        return penalty;
    }

    public static DeadlinePenalty clonePenalty(DeadlinePenalty penalty) {
        if (penalty == null) {
            return null;
        }
        return DeadlinePenalty.builder()
                .unit(penalty.getUnit())
                .step(penalty.getStep())
                .value(penalty.getValue())
                .build();
    }
}
