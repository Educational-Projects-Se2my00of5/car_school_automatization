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
public class VoteResultDto {
    private UUID solutionId;
    private String solutionLabel;
    private Integer votesCount;
    private Double percentage;
    private List<VoterInfoDto> voters;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VoterInfoDto {
        private Long voterId;
        private String voterName;
    }
}