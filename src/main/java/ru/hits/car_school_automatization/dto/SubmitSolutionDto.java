package ru.hits.car_school_automatization.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubmitSolutionDto {
    @NotNull(message = "ID задания не может быть null")
    private UUID taskId;

    private String text;

    private MultipartFile file;
}
