package com.collabos.backend.websocket;

import java.time.Instant;

/**
 * Deliberately just a "something changed, go refetch" signal rather than a
 * diff/patch payload. The client already has a well-tested React Query cache
 * for the task list (built in Phase 4) — reusing "invalidate and refetch" as
 * the reaction to this event means the WebSocket layer doesn't need to
 * reinvent client-side state merging just to stay in sync.
 */
public record TaskBoardEvent(String type, Instant at) {
    public static TaskBoardEvent of(String type) {
        return new TaskBoardEvent(type, Instant.now());
    }
}
