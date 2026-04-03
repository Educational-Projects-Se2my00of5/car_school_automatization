package ru.hits.car_school_automatization.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeamDto {

    private UUID id;
    private String name;
    private UUID taskId;
    private Long captainId;
    private Boolean isAvailableRevote;
    private Boolean isCaptainVotingActive;
    private Float mark;
    private Instant deadline;
    private Instant softDeadline;
    private Set<Long> userIds;
}
