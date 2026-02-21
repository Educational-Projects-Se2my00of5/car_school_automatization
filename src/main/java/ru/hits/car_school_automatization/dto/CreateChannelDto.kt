package ru.hits.car_school_automatization.dto

import jakarta.validation.constraints.Size
import org.springframework.web.multipart.MultipartFile

data class CreateChannelDto(
    @param:Size(min = 1, max = 255)
    val name: String,
    val description: String?,
    val userIds: List<Long>,
    val image: MultipartFile?,
)
