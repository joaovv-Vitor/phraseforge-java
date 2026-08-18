package com.phraseforge.phraseforge_api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.DelegatingPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Map;

@Configuration
public class PasswordConfig {

    private static final int BCRYPT_STRENGTH = 12;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new DelegatingPasswordEncoder(
                "bcrypt",
                Map.of("bcrypt", new BCryptPasswordEncoder(BCRYPT_STRENGTH)));
    }
}
