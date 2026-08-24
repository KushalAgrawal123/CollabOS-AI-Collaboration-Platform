package com.collabos.backend.controller;

import com.collabos.backend.dto.ChatMessageRequest;
import com.collabos.backend.dto.ChatMessageResponse;
import com.collabos.backend.entity.ChatMessage;
import com.collabos.backend.security.JwtService.AuthenticatedUser;
import com.collabos.backend.service.ChatMessageService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/organizations/{orgId}/channels/{channelId}/messages")
public class ChatMessageController {

    private final ChatMessageService chatMessageService;

    public ChatMessageController(ChatMessageService chatMessageService) {
        this.chatMessageService = chatMessageService;
    }

    @GetMapping
    public List<ChatMessageResponse> list(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @PathVariable Long orgId,
            @PathVariable Long channelId) {
        return chatMessageService.list(orgId, channelId, currentUser.id());
    }

    @PostMapping
    public ResponseEntity<ChatMessageResponse> create(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @PathVariable Long orgId,
            @PathVariable Long channelId,
            @Valid @RequestBody ChatMessageRequest request) {
        ChatMessage message = chatMessageService.create(orgId, channelId, currentUser, request.body());
        return ResponseEntity.status(HttpStatus.CREATED).body(ChatMessageResponse.from(message));
    }
}
