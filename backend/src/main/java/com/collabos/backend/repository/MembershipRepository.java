package com.collabos.backend.repository;

import com.collabos.backend.entity.Membership;
import com.collabos.backend.entity.Role;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MembershipRepository extends JpaRepository<Membership, Long> {
    Optional<Membership> findByOrganizationIdAndUserId(Long organizationId, Long userId);

    @EntityGraph(attributePaths = "organization")
    List<Membership> findByUserIdOrderByJoinedAtAsc(Long userId);

    @EntityGraph(attributePaths = "user")
    List<Membership> findByOrganizationIdOrderByJoinedAtAsc(Long organizationId);

    long countByOrganizationIdAndRole(Long organizationId, Role role);

    boolean existsByOrganizationIdAndUserId(Long organizationId, Long userId);
}
