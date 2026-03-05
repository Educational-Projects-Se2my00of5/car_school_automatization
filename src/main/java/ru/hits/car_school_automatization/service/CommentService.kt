@file:OptIn(ExperimentalTime::class)

package ru.hits.car_school_automatization.service

import org.springframework.stereotype.Service
import ru.hits.car_school_automatization.dto.CreateCommentRequest
import ru.hits.car_school_automatization.entity.Comment
import ru.hits.car_school_automatization.enums.Role
import ru.hits.car_school_automatization.exception.BadRequestException
import ru.hits.car_school_automatization.exception.ForbiddenException
import ru.hits.car_school_automatization.repository.CommentRepository
import ru.hits.car_school_automatization.repository.PostRepository
import ru.hits.car_school_automatization.repository.UserRepository
import java.util.*
import kotlin.jvm.optionals.getOrElse
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@Service
class CommentService(
    private val commentRepository: CommentRepository,
    private val postRepository: PostRepository,
    private val userRepository: UserRepository,
) {

    fun createComment(authorId: Long, postId: UUID, comment: CreateCommentRequest) {
        val user = userRepository.findById(authorId).orElseThrow { BadRequestException("User not found") }
        val post = postRepository.findById(postId).orElseThrow { BadRequestException("Post not found") }
        commentRepository.save(
            Comment(
                text = comment.text,
                editAt = Clock.System.now(),
                createAt = Clock.System.now(),
                author = user,
                post = post
            )
        )
    }

    fun deleteComment(userId: Long, commentId: Int) {
        val comment = commentRepository.findById(commentId).orElseThrow { BadRequestException("Comment not found") }
        val user = userRepository.findById(userId).orElseThrow { BadRequestException("User not found") }
        if (!user.role.contains(Role.MANAGER) && comment.author.id != user.id) {
            throw ForbiddenException("User not allowed to delete this comment")
        }
        commentRepository.delete(comment)
    }

    fun getComments(postId: UUID): List<Comment> {
        val post = postRepository.findById(postId).getOrElse { throw BadRequestException("Post not found") }
        return commentRepository.findAllByPostId(post.id)
    }

    fun updateComment(userId: Long, text: String, commentId: Int) {
        val comment = commentRepository.findById(commentId).orElseThrow { BadRequestException("Comment not found") }
        if (comment.author.id != userId) {
            throw ForbiddenException("User not allowed to update this comment")
        }
        comment.text = text
        comment.editAt = Clock.System.now()
        commentRepository.save(
            comment.copy(
                text = text,
                editAt = Clock.System.now(),
            )
        )
    }
}