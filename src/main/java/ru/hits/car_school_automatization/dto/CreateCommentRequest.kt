package ru.hits.car_school_automatization.dto

import jakarta.validation.constraints.Size

data class CreateCommentRequest(
    @param:Size(max = 64, min = 2)
    val text: String
)