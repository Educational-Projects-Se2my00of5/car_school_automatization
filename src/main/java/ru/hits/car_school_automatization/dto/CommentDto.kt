package ru.hits.car_school_automatization.dto

import java.time.LocalDateTime

data class CommentDto(
    val id: String,
    val text: String,
    val authorDto: UserDto,
    val updateAt: LocalDateTime,
    val createAt: LocalDateTime,
)
