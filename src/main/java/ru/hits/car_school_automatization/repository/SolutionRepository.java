package ru.hits.car_school_automatization.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.hits.car_school_automatization.entity.Solution;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SolutionRepository extends JpaRepository<Solution, UUID> {

    // Найти решение студента по заданию
    Optional<Solution> findByTaskIdAndStudentId(UUID taskId, Long studentId);

    // Все решения студента
    List<Solution> findByStudentId(Long studentId);

    // Все решения по заданию
    List<Solution> findByTaskId(UUID taskId);

    // Все решения, оценённые преподавателем
    List<Solution> findByTeacherId(Long teacherId);

    // Решения без оценки
    @Query("SELECT s FROM Solution s WHERE s.taskId = :taskId AND s.mark IS NULL")
    List<Solution> findUngradedByTaskId(@Param("taskId") UUID taskId);

    // Проверить, отправлял ли студент решение
    boolean existsByTaskIdAndStudentId(UUID taskId, Long studentId);

    // Получить все решения студента с сортировкой
    @Query("SELECT s FROM Solution s WHERE s.studentId = :studentId ORDER BY s.submittedAt DESC")
    List<Solution> findStudentSolutions(@Param("studentId") Long studentId);

    // Получить решения по заданию с сортировкой
    @Query("SELECT s FROM Solution s WHERE s.taskId = :taskId ORDER BY s.submittedAt DESC")
    List<Solution> findTaskSolutions(@Param("taskId") UUID taskId);
}