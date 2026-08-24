package com.collabos.backend.controller;

import com.collabos.backend.dto.CommentRequest;
import com.collabos.backend.dto.CommentResponse;
import com.collabos.backend.security.JwtService.AuthenticatedUser;
import com.collabos.backend.service.TaskCommentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/organizations/{orgId}/projects/{projectId}/tasks/{taskId}/comments")
public class TaskCommentController {

    private final TaskCommentService commentService;

    public TaskCommentController(TaskCommentService commentService) {
        this.commentService = commentService;
    }

    @GetMapping
    public List<CommentResponse> list(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @PathVariable Long orgId,
            @PathVariable Long projectId,
            @PathVariable Long taskId) {
        return commentService.list(orgId, projectId, taskId, currentUser.id()).stream().map(CommentResponse::from).toList();
    }

    @PostMapping
    public ResponseEntity<CommentResponse> create(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @PathVariable Long orgId,
            @PathVariable Long projectId,
            @PathVariable Long taskId,
            @Valid @RequestBody CommentRequest request) {
        var comment = commentService.create(orgId, projectId, taskId, currentUser, request.body());
        return ResponseEntity.status(HttpStatus.CREATED).body(CommentResponse.from(comment));
    }
}
