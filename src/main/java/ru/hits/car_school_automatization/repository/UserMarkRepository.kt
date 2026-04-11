package ru.hits.car_school_automatization.repository

import org.springframework.data.jpa.repository.JpaRepository
import ru.hits.car_school_automatization.entity.UserMark
import java.util.UUID

interface UserMarkRepository : JpaRepository<UserMark, UUID> {

    fun findMarkByUserIdAndTaskId(userId: Long, taskId: UUID): UserMark?
}