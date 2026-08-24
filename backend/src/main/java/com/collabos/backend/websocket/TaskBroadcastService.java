package com.collabos.backend.websocket;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
public class TaskBroadcastService {

    private final SimpMessagingTemplate messagingTemplate;

    public TaskBroadcastService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void broadcast(Long organizationId, Long projectId, String eventType) {
        String destination = "/topic/organizations/%d/projects/%d/tasks".formatted(organizationId, projectId);
        messagingTemplate.convertAndSend(destination, TaskBoardEvent.of(eventType));
    }
}
