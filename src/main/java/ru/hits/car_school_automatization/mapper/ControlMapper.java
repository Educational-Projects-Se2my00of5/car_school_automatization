package ru.hits.car_school_automatization.mapper;

import org.mapstruct.Mapper;
import ru.hits.car_school_automatization.dto.ControlDto;
import ru.hits.car_school_automatization.entity.Control;

@Mapper(componentModel = "spring")
public interface ControlMapper {
    ControlDto toDto(Control control);
}
