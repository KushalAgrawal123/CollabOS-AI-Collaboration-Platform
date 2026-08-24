package com.collabos.backend.repository;

import com.collabos.backend.entity.TaskComment;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TaskCommentRepository extends JpaRepository<TaskComment, Long> {

    @EntityGraph(attributePaths = "author")
    List<TaskComment> findByTaskIdOrderByCreatedAtAsc(Long taskId);
}
