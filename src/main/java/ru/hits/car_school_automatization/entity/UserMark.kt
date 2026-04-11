package ru.hits.car_school_automatization.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.util.UUID

@Entity
@Table(name = "user_mark")
data class UserMark(
    @GeneratedValue(strategy = GenerationType.UUID)
    @Id val markId: UUID? = null,
    val mark: Float,
    @Column(name = "user_id", nullable = false)
    val userId: Long,
    @Column(name = "task_id", nullable = false)
    val taskId: UUID,
    val isAccessedDistribution: Boolean = false
){
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    lateinit var user: User

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id", insertable = false, updatable = false)
    lateinit var task: Task
}