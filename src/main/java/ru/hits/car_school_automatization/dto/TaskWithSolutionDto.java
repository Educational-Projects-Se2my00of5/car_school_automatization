package ru.hits.car_school_automatization.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskWithSolutionDto {
    private UUID taskId;
    private String taskLabel;
    private String taskText;
    private LocalDateTime deadline;
    private SolutionDto solution;
}
