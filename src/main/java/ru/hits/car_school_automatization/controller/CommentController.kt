package ru.hits.car_school_automatization.controller

import io.swagger.v3.oas.annotations.Operation
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.*
import ru.hits.car_school_automatization.dto.CommentDto
import ru.hits.car_school_automatization.dto.CreateCommentRequest
import ru.hits.car_school_automatization.dto.toCommentDto
import ru.hits.car_school_automatization.exception.BadRequestException
import ru.hits.car_school_automatization.mapper.UserMapper
import ru.hits.car_school_automatization.service.CommentService
import ru.hits.car_school_automatization.service.JwtTokenProvider
import java.util.UUID

@RestController
@RequestMapping("/comment")
class CommentController(
    private val commentService: CommentService,
    private val tokenProvider: JwtTokenProvider,
    private val userMapper: UserMapper,
) {

    @Operation(summary = "Создание комментариев")
    @PostMapping("post/{id}")
    fun createComment(
        @RequestHeader("Authorization") authHeader: String,
        @Valid @RequestBody comment: CreateCommentRequest,
        @PathVariable id: UUID
    ) {
        val userId = loadIdFromHeader(authHeader)
        commentService.createComment(userId, id, comment)
    }

    @Operation(summary = "Редактирование комментариев")
    @PutMapping("/comment/{id}")
    fun editComment(
        @RequestHeader("Authorization") authHeader: String,
        @RequestBody comment: String,
        @PathVariable id: Int
    ) {
        val userId = loadIdFromHeader(authHeader)
        commentService.updateComment(userId, comment, id)
    }

    @Operation(summary = "Удаление комментариев")
    @DeleteMapping("/{id}")
    fun deleteComment(@RequestHeader("Authorization") authHeader: String, @PathVariable id: Int) {
        val userId = loadIdFromHeader(authHeader)
        commentService.deleteComment(userId, id)
    }

    @Operation(summary = "Получение всех комментариев поста")
    @GetMapping("/post/{id}")
    fun getPostComments(
        @PathVariable id: UUID
    ): List<CommentDto> {
        return commentService.getComments(id).map { it.toCommentDto(userMapper) }
    }

    private fun loadIdFromHeader(header: String): Long {
        val token = tokenProvider.extractTokenFromHeader(header)

        if (!tokenProvider.validateToken(token)) {
            throw BadRequestException("Невалидный или истёкший токен")
        }

        return tokenProvider.getUserIdFromToken(token)
    }
}