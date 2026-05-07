package ru.hits.car_school_automatization.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "metric_changes")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MetricChange {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "metric_value_id", nullable = false)
    private UUID metricValueId;

    @Column(name = "editor_id", nullable = false)
    private Long editorId;

    @Column(name = "edit_value", nullable = false)
    private Double editValue;

    @CreationTimestamp
    @Column(name = "edited_at", nullable = false, updatable = false)
    private Instant editedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "metric_value_id", insertable = false, updatable = false)
    private MetricValue metricValue;
}
