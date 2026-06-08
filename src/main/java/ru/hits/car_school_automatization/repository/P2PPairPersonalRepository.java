package ru.hits.car_school_automatization.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.hits.car_school_automatization.entity.P2PPairPersonal;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface P2PPairPersonalRepository extends JpaRepository<P2PPairPersonal, UUID> {
    List<P2PPairPersonal> findByPostId(UUID postId);

    List<P2PPairPersonal> findByReviewerId(Long reviewerId);

    @Query("SELECT p FROM P2PPairPersonal p WHERE p.status = 'PENDING' AND p.postId IN " +
            "(SELECT param.id FROM P2PParam param WHERE param.p2pDeadline < :now)")
    List<P2PPairPersonal> findExpiredPendingPairs(@Param("now") Instant now);
}
