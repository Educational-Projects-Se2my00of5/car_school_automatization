package ru.hits.car_school_automatization.controller

import io.swagger.v3.oas.annotations.Operation
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.*
import ru.hits.car_school_automatization.dto.CommentDto
import ru.hits.car_school_automatization.dto.CreateCommentRequest

@RestController
@RequestMapping("/comment")
class CommentController() {

    @Operation(summary = "Создание комментариев")
    @PostMapping("post/{id}")
    fun createComment(@Valid @RequestBody comment: CreateCommentRequest, @PathVariable id: String) {
        TODO()
    }

    @Operation(summary = "Редактирование комментариев")
    @PutMapping("/comment/{id}")
    fun editComment(@RequestBody comment: String, @PathVariable id: String) {
        TODO()
    }

    @Operation(summary = "Удаление комментариев")
    @DeleteMapping("/{id}")
    fun deleteComment(@PathVariable id: String) {
        TODO()
    }

    @Operation(summary = "Получение всех комментариев поста")
    @GetMapping("/post/{id}")
    fun getPostComments(@PathVariable id: String): List<CommentDto> {
        TODO()
    }
}