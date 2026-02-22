package ru.hits.car_school_automatization.service

import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import ru.hits.car_school_automatization.dto.ChannelDto
import ru.hits.car_school_automatization.dto.ChannelPatchDto
import ru.hits.car_school_automatization.dto.CreateChannelDto
import ru.hits.car_school_automatization.dto.ShortChannelDto
import ru.hits.car_school_automatization.exception.BadRequestException
import ru.hits.car_school_automatization.mapper.toChannelDto
import ru.hits.car_school_automatization.mapper.toEntity
import ru.hits.car_school_automatization.mapper.toShortChannelDto
import ru.hits.car_school_automatization.repository.ChannelRepository
import ru.hits.car_school_automatization.repository.UserRepository
import ru.hits.car_school_automatization.validator.validateChannelName
import ru.hits.car_school_automatization.validator.validateImage
import java.util.*
import kotlin.collections.all

@Service
open class ChannelService(
    private val channelRepository: ChannelRepository,
    private val userRepository: UserRepository,
    private val fileStorageService: FileStorageService,
    private val tokenProvider: JwtTokenProvider,
) {

    open fun createChanel(createChannelDto: CreateChannelDto, image: MultipartFile?, header: String) {
        validateChannelName(createChannelDto.name)

        val creatorId = loadIdFromHeader(header)
        val creator = userRepository.getReferenceById(creatorId)
        val users = createChannelDto.userIds.map {
            userRepository.getReferenceById(it)
        }.toMutableSet()
        val imagePath = image?.let { fileStorageService.store(it) }
        users.apply {
            if (all { it.id != creator.id }) {
                add(creator)
            }
        }
        channelRepository.save(createChannelDto.toEntity(creator, imagePath, users))
    }

    open fun deleteChanel(id: UUID) {
        channelRepository.getChannelById(id) ?: throw BadRequestException("Channel with id $id does not exist")
        channelRepository.deleteById(id)
    }

    fun editChannel(dto: ChannelPatchDto, channelId: UUID) {
        dto.image?.let {
            validateImage(it)
        }

        dto.name?.let {
            validateChannelName(it)
        }

        val channel = channelRepository.getChannelById(channelId)
            ?: throw BadRequestException("Channel with id $channelId not found")

        val imagePath = dto.image?.let { fileStorageService.store(it) }
        channelRepository.save(
            channel.copy(
                label = dto.name ?: channel.label,
                image = imagePath ?: channel.image
            )
        )
    }

    fun getUserChannels(userId: Long): List<ShortChannelDto> {
        return channelRepository.getUsersChannelByUserId(userId).map {
            it.toShortChannelDto()
        }
    }

    fun getUserChannels(header: String): List<ShortChannelDto> {
        val userId = loadIdFromHeader(header)
        print(userId)
        return channelRepository.getUsersChannelByUserId(userId).map {
            it.toShortChannelDto()
        }
    }

    fun getChannelById(channelId: UUID): ChannelDto {
        return (channelRepository.getChannelById(channelId)
            ?: throw BadRequestException("Channel with id $channelId not found")
                ).toChannelDto()
    }

    private fun loadIdFromHeader(header: String): Long {
        val token = tokenProvider.extractTokenFromHeader(header)

        if (!tokenProvider.validateToken(token)) {
            throw BadRequestException("Невалидный или истёкший токен")
        }

        return tokenProvider.getUserIdFromToken(token)
    }
}