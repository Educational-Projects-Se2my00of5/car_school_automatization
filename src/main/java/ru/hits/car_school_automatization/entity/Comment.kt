@file:OptIn(ExperimentalTime::class)

package ru.hits.car_school_automatization.entity

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id
import jakarta.persistence.ManyToOne
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@Entity(name = "comment")
data class Comment(
    @Id @GeneratedValue
    val id: Int = 0,
    var text: String,
    var editAt: Instant,
    val createAt: Instant,
    @ManyToOne
    val author: User,
    @ManyToOne
    val post: Post
)
