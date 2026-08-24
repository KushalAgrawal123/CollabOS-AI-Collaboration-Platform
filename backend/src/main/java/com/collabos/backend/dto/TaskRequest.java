package com.collabos.backend.dto;

import com.collabos.backend.entity.TaskPriority;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;

public record TaskRequest(
        @NotBlank String title,
        String description,
        TaskPriority priority,
        Long assigneeId,
        LocalDate dueDate
) {
}
