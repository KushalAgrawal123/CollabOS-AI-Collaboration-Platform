package com.collabos.backend.websocket;

import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

/**
 * Typing indicators are intentionally not persisted anywhere — this just
 * relays "user X is typing" straight back out to the channel's typing topic.
 * The client debounces sends and expires the indicator itself after a
 * couple of seconds of silence, so there's no matching "stopped typing"
 * message to handle here.
 */
@Controller
public class ChatWebSocketController {

    private final SimpMessagingTemplate messagingTemplate;

    public ChatWebSocketController(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @MessageMapping("/organizations/{orgId}/channels/{channelId}/typing")
    public void typing(@DestinationVariable Long orgId, @DestinationVariable Long channelId, StompPrincipal principal) {
        String destination = "/topic/organizations/%d/channels/%d/typing".formatted(orgId, channelId);
        messagingTemplate.convertAndSend(destination, new TypingEvent(principal.user().id(), principal.user().name()));
    }
}
