package com.collabos.backend.service;

import com.collabos.backend.dto.ReorderRequest;
import com.collabos.backend.dto.TaskRequest;
import com.collabos.backend.dto.TaskResponse;
import com.collabos.backend.entity.*;
import com.collabos.backend.event.EventPublisher;
import com.collabos.backend.event.KafkaTopics;
import com.collabos.backend.event.TaskCreatedEvent;
import com.collabos.backend.exception.ApiException;
import com.collabos.backend.repository.TaskRepository;
import com.collabos.backend.repository.UserRepository;
import com.collabos.backend.security.JwtService.AuthenticatedUser;
import com.collabos.backend.websocket.TaskBroadcastService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class TaskService {

    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final ProjectService projectService;
    private final MembershipService membershipService;
    private final TaskCacheService taskCacheService;
    private final TaskBroadcastService taskBroadcastService;
    private final EventPublisher eventPublisher;

    public TaskService(
            TaskRepository taskRepository,
            UserRepository userRepository,
            ProjectService projectService,
            MembershipService membershipService,
            TaskCacheService taskCacheService,
            TaskBroadcastService taskBroadcastService,
            EventPublisher eventPublisher) {
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
        this.projectService = projectService;
        this.membershipService = membershipService;
        this.taskCacheService = taskCacheService;
        this.taskBroadcastService = taskBroadcastService;
        this.eventPublisher = eventPublisher;
    }

    public Task create(Long organizationId, Long projectId, AuthenticatedUser currentUser, TaskRequest request) {
        requireEditor(organizationId, currentUser.id());
        Project project = projectService.getVerified(organizationId, projectId, currentUser.id());

        User creator = userRepository.findById(currentUser.id())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));

        Task task = new Task();
        task.setProject(project);
        task.setTitle(request.title());
        task.setDescription(request.description());
        task.setPriority(request.priority() != null ? request.priority() : TaskPriority.MEDIUM);
        task.setDueDate(request.dueDate());
        task.setCreatedBy(creator);
        task.setAssignee(resolveAssignee(organizationId, request.assigneeId()));
        task.setStatus(TaskStatus.TODO);
        task.setPosition(taskRepository.countByProjectIdAndStatus(projectId, TaskStatus.TODO));

        task = taskRepository.save(task);
        taskCacheService.evict(organizationId, projectId);
        taskBroadcastService.broadcast(organizationId, projectId, "TASK_CREATED");

        eventPublisher.publish(KafkaTopics.TASK_EVENTS, organizationId.toString(), new TaskCreatedEvent(
                organizationId, projectId, project.getName(),
                task.getId(), task.getTitle(),
                creator.getId(), creator.getName(),
                Instant.now().toString()));

        return task;
    }

    public List<TaskResponse> list(Long organizationId, Long projectId, Long currentUserId) {
        // getVerified() already checks membership internally — no need to check it twice.
        projectService.getVerified(organizationId, projectId, currentUserId);
        return taskCacheService.list(organizationId, projectId);
    }

    public Task update(Long organizationId, Long projectId, Long taskId, AuthenticatedUser currentUser, TaskRequest request) {
        requireEditor(organizationId, currentUser.id());
        Task task = getVerified(organizationId, projectId, taskId, currentUser.id());

        task.setTitle(request.title());
        task.setDescription(request.description());
        if (request.priority() != null) {
            task.setPriority(request.priority());
        }
        task.setDueDate(request.dueDate());
        task.setAssignee(resolveAssignee(organizationId, request.assigneeId()));

        task = taskRepository.save(task);
        taskCacheService.evict(organizationId, projectId);
        taskBroadcastService.broadcast(organizationId, projectId, "TASK_UPDATED");
        return task;
    }

    public void reorder(Long organizationId, Long projectId, AuthenticatedUser currentUser, ReorderRequest request) {
        requireEditor(organizationId, currentUser.id());
        projectService.getVerified(organizationId, projectId, currentUser.id());

        int position = 0;
        for (Long taskId : request.taskIds()) {
            Task task = taskRepository.findByIdAndProjectId(taskId, projectId)
                    .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Task not found in this project"));
            task.setStatus(request.status());
            task.setPosition(position++);
            taskRepository.save(task);
        }
        taskCacheService.evict(organizationId, projectId);
        taskBroadcastService.broadcast(organizationId, projectId, "TASK_REORDERED");
    }

    public void delete(Long organizationId, Long projectId, Long taskId, AuthenticatedUser currentUser) {
        Membership membership = requireEditor(organizationId, currentUser.id());
        Task task = getVerified(organizationId, projectId, taskId, currentUser.id());

        boolean isAdmin = membership.getRole() == Role.OWNER || membership.getRole() == Role.ADMIN;
        boolean isCreator = task.getCreatedBy().getId().equals(currentUser.id());

        if (!isAdmin && !isCreator) {
            throw new ApiException(HttpStatus.FORBIDDEN, "You don't have permission to delete this task");
        }

        taskRepository.delete(task);
        taskCacheService.evict(organizationId, projectId);
        taskBroadcastService.broadcast(organizationId, projectId, "TASK_DELETED");
    }

    private Task getVerified(Long organizationId, Long projectId, Long taskId, Long currentUserId) {
        projectService.getVerified(organizationId, projectId, currentUserId);
        return taskRepository.findByIdAndProjectId(taskId, projectId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Task not found"));
    }

    private Membership requireEditor(Long organizationId, Long userId) {
        Membership membership = membershipService.requireMembership(organizationId, userId);
        if (membership.getRole() == Role.VIEWER) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Viewers cannot make changes");
        }
        return membership;
    }

    private User resolveAssignee(Long organizationId, Long assigneeId) {
        if (assigneeId == null) {
            return null;
        }
        membershipService.requireMembership(organizationId, assigneeId);
        return userRepository.findById(assigneeId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Assignee not found"));
    }
}
