package ru.hits.car_school_automatization.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.hits.car_school_automatization.entity.TeamP2PReviewGrade;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TeamP2PReviewGradeRepository extends JpaRepository<TeamP2PReviewGrade, UUID> {
    Optional<TeamP2PReviewGrade> findByP2pPairTeamIdAndReviewerIdAndMetricId(UUID p2pPairTeamId, Long reviewerId, UUID metricId);

    List<TeamP2PReviewGrade> findByP2pPairTeamIdAndMetricId(UUID p2pPairTeamId, UUID metricId);
}
