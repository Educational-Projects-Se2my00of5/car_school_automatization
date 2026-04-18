package ru.hits.car_school_automatization.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.hits.car_school_automatization.entity.SolutionVote;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SolutionVoteRepository extends JpaRepository<SolutionVote, UUID> {

    List<SolutionVote> findByTaskId(UUID taskId);

    List<SolutionVote> findBySolutionId(UUID solutionId);

    Optional<SolutionVote> findByTaskIdAndVoterId(UUID taskId, Long voterId);

    @Query("SELECT sv.solutionId, COUNT(sv) FROM SolutionVote sv WHERE sv.taskId = :taskId GROUP BY sv.solutionId")
    List<Object[]> countVotesBySolution(@Param("taskId") UUID taskId);

    boolean existsByTaskIdAndVoterId(UUID taskId, Long voterId);

    @Query("SELECT sv.solutionId, COUNT(sv) FROM SolutionVote sv WHERE sv.solutionId IN :solutionIds GROUP BY sv.solutionId")
    List<Object[]> countVotesBySolutionIds(@Param("solutionIds") List<UUID> solutionIds);

    @Modifying
    void deleteByTaskId(UUID taskId);

    @Modifying
    void deleteBySolutionId(UUID solutionId);
}