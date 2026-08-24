package com.collabos.backend.controller;

import com.collabos.backend.dto.ProjectRequest;
import com.collabos.backend.dto.ProjectResponse;
import com.collabos.backend.entity.Project;
import com.collabos.backend.security.JwtService.AuthenticatedUser;
import com.collabos.backend.service.ProjectService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/organizations/{orgId}/projects")
public class ProjectController {

    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @PostMapping
    public ResponseEntity<ProjectResponse> create(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @PathVariable Long orgId,
            @Valid @RequestBody ProjectRequest request) {
        Project project = projectService.create(orgId, currentUser, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ProjectResponse.from(project));
    }

    @GetMapping
    public List<ProjectResponse> list(@AuthenticationPrincipal AuthenticatedUser currentUser, @PathVariable Long orgId) {
        return projectService.listForOrganization(orgId, currentUser.id());
    }

    @GetMapping("/{id}")
    public ProjectResponse get(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @PathVariable Long orgId,
            @PathVariable Long id) {
        return ProjectResponse.from(projectService.getVerified(orgId, id, currentUser.id()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @PathVariable Long orgId,
            @PathVariable Long id) {
        projectService.delete(orgId, currentUser, id);
        return ResponseEntity.noContent().build();
    }
}
