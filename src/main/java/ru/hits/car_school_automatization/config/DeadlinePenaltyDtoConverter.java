package ru.hits.car_school_automatization.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;
import ru.hits.car_school_automatization.dto.DeadlinePenaltyDto;

@Component
@RequiredArgsConstructor
public class DeadlinePenaltyDtoConverter implements Converter<String, DeadlinePenaltyDto> {

    private final ObjectMapper objectMapper;

    @Override
    public DeadlinePenaltyDto convert(String source) {
        if (source == null) {
            return null;
        }

        String trimmed = source.trim();
        if (trimmed.isEmpty() || "null".equalsIgnoreCase(trimmed)) {
            return null;
        }

        try {
            return objectMapper.readValue(trimmed, DeadlinePenaltyDto.class);
        } catch (Exception e) {
            throw new IllegalArgumentException("Некорректный формат deadlinePenalty. Ожидается JSON, например: {\"unit\":\"DAYS\",\"step\":1,\"value\":0.5}", e);
        }
    }
}
