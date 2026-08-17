package com.phraseforge.phraseforge_api.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI phraseforgeOpenApi() {
        return new OpenAPI().info(new Info()
                .title("PhraseForge API")
                .description("REST API for the PhraseForge quote library (MVP)")
                .version("1.0.0"));
    }
}
