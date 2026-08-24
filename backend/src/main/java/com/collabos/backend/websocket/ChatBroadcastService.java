package com.collabos.backend.websocket;

import com.collabos.backend.dto.ChatMessageResponse;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

/**
 * Unlike TaskBroadcastService (which sends a bare "something changed, go
 * refetch" signal), chat pushes the actual message payload — refetching the
 * whole channel history on every new message would be a much worse chat UX
 * than it is a Kanban-board one.
 */
@Service
public class ChatBroadcastService {

    private final SimpMessagingTemplate messagingTemplate;

    public ChatBroadcastService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void broadcast(Long organizationId, Long channelId, ChatMessageResponse message) {
        String destination = "/topic/organizations/%d/channels/%d/messages".formatted(organizationId, channelId);
        messagingTemplate.convertAndSend(destination, message);
    }
}
