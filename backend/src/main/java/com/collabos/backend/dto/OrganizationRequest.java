package com.collabos.backend.dto;

import jakarta.validation.constraints.NotBlank;

public record OrganizationRequest(@NotBlank String name) {
}
