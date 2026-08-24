package com.collabos.backend.repository;

import com.collabos.backend.entity.Invite;
import com.collabos.backend.entity.InviteStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InviteRepository extends JpaRepository<Invite, Long> {
    Optional<Invite> findByToken(String token);

    List<Invite> findByOrganizationIdAndStatusOrderByCreatedAtDesc(Long organizationId, InviteStatus status);
}
