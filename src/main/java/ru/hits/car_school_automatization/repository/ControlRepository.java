package ru.hits.car_school_automatization.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.hits.car_school_automatization.entity.Control;

import java.util.List;
import java.util.UUID;

@Repository
public interface ControlRepository extends JpaRepository<Control, UUID> {
    List<Control> findByChannelId(UUID channelId);
}
