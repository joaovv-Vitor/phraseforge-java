package com.phraseforge.phraseforge_api.user;

import java.nio.charset.StandardCharsets;

public final class PasswordPolicy {

    public static final int MIN_LENGTH = 12;
    public static final int MAX_UTF8_BYTES = 72;

    private PasswordPolicy() {
    }

    public static void requireValid(String password) {
        if (password == null || password.isBlank() || password.length() < MIN_LENGTH
                || password.getBytes(StandardCharsets.UTF_8).length > MAX_UTF8_BYTES) {
            throw new IllegalArgumentException(
                    "Password must contain at least " + MIN_LENGTH
                            + " characters and at most " + MAX_UTF8_BYTES + " UTF-8 bytes");
        }
    }
}
