package com.collabos.backend.controller;

import com.collabos.backend.dto.ReorderRequest;
import com.collabos.backend.dto.TaskRequest;
import com.collabos.backend.dto.TaskResponse;
import com.collabos.backend.entity.Task;
import com.collabos.backend.security.JwtService.AuthenticatedUser;
import com.collabos.backend.service.TaskService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/organizations/{orgId}/projects/{projectId}/tasks")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @PostMapping
    public ResponseEntity<TaskResponse> create(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @PathVariable Long orgId,
            @PathVariable Long projectId,
            @Valid @RequestBody TaskRequest request) {
        Task task = taskService.create(orgId, projectId, currentUser, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(TaskResponse.from(task));
    }

    @GetMapping
    public List<TaskResponse> list(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @PathVariable Long orgId,
            @PathVariable Long projectId) {
        return taskService.list(orgId, projectId, currentUser.id());
    }

    @PatchMapping("/{taskId}")
    public TaskResponse update(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @PathVariable Long orgId,
            @PathVariable Long projectId,
            @PathVariable Long taskId,
            @Valid @RequestBody TaskRequest request) {
        return TaskResponse.from(taskService.update(orgId, projectId, taskId, currentUser, request));
    }

    @PatchMapping("/reorder")
    public ResponseEntity<Void> reorder(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @PathVariable Long orgId,
            @PathVariable Long projectId,
            @Valid @RequestBody ReorderRequest request) {
        taskService.reorder(orgId, projectId, currentUser, request);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{taskId}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @PathVariable Long orgId,
            @PathVariable Long projectId,
            @PathVariable Long taskId) {
        taskService.delete(orgId, projectId, taskId, currentUser);
        return ResponseEntity.noContent().build();
    }
}
