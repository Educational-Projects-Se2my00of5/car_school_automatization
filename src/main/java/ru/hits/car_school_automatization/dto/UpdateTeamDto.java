package ru.hits.car_school_automatization.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateTeamDto {

    private String name;
    private Long captainId;
    private Boolean isAvailableRevote;
    private Boolean isCaptainVotingActive;
    private Float mark;
    private Instant deadline;
    private Instant softDeadline;
}
