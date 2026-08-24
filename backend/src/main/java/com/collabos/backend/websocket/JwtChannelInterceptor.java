package com.collabos.backend.websocket;

import com.collabos.backend.security.JwtService;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

/**
 * REST auth reads the Authorization header per request. A WebSocket connection
 * has no per-message HTTP headers after the handshake, so STOMP carries the
 * token as a custom header on the CONNECT frame instead — this interceptor
 * validates it once, at connect time, and attaches the identity to the STOMP
 * session so every later frame on that session is already "authenticated".
 */
@Component
public class JwtChannelInterceptor implements ChannelInterceptor {

    private final JwtService jwtService;

    public JwtChannelInterceptor(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        // StompHeaderAccessor.wrap(message) builds a *new* accessor instance over the
        // message's headers. StompSubProtocolHandler registers a setUserChangeCallback
        // on the *original* accessor it decoded the CONNECT frame with — the callback
        // is what actually associates a Principal with the session going forward — so
        // calling setUser() on a wrap()'d copy mutates an accessor nothing is listening
        // to, and the identity silently never attaches. getAccessor() recovers that
        // original, "live" accessor instead.
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            String authHeader = accessor.getFirstNativeHeader("Authorization");
            String token = authHeader != null && authHeader.startsWith("Bearer ") ? authHeader.substring(7) : null;

            JwtService.AuthenticatedUser user = token == null
                    ? null
                    : jwtService.parse(token).orElse(null);

            if (user == null) {
                throw new IllegalArgumentException("A valid access token is required to connect");
            }

            accessor.setUser(new StompPrincipal(user));
        }

        return message;
    }
}
