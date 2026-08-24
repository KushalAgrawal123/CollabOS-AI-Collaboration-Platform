package com.collabos.backend.event;

import com.collabos.backend.entity.NotificationType;
import com.collabos.backend.service.NotificationService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Both event types currently feed the same in-app notification fan-out.
 * Phase 10 will add a *second* consumer group on document-events for AI
 * ingestion — a separate class subscribing independently, needing zero
 * changes here or to DocumentService, which is the actual point of putting
 * this behind Kafka instead of just calling notificationService directly
 * from the upload/create code.
 */
@Component
public class NotificationEventConsumer {

    private final NotificationService notificationService;

    public NotificationEventConsumer(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @KafkaListener(topics = KafkaTopics.TASK_EVENTS)
    public void onTaskCreated(TaskCreatedEvent event) {
        String message = "%s created \"%s\" in %s".formatted(event.createdByName(), event.taskTitle(), event.projectName());
        String link = "/organizations/%d/projects/%d".formatted(event.organizationId(), event.projectId());
        notificationService.notifyOrganizationMembersExcept(
                event.organizationId(), event.createdById(), NotificationType.TASK_CREATED, message, link);
    }

    @KafkaListener(topics = KafkaTopics.DOCUMENT_EVENTS)
    public void onDocumentUploaded(DocumentUploadedEvent event) {
        String message = "%s uploaded \"%s\" to %s".formatted(event.uploadedByName(), event.originalFileName(), event.projectName());
        String link = "/organizations/%d/projects/%d/documents".formatted(event.organizationId(), event.projectId());
        notificationService.notifyOrganizationMembersExcept(
                event.organizationId(), event.uploadedById(), NotificationType.DOCUMENT_UPLOADED, message, link);
    }
}
