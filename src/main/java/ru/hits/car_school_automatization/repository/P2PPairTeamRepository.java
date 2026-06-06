package ru.hits.car_school_automatization.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.hits.car_school_automatization.entity.P2PPairTeam;

import java.util.List;
import java.util.UUID;

@Repository
public interface P2PPairTeamRepository extends JpaRepository<P2PPairTeam, UUID> {
    List<P2PPairTeam> findByTaskId(UUID taskId);
}
