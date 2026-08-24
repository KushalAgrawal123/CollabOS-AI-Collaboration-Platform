package com.collabos.backend.event;

public record DocumentDeletedEvent(
        Long organizationId,
        Long projectId,
        Long documentId
) {
}
