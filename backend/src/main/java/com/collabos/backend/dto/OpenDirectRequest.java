package com.collabos.backend.dto;

import jakarta.validation.constraints.NotNull;

public record OpenDirectRequest(@NotNull Long userId) {
}
