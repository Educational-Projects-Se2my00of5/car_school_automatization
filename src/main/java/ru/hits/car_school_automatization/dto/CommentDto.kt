package ru.hits.car_school_automatization.dto

import kotlin.time.ExperimentalTime
import kotlin.time.Instant

data class CommentDto @OptIn(ExperimentalTime::class) constructor(
    val id: Int,
    val text: String,
    val authorDto: UserDto.FullInfo,
    val updateAt: Instant,
    val createAt: Instant,
)
