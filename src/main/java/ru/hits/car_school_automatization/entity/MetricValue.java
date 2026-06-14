package ru.hits.car_school_automatization.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "metric_values", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"metric_id", "user_id"})
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MetricValue {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "metric_id", nullable = false)
    private UUID metricId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "metric_value", nullable = false)
    private Double value;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "metric_id", insertable = false, updatable = false)
    private Metric metric;

    @OneToMany(mappedBy = "metricValue", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<MetricChange> changes = new ArrayList<>();
}
