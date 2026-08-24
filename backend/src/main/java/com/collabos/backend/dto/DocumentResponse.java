package com.collabos.backend.dto;

import com.collabos.backend.entity.Document;

import java.time.Instant;

public record DocumentResponse(
        Long id,
        Long projectId,
        Long taskId,
        String taskTitle,
        Long uploadedById,
        String uploadedByName,
        String originalFileName,
        String contentType,
        long fileSizeBytes,
        Instant createdAt
) {
    public static DocumentResponse from(Document document) {
        return new DocumentResponse(
                document.getId(),
                document.getProject().getId(),
                document.getTask() != null ? document.getTask().getId() : null,
                document.getTask() != null ? document.getTask().getTitle() : null,
                document.getUploadedBy().getId(),
                document.getUploadedBy().getName(),
                document.getOriginalFileName(),
                document.getContentType(),
                document.getFileSizeBytes(),
                document.getCreatedAt());
    }
}
