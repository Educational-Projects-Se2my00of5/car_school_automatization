package ru.hits.car_school_automatization.dto;

import jakarta.annotation.Nullable;
import jakarta.persistence.PrePersist;
import jakarta.validation.constraints.NotNull;
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
    @NotNull
    private String label;

    @NotNull
    private String text;

    @NotNull
    private PostType type;

    @Nullable
    private LocalDateTime deadline;

    @NotNull
    private Long authorId;
    private Boolean needMark;

    @NotNull
    private String channelId;

    @PrePersist
    void setNeedMark() {
        this.needMark = PostType.TASK.equals(this.type);
    }
}
