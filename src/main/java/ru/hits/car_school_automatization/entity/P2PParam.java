package ru.hits.car_school_automatization.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.hits.car_school_automatization.enums.P2PType;
import ru.hits.car_school_automatization.enums.P2PVisibility;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "p2p_params")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class P2PParam {

    @Id
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private P2PType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private P2PVisibility visibility;

    @Column(name = "p2p_deadline")
    private Instant p2pDeadline;

}
