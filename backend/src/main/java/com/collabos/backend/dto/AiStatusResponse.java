package com.collabos.backend.dto;

public record AiStatusResponse(boolean configured, String provider, String model) {
}
