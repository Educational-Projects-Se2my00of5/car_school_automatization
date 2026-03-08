package ru.hits.car_school_automatization.repository

import org.springframework.data.jpa.repository.JpaRepository
import ru.hits.car_school_automatization.entity.Comment
import java.util.UUID

interface CommentRepository : JpaRepository<Comment, Int> {

    fun findAllByPostId(postId: UUID): List<Comment>
    fun countByPostId(postId: UUID): Int
}