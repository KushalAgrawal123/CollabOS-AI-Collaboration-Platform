package com.collabos.backend.repository;

import com.collabos.backend.entity.Task;
import com.collabos.backend.entity.TaskStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TaskRepository extends JpaRepository<Task, Long> {

    @EntityGraph(attributePaths = {"project", "assignee", "createdBy"})
    List<Task> findByProjectIdOrderByStatusAscPositionAsc(Long projectId);

    Optional<Task> findByIdAndProjectId(Long id, Long projectId);

    int countByProjectIdAndStatus(Long projectId, TaskStatus status);
}
