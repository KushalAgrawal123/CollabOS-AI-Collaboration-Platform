package com.collabos.backend.controller;

import com.collabos.backend.dto.MemberResponse;
import com.collabos.backend.dto.MemberRoleUpdateRequest;
import com.collabos.backend.security.JwtService.AuthenticatedUser;
import com.collabos.backend.service.MembershipService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/organizations/{orgId}/members")
public class MembershipController {

    private final MembershipService membershipService;

    public MembershipController(MembershipService membershipService) {
        this.membershipService = membershipService;
    }

    @GetMapping
    public List<MemberResponse> list(@AuthenticationPrincipal AuthenticatedUser currentUser, @PathVariable Long orgId) {
        return membershipService.listMembers(orgId, currentUser.id()).stream().map(MemberResponse::from).toList();
    }

    @PatchMapping("/{userId}/role")
    public MemberResponse updateRole(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @PathVariable Long orgId,
            @PathVariable Long userId,
            @Valid @RequestBody MemberRoleUpdateRequest request) {
        return MemberResponse.from(membershipService.updateRole(orgId, currentUser.id(), userId, request.role()));
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> remove(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @PathVariable Long orgId,
            @PathVariable Long userId) {
        membershipService.removeMember(orgId, currentUser.id(), userId);
        return ResponseEntity.noContent().build();
    }
}
