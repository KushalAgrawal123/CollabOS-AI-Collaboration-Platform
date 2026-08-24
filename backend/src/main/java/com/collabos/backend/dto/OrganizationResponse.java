package com.collabos.backend.dto;

import com.collabos.backend.entity.Organization;
import com.collabos.backend.entity.Role;

import java.time.Instant;

public record OrganizationResponse(Long id, String name, String slug, Role role, Instant createdAt) {
    public static OrganizationResponse from(Organization organization, Role role) {
        return new OrganizationResponse(
                organization.getId(),
                organization.getName(),
                organization.getSlug(),
                role,
                organization.getCreatedAt());
    }
}
