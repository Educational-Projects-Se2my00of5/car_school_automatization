package ru.hits.car_school_automatization.entity

import jakarta.persistence.*
import java.util.*

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
    var users: MutableSet<User>,
    @ManyToOne(fetch = FetchType.LAZY)
    val creator: User,
)