package ru.hits.car_school_automatization.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ControlDto {
    private UUID postId;
    private UUID channelId;
    private Set<UUID> postTaskIds;
    private Set<UUID> taskIds;
}
