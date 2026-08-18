package com.phraseforge.phraseforge_api.config;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;

class PasswordConfigTest {

    private final PasswordEncoder passwordEncoder = new PasswordConfig().passwordEncoder();

    @Test
    void passwordEncoder_storesVersionedBcryptHashAndMatchesPassword() {
        String password = "a-long-bootstrap-password";

        String hash = passwordEncoder.encode(password);

        assertThat(hash).startsWith("{bcrypt}");
        assertThat(passwordEncoder.matches(password, hash)).isTrue();
        assertThat(passwordEncoder.matches("another-password", hash)).isFalse();
    }
}
