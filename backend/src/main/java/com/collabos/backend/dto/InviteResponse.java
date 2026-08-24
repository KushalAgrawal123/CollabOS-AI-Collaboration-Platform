package com.collabos.backend.dto;

import com.collabos.backend.entity.Invite;
import com.collabos.backend.entity.InviteStatus;
import com.collabos.backend.entity.Role;

import java.time.Instant;

public record InviteResponse(
        Long id,
        String email,
        Role role,
        String token,
        InviteStatus status,
        Instant expiresAt,
        Instant createdAt
) {
    public static InviteResponse from(Invite invite) {
        return new InviteResponse(
                invite.getId(),
                invite.getEmail(),
                invite.getRole(),
                invite.getToken(),
                invite.getStatus(),
                invite.getExpiresAt(),
                invite.getCreatedAt());
    }
}
