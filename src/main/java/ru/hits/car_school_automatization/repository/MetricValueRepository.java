package ru.hits.car_school_automatization.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.hits.car_school_automatization.entity.MetricValue;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MetricValueRepository extends JpaRepository<MetricValue, UUID> {
    Optional<MetricValue> findByMetricIdAndUserId(UUID metricId, Long userId);
    List<MetricValue> findByMetricIdInAndUserId(List<UUID> metricIds, Long userId);
    List<MetricValue> findByMetricIdIn(List<UUID> metricIds);
    List<MetricValue> findByMetricId(UUID metricId);
}
