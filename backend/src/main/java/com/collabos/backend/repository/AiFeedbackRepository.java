package com.collabos.backend.repository;

import com.collabos.backend.entity.AiFeedback;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AiFeedbackRepository extends JpaRepository<AiFeedback, Long> {

    List<AiFeedback> findByProjectIdOrderByCreatedAtDesc(Long projectId);
}
