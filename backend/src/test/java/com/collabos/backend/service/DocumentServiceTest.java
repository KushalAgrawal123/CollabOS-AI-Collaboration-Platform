package com.collabos.backend.service;

import com.collabos.backend.entity.Membership;
import com.collabos.backend.entity.Role;
import com.collabos.backend.exception.ApiException;
import com.collabos.backend.repository.DocumentRepository;
import com.collabos.backend.repository.TaskRepository;
import com.collabos.backend.repository.UserRepository;
import com.collabos.backend.security.JwtService.AuthenticatedUser;
import com.collabos.backend.storage.FileStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentServiceTest {

    @Mock private DocumentRepository documentRepository;
    @Mock private TaskRepository taskRepository;
    @Mock private UserRepository userRepository;
    @Mock private ProjectService projectService;
    @Mock private MembershipService membershipService;
    @Mock private FileStorageService fileStorageService;
    @Mock private com.collabos.backend.event.EventPublisher eventPublisher;

    private DocumentService documentService;

    @BeforeEach
    void setUp() {
        documentService = new DocumentService(
                documentRepository, taskRepository, userRepository, projectService,
                membershipService, fileStorageService, eventPublisher);
    }

    private Membership editorMembership() {
        Membership membership = new Membership();
        membership.setRole(Role.MEMBER);
        return membership;
    }

    @Test
    void uploadRejectsDisallowedFileExtension() {
        AuthenticatedUser currentUser = new AuthenticatedUser(1L, "member@example.com", "Member");
        lenient().when(membershipService.requireMembership(10L, 1L)).thenReturn(editorMembership());
        lenient().when(projectService.getVerified(10L, 20L, 1L)).thenReturn(new com.collabos.backend.entity.Project());

        MockMultipartFile executable = new MockMultipartFile(
                "file", "malware.exe", "application/octet-stream", "not really an exe".getBytes());

        assertThatThrownBy(() -> documentService.upload(10L, 20L, null, currentUser, executable))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("PDF, text");
    }

    @Test
    void uploadRejectsEmptyFile() {
        AuthenticatedUser currentUser = new AuthenticatedUser(1L, "member@example.com", "Member");
        lenient().when(membershipService.requireMembership(10L, 1L)).thenReturn(editorMembership());
        lenient().when(projectService.getVerified(10L, 20L, 1L)).thenReturn(new com.collabos.backend.entity.Project());

        MockMultipartFile empty = new MockMultipartFile("file", "notes.txt", "text/plain", new byte[0]);

        assertThatThrownBy(() -> documentService.upload(10L, 20L, null, currentUser, empty))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Choose a file");
    }

    @Test
    void uploadRejectsViewerRole() {
        AuthenticatedUser currentUser = new AuthenticatedUser(1L, "viewer@example.com", "Viewer");
        Membership viewerMembership = new Membership();
        viewerMembership.setRole(Role.VIEWER);
        when(membershipService.requireMembership(10L, 1L)).thenReturn(viewerMembership);

        MockMultipartFile file = new MockMultipartFile("file", "notes.txt", "text/plain", "hello".getBytes());

        assertThatThrownBy(() -> documentService.upload(10L, 20L, null, currentUser, file))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Viewers cannot");
    }
}
