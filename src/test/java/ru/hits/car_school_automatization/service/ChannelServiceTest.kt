package ru.hits.car_school_automatization.service

import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import ru.hits.car_school_automatization.entity.Channel
import ru.hits.car_school_automatization.entity.User
import ru.hits.car_school_automatization.exception.BadRequestException
import ru.hits.car_school_automatization.factory.ChannelFactory.createChannel
import ru.hits.car_school_automatization.factory.ChannelFactory.createCreateChannelDto
import ru.hits.car_school_automatization.factory.createUser
import ru.hits.car_school_automatization.mapper.toChannelDto
import ru.hits.car_school_automatization.repository.ChannelRepository
import ru.hits.car_school_automatization.repository.UserRepository
import java.util.UUID
import java.util.UUID.randomUUID

@DisplayName("Channel Service Tests")
@ExtendWith(MockitoExtension::class)
class ChannelServiceTest(
) {
    private val incorrectChannelId = randomUUID()
    private val channelId: UUID = randomUUID()
    private val channelRepository = mock<ChannelRepository> {
        on { getChannelById(channelId) } doReturn createChannel(channelId)
        on { getChannelById(incorrectChannelId) } doReturn null

        on { deleteById(channelId) } doReturn Unit
        on { deleteById(incorrectChannelId) } doReturn Unit

        on { save(any<Channel>()) } doAnswer { invocation -> invocation.getArgument(0) }

        on { getUserByChannelId(channelId) } doReturn (0..5).map { User() }
        on { getUserByChannelId(incorrectChannelId) } doReturn listOf()

        on { getUsersChannelByUserId(any<UUID>()) } doReturn (0..3).map { createChannel() }
    }

    private val userRepository = mock<UserRepository> {
        on { getReferenceById(any<Long>()) } doReturn createUser()
    }

    private val channelService = ChannelService(channelRepository, userRepository)

    @Test
    fun `create channel with incorrect name`() {
        val channelDto = createCreateChannelDto()

        assertThrows<BadRequestException> {
            channelService.createChanel(channelDto, 0L)
        }

        verifyNoInteractions(channelRepository)
    }

    @Test
    fun `create channel with correct name`() {
        val channelDto = createCreateChannelDto(name = "test22")
        val creatorId = 0L
        channelService.createChanel(channelDto, creatorId)

        verify(channelRepository).save(any())
    }

    @Test
    fun `delete a non-existent channel`() {
        assertThrows<BadRequestException> {
            channelService.deleteChanel(incorrectChannelId)
        }

        verify(channelRepository).getChannelById(incorrectChannelId)
    }

    @Test
    fun `delete channel`() {
        channelService.deleteChanel(channelId)

        verify(channelRepository).deleteById(channelId)
    }

    @Test
    fun `edit channel with incorrect name`() {
        val (newName, newImage) = "" to null

        assertThrows<BadRequestException> {
            channelService.editChannel(newName, newImage, channelId)
        }

        verifyNoInteractions(channelRepository)
    }

    @Test
    fun `edit channel with correct name`() {
        val (newName, newImage) = "asdads" to null
        channelService.editChannel(newName, newImage, channelId)

        verify(channelRepository).save(any())
    }

    @Test
    fun `get channel by id`() {
        val channel = channelService.getChannelById(channelId)
        assert(channel == createChannel(channelId).toChannelDto())

        verify(channelRepository).getChannelById(channelId)
    }

    @Test
    fun `get channel by incorrect id`() {

        assertThrows<BadRequestException> {
            channelService.getChannelById(incorrectChannelId)
        }

        verify(channelRepository).getChannelById(incorrectChannelId)
    }


    @Test
    fun `get user channels`() {
        val id = randomUUID()
        channelService.getUserChannels(userId = id)

        verify(channelRepository).getUsersChannelByUserId(id)
    }
}


/*private val channelRepository = mock<ChannelRepository> {
        on { getChannelById(channelId) } doReturn createChannel(channelId)
        on { getChannelById(incorrectChannelId) } doReturn null

        on { deleteById(channelId) } doReturn Unit
        on { deleteById(incorrectChannelId) } doReturn Unit

        on { save(any()) } doReturn any()

        on { getUserByChannelId(channelId) } doReturn (0..5).map { User() }
        on { getUserByChannelId(incorrectChannelId) } doReturn listOf()

        on { getUsersChannelByUserId(any()) } doReturn (0..3).map { createChannel() }
    }*/