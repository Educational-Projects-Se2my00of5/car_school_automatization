package ru.hits.car_school_automatization.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.hits.car_school_automatization.entity.Task;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface TaskRepository extends JpaRepository<Task, UUID> {
	List<Task> findByChannel_Id(UUID channelId);

	List<Task> findByVotingDeadlineBefore(Instant now);
}
