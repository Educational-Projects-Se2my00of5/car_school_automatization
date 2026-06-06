package ru.hits.car_school_automatization.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.hits.car_school_automatization.enums.P2PPairStatus;

import java.util.UUID;

@Entity
@Table(name = "p2p_pairs_personal")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class P2PPairPersonal {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "post_id", nullable = false)
    private UUID postId;

    @Column(name = "reviewer_id", nullable = false)
    private Long reviewerId;

    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    @Column(name = "target_solution_id")
    private UUID targetSolutionId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private P2PPairStatus status = P2PPairStatus.PENDING;
}
