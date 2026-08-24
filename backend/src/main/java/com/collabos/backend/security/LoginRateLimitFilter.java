package com.collabos.backend.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Guards /api/auth/login against brute-forcing a password: a fixed number of
 * attempts per IP per window, tracked in Redis so it survives app restarts
 * and would work the same way across multiple backend instances.
 */
@Component
public class LoginRateLimitFilter extends OncePerRequestFilter {

    private final RateLimiter rateLimiter;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final int maxAttempts;
    private final Duration window;

    public LoginRateLimitFilter(
            RateLimiter rateLimiter,
            @Value("${app.ratelimit.login.max-attempts}") int maxAttempts,
            @Value("${app.ratelimit.login.window-seconds}") long windowSeconds) {
        this.rateLimiter = rateLimiter;
        this.maxAttempts = maxAttempts;
        this.window = Duration.ofSeconds(windowSeconds);
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        if (!isLoginRequest(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        String key = "ratelimit:login:" + request.getRemoteAddr();
        if (rateLimiter.tryAcquire(key, maxAttempts, window)) {
            filterChain.doFilter(request, response);
            return;
        }

        response.setStatus(429);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("status", 429);
        body.put("error", "Too Many Requests");
        body.put("message", "Too many login attempts. Try again in a minute.");
        objectMapper.writeValue(response.getWriter(), body);
    }

    private boolean isLoginRequest(HttpServletRequest request) {
        return "POST".equalsIgnoreCase(request.getMethod()) && "/api/auth/login".equals(request.getRequestURI());
    }
}
