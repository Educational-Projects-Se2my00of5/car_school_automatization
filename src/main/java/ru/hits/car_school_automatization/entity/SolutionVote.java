package ru.hits.car_school_automatization.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "solution_votes", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"task_id", "voter_id"})
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SolutionVote {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "task_id", nullable = false)
    private UUID taskId;

    @Column(name = "solution_id", nullable = false)
    private UUID solutionId;

    @Column(name = "voter_id", nullable = false)
    private Long voterId;
}