package ru.hits.car_school_automatization.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateTeamDto {

    private String name;

    @NotNull
    private UUID taskId;

    private Instant deadline;

    private Instant softDeadline;
}
