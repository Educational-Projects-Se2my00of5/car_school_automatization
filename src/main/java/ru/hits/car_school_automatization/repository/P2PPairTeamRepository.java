package ru.hits.car_school_automatization.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.hits.car_school_automatization.entity.P2PPairTeam;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface P2PPairTeamRepository extends JpaRepository<P2PPairTeam, UUID> {
    List<P2PPairTeam> findByTaskId(UUID taskId);

    List<P2PPairTeam> findByReviewerTeamIdIn(List<UUID> reviewerTeamIds);

    @Query("SELECT p FROM P2PPairTeam p WHERE p.status = 'PENDING' AND p.taskId IN " +
            "(SELECT param.id FROM P2PParam param WHERE param.p2pDeadline < :now)")
    List<P2PPairTeam> findExpiredPendingPairs(@Param("now") Instant now);
}
