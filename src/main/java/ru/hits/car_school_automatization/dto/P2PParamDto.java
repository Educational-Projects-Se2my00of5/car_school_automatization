package ru.hits.car_school_automatization.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.hits.car_school_automatization.enums.P2PType;
import ru.hits.car_school_automatization.enums.P2PVisibility;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class P2PParamDto {
    private P2PType type;
    private P2PVisibility visibility;
    private Instant p2pDeadline;
}
