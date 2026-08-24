package com.collabos.backend.controller;

import com.collabos.backend.dto.DocumentResponse;
import com.collabos.backend.entity.Document;
import com.collabos.backend.security.JwtService.AuthenticatedUser;
import com.collabos.backend.service.DocumentService;
import com.collabos.backend.storage.FileStorageService;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/organizations/{orgId}/projects/{projectId}/documents")
public class DocumentController {

    private final DocumentService documentService;
    private final FileStorageService fileStorageService;

    public DocumentController(DocumentService documentService, FileStorageService fileStorageService) {
        this.documentService = documentService;
        this.fileStorageService = fileStorageService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DocumentResponse> upload(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @PathVariable Long orgId,
            @PathVariable Long projectId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "taskId", required = false) Long taskId) {
        Document document = documentService.upload(orgId, projectId, taskId, currentUser, file);
        return ResponseEntity.status(HttpStatus.CREATED).body(DocumentResponse.from(document));
    }

    @GetMapping
    public List<DocumentResponse> list(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @PathVariable Long orgId,
            @PathVariable Long projectId,
            @RequestParam(required = false) Long taskId) {
        return documentService.list(orgId, projectId, taskId, currentUser.id());
    }

    @GetMapping("/{documentId}/download")
    public ResponseEntity<Resource> download(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @PathVariable Long orgId,
            @PathVariable Long projectId,
            @PathVariable Long documentId) {
        Document document = documentService.getVerified(orgId, projectId, documentId, currentUser.id());
        Resource resource = fileStorageService.load(document.getStoredFileName());

        String encodedName = URLEncoder.encode(document.getOriginalFileName(), StandardCharsets.UTF_8).replace("+", "%20");
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(document.getContentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodedName)
                .body(resource);
    }

    @DeleteMapping("/{documentId}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @PathVariable Long orgId,
            @PathVariable Long projectId,
            @PathVariable Long documentId) {
        documentService.delete(orgId, projectId, documentId, currentUser);
        return ResponseEntity.noContent().build();
    }
}
