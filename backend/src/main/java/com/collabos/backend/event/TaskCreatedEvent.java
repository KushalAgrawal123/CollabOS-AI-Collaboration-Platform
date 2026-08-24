package com.collabos.backend.event;

public record TaskCreatedEvent(
        Long organizationId,
        Long projectId,
        String projectName,
        Long taskId,
        String taskTitle,
        Long createdById,
        String createdByName,
        String at
) {
}
