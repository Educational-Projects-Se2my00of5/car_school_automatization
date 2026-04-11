package ru.hits.car_school_automatization.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.hits.car_school_automatization.entity.Team;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TeamRepository extends JpaRepository<Team, UUID> {

    List<Team> findByTask_Id(UUID taskId);

    // Есть ли у этого пользователя другая команда в рамках того же задания?
    boolean existsByTask_IdAndUsers_IdAndIdNot(UUID taskId, Long userId, UUID teamId);
    boolean existsByTask_Id(UUID taskId);
    Optional<Team> findByTask_IdAndUsers_Id(UUID taskId, Long userId);
}
