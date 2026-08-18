package com.phraseforge.phraseforge_api.auth.dto;

import com.phraseforge.phraseforge_api.user.UserRole;

public record UserResponse(
        Long id,
        String email,
        String displayName,
        UserRole role) {
}
