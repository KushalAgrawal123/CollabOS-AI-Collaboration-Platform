package com.collabos.backend.repository;

import com.collabos.backend.entity.Document;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DocumentRepository extends JpaRepository<Document, Long> {

    @EntityGraph(attributePaths = {"uploadedBy", "task"})
    List<Document> findByProjectIdOrderByCreatedAtDesc(Long projectId);

    Optional<Document> findByIdAndProjectId(Long id, Long projectId);
}
