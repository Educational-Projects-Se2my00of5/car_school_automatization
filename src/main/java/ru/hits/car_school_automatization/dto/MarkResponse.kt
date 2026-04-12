package ru.hits.car_school_automatization.dto

data class MarkResponse(
    val user: UserDto.FullInfo,
    val mark: Float
)
