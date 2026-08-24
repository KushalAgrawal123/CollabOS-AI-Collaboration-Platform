package com.collabos.backend.dto;

import jakarta.validation.constraints.NotBlank;

public record CommentRequest(@NotBlank String body) {
}
