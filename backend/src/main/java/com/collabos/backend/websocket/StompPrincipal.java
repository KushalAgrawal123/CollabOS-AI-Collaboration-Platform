package com.collabos.backend.websocket;

import com.collabos.backend.security.JwtService.AuthenticatedUser;

import java.security.Principal;

/**
 * Wraps our JWT-derived identity so it can sit in a STOMP session's Principal
 * slot — that's how @MessageMapping handlers and event listeners recover
 * "who is this" for a given WebSocket session.
 */
public record StompPrincipal(AuthenticatedUser user) implements Principal {
    @Override
    public String getName() {
        return user.id().toString();
    }
}
