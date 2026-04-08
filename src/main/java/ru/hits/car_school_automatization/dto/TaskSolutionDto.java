package ru.hits.car_school_automatization.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskSolutionDto {
    private UUID id;
    private UUID taskId;
    private Long studentId;
    private List<TaskDocumentDto> documents;
    private Integer mark;
    private Instant createdAt;
    private Instant updatedAt;
}
