package ru.hits.car_school_automatization.mapper

import ru.hits.car_school_automatization.dto.UserShortDto
import ru.hits.car_school_automatization.entity.User

fun User.toShort() = UserShortDto(
    id = id,
    name = firstName,
    surname = lastName,
    email = email
)