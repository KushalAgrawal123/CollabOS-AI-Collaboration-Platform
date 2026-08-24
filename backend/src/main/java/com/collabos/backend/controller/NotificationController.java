package com.collabos.backend.controller;

import com.collabos.backend.dto.NotificationResponse;
import com.collabos.backend.security.JwtService.AuthenticatedUser;
import com.collabos.backend.service.NotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/organizations/{orgId}/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public List<NotificationResponse> list(@AuthenticationPrincipal AuthenticatedUser currentUser, @PathVariable Long orgId) {
        return notificationService.listForUser(orgId, currentUser.id());
    }

    @GetMapping("/unread-count")
    public Map<String, Long> unreadCount(@AuthenticationPrincipal AuthenticatedUser currentUser, @PathVariable Long orgId) {
        return Map.of("count", notificationService.unreadCount(orgId, currentUser.id()));
    }

    @PostMapping("/{notificationId}/read")
    public ResponseEntity<Void> markRead(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @PathVariable Long orgId,
            @PathVariable Long notificationId) {
        notificationService.markRead(currentUser.id(), notificationId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/read-all")
    public ResponseEntity<Void> markAllRead(@AuthenticationPrincipal AuthenticatedUser currentUser, @PathVariable Long orgId) {
        notificationService.markAllRead(orgId, currentUser.id());
        return ResponseEntity.noContent().build();
    }
}
