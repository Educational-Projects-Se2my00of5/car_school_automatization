package ru.hits.car_school_automatization.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.hits.car_school_automatization.enums.P2PPairStatus;

import java.util.UUID;

@Entity
@Table(name = "p2p_pairs_team")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class P2PPairTeam {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "task_id", nullable = false)
    private UUID taskId;

    @Column(name = "reviewer_team_id", nullable = false)
    private UUID reviewerTeamId;

    @Column(name = "owner_team_id", nullable = false)
    private UUID ownerTeamId;

    @Column(name = "target_task_solution_id")
    private UUID targetTaskSolutionId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private P2PPairStatus status = P2PPairStatus.PENDING;
}
