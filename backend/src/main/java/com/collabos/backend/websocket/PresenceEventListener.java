package com.collabos.backend.websocket;

import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;
import org.springframework.web.socket.messaging.SessionUnsubscribeEvent;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Subscribing to any resource's presence topic doubles as "I'm here now" —
 * there's no separate join/leave message the client has to remember to
 * send. Unsubscribing or disconnecting (closing the tab) is the leave
 * signal, symmetrically. Generic over *what* has presence — the destination
 * string itself is the presence group key, so a project board (Phase 6) and
 * a chat channel (Phase 7) both work with zero listener changes; only the
 * pattern below needs a new alternative when a third kind of resource wants
 * presence tracking.
 */
@Component
public class PresenceEventListener {

    private static final Pattern PRESENCE_DESTINATION =
            Pattern.compile("^/topic/organizations/\\d+/(?:projects|channels)/\\d+/presence$");

    private final PresenceService presenceService;
    private final SimpMessagingTemplate messagingTemplate;

    public PresenceEventListener(PresenceService presenceService, SimpMessagingTemplate messagingTemplate) {
        this.presenceService = presenceService;
        this.messagingTemplate = messagingTemplate;
    }

    @EventListener
    public void handleSubscribe(SessionSubscribeEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String destination = accessor.getDestination();
        if (destination == null || !PRESENCE_DESTINATION.matcher(destination).matches()) {
            return;
        }
        if (!(accessor.getUser() instanceof StompPrincipal principal)) {
            return;
        }

        List<PresenceService.Viewer> viewers = presenceService.join(
                destination, accessor.getSessionId(), principal.user().id(), principal.user().name());

        messagingTemplate.convertAndSend(destination, viewers);
    }

    @EventListener
    public void handleUnsubscribe(SessionUnsubscribeEvent event) {
        broadcastLeave(StompHeaderAccessor.wrap(event.getMessage()).getSessionId());
    }

    @EventListener
    public void handleDisconnect(SessionDisconnectEvent event) {
        broadcastLeave(StompHeaderAccessor.wrap(event.getMessage()).getSessionId());
    }

    private void broadcastLeave(String sessionId) {
        if (sessionId == null) {
            return;
        }
        presenceService.leave(sessionId)
                .ifPresent(result -> messagingTemplate.convertAndSend(result.topicKey(), result.remaining()));
    }
}
