package com.collabos.backend.service;

import com.collabos.backend.entity.*;
import com.collabos.backend.exception.ApiException;
import com.collabos.backend.repository.InviteRepository;
import com.collabos.backend.repository.MembershipRepository;
import com.collabos.backend.repository.UserRepository;
import com.collabos.backend.security.JwtService.AuthenticatedUser;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
public class InviteService {

    private final InviteRepository inviteRepository;
    private final MembershipRepository membershipRepository;
    private final UserRepository userRepository;
    private final MembershipService membershipService;
    private final OrganizationService organizationService;

    public InviteService(
            InviteRepository inviteRepository,
            MembershipRepository membershipRepository,
            UserRepository userRepository,
            MembershipService membershipService,
            OrganizationService organizationService) {
        this.inviteRepository = inviteRepository;
        this.membershipRepository = membershipRepository;
        this.userRepository = userRepository;
        this.membershipService = membershipService;
        this.organizationService = organizationService;
    }

    public Invite createInvite(Long organizationId, AuthenticatedUser actingUser, String email, Role role) {
        membershipService.requireRole(organizationId, actingUser.id(), Role.OWNER, Role.ADMIN);

        User inviter = userRepository.findById(actingUser.id())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));

        Invite invite = new Invite();
        invite.setOrganization(organizationService.getById(organizationId));
        invite.setEmail(email);
        invite.setRole(role);
        invite.setToken(UUID.randomUUID().toString());
        invite.setInvitedBy(inviter);
        invite.setExpiresAt(Instant.now().plus(7, ChronoUnit.DAYS));
        return inviteRepository.save(invite);
    }

    public List<Invite> listPending(Long organizationId, AuthenticatedUser actingUser) {
        membershipService.requireRole(organizationId, actingUser.id(), Role.OWNER, Role.ADMIN);
        return inviteRepository.findByOrganizationIdAndStatusOrderByCreatedAtDesc(organizationId, InviteStatus.PENDING);
    }

    public void revoke(Long organizationId, Long inviteId, AuthenticatedUser actingUser) {
        membershipService.requireRole(organizationId, actingUser.id(), Role.OWNER, Role.ADMIN);
        Invite invite = inviteRepository.findById(inviteId)
                .filter(i -> i.getOrganization().getId().equals(organizationId))
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Invite not found"));
        invite.setStatus(InviteStatus.REVOKED);
        inviteRepository.save(invite);
    }

    public Membership acceptInvite(String token, AuthenticatedUser currentUser) {
        Invite invite = inviteRepository.findByToken(token)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Invite not found"));

        if (invite.getStatus() != InviteStatus.PENDING) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "This invite is no longer valid");
        }
        if (invite.getExpiresAt().isBefore(Instant.now())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "This invite has expired");
        }
        if (!invite.getEmail().equalsIgnoreCase(currentUser.email())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "This invite was sent to a different email address");
        }

        Organization organization = invite.getOrganization();
        Membership membership = joinOrganization(organization, invite.getRole(), currentUser.id());

        invite.setStatus(InviteStatus.ACCEPTED);
        inviteRepository.save(invite);

        organizationService.evictUserOrganizations(currentUser.id());
        return membership;
    }

    /**
     * Two accept requests for the same invite can race (double-click, two tabs, or a
     * client retry) and both pass the "not yet a member" check before either commits.
     * The unique (organization_id, user_id) constraint is the real guard; if we lose
     * the race we just look up the membership the winner created instead of failing.
     */
    private Membership joinOrganization(Organization organization, Role role, Long userId) {
        return membershipRepository.findByOrganizationIdAndUserId(organization.getId(), userId)
                .orElseGet(() -> {
                    try {
                        User user = userRepository.findById(userId)
                                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));
                        Membership created = new Membership();
                        created.setOrganization(organization);
                        created.setUser(user);
                        created.setRole(role);
                        return membershipRepository.save(created);
                    } catch (DataIntegrityViolationException raceLost) {
                        return membershipRepository.findByOrganizationIdAndUserId(organization.getId(), userId)
                                .orElseThrow(() -> raceLost);
                    }
                });
    }
}
