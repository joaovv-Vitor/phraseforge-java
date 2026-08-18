package com.phraseforge.phraseforge_api.auth;

import java.time.Instant;

public record AccessToken(String value, Instant expiresAt) {
}
