package com.collabos.backend.dto;

import jakarta.validation.constraints.NotBlank;

public record ChatMessageRequest(@NotBlank String body) {
}
