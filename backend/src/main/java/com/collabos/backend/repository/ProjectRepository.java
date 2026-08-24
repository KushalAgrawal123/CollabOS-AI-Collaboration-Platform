package com.collabos.backend.repository;

import com.collabos.backend.entity.Project;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProjectRepository extends JpaRepository<Project, Long> {

    @EntityGraph(attributePaths = "owner")
    List<Project> findByOrganizationIdOrderByCreatedAtDesc(Long organizationId);

    @EntityGraph(attributePaths = "owner")
    Optional<Project> findByIdAndOrganizationId(Long id, Long organizationId);
}
