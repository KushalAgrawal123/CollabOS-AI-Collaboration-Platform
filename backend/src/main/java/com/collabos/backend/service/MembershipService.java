package com.collabos.backend.service;

import com.collabos.backend.entity.Membership;
import com.collabos.backend.entity.Role;
import com.collabos.backend.exception.ApiException;
import com.collabos.backend.repository.MembershipRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.EnumSet;
import java.util.List;

@Service
public class MembershipService {

    private final MembershipRepository membershipRepository;
    private final OrganizationService organizationService;

    public MembershipService(MembershipRepository membershipRepository, OrganizationService organizationService) {
        this.membershipRepository = membershipRepository;
        this.organizationService = organizationService;
    }

    public Membership requireMembership(Long organizationId, Long userId) {
        return membershipRepository.findByOrganizationIdAndUserId(organizationId, userId)
                .orElseThrow(() -> new ApiException(HttpStatus.FORBIDDEN, "You are not a member of this organization"));
    }

    public Membership requireRole(Long organizationId, Long userId, Role... allowedRoles) {
        Membership membership = requireMembership(organizationId, userId);
        if (!EnumSet.copyOf(List.of(allowedRoles)).contains(membership.getRole())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "You don't have permission to perform this action");
        }
        return membership;
    }

    public List<Membership> listMembers(Long organizationId, Long requestingUserId) {
        requireMembership(organizationId, requestingUserId);
        return membershipRepository.findByOrganizationIdOrderByJoinedAtAsc(organizationId);
    }

    public Membership updateRole(Long organizationId, Long actingUserId, Long targetUserId, Role newRole) {
        requireRole(organizationId, actingUserId, Role.OWNER);
        Membership target = membershipRepository.findByOrganizationIdAndUserId(organizationId, targetUserId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "That user is not a member of this organization"));

        if (target.getRole() == Role.OWNER && newRole != Role.OWNER
                && membershipRepository.countByOrganizationIdAndRole(organizationId, Role.OWNER) <= 1) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "An organization must always have at least one owner");
        }

        target.setRole(newRole);
        target = membershipRepository.save(target);
        // The cached org list embeds each org's role for this user, so it goes stale on a role change.
        organizationService.evictUserOrganizations(targetUserId);
        return target;
    }

    public void removeMember(Long organizationId, Long actingUserId, Long targetUserId) {
        requireRole(organizationId, actingUserId, Role.OWNER);
        Membership target = membershipRepository.findByOrganizationIdAndUserId(organizationId, targetUserId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "That user is not a member of this organization"));

        if (target.getRole() == Role.OWNER
                && membershipRepository.countByOrganizationIdAndRole(organizationId, Role.OWNER) <= 1) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "An organization must always have at least one owner");
        }

        membershipRepository.delete(target);
        organizationService.evictUserOrganizations(targetUserId);
    }
}
