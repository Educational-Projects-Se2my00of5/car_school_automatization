package ru.hits.car_school_automatization.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;
import ru.hits.car_school_automatization.dto.DeadlinePenaltyDto;


@Component
@RequiredArgsConstructor
public class StringToDeadlinePenaltyDtoConverter implements Converter<String, DeadlinePenaltyDto> {

    private final ObjectMapper objectMapper;

    @SneakyThrows
    @Override
    public DeadlinePenaltyDto convert(String source) {
        return objectMapper.readValue(source, DeadlinePenaltyDto.class);
    }
}