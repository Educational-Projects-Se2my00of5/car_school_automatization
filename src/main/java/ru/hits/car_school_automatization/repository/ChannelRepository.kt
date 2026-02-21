package ru.hits.car_school_automatization.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import ru.hits.car_school_automatization.entity.Channel
import ru.hits.car_school_automatization.entity.User
import java.util.UUID

@Repository
interface ChannelRepository : JpaRepository<Channel, UUID> {

    fun getChannelById(id: UUID): Channel?

    @Query("SELECT c FROM Channel c LEFT JOIN FETCH c.users WHERE c.id = :channelId")
    fun getUserByChannelId(chanelId: UUID): List<User>

    @Query("SELECT c FROM Channel c JOIN c.users u WHERE u.id = :userId")
    fun getUsersChannelByUserId(userId: Long): List<Channel>
}