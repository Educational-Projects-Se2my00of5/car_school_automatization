package ru.hits.car_school_automatization.dto

import org.springframework.web.multipart.MultipartFile

data class ChannelPatchDto (
    val name: String?,
    val image: MultipartFile?,
)