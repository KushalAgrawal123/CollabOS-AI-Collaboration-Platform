package com.collabos.backend.service;

import com.collabos.backend.dto.TaskRequest;
import com.collabos.backend.entity.*;
import com.collabos.backend.event.EventPublisher;
import com.collabos.backend.event.KafkaTopics;
import com.collabos.backend.exception.ApiException;
import com.collabos.backend.repository.TaskRepository;
import com.collabos.backend.repository.UserRepository;
import com.collabos.backend.security.JwtService.AuthenticatedUser;
import com.collabos.backend.websocket.TaskBroadcastService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock private TaskRepository taskRepository;
    @Mock private UserRepository userRepository;
    @Mock private ProjectService projectService;
    @Mock private MembershipService membershipService;
    @Mock private TaskCacheService taskCacheService;
    @Mock private TaskBroadcastService taskBroadcastService;
    @Mock private EventPublisher eventPublisher;

    private TaskService taskService;

    @BeforeEach
    void setUp() {
        taskService = new TaskService(
                taskRepository, userRepository, projectService, membershipService,
                taskCacheService, taskBroadcastService, eventPublisher);
    }

    @Test
    void createThrowsForbiddenForViewerRole() {
        AuthenticatedUser currentUser = new AuthenticatedUser(1L, "viewer@example.com", "Viewer");
        Membership viewerMembership = new Membership();
        viewerMembership.setRole(Role.VIEWER);
        when(membershipService.requireMembership(10L, 1L)).thenReturn(viewerMembership);

        TaskRequest request = new TaskRequest("New task", null, null, null, null);

        assertThatThrownBy(() -> taskService.create(10L, 20L, currentUser, request))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Viewers cannot");

        verifyNoInteractions(taskRepository, eventPublisher);
    }

    @Test
    void createSavesTaskAndPublishesEventForEditor() {
        AuthenticatedUser currentUser = new AuthenticatedUser(1L, "member@example.com", "Member");
        Membership editorMembership = new Membership();
        editorMembership.setRole(Role.MEMBER);
        when(membershipService.requireMembership(10L, 1L)).thenReturn(editorMembership);

        Project project = new Project();
        project.setId(20L);
        project.setName("Test Project");
        when(projectService.getVerified(10L, 20L, 1L)).thenReturn(project);

        User creator = new User();
        creator.setId(1L);
        creator.setName("Member");
        when(userRepository.findById(1L)).thenReturn(Optional.of(creator));

        when(taskRepository.countByProjectIdAndStatus(20L, TaskStatus.TODO)).thenReturn(0);
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TaskRequest request = new TaskRequest("Ship it", "description", TaskPriority.HIGH, null, null);

        Task result = taskService.create(10L, 20L, currentUser, request);

        assertThat(result.getTitle()).isEqualTo("Ship it");
        assertThat(result.getStatus()).isEqualTo(TaskStatus.TODO);
        assertThat(result.getPriority()).isEqualTo(TaskPriority.HIGH);

        verify(taskCacheService).evict(10L, 20L);
        verify(taskBroadcastService).broadcast(10L, 20L, "TASK_CREATED");
        verify(eventPublisher).publish(eq(KafkaTopics.TASK_EVENTS), anyString(), any());
    }
}
