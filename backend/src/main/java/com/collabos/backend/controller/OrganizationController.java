package com.collabos.backend.controller;

import com.collabos.backend.dto.OrganizationRequest;
import com.collabos.backend.dto.OrganizationResponse;
import com.collabos.backend.entity.Organization;
import com.collabos.backend.entity.Role;
import com.collabos.backend.security.JwtService.AuthenticatedUser;
import com.collabos.backend.service.MembershipService;
import com.collabos.backend.service.OrganizationService;
import com.collabos.backend.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/organizations")
public class OrganizationController {

    private final OrganizationService organizationService;
    private final MembershipService membershipService;

    public OrganizationController(OrganizationService organizationService, MembershipService membershipService) {
        this.organizationService = organizationService;
        this.membershipService = membershipService;
    }

    @GetMapping
    public List<OrganizationResponse> list(@AuthenticationPrincipal AuthenticatedUser currentUser) {
        return organizationService.listForUser(currentUser.id());
    }

    @PostMapping
    public ResponseEntity<OrganizationResponse> create(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @Valid @RequestBody OrganizationRequest request) {
        // organizationService.createOrganization expects a User entity; membershipService
        // already resolves membership for the acting user elsewhere, but creation needs
        // the full entity, so we look it up the same way InviteService does.
        Organization organization = organizationService.createOrganizationFor(currentUser.id(), request.name());
        return ResponseEntity.status(HttpStatus.CREATED).body(OrganizationResponse.from(organization, Role.OWNER));
    }

    @GetMapping("/{id}")
    public OrganizationResponse get(@AuthenticationPrincipal AuthenticatedUser currentUser, @PathVariable Long id) {
        var membership = membershipService.requireMembership(id, currentUser.id());
        return OrganizationResponse.from(membership.getOrganization(), membership.getRole());
    }
}
