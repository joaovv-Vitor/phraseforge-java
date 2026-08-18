package com.phraseforge.phraseforge_api.auth.dto;

public record AuthResponse(
        String accessToken,
        long expiresIn,
        UserResponse user) {
}
