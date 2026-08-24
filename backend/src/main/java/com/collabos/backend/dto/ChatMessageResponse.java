package com.collabos.backend.dto;

import com.collabos.backend.entity.ChatMessage;

import java.time.Instant;

public record ChatMessageResponse(
        Long id,
        Long channelId,
        Long authorId,
        String authorName,
        String body,
        Instant createdAt
) {
    public static ChatMessageResponse from(ChatMessage message) {
        return new ChatMessageResponse(
                message.getId(),
                message.getChannel().getId(),
                message.getAuthor().getId(),
                message.getAuthor().getName(),
                message.getBody(),
                message.getCreatedAt());
    }
}
