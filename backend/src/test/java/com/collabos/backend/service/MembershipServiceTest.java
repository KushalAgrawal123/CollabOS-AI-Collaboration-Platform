package com.collabos.backend.service;

import com.collabos.backend.entity.Membership;
import com.collabos.backend.entity.Role;
import com.collabos.backend.exception.ApiException;
import com.collabos.backend.repository.MembershipRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MembershipServiceTest {

    @Mock
    private MembershipRepository membershipRepository;

    @Mock
    private OrganizationService organizationService;

    private MembershipService membershipService;

    @BeforeEach
    void setUp() {
        membershipService = new MembershipService(membershipRepository, organizationService);
    }

    @Test
    void requireMembershipThrowsForbiddenWhenNotAMember() {
        when(membershipRepository.findByOrganizationIdAndUserId(1L, 99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> membershipService.requireMembership(1L, 99L))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("not a member");
    }

    @Test
    void requireRoleThrowsForbiddenWhenRoleNotAllowed() {
        Membership membership = new Membership();
        membership.setRole(Role.VIEWER);
        when(membershipRepository.findByOrganizationIdAndUserId(1L, 99L)).thenReturn(Optional.of(membership));

        assertThatThrownBy(() -> membershipService.requireRole(1L, 99L, Role.OWNER, Role.ADMIN))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("permission");
    }

    @Test
    void requireRolePassesWhenRoleIsAllowed() {
        Membership membership = new Membership();
        membership.setRole(Role.ADMIN);
        when(membershipRepository.findByOrganizationIdAndUserId(1L, 99L)).thenReturn(Optional.of(membership));

        Membership result = membershipService.requireRole(1L, 99L, Role.OWNER, Role.ADMIN);

        assertThat(result.getRole()).isEqualTo(Role.ADMIN);
    }
}
