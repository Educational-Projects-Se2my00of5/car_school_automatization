package ru.hits.car_school_automatization.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.hits.car_school_automatization.enums.PostType;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreatePostDto {
    private String label;
    private String text;
    private PostType type;
    private LocalDateTime deadline;
    private Long authorId;
    private Boolean needMark;
    private String channelId;
}
