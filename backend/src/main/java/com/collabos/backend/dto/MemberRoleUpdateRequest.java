package com.collabos.backend.dto;

import com.collabos.backend.entity.Role;
import jakarta.validation.constraints.NotNull;

public record MemberRoleUpdateRequest(@NotNull Role role) {
}
