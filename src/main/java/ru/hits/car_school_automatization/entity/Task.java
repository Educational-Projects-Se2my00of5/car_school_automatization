package ru.hits.car_school_automatization.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ru.hits.car_school_automatization.enums.TaskType;
import ru.hits.car_school_automatization.enums.TeamType;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "tasks")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String label;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String text;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "channel_id", nullable = false)
    private Channel channel;

    @ElementCollection
    @CollectionTable(name = "task_documents", joinColumns = @JoinColumn(name = "task_id"))
    @Builder.Default
    private List<TaskDocument> documents = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TeamType teamType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TaskType type;


    @Column(nullable = false)
    private Integer minTeamSize;

    private Instant votingDeadline;

    private Integer qualifiedMin;

    @Column(nullable = false)
    private Boolean isCanRedistribute;

    @OneToMany(mappedBy = "task", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<Team> teams = new HashSet<>();

    private Instant startAt;

    @PrePersist
    private void prePersist() {
        if (startAt == null) {
            startAt = Instant.now();
        }
    }
}