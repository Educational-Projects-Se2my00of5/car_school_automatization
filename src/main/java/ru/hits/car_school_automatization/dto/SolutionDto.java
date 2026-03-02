package ru.hits.car_school_automatization.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
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
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SolutionDto {
    private UUID id;
    private Long studentId;
    private String studentName;
    private UUID taskId;
    private String taskLabel;
    private Long teacherId;
    private String teacherName;
    private String text;
    private String fileUrl;
    private String fileName;
    private Integer mark;
    private LocalDateTime submittedAt;
    private LocalDateTime updatedAt;
    private LocalDateTime markedAt;
}
