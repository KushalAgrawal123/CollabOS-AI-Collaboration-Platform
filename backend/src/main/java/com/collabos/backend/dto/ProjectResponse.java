package com.collabos.backend.dto;

import com.collabos.backend.entity.Project;

import java.time.Instant;

public record ProjectResponse(
        Long id,
        String name,
        String description,
        Long organizationId,
        Long ownerId,
        String ownerName,
        Instant createdAt
) {
    public static ProjectResponse from(Project project) {
        return new ProjectResponse(
                project.getId(),
                project.getName(),
                project.getDescription(),
                project.getOrganization().getId(),
                project.getOwner().getId(),
                project.getOwner().getName(),
                project.getCreatedAt());
    }
}
