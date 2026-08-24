package com.collabos.backend.dto;

import jakarta.validation.constraints.NotBlank;

public record AiAskRequest(@NotBlank String question) {
}
