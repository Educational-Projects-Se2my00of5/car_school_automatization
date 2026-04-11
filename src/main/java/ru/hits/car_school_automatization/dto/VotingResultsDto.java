package ru.hits.car_school_automatization.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VotingResultsDto {
    private UUID taskId;
    private String taskLabel;
    private Boolean isAnonymous;
    private Integer totalVotes;
    private List<VoteResultDto> results;
}