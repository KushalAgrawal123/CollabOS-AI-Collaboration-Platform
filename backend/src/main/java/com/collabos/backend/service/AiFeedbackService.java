package com.collabos.backend.service;

import com.collabos.backend.dto.AiFeedbackRequest;
import com.collabos.backend.dto.AiFeedbackResponse;
import com.collabos.backend.entity.AiFeedback;
import com.collabos.backend.entity.Project;
import com.collabos.backend.entity.Role;
import com.collabos.backend.entity.User;
import com.collabos.backend.exception.ApiException;
import com.collabos.backend.repository.AiFeedbackRepository;
import com.collabos.backend.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AiFeedbackService {

    private final AiFeedbackRepository aiFeedbackRepository;
    private final UserRepository userRepository;
    private final ProjectService projectService;
    private final MembershipService membershipService;

    public AiFeedbackService(
            AiFeedbackRepository aiFeedbackRepository,
            UserRepository userRepository,
            ProjectService projectService,
            MembershipService membershipService) {
        this.aiFeedbackRepository = aiFeedbackRepository;
        this.userRepository = userRepository;
        this.projectService = projectService;
        this.membershipService = membershipService;
    }

    public AiFeedback submit(Long organizationId, Long projectId, Long userId, AiFeedbackRequest request) {
        Project project = projectService.getVerified(organizationId, projectId, userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));

        AiFeedback feedback = new AiFeedback();
        feedback.setOrganization(project.getOrganization());
        feedback.setProject(project);
        feedback.setUser(user);
        feedback.setAgentType(request.agentType());
        feedback.setQuestion(request.question());
        feedback.setAnswer(request.answer());
        feedback.setRating(request.rating());
        feedback.setCorrection(request.correction());
        return aiFeedbackRepository.save(feedback);
    }

    // Admin-only — this is the "point at real logged examples in an interview" surface.
    public List<AiFeedbackResponse> listForProject(Long organizationId, Long projectId, Long currentUserId) {
        projectService.getVerified(organizationId, projectId, currentUserId);
        membershipService.requireRole(organizationId, currentUserId, Role.OWNER, Role.ADMIN);
        return aiFeedbackRepository.findByProjectIdOrderByCreatedAtDesc(projectId).stream()
                .map(AiFeedbackResponse::from)
                .toList();
    }
}
