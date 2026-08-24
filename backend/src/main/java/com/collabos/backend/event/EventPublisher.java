package com.collabos.backend.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

/**
 * Thin wrapper so callers (TaskService, DocumentService) don't touch
 * KafkaTemplate directly. Publishing is fire-and-forget on purpose — the
 * request that created the task or document shouldn't block on, or fail
 * because of, whatever happens downstream in a consumer. If Kafka is briefly
 * unavailable, the log line below is the only symptom; the task/document
 * itself is already safely committed to Postgres by the time this runs.
 */
@Service
public class EventPublisher {

    private static final Logger log = LoggerFactory.getLogger(EventPublisher.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public EventPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publish(String topic, String key, Object event) {
        // send() can throw synchronously (e.g. serialization failures happen
        // before any future is returned), so a try/catch around the call is
        // required in addition to the async whenComplete() below — otherwise
        // a bad payload would propagate out of the caller's request instead
        // of just being logged, defeating the point of fire-and-forget.
        try {
            kafkaTemplate.send(topic, key, event).whenComplete((result, ex) -> {
                if (ex != null) {
                    log.warn("Failed to publish event to topic {}: {}", topic, ex.getMessage());
                }
            });
        } catch (Exception ex) {
            log.warn("Failed to publish event to topic {}: {}", topic, ex.getMessage());
        }
    }
}
