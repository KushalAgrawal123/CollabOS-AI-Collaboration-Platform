package com.collabos.backend.dto;

public record AuthResponse(String token, UserResponse user) {
}
