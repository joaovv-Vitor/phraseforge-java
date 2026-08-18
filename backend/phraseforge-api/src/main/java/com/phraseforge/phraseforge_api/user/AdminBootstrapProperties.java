package com.phraseforge.phraseforge_api.user;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.bootstrap-admin")
public record AdminBootstrapProperties(String email, String password) {

    public boolean isConfigured() {
        return hasText(email) && hasText(password);
    }

    public boolean isPartiallyConfigured() {
        return hasText(email) != hasText(password);
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
