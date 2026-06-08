package ru.hits.car_school_automatization.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "team_p2p_review_grades", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"p2p_pair_team_id", "reviewer_id", "metric_id"})
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeamP2PReviewGrade {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "p2p_pair_team_id", nullable = false)
    private UUID p2pPairTeamId;

    @Column(name = "reviewer_id", nullable = false)
    private Long reviewerId;

    @Column(name = "metric_id", nullable = false)
    private UUID metricId;

    @Column(nullable = false)
    private Double value;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
}
