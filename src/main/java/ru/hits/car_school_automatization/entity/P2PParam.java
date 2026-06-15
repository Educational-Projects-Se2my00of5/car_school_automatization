package ru.hits.car_school_automatization.entity;

import jakarta.persistence.*;
import lombok.*;
import ru.hits.car_school_automatization.enums.P2PType;
import ru.hits.car_school_automatization.enums.P2PVisibility;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "p2p_params")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class P2PParam {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id")
    private Task task;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private P2PType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private P2PVisibility visibility;

    @Column(name = "p2p_deadline")
    private Instant p2pDeadline;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        P2PParam p2PParam = (P2PParam) o;
        return Objects.equals(id, p2PParam.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
