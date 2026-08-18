package com.phraseforge.phraseforge_api.config;

import com.phraseforge.phraseforge_api.auth.JwtService;
import com.phraseforge.phraseforge_api.user.User;
import com.phraseforge.phraseforge_api.user.UserRole;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityAuthorizationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Test
    void catalogReads_arePublic() throws Exception {
        mockMvc.perform(get("/api/v1/phrases"))
                .andExpect(status().isOk());
    }

    @Test
    void catalogWrites_requireAuthentication() throws Exception {
        mockMvc.perform(post("/api/v1/phrases")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void catalogWrites_rejectRegularUsers() throws Exception {
        mockMvc.perform(post("/api/v1/phrases")
                        .header(HttpHeaders.AUTHORIZATION, bearer(UserRole.USER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void catalogWrites_acceptAdministratorsBeforeControllerValidation() throws Exception {
        mockMvc.perform(post("/api/v1/phrases")
                        .header(HttpHeaders.AUTHORIZATION, bearer(UserRole.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    private String bearer(UserRole role) {
        User user = new User("user@example.com", "{bcrypt}hash", "User", role);
        ReflectionTestUtils.setField(user, "id", 1L);
        return "Bearer " + jwtService.issue(user).value();
    }
}
