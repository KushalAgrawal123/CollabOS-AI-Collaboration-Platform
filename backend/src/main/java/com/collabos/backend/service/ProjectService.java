package com.collabos.backend.service;

import com.collabos.backend.dto.ProjectRequest;
import com.collabos.backend.dto.ProjectResponse;
import com.collabos.backend.entity.Membership;
import com.collabos.backend.entity.Project;
import com.collabos.backend.entity.Role;
import com.collabos.backend.entity.User;
import com.collabos.backend.exception.ApiException;
import com.collabos.backend.repository.ProjectRepository;
import com.collabos.backend.repository.UserRepository;
import com.collabos.backend.security.JwtService.AuthenticatedUser;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final MembershipService membershipService;
    private final OrganizationService organizationService;
    private final ProjectCacheService projectCacheService;

    public ProjectService(
            ProjectRepository projectRepository,
            UserRepository userRepository,
            MembershipService membershipService,
            OrganizationService organizationService,
            ProjectCacheService projectCacheService) {
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
        this.membershipService = membershipService;
        this.organizationService = organizationService;
        this.projectCacheService = projectCacheService;
    }

    public Project create(Long organizationId, AuthenticatedUser currentUser, ProjectRequest request) {
        Membership membership = membershipService.requireMembership(organizationId, currentUser.id());
        if (membership.getRole() == Role.VIEWER) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Viewers cannot create projects");
        }

        User owner = userRepository.findById(currentUser.id())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));

        Project project = new Project();
        project.setName(request.name());
        project.setDescription(request.description());
        project.setOrganization(organizationService.getById(organizationId));
        project.setOwner(owner);
        project = projectRepository.save(project);

        projectCacheService.evict(organizationId);
        return project;
    }

    public List<ProjectResponse> listForOrganization(Long organizationId, Long currentUserId) {
        membershipService.requireMembership(organizationId, currentUserId);
        return projectCacheService.list(organizationId);
    }

    public Project getVerified(Long organizationId, Long projectId, Long currentUserId) {
        membershipService.requireMembership(organizationId, currentUserId);
        return projectRepository.findByIdAndOrganizationId(projectId, organizationId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Project not found"));
    }

    public void delete(Long organizationId, AuthenticatedUser currentUser, Long projectId) {
        Membership membership = membershipService.requireMembership(organizationId, currentUser.id());

        Project project = projectRepository.findById(projectId)
                .filter(p -> p.getOrganization().getId().equals(organizationId))
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Project not found"));

        boolean isAdmin = membership.getRole() == Role.OWNER || membership.getRole() == Role.ADMIN;
        boolean isOwnMemberProject = membership.getRole() == Role.MEMBER
                && project.getOwner().getId().equals(currentUser.id());

        if (!isAdmin && !isOwnMemberProject) {
            throw new ApiException(HttpStatus.FORBIDDEN, "You don't have permission to delete this project");
        }

        projectRepository.delete(project);
        projectCacheService.evict(organizationId);
    }
}
