package ru.hits.car_school_automatization.dto

import java.util.UUID

data class ChannelDto(
    val id: UUID,
    val name: String,
    val description: String?,
    val imageUrl: String?,
    val users: List<UserShortDto>

)
