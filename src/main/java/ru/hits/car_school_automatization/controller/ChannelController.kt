package ru.hits.car_school_automatization.controller

import io.swagger.v3.oas.annotations.Parameter
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import ru.hits.car_school_automatization.dto.ChannelDto
import ru.hits.car_school_automatization.dto.ChannelPatchDto
import ru.hits.car_school_automatization.dto.CreateChannelDto
import ru.hits.car_school_automatization.dto.ShortChannelDto
import ru.hits.car_school_automatization.service.ChannelService
import ru.hits.car_school_automatization.validator.validateImage
import java.util.UUID

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
        print(authHeader)
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