package ru.hits.car_school_automatization.dto

import java.util.UUID

data class CreateChannelDto(
    val name: String,
    val description: String?,
    val userIds: List<UUID>,
    val image: ByteArray?,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as CreateChannelDto

        if (name != other.name) return false
        if (description != other.description) return false
        if (userIds != other.userIds) return false
        if (!image.contentEquals(other.image)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = result + description.hashCode()
        result = result + userIds.hashCode()
        result = result + image.contentHashCode()
        return result
    }
}
