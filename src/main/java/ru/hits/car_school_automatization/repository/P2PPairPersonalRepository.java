package ru.hits.car_school_automatization.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.hits.car_school_automatization.entity.P2PPairPersonal;

import java.util.List;
import java.util.UUID;

@Repository
public interface P2PPairPersonalRepository extends JpaRepository<P2PPairPersonal, UUID> {
    List<P2PPairPersonal> findByPostId(UUID postId);
}
