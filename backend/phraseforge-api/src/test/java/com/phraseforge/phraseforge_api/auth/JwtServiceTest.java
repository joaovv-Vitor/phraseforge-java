package com.phraseforge.phraseforge_api.auth;

import com.phraseforge.phraseforge_api.user.User;
import com.phraseforge.phraseforge_api.user.UserRole;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private static final String SECRET = "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=";
    private static final Instant NOW = Instant.now().truncatedTo(ChronoUnit.SECONDS);

    @Test
    void issue_createsSignedTokenWithIdentityAndRoleClaims() {
        JwtService jwtService = new JwtService(properties(), Clock.fixed(NOW, ZoneOffset.UTC));
        User user = user(42L, UserRole.ADMIN);

        AccessToken token = jwtService.issue(user);
        var decoded = jwtService.decode(token.value());

        assertThat(decoded.getSubject()).isEqualTo("42");
        assertThat(decoded.getClaimAsString("iss")).isEqualTo("phraseforge-test");
        assertThat(decoded.getClaimAsString("role")).isEqualTo("ADMIN");
        assertThat(decoded.getExpiresAt()).isEqualTo(NOW.plus(Duration.ofMinutes(15)));
    }

    @Test
    void constructor_rejectsWeakOrMissingSecret() {
        assertThatThrownBy(() -> new JwtService(new AuthProperties("", "issuer", Duration.ofMinutes(1), Duration.ofDays(1), false)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT_SECRET");
        assertThatThrownBy(() -> new JwtService(new AuthProperties("c2hvcnQ=", "issuer", Duration.ofMinutes(1), Duration.ofDays(1), false)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("at least 32 bytes");
    }

    private static AuthProperties properties() {
        return new AuthProperties(SECRET, "phraseforge-test", Duration.ofMinutes(15), Duration.ofDays(30), false);
    }

    private static User user(Long id, UserRole role) {
        User user = new User("user@example.com", "{bcrypt}hash", "User", role);
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }
}
