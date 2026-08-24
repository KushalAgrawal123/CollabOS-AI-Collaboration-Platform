package com.collabos.backend.dto;

import com.collabos.backend.entity.Task;
import com.collabos.backend.entity.TaskPriority;
import com.collabos.backend.entity.TaskStatus;

import java.time.Instant;
import java.time.LocalDate;

public record TaskResponse(
        Long id,
        Long projectId,
        String title,
        String description,
        TaskStatus status,
        TaskPriority priority,
        Long assigneeId,
        String assigneeName,
        Long createdById,
        String createdByName,
        LocalDate dueDate,
        int position,
        Instant createdAt,
        Instant updatedAt
) {
    public static TaskResponse from(Task task) {
        return new TaskResponse(
                task.getId(),
                task.getProject().getId(),
                task.getTitle(),
                task.getDescription(),
                task.getStatus(),
                task.getPriority(),
                task.getAssignee() != null ? task.getAssignee().getId() : null,
                task.getAssignee() != null ? task.getAssignee().getName() : null,
                task.getCreatedBy().getId(),
                task.getCreatedBy().getName(),
                task.getDueDate(),
                task.getPosition(),
                task.getCreatedAt(),
                task.getUpdatedAt());
    }
}
