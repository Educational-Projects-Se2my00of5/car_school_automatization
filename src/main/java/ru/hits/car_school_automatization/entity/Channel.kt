package ru.hits.car_school_automatization.entity

import jakarta.persistence.CascadeType
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.JoinTable
import jakarta.persistence.ManyToMany
import jakarta.persistence.ManyToOne
import java.util.UUID

@Entity
data class Channel(
    @Id @GeneratedValue(strategy = GenerationType.AUTO)
    val id: UUID? = null,
    val label: String,
    val description: String?,
    val image: String?,
    @ManyToMany(cascade = [(CascadeType.MERGE)])
    @JoinTable(
        name = "channel_users",
        joinColumns = [(JoinColumn(name = "channel_user_id", referencedColumnName = "id"))],
        inverseJoinColumns = [(JoinColumn(name = "user_id", referencedColumnName = "id"))]
    )
    val users: Set<User>,
    @ManyToOne(fetch = FetchType.LAZY)
    val creator: User,
)