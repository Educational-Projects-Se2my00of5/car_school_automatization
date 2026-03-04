package ru.hits.car_school_automatization.dto;

import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.multipart.MultipartFile;
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
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime deadline;

    private Boolean needMark;

    @NotNull
    private String channelId;

    private MultipartFile file;
}
