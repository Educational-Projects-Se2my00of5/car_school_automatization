package ru.hits.car_school_automatization.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.hits.car_school_automatization.entity.Invite;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface InviteRepository extends JpaRepository<Invite, UUID> {

    List<Invite> findByInviteeId(Long inviteeId);

    Optional<Invite> findByTeamIdAndInviteeId(UUID teamId, Long inviteeId);

    void deleteByTeamId(UUID teamId);

    boolean existsByTeamIdAndInviteeId(UUID teamId, Long inviteeId);

    void deleteByInviteeId(Long inviteeId);
}