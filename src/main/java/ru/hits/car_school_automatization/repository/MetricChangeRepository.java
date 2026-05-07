package ru.hits.car_school_automatization.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.hits.car_school_automatization.entity.MetricChange;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface MetricChangeRepository extends JpaRepository<MetricChange, UUID> {
	Optional<MetricChange> findFirstByMetricValueIdOrderByEditedAtDesc(UUID metricValueId);
}
