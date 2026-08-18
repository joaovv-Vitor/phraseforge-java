package com.phraseforge.phraseforge_api.user;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UserControllerTest {

    @Test
    void me_returnsSafeCurrentUser() {
        UserRepository repository = mock(UserRepository.class);
        User user = new User("user@example.com", "{bcrypt}secret", "User", UserRole.USER);
        ReflectionTestUtils.setField(user, "id", 7L);
        when(repository.findById(7L)).thenReturn(Optional.of(user));
        UserController controller = new UserController(repository);

        var response = controller.me(jwt("7"));

        assertThat(response.id()).isEqualTo(7L);
        assertThat(response.email()).isEqualTo("user@example.com");
        assertThat(response.role()).isEqualTo(UserRole.USER);
    }

    private Jwt jwt(String subject) {
        Instant now = Instant.now();
        return new Jwt("token", now, now.plusSeconds(60), Map.of("alg", "none"), Map.of("sub", subject));
    }
}
