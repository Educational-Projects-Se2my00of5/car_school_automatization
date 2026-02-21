package ru.hits.car_school_automatization.validator

import org.springframework.web.multipart.MultipartFile
import ru.hits.car_school_automatization.exception.BadRequestException

fun validateImage(file: MultipartFile) {
    if ( file.contentType !in allowedTypes) {
        throw BadRequestException("Only images are allowed")
    }
}

private val allowedTypes = listOf("image/jpeg", "image/png", "image/webp")
