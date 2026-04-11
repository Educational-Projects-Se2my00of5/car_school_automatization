package ru.hits.car_school_automatization.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.hits.car_school_automatization.entity.CaptainVote;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CaptainVoteRepository extends JpaRepository<CaptainVote, UUID> {

    List<CaptainVote> findByTeamId(UUID teamId);

    Optional<CaptainVote> findByTeamIdAndVoterId(UUID teamId, Long voterId);

    @Query("SELECT cv.candidateId, COUNT(cv) FROM CaptainVote cv WHERE cv.team.id = :teamId GROUP BY cv.candidateId")
    List<Object[]> countVotesByCandidate(@Param("teamId") UUID teamId);

    @Modifying
    @Query("DELETE FROM CaptainVote cv WHERE cv.team.id = :teamId")
    void deleteByTeamId(@Param("teamId") UUID teamId);

    boolean existsByTeamId(UUID teamId);
}