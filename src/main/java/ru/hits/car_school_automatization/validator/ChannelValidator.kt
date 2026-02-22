package ru.hits.car_school_automatization.validator

import ru.hits.car_school_automatization.exception.BadRequestException

fun validateChannelName(name: String) {
    if (name.length < 5) throw BadRequestException("Channel name must be at least 5 characters long")
}