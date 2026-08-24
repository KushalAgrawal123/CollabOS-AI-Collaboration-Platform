import json
import logging
import threading

from kafka import KafkaConsumer

from app.config import settings
from app.ingest import delete_document_chunks, ingest_document

log = logging.getLogger(__name__)


def _run() -> None:
    # A separate consumer group from the backend's `collabos-backend` group —
    # this is exactly the fan-out Phase 9's design intentionally left room
    # for: both consumers read document-events independently, with zero
    # changes to the producer (DocumentService) or the backend's own consumer.
    consumer = KafkaConsumer(
        settings.kafka_topic_document_events,
        settings.kafka_topic_document_deleted_events,
        bootstrap_servers=settings.kafka_bootstrap_servers,
        group_id=settings.kafka_consumer_group,
        auto_offset_reset="earliest",
        value_deserializer=lambda v: json.loads(v.decode("utf-8")),
    )
    log.info("ai-service Kafka consumer started, group=%s", settings.kafka_consumer_group)

    for message in consumer:
        try:
            payload = message.value
            if message.topic == settings.kafka_topic_document_events:
                ingest_document(
                    document_id=payload["documentId"],
                    organization_id=payload["organizationId"],
                    project_id=payload["projectId"],
                    stored_file_name=payload["storedFileName"],
                    content_type=payload["contentType"],
                )
            elif message.topic == settings.kafka_topic_document_deleted_events:
                delete_document_chunks(payload["documentId"])
        except Exception:
            log.exception("Failed to process message from topic %s", message.topic)


def start_consumer_thread() -> threading.Thread:
    thread = threading.Thread(target=_run, name="kafka-document-events-consumer", daemon=True)
    thread.start()
    return thread
