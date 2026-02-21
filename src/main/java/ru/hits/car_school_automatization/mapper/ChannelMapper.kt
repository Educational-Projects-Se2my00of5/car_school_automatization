package ru.hits.car_school_automatization.mapper

import org.mapstruct.Mapper
import ru.hits.car_school_automatization.dto.ChannelDto
import ru.hits.car_school_automatization.dto.CreateChannelDto
import ru.hits.car_school_automatization.dto.ShortChannelDto
import ru.hits.car_school_automatization.entity.Channel

@Mapper
interface ChannelMapper {

    fun toEntity(channel: CreateChannelDto): Channel

    fun toShortChannelDto(channel: Channel): ShortChannelDto

    fun toChannelDto(channel: Channel): ChannelDto
}