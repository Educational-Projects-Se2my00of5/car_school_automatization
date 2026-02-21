package ru.hits.car_school_automatization.mapper

import ru.hits.car_school_automatization.dto.ChannelDto
import ru.hits.car_school_automatization.dto.CreateChannelDto
import ru.hits.car_school_automatization.dto.ShortChannelDto
import ru.hits.car_school_automatization.entity.Channel
import ru.hits.car_school_automatization.entity.User

fun CreateChannelDto.toEntity(creator: User, imagePath: String, users: Set<User>) = Channel(
    label = name,
    description = description,
    image = imagePath,
    users = users,
    creator = creator
)

fun Channel.toShortChannelDto() = ShortChannelDto(
    id = id!!,
    name = label,
    image = image
)

fun Channel.toChannelDto() = ChannelDto(
    id = id!!,
    name = label,
    description = description,
    imageUrl = image,
    users = users.map { it.toShort() }
)
