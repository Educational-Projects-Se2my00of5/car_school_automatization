package ru.hits.car_school_automatization.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.hits.car_school_automatization.enums.PostType;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShortPostDto {
    private String id;
    private String label;
    private PostType type;
}
