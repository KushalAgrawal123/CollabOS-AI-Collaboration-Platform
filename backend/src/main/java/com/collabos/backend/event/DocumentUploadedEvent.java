package com.collabos.backend.event;

public record DocumentUploadedEvent(
        Long organizationId,
        Long projectId,
        String projectName,
        Long documentId,
        String originalFileName,
        // storedFileName + contentType are here for Phase 10's ai-service consumer, which
        // needs to locate the file on disk and know how to parse it — carrying them on the
        // event avoids ai-service needing to know the backend's `documents` table schema.
        String storedFileName,
        String contentType,
        Long uploadedById,
        String uploadedByName,
        String at
) {
}
