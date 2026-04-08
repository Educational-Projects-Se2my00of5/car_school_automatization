package ru.hits.car_school_automatization.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.hits.car_school_automatization.entity.TaskSolution;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TaskSolutionRepository extends JpaRepository<TaskSolution, UUID> {
    Optional<TaskSolution> findByTaskIdAndStudentId(UUID taskId, Long studentId);

    boolean existsByTaskIdAndStudentId(UUID taskId, Long studentId);

    List<TaskSolution> findByTaskIdOrderByCreatedAtDesc(UUID taskId);

    List<TaskSolution> findByStudentIdOrderByCreatedAtDesc(Long studentId);
}
