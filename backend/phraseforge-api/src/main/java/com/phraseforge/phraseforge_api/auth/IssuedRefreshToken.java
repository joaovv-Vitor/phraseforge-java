package com.phraseforge.phraseforge_api.auth;

import com.phraseforge.phraseforge_api.user.User;

import java.time.Instant;

public record IssuedRefreshToken(User user, String value, Instant expiresAt) {
}
