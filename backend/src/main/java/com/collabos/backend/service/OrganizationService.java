package com.collabos.backend.service;

import com.collabos.backend.dto.OrganizationResponse;
import com.collabos.backend.entity.Membership;
import com.collabos.backend.entity.Organization;
import com.collabos.backend.entity.Role;
import com.collabos.backend.entity.User;
import com.collabos.backend.exception.ApiException;
import com.collabos.backend.repository.MembershipRepository;
import com.collabos.backend.repository.OrganizationRepository;
import com.collabos.backend.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.List;
import java.util.Locale;

@Service
public class OrganizationService {

    private static final String SLUG_ALPHABET = "abcdefghijklmnopqrstuvwxyz0123456789";
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Duration ORG_LIST_TTL = Duration.ofMinutes(5);

    private final OrganizationRepository organizationRepository;
    private final MembershipRepository membershipRepository;
    private final UserRepository userRepository;
    private final JsonCache jsonCache;

    public OrganizationService(
            OrganizationRepository organizationRepository,
            MembershipRepository membershipRepository,
            UserRepository userRepository,
            JsonCache jsonCache) {
        this.organizationRepository = organizationRepository;
        this.membershipRepository = membershipRepository;
        this.userRepository = userRepository;
        this.jsonCache = jsonCache;
    }

    public Organization createOrganizationFor(Long userId, String name) {
        User owner = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));
        return createOrganization(owner, name);
    }

    public Organization createOrganization(User owner, String name) {
        Organization organization = new Organization();
        organization.setName(name);
        organization.setSlug(generateUniqueSlug(name));
        organization = organizationRepository.save(organization);

        Membership membership = new Membership();
        membership.setOrganization(organization);
        membership.setUser(owner);
        membership.setRole(Role.OWNER);
        membershipRepository.save(membership);

        evictUserOrganizations(owner.getId());
        return organization;
    }

    // A user's own org list is always requested for their own JWT-verified id
    // (never a client-supplied id), so unlike project/task lists it's safe to
    // cache directly here without a separate uncached authorization gate.
    public List<OrganizationResponse> listForUser(Long userId) {
        String key = orgListKey(userId);
        List<OrganizationResponse> cached = jsonCache.getList(key, OrganizationResponse.class);
        if (cached != null) {
            return cached;
        }

        List<OrganizationResponse> fresh = membershipRepository.findByUserIdOrderByJoinedAtAsc(userId).stream()
                .map(m -> OrganizationResponse.from(m.getOrganization(), m.getRole()))
                .toList();
        jsonCache.putList(key, fresh, ORG_LIST_TTL);
        return fresh;
    }

    public void evictUserOrganizations(Long userId) {
        jsonCache.evict(orgListKey(userId));
    }

    private String orgListKey(Long userId) {
        return "userOrganizations::" + userId;
    }

    public Organization getById(Long organizationId) {
        return organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Organization not found"));
    }

    private String generateUniqueSlug(String name) {
        String base = name.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
        if (base.isBlank()) {
            base = "workspace";
        }
        String slug;
        do {
            slug = base + "-" + randomSuffix();
        } while (organizationRepository.existsBySlug(slug));
        return slug;
    }

    private String randomSuffix() {
        StringBuilder sb = new StringBuilder(6);
        for (int i = 0; i < 6; i++) {
            sb.append(SLUG_ALPHABET.charAt(RANDOM.nextInt(SLUG_ALPHABET.length())));
        }
        return sb.toString();
    }
}
