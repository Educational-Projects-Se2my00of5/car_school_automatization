package ru.hits.car_school_automatization.dto

import java.util.UUID

data class ShortChannelDto (
    val id: UUID,
    val name: String,
    val image: String?,
)