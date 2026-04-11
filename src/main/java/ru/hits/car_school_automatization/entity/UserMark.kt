package ru.hits.car_school_automatization.entity

import jakarta.persistence.CascadeType
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.util.UUID

@Entity
@Table(name = "user_mark")
data class UserMark(
    @GeneratedValue(strategy = GenerationType.UUID)
    @Id val markId: UUID? = null,
    val mark: Float,
    @ManyToOne(fetch = FetchType.LAZY, cascade = [CascadeType.ALL], targetEntity = User::class)
    val userId: Long,
    @ManyToOne(fetch = FetchType.LAZY, cascade = [CascadeType.ALL], targetEntity = Task::class)
    val taskId: UUID,
    val isAccessedDistribution: Boolean = false
)