package com.collabos.backend.dto;

import com.collabos.backend.entity.Membership;
import com.collabos.backend.entity.Role;

import java.time.Instant;

public record MemberResponse(Long userId, String name, String email, Role role, Instant joinedAt) {
    public static MemberResponse from(Membership membership) {
        return new MemberResponse(
                membership.getUser().getId(),
                membership.getUser().getName(),
                membership.getUser().getEmail(),
                membership.getRole(),
                membership.getJoinedAt());
    }
}
