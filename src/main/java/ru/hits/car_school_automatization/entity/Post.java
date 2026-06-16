package ru.hits.car_school_automatization.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.hits.car_school_automatization.enums.PostType;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "posts")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String label;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String text;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PostType type;

    private LocalDateTime deadline;

    @Embedded
    private DeadlinePenalty deadlinePenalty;

    @Column(name = "channel_id", nullable = false)
    private UUID channelId;

    @Column(name = "author_id", nullable = false)
    private Long authorId;

    @Column(name = "file_url")
    private String fileUrl;

    @Column(name = "file_name")
    private String fileName;

    @Column(nullable = false)
    private Boolean needMark;

    @Builder.Default
    private Boolean isMetricsVisibleToStudents = false;

    @Builder.Default
    private Boolean isMetricValuesVisibleToStudents = false;

    @Builder.Default
    @Column(name = "is_p2p_enabled", nullable = false)
    private Boolean isP2pEnabled = false;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (needMark == null) {
            needMark = PostType.TASK.equals(type);
        }
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
