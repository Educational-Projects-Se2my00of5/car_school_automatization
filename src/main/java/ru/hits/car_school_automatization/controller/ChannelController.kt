package ru.hits.car_school_automatization.controller

import io.swagger.v3.oas.annotations.Parameter
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.*
import ru.hits.car_school_automatization.dto.ChannelDto
import ru.hits.car_school_automatization.dto.ChannelPatchDto
import ru.hits.car_school_automatization.dto.CreateChannelDto
import ru.hits.car_school_automatization.dto.ShortChannelDto
import ru.hits.car_school_automatization.service.ChannelService
import ru.hits.car_school_automatization.validator.validateImage
import java.util.*

@RestController
@RequestMapping("/channel")
open class ChannelController(
    private val channelService: ChannelService
) {

    @PostMapping("/create", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    @ResponseStatus(HttpStatus.CREATED)
    fun createChannel(
        @Valid dto: CreateChannelDto,
        @Parameter(hidden = true) @RequestHeader("Authorization") authHeader: String
    ) {
        dto.image?.let {
            validateImage(it)
        }
        print(authHeader)
        channelService.createChanel(dto, dto.image, authHeader)
    }

    @GetMapping("/user/{id}")
    fun getChannelsByUserId(@PathVariable("id") id: Long): List<ShortChannelDto> {
        return channelService.getUserChannels(id)
    }

    @GetMapping
    fun getChannels(
        @Parameter(hidden = true) @RequestHeader("Authorization") authHeader: String
    ): List<ShortChannelDto> {
        return channelService.getUserChannels(authHeader)
    }

    @GetMapping("/{id}")
    fun getChannelById(@PathVariable("id") id: UUID): ChannelDto {
        return channelService.getChannelById(id)
    }

    @DeleteMapping("/delete/{id}")
    fun deleteChannel(@PathVariable("id") id: UUID) {
        channelService.deleteChanel(id)
    }

    @PatchMapping("/update/{id}", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun updateChannel(
        @PathVariable("id") id: UUID,
        dto: ChannelPatchDto,
    ) {
        channelService.editChannel(dto, id)
    }
}