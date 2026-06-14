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
@Table(name = "task_solutions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskSolution {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "team_id", nullable = false)
    private UUID teamId;

    @Column(name = "task_id", nullable = false)
    private UUID taskId;

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @Column(name = "is_selected")
    private Boolean isSelected;

    @ElementCollection
    @CollectionTable(name = "task_solution_documents", joinColumns = @JoinColumn(name = "task_solution_id"))
    @Builder.Default
    private List<TaskDocument> documents = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
}
