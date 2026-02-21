package ru.hits.car_school_automatization.dto

import java.util.UUID

data class UserShortDto(
    val id: UUID,
    val name: String,
    val surname: String,
    val email: String,
)
