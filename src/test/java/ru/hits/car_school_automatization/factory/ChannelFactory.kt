package ru.hits.car_school_automatization.factory

import ru.hits.car_school_automatization.dto.CreateChannelDto
import ru.hits.car_school_automatization.dto.ShortChannelDto
import ru.hits.car_school_automatization.entity.Channel
import ru.hits.car_school_automatization.entity.User
import java.util.UUID
import java.util.UUID.randomUUID

object ChannelFactory {

    fun createCreateChannelDto(
        name: String = "",
        description: String? = null,
        userId: List<UUID> = listOf(),
        image: ByteArray? = null,
    ) = CreateChannelDto(
        name = name,
        description = description,
        userIds = userId,
        image = image
    )

    fun createShortChannel(
        id: UUID = randomUUID(),
        name: String = randomUUID().toString(),
        image: String? = null
    ) = ShortChannelDto(
        id = id,
        name = name,
        image = image
    )

    fun createChannel(
        id: UUID = randomUUID(),
        label: String = "",
        description: String? = null,
        image: String? = null,
        users: Set<User> = setOf(),
    ) = Channel(
        id = id,
        label = label,
        description = description,
        image = image,
        users = users
    )
}