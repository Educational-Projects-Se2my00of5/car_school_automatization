package ru.hits.car_school_automatization.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.hits.car_school_automatization.entity.Post;

import java.util.List;
import java.util.UUID;

@Repository
public interface PostRepository extends JpaRepository<Post, UUID> {

    List<Post> findByChannelIdOrderByCreatedAtDesc(UUID channelId);

    @Query("SELECT p FROM Post p WHERE p.channelId = :channelId AND p.authorId = :userId ORDER BY p.createdAt DESC")
    List<Post> findByChannelIdAndAuthorId(@Param("channelId") UUID channelId, @Param("userId") Long userId);
}
