package ru.hits.car_school_automatization.mapper;

import org.mapstruct.Mapper;
import ru.hits.car_school_automatization.dto.MetricDto;
import ru.hits.car_school_automatization.entity.Metric;

import java.util.List;

@Mapper(componentModel = "spring")
public interface MetricMapper {
    MetricDto toDto(Metric metric);

    List<MetricDto> toDtoList(List<Metric> metrics);
}
