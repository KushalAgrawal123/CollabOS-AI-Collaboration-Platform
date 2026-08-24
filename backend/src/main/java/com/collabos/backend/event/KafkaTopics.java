package com.collabos.backend.event;

public final class KafkaTopics {
    public static final String TASK_EVENTS = "task-events";
    public static final String DOCUMENT_EVENTS = "document-events";
    public static final String DOCUMENT_DELETED_EVENTS = "document-deleted-events";

    private KafkaTopics() {
    }
}
