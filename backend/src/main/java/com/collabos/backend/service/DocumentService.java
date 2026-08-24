package com.collabos.backend.service;

import com.collabos.backend.dto.DocumentResponse;
import com.collabos.backend.entity.*;
import com.collabos.backend.event.DocumentDeletedEvent;
import com.collabos.backend.event.DocumentUploadedEvent;
import com.collabos.backend.event.EventPublisher;
import com.collabos.backend.event.KafkaTopics;
import com.collabos.backend.exception.ApiException;
import com.collabos.backend.repository.DocumentRepository;
import com.collabos.backend.repository.TaskRepository;
import com.collabos.backend.repository.UserRepository;
import com.collabos.backend.security.JwtService.AuthenticatedUser;
import com.collabos.backend.storage.FileStorageService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class DocumentService {

    private static final Map<String, String> ALLOWED_EXTENSIONS = Map.of(
            ".pdf", "application/pdf",
            ".txt", "text/plain",
            ".md", "text/markdown");
    private static final long MAX_FILE_SIZE_BYTES = 10L * 1024 * 1024;

    private final DocumentRepository documentRepository;
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final ProjectService projectService;
    private final MembershipService membershipService;
    private final FileStorageService fileStorageService;
    private final EventPublisher eventPublisher;

    public DocumentService(
            DocumentRepository documentRepository,
            TaskRepository taskRepository,
            UserRepository userRepository,
            ProjectService projectService,
            MembershipService membershipService,
            FileStorageService fileStorageService,
            EventPublisher eventPublisher) {
        this.documentRepository = documentRepository;
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
        this.projectService = projectService;
        this.membershipService = membershipService;
        this.fileStorageService = fileStorageService;
        this.eventPublisher = eventPublisher;
    }

    public Document upload(Long organizationId, Long projectId, Long taskId, AuthenticatedUser currentUser, MultipartFile file) {
        requireEditor(organizationId, currentUser.id());
        Project project = projectService.getVerified(organizationId, projectId, currentUser.id());

        if (file == null || file.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Choose a file to upload");
        }
        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Files must be 10MB or smaller");
        }

        String originalName = sanitizeFileName(file.getOriginalFilename());
        String extension = extensionOf(originalName).toLowerCase(Locale.ROOT);
        String contentType = ALLOWED_EXTENSIONS.get(extension);
        if (contentType == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Only PDF, text (.txt), and markdown (.md) files are supported");
        }

        Task task = null;
        if (taskId != null) {
            task = taskRepository.findByIdAndProjectId(taskId, projectId)
                    .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Task not found in this project"));
        }

        User uploader = userRepository.findById(currentUser.id())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));

        // Trusts our own extension whitelist for the Content-Type, not whatever the
        // browser sent — an uploaded .md file served back with a browser-supplied
        // "text/html" would be a stored-XSS vector when someone later opens it.
        String storedFileName = fileStorageService.store(file, extension);

        Document document = new Document();
        document.setProject(project);
        document.setTask(task);
        document.setUploadedBy(uploader);
        document.setOriginalFileName(originalName);
        document.setStoredFileName(storedFileName);
        document.setContentType(contentType);
        document.setFileSizeBytes(file.getSize());
        document = documentRepository.save(document);

        eventPublisher.publish(KafkaTopics.DOCUMENT_EVENTS, organizationId.toString(), new DocumentUploadedEvent(
                organizationId, projectId, project.getName(),
                document.getId(), document.getOriginalFileName(),
                document.getStoredFileName(), document.getContentType(),
                uploader.getId(), uploader.getName(),
                Instant.now().toString()));

        return document;
    }

    public List<DocumentResponse> list(Long organizationId, Long projectId, Long taskId, Long currentUserId) {
        projectService.getVerified(organizationId, projectId, currentUserId);
        List<Document> documents = documentRepository.findByProjectIdOrderByCreatedAtDesc(projectId);
        if (taskId != null) {
            documents = documents.stream()
                    .filter(d -> d.getTask() != null && d.getTask().getId().equals(taskId))
                    .toList();
        }
        return documents.stream().map(DocumentResponse::from).toList();
    }

    public Document getVerified(Long organizationId, Long projectId, Long documentId, Long currentUserId) {
        projectService.getVerified(organizationId, projectId, currentUserId);
        return documentRepository.findByIdAndProjectId(documentId, projectId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Document not found"));
    }

    public void delete(Long organizationId, Long projectId, Long documentId, AuthenticatedUser currentUser) {
        Membership membership = requireEditor(organizationId, currentUser.id());
        Document document = getVerified(organizationId, projectId, documentId, currentUser.id());

        boolean isAdmin = membership.getRole() == Role.OWNER || membership.getRole() == Role.ADMIN;
        boolean isUploader = document.getUploadedBy().getId().equals(currentUser.id());
        if (!isAdmin && !isUploader) {
            throw new ApiException(HttpStatus.FORBIDDEN, "You don't have permission to delete this document");
        }

        fileStorageService.delete(document.getStoredFileName());
        documentRepository.delete(document);

        eventPublisher.publish(KafkaTopics.DOCUMENT_DELETED_EVENTS, organizationId.toString(),
                new DocumentDeletedEvent(organizationId, projectId, documentId));
    }

    private String sanitizeFileName(String originalFilename) {
        if (originalFilename == null || originalFilename.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "The uploaded file has no name");
        }
        // Browsers only ever send the base name, but strip any path components
        // defensively in case a non-browser client sends a full path.
        String name = originalFilename.replace('\\', '/');
        return name.substring(name.lastIndexOf('/') + 1);
    }

    private String extensionOf(String fileName) {
        int dot = fileName.lastIndexOf('.');
        return dot < 0 ? "" : fileName.substring(dot);
    }

    private Membership requireEditor(Long organizationId, Long userId) {
        Membership membership = membershipService.requireMembership(organizationId, userId);
        if (membership.getRole() == Role.VIEWER) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Viewers cannot do this");
        }
        return membership;
    }
}
