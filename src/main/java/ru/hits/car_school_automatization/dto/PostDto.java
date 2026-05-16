package ru.hits.car_school_automatization.dto;

import jakarta.annotation.Nullable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.hits.car_school_automatization.enums.PostType;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostDto {
    private UUID id;
    private String label;
    private String text;
    private PostType type;

    @Nullable
    private LocalDateTime deadline;

    private DeadlinePenaltyDto deadlinePenalty;

    private Boolean isMetricsVisibleToStudents;
    private Boolean isMetricValuesVisibleToStudents;

    private String authorName;
    private String fileUrl;
    private String fileName;

    @Nullable
    private ControlDto control;

    @Nullable
    private SolutionDto studentSolution;
}