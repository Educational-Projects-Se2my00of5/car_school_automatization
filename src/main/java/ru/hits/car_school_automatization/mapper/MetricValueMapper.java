package ru.hits.car_school_automatization.mapper;

import org.mapstruct.Mapper;
import ru.hits.car_school_automatization.dto.MetricValueDto;
import ru.hits.car_school_automatization.entity.MetricValue;

import java.util.List;

@Mapper(componentModel = "spring")
public interface MetricValueMapper {
    MetricValueDto toDto(MetricValue metricValue);

    default MetricValueDto toDto(Long userId, Double value) {
        return MetricValueDto.builder()
                .userId(userId)
                .value(value)
                .build();
    }

    default List<MetricValueDto> toDtoList(List<MetricValue> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        return values.stream()
                .map(this::toDto)
                .toList();
    }
}
