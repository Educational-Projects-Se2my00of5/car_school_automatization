package ru.hits.car_school_automatization.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CaptainVoteDto {
    private UUID id;
    private UUID teamId;
    private Long voterId;
    private String voterName;
    private Long candidateId;
    private String candidateName;
}