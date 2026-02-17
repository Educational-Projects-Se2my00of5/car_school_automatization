package ru.hits.car_school_automatization.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.hits.car_school_automatization.dto.UserDto;
import ru.hits.car_school_automatization.entity.User;

/**
 * Маппер для преобразования User entity в DTO и обратно
 */
@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "passwordHash", ignore = true)
    @Mapping(target = "isActive", constant = "true")
    User toEntity(UserDto.CreateUser dto);


    UserDto.FullInfo toDto(User user);
}
