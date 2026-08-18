package com.phraseforge.phraseforge_api.auth;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(AuthProperties.class)
public class AuthConfig {
}
