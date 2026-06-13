package ru.hits.car_school_automatization.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;
import ru.hits.car_school_automatization.dto.P2PParamDto;

@Component
@RequiredArgsConstructor
public class StringToP2PParamDtoConverter implements Converter<String, P2PParamDto> {

    private final ObjectMapper objectMapper;

    @SneakyThrows // или обработай JsonProcessingException вручную
    @Override
    public P2PParamDto convert(String source) {
        return objectMapper.readValue(source, P2PParamDto.class);
    }
}