package ru.hits.car_school_automatization.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.hits.car_school_automatization.entity.P2PParam;

import java.util.UUID;

@Repository
public interface P2PParamRepository extends JpaRepository<P2PParam, UUID> {
}
