package com.collabos.backend.dto;

import com.collabos.backend.entity.TaskComment;

import java.time.Instant;

public record CommentResponse(Long id, Long taskId, Long authorId, String authorName, String body, Instant createdAt) {
    public static CommentResponse from(TaskComment comment) {
        return new CommentResponse(
                comment.getId(),
                comment.getTask().getId(),
                comment.getAuthor().getId(),
                comment.getAuthor().getName(),
                comment.getBody(),
                comment.getCreatedAt());
    }
}
