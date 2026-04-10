package ru.hits.car_school_automatization.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "captain_votes")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CaptainVote {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    @Column(name = "voter_id", nullable = false)
    private Long voterId;

    @Column(name = "candidate_id", nullable = false)
    private Long candidateId;

    @Column(nullable = false)
    private Instant votedAt;
}
