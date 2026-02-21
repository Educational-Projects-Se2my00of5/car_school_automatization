package ru.hits.car_school_automatization.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import ru.hits.car_school_automatization.dto.ChannelDto
import ru.hits.car_school_automatization.dto.CreateChannelDto
import ru.hits.car_school_automatization.dto.ShortChannelDto
import ru.hits.car_school_automatization.exception.BadRequestException
import ru.hits.car_school_automatization.mapper.toChannelDto
import ru.hits.car_school_automatization.mapper.toEntity
import ru.hits.car_school_automatization.mapper.toShortChannelDto
import ru.hits.car_school_automatization.repository.ChannelRepository
import ru.hits.car_school_automatization.repository.UserRepository
import ru.hits.car_school_automatization.validator.validateChannelName
import java.util.UUID

@Service
@Transactional
open class ChannelService(
    private val channelRepository: ChannelRepository,
    private val userRepository: UserRepository
) {

    @Transactional
    open fun createChanel(createChannelDto: CreateChannelDto, creatorId: Long) {
        validateChannelName(createChannelDto.name)
        val creator = userRepository.getReferenceById(creatorId)
        val users = createChannelDto.userIds.map {
            userRepository.getReferenceById(it)
        }.toSet()
        channelRepository.save(createChannelDto.toEntity(creator, "", users))
    }

    @Transactional
    open fun deleteChanel(id: UUID) {
        channelRepository.getChannelById(id)?: throw BadRequestException("Channel with id $id does not exist")
        channelRepository.deleteById(id)
    }

    fun editChannel(name: String?, image: ByteArray?, channelId: UUID) {
        name?.let {
            validateChannelName(it)
        }

        val channel = channelRepository.getChannelById(channelId)
            ?: throw BadRequestException("Channel with id $channelId not found")
        channelRepository.save(
            channel.copy(
                label = name ?: channel.label,
            )
        )
    }

    fun getUserChannels(userId: UUID): List<ShortChannelDto> {
        return channelRepository.getUsersChannelByUserId(userId).map {
            it.toShortChannelDto()
        }
    }

    fun getChannelById(channelId: UUID): ChannelDto {
        return (channelRepository.getChannelById(channelId)
            ?: throw BadRequestException("Channel with id $channelId not found")
                ).toChannelDto()
    }
}