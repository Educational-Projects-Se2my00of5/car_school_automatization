package ru.hits.car_school_automatization.service

import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.extension.ExtendWith
import org.mapstruct.Mapper
import org.mapstruct.factory.Mappers
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import ru.hits.car_school_automatization.entity.User
import ru.hits.car_school_automatization.factory.ChannelFactory.createChannel
import ru.hits.car_school_automatization.factory.ChannelFactory.createCreateChannelDto
import ru.hits.car_school_automatization.mapper.ChannelMapper
import ru.hits.car_school_automatization.repository.ChannelRepository
import java.util.UUID
import java.util.UUID.randomUUID

@DisplayName("Channel Service Tests")
@ExtendWith(MockitoExtension::class)
class ChannelServiceTest(
    private val userMapper: ChannelMapper = Mappers.getMapper(ChannelMapper::class.java),
) {
    private val incorrectChannelId = randomUUID()
    private val channelId: UUID = randomUUID()
    private val channelRepository = mock<ChannelRepository> {
        on { getChannelById(channelId) } doReturn createChannel(channelId)
        on { getChannelById(incorrectChannelId) } doReturn null

        on { delete(channelId) } doReturn Unit
        on { delete(incorrectChannelId) } doReturn Unit

        on { create(any()) } doReturn channelId

        on { update(createChannel(incorrectChannelId)) } doReturn null
        on { update(createChannel(channelId)) } doReturn channelId

        on { getUserByChannelId(channelId) } doReturn (0..5).map { User() }
        on { getUserByChannelId(incorrectChannelId) } doReturn listOf()

        on { getUsersChannelByUserId(any()) } doReturn (0..3).map { createChannel() }
    }

    private val channelService = ChannelService(channelRepository)

    fun `create channel with incorrect name`() {
        val channelDto = createCreateChannelDto()
        val id = channelService.createChanel()

        assert(id == channelId)

        verifyNoInteractions(channelRepository)
    }

    fun `create channel with correct name`() {
        val channelDto = createCreateChannelDto()
        val id = channelService.createChanel()

        assert(id == channelId)

        verify(channelRepository).create(userMapper.toEntity(channelDto))
    }

    fun `delete a non-existent channel`() {
        channelService.deleteChanel(channelId)
        verifyNoInteractions(channelRepository)
    }

    fun `delete channel`() {
        channelService.deleteChanel(channelId)
        verify(channelRepository).delete(channelId)
    }

    fun `edit channel with incorrect name`() {
        val (newName, newImage) = "" to null
        channelService.editChannel(newName, newImage, channelId)

        verifyNoInteractions(channelRepository)
    }

    fun `edit channel with correct name`() {
        val (newName, newImage) = "" to null
        val id = channelService.editChannel(newName, newImage, channelId)

        assert(id == channelId)

        verify(channelRepository).update(any())
    }

    fun `get all users from channel`() {
        val users = channelService.getAllUsers(channelId)
        assert(users.size == 5)

        verify(channelRepository).getChannelById(channelId)
    }

    fun `get all users from channel with incorrect id`() {
        val users = channelService.getAllUsers(incorrectChannelId)
        assert(users.size == 0)

        verify(channelRepository).getChannelById(incorrectChannelId)
    }

}