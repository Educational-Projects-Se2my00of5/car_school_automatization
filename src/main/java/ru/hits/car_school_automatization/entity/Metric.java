package ru.hits.car_school_automatization.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.hits.car_school_automatization.enums.MetricType;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "metrics")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Metric {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String comment;

    @Column(nullable = false)
    private Double minValue;

    @Column(nullable = false)
    private Double maxValue;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MetricType type;

    @Column(name = "post_id")
    private UUID postId;

    @Column(name = "task_id")
    private UUID taskId;

    @OneToMany(mappedBy = "metric", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<MetricValue> values = new ArrayList<>();
}
