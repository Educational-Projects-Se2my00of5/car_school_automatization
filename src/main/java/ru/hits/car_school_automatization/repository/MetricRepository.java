package ru.hits.car_school_automatization.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.hits.car_school_automatization.entity.Metric;

import java.util.List;
import java.util.UUID;

@Repository
public interface MetricRepository extends JpaRepository<Metric, UUID> {
    List<Metric> findByPostId(UUID postId);
    List<Metric> findByTaskId(UUID taskId);
    boolean existsByPostId(UUID postId);
}
