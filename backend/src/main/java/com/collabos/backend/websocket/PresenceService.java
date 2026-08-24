package com.collabos.backend.websocket;

import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory, single-instance presence tracking: who currently has a given
 * resource (a project board, a chat channel, ...) open. Grouped by the STOMP
 * destination string itself rather than a parsed id — that's what makes this
 * one service reusable across resource types with no per-type branching.
 * Deliberately plain JVM state rather than Redis — a WebSocket session only
 * ever exists on the one server instance that accepted it, so there's
 * nothing here that needs to be shared across instances. (A multi-instance
 * deployment would need Redis pub/sub to fan presence changes out to every
 * instance's connected clients — worth knowing as the scaling story, not
 * worth building for a single-instance local app.)
 */
@Service
public class PresenceService {

    public record Viewer(Long userId, String userName) {
    }

    // presence topic destination -> (sessionId -> viewer)
    private final Map<String, Map<String, Viewer>> viewersByTopic = new ConcurrentHashMap<>();
    // sessionId -> presence topic destination, so a disconnect (which only knows the session) can find it
    private final Map<String, String> topicBySession = new ConcurrentHashMap<>();

    public List<Viewer> join(String topicKey, String sessionId, Long userId, String userName) {
        viewersByTopic
                .computeIfAbsent(topicKey, key -> new ConcurrentHashMap<>())
                .put(sessionId, new Viewer(userId, userName));
        topicBySession.put(sessionId, topicKey);
        return currentViewers(topicKey);
    }

    public Optional<LeaveEvent> leave(String sessionId) {
        String topicKey = topicBySession.remove(sessionId);
        if (topicKey == null) {
            return Optional.empty();
        }
        Map<String, Viewer> sessions = viewersByTopic.get(topicKey);
        if (sessions != null) {
            sessions.remove(sessionId);
        }
        return Optional.of(new LeaveEvent(topicKey, currentViewers(topicKey)));
    }

    public record LeaveEvent(String topicKey, List<Viewer> remaining) {
    }

    private List<Viewer> currentViewers(String topicKey) {
        Map<String, Viewer> sessions = viewersByTopic.get(topicKey);
        if (sessions == null) {
            return List.of();
        }
        return sessions.values().stream()
                .sorted(Comparator.comparing(Viewer::userId))
                .distinct()
                .toList();
    }
}
