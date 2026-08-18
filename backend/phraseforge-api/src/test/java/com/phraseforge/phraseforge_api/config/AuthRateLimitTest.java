package com.phraseforge.phraseforge_api.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "app.auth.rate-limit.max-attempts=2")
@AutoConfigureMockMvc
class AuthRateLimitTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void login_isRateLimitedPerClient() throws Exception {
        String request = "{\"email\":\"missing@example.com\",\"password\":\"not-the-password\"}";

        mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                .andExpect(jsonPath("$.code").value("RATE_LIMITED"));
    }
}
