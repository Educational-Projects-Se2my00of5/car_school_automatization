@file:OptIn(ExperimentalTime::class)

package ru.hits.car_school_automatization.service

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentCaptor
import org.mockito.Captor
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.doNothing
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.firstValue
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import ru.hits.car_school_automatization.dto.CreateCommentRequest
import ru.hits.car_school_automatization.entity.Comment
import ru.hits.car_school_automatization.entity.Post
import ru.hits.car_school_automatization.entity.User
import ru.hits.car_school_automatization.enums.Role
import ru.hits.car_school_automatization.exception.BadRequestException
import ru.hits.car_school_automatization.exception.ForbiddenException
import ru.hits.car_school_automatization.repository.CommentRepository
import ru.hits.car_school_automatization.repository.PostRepository
import ru.hits.car_school_automatization.repository.UserRepository
import java.util.Optional
import java.util.UUID
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@DisplayName("Comment Service Tests")
@ExtendWith(MockitoExtension::class)
class CommentServiceTest {

    @Mock
    private lateinit var commentRepository: CommentRepository

    @Mock
    private lateinit var postRepository: PostRepository

    @Mock
    private lateinit var userRepository: UserRepository

    private lateinit var commentService: CommentService

    @Captor
    private lateinit var commentCaptor: ArgumentCaptor<Comment>

    @BeforeEach
    fun setUp() {
        commentService = CommentService(commentRepository, postRepository, userRepository)
    }

    @Test
    fun `createComment should save comment when user and post exist`() {
        val authorId = 1L
        val postId = UUID.randomUUID()
        val request = CreateCommentRequest("Test comment")
        val user = mock<User>()
        val post = mock<Post>()

        whenever(userRepository.findById(authorId)).thenReturn(Optional.of(user))
        whenever(postRepository.findById(postId)).thenReturn(Optional.of(post))
        doNothing().whenever(commentRepository).save(any())

        commentService.createComment(authorId, postId, request)

        verify(commentRepository).save(commentCaptor.capture())
        val savedComment = commentCaptor.firstValue

        assert(savedComment.text == request.text)
        assert(savedComment.author == user)
        assert(savedComment.post == post)
    }

    @Test
    fun `createComment should throw BadRequestException when user not found`() {
        val authorId = 1L
        val postId = UUID.randomUUID()
        val request = CreateCommentRequest("text")

        whenever(userRepository.findById(authorId)).thenReturn(Optional.empty())

        assertThrows<BadRequestException> {
            commentService.createComment(authorId, postId, request)
        }

        verify(commentRepository, never()).save(any())
    }

    @Test
    fun `createComment should throw BadRequestException when post not found`() {
        val authorId = 1L
        val postId = UUID.randomUUID()
        val request = CreateCommentRequest("text")
        val user = mock<User>()

        whenever(userRepository.findById(authorId)).thenReturn(Optional.of(user))
        whenever(postRepository.findById(postId)).thenReturn(Optional.empty())

        assertThrows<BadRequestException> {
            commentService.createComment(authorId, postId, request)
        }

        verify(commentRepository, never()).save(any())
    }

    @Test
    fun `deleteComment should delete comment when user is author`() {
        val userId = 1L
        val commentId = 1
        val comment = mock<Comment>()
        val user = mock<User>()
        val author = mock<User>()

        whenever(comment.author).thenReturn(author)
        whenever(author.id).thenReturn(userId)
        whenever(user.id).thenReturn(userId)
        whenever(user.role).thenReturn(listOf())
        whenever(commentRepository.findById(commentId)).thenReturn(Optional.of(comment))
        whenever(userRepository.findById(userId)).thenReturn(Optional.of(user))
        doNothing().whenever(commentRepository).delete(comment)

        commentService.deleteComment(userId, commentId)

        verify(commentRepository).delete(comment)
    }

    @Test
    fun `deleteComment should delete comment when user is manager even if not author`() {
        val userId = 1L
        val commentId = 1
        val comment = mock<Comment>()
        val user = mock<User>()

        whenever(user.role).thenReturn(listOf(Role.MANAGER))
        whenever(commentRepository.findById(commentId)).thenReturn(Optional.of(comment))
        whenever(userRepository.findById(userId)).thenReturn(Optional.of(user))
        doNothing().whenever(commentRepository).delete(comment)

        commentService.deleteComment(userId, commentId)

        verify(commentRepository).delete(comment)
    }

    @Test
    fun `deleteComment should throw ForbiddenException when user not author and not manager`() {
        val userId = 1L
        val commentId = 1
        val comment = mock<Comment>()
        val user = mock<User>()
        val author = mock<User>()

        whenever(comment.author).thenReturn(author)
        whenever(author.id).thenReturn(2L)
        whenever(user.id).thenReturn(userId)
        whenever(user.role).thenReturn(listOf())
        whenever(commentRepository.findById(commentId)).thenReturn(Optional.of(comment))
        whenever(userRepository.findById(userId)).thenReturn(Optional.of(user))

        assertThrows<ForbiddenException> {
            commentService.deleteComment(userId, commentId)
        }

        verify(commentRepository, never()).delete(any())
    }

    @Test
    fun `deleteComment should throw BadRequestException when comment not found`() {
        val userId = 1L
        val commentId = 1

        whenever(commentRepository.findById(commentId)).thenReturn(Optional.empty())

        assertThrows<BadRequestException> {
            commentService.deleteComment(userId, commentId)
        }
    }

    @Test
    fun `deleteComment should throw BadRequestException when user not found`() {
        val userId = 1L
        val commentId = 1
        val comment = mock<Comment>()

        whenever(commentRepository.findById(commentId)).thenReturn(Optional.of(comment))
        whenever(userRepository.findById(userId)).thenReturn(Optional.empty())

        assertThrows<BadRequestException> {
            commentService.deleteComment(userId, commentId)
        }
    }

    @Test
    fun `getComments should return comments when post exists`() {
        val postId = UUID.randomUUID()
        val post = mock<Post>()
        val comments = listOf(mock<Comment>(), mock<Comment>())

        whenever(post.id).thenReturn(postId)
        whenever(postRepository.findById(postId)).thenReturn(Optional.of(post))
        whenever(commentRepository.findAllByPostId(postId)).thenReturn(comments)

        val result = commentService.getComments(postId)

        assert(result == comments)
    }

    @Test
    fun `getComments should throw BadRequestException when post not found`() {
        val postId = UUID.randomUUID()

        whenever(postRepository.findById(postId)).thenReturn(Optional.empty())

        assertThrows<BadRequestException> {
            commentService.getComments(postId)
        }
    }

    @Test
    fun `updateComment should update comment when user is author`() {
        val userId = 1L
        val commentId = 1
        val newText = "Updated text"

        val author = mock<User>{
            on { this.id }.doReturn(userId)
        }
        val comment = Comment(
            id = commentId,
            text = "old text",
            author = author,
            createAt = Clock.System.now(),
            editAt = Clock.System.now(),
            post = mock()
        )

        whenever(commentRepository.findById(commentId)).thenReturn(Optional.of(comment))
        doNothing().whenever(commentRepository).save(any<Comment>())

        commentService.updateComment(userId, newText, commentId)

        verify(commentRepository).save(commentCaptor.capture())
        val savedComment = commentCaptor.firstValue

        assert(savedComment.text == newText)
    }

    @Test
    fun `updateComment should throw ForbiddenException when user not author`() {
        val userId = 1L
        val commentId = 1
        val newText = "Updated text"
        val comment = mock<Comment>()
        val author = mock<User>()

        whenever(comment.author).thenReturn(author)
        whenever(author.id).thenReturn(2L)
        whenever(commentRepository.findById(commentId)).thenReturn(Optional.of(comment))

        assertThrows<ForbiddenException> {
            commentService.updateComment(userId, newText, commentId)
        }

        verify(commentRepository, never()).save(any())
    }

    @Test
    fun `updateComment should throw BadRequestException when comment not found`() {
        val userId = 1L
        val commentId = 1
        val newText = "Updated text"

        whenever(commentRepository.findById(commentId)).thenReturn(Optional.empty())

        assertThrows<BadRequestException> {
            commentService.updateComment(userId, newText, commentId)
        }
    }
}