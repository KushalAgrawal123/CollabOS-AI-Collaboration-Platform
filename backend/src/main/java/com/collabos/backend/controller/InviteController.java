package com.collabos.backend.controller;

import com.collabos.backend.dto.InviteRequest;
import com.collabos.backend.dto.InviteResponse;
import com.collabos.backend.dto.OrganizationResponse;
import com.collabos.backend.entity.Invite;
import com.collabos.backend.entity.Membership;
import com.collabos.backend.security.JwtService.AuthenticatedUser;
import com.collabos.backend.service.InviteService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class InviteController {

    private final InviteService inviteService;

    public InviteController(InviteService inviteService) {
        this.inviteService = inviteService;
    }

    @PostMapping("/api/organizations/{orgId}/invites")
    public ResponseEntity<InviteResponse> create(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @PathVariable Long orgId,
            @Valid @RequestBody InviteRequest request) {
        Invite invite = inviteService.createInvite(orgId, currentUser, request.email(), request.role());
        return ResponseEntity.status(HttpStatus.CREATED).body(InviteResponse.from(invite));
    }

    @GetMapping("/api/organizations/{orgId}/invites")
    public List<InviteResponse> list(@AuthenticationPrincipal AuthenticatedUser currentUser, @PathVariable Long orgId) {
        return inviteService.listPending(orgId, currentUser).stream().map(InviteResponse::from).toList();
    }

    @DeleteMapping("/api/organizations/{orgId}/invites/{inviteId}")
    public ResponseEntity<Void> revoke(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @PathVariable Long orgId,
            @PathVariable Long inviteId) {
        inviteService.revoke(orgId, inviteId, currentUser);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/api/invites/{token}/accept")
    public OrganizationResponse accept(@AuthenticationPrincipal AuthenticatedUser currentUser, @PathVariable String token) {
        Membership membership = inviteService.acceptInvite(token, currentUser);
        return OrganizationResponse.from(membership.getOrganization(), membership.getRole());
    }
}
