package ru.hits.car_school_automatization.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "controls")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Control {

    @Id
    @Column(name = "post_id", nullable = false)
    private UUID postId;

    @Column(name = "channel_id", nullable = false)
    private UUID channelId;

    @ElementCollection
    @CollectionTable(name = "control_post_tasks", joinColumns = @JoinColumn(name = "control_id"))
    @Column(name = "post_id", nullable = false)
    @Builder.Default
    private Set<UUID> postTaskIds = new HashSet<>();

    @ElementCollection
    @CollectionTable(name = "control_tasks", joinColumns = @JoinColumn(name = "control_id"))
    @Column(name = "task_id", nullable = false)
    @Builder.Default
    private Set<UUID> taskIds = new HashSet<>();
}
