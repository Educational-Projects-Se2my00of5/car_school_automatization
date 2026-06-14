package ru.hits.car_school_automatization.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "teams")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Team {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "task_id", nullable = false)
    private Task task;

    private Long captainId;

    @Column(nullable = false)
    @Builder.Default
    private Boolean isAvailableRevote = false;

    @Column(nullable = false)
    @Builder.Default
    private Boolean isCaptainVotingActive = false;

    private Instant deadline;

    private Instant softDeadline;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "team_users",
            joinColumns = @JoinColumn(name = "team_id", referencedColumnName = "id"),
            inverseJoinColumns = @JoinColumn(name = "user_id", referencedColumnName = "id")
    )
    @Builder.Default
    private Set<User> users = new HashSet<>();
}
