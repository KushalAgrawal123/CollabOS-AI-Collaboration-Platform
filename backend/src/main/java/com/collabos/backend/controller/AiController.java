package com.collabos.backend.controller;

import com.collabos.backend.dto.AiAskRequest;
import com.collabos.backend.dto.AiAskResponse;
import com.collabos.backend.dto.AiFeedbackRequest;
import com.collabos.backend.dto.AiFeedbackResponse;
import com.collabos.backend.dto.AiProjectManagerResponse;
import com.collabos.backend.dto.AiStatusResponse;
import com.collabos.backend.dto.AiSummaryResponse;
import com.collabos.backend.security.JwtService.AuthenticatedUser;
import com.collabos.backend.service.AiFeedbackService;
import com.collabos.backend.service.AiService;
import com.collabos.backend.service.DocumentService;
import com.collabos.backend.service.MembershipService;
import com.collabos.backend.service.ProjectService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/organizations/{orgId}")
public class AiController {

    private final AiService aiService;
    private final AiFeedbackService aiFeedbackService;
    private final ProjectService projectService;
    private final DocumentService documentService;
    private final MembershipService membershipService;

    public AiController(
            AiService aiService,
            AiFeedbackService aiFeedbackService,
            ProjectService projectService,
            DocumentService documentService,
            MembershipService membershipService) {
        this.aiService = aiService;
        this.aiFeedbackService = aiFeedbackService;
        this.projectService = projectService;
        this.documentService = documentService;
        this.membershipService = membershipService;
    }

    @GetMapping("/ai/status")
    public AiStatusResponse status(@AuthenticationPrincipal AuthenticatedUser currentUser, @PathVariable Long orgId) {
        membershipService.requireMembership(orgId, currentUser.id());
        return aiService.status();
    }

    // Asking is read-only, so any org member (including VIEWER) can use it —
    // same access level as viewing tasks or documents.
    @PostMapping("/projects/{projectId}/ai/ask")
    public AiAskResponse ask(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @PathVariable Long orgId,
            @PathVariable Long projectId,
            @Valid @RequestBody AiAskRequest request) {
        projectService.getVerified(orgId, projectId, currentUser.id());
        return aiService.ask(projectId, request.question());
    }

    // Read-only, same access level as viewing the board — any member including VIEWER.
    @PostMapping("/projects/{projectId}/ai/project-manager")
    public AiProjectManagerResponse projectManagerReport(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @PathVariable Long orgId,
            @PathVariable Long projectId) {
        projectService.getVerified(orgId, projectId, currentUser.id());
        return aiService.projectManagerReport(projectId);
    }

    @PostMapping("/projects/{projectId}/documents/{documentId}/ai/summarize")
    public AiSummaryResponse summarize(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @PathVariable Long orgId,
            @PathVariable Long projectId,
            @PathVariable Long documentId) {
        documentService.getVerified(orgId, projectId, documentId, currentUser.id());
        return aiService.summarize(documentId);
    }

    @PostMapping("/projects/{projectId}/ai/feedback")
    public ResponseEntity<Void> submitFeedback(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @PathVariable Long orgId,
            @PathVariable Long projectId,
            @Valid @RequestBody AiFeedbackRequest request) {
        aiFeedbackService.submit(orgId, projectId, currentUser.id(), request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping("/projects/{projectId}/ai/feedback")
    public List<AiFeedbackResponse> listFeedback(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @PathVariable Long orgId,
            @PathVariable Long projectId) {
        return aiFeedbackService.listForProject(orgId, projectId, currentUser.id());
    }
}
