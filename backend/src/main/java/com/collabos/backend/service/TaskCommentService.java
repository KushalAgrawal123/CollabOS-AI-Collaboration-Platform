package com.collabos.backend.service;

import com.collabos.backend.entity.Role;
import com.collabos.backend.entity.Task;
import com.collabos.backend.entity.TaskComment;
import com.collabos.backend.entity.User;
import com.collabos.backend.exception.ApiException;
import com.collabos.backend.repository.TaskCommentRepository;
import com.collabos.backend.repository.TaskRepository;
import com.collabos.backend.repository.UserRepository;
import com.collabos.backend.security.JwtService.AuthenticatedUser;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TaskCommentService {

    private final TaskCommentRepository commentRepository;
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final ProjectService projectService;
    private final MembershipService membershipService;

    public TaskCommentService(
            TaskCommentRepository commentRepository,
            TaskRepository taskRepository,
            UserRepository userRepository,
            ProjectService projectService,
            MembershipService membershipService) {
        this.commentRepository = commentRepository;
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
        this.projectService = projectService;
        this.membershipService = membershipService;
    }

    public List<TaskComment> list(Long organizationId, Long projectId, Long taskId, Long currentUserId) {
        Task task = getVerifiedTask(organizationId, projectId, taskId, currentUserId);
        return commentRepository.findByTaskIdOrderByCreatedAtAsc(task.getId());
    }

    public TaskComment create(Long organizationId, Long projectId, Long taskId, AuthenticatedUser currentUser, String body) {
        if (membershipService.requireMembership(organizationId, currentUser.id()).getRole() == Role.VIEWER) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Viewers cannot comment");
        }
        Task task = getVerifiedTask(organizationId, projectId, taskId, currentUser.id());
        User author = userRepository.findById(currentUser.id())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));

        TaskComment comment = new TaskComment();
        comment.setTask(task);
        comment.setAuthor(author);
        comment.setBody(body);
        return commentRepository.save(comment);
    }

    private Task getVerifiedTask(Long organizationId, Long projectId, Long taskId, Long currentUserId) {
        // getVerified() already checks membership internally — no need to check it twice.
        projectService.getVerified(organizationId, projectId, currentUserId);
        return taskRepository.findByIdAndProjectId(taskId, projectId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Task not found"));
    }
}
