package com.collabos.backend.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateChannelRequest(@NotBlank String name) {
}
