package com.phraseforge.phraseforge_api.auth;

import com.phraseforge.phraseforge_api.auth.dto.AuthResponse;
import com.phraseforge.phraseforge_api.auth.dto.RegisterRequest;
import com.phraseforge.phraseforge_api.auth.dto.UserResponse;
import com.phraseforge.phraseforge_api.user.UserRole;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuthControllerTest {

    @Test
    void register_returnsSessionAndHttpOnlyRefreshCookie() {
        AuthService authService = mock(AuthService.class);
        AuthController controller = new AuthController(authService, properties());
        AuthResponse response = new AuthResponse("access-token", 900, new UserResponse(1L, "user@example.com", "User", UserRole.USER));
        AuthService.AuthSession session = new AuthService.AuthSession(response, "refresh-token", Instant.now().plus(Duration.ofDays(30)));
        RegisterRequest request = new RegisterRequest("user@example.com", "User", "a-long-bootstrap-password");
        when(authService.register(request)).thenReturn(session);

        var result = controller.register(request);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(result.getBody()).isEqualTo(response);
        assertThat(result.getHeaders().getFirst("Set-Cookie"))
                .contains("phraseforge_refresh=refresh-token")
                .contains("HttpOnly")
                .contains("SameSite=Strict")
                .contains("Path=/api/v1/auth");
    }

    private static AuthProperties properties() {
        return new AuthProperties(
                "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=",
                "phraseforge-test", Duration.ofMinutes(15), Duration.ofDays(30), false);
    }
}
