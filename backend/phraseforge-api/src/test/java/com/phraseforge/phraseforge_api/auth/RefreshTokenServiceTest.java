package com.phraseforge.phraseforge_api.auth;

import com.phraseforge.phraseforge_api.user.User;
import com.phraseforge.phraseforge_api.user.UserRole;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    private static final Instant NOW = Instant.parse("2026-08-17T00:00:00Z");

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Test
    void issue_persistsOnlyHashAndReturnsOpaqueToken() {
        RefreshTokenService service = service();
        User user = new User("user@example.com", "{bcrypt}hash", "User", UserRole.USER);
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

        IssuedRefreshToken issued = service.issue(user);

        ArgumentCaptor<RefreshToken> tokenCaptor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(tokenCaptor.capture());
        RefreshToken persisted = tokenCaptor.getValue();
        assertThat(issued.value()).isNotBlank();
        assertThat(persisted.getTokenHash()).isEqualTo(RefreshTokenService.hash(issued.value()));
        assertThat(persisted.getTokenHash()).isNotEqualTo(issued.value());
        assertThat(issued.expiresAt()).isEqualTo(NOW.plus(Duration.ofDays(30)));
    }

    @Test
    void rotate_revokesCurrentTokenAndKeepsItsFamily() {
        RefreshTokenService service = service();
        User user = new User("user@example.com", "{bcrypt}hash", "User", UserRole.USER);
        RefreshToken current = new RefreshToken(user, RefreshTokenService.hash("current"), "family-id", NOW.plus(Duration.ofDays(1)), NOW);
        when(refreshTokenRepository.findByTokenHashForUpdate(RefreshTokenService.hash("current")))
                .thenReturn(Optional.of(current));
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

        IssuedRefreshToken replacement = service.rotate("current");

        ArgumentCaptor<RefreshToken> tokenCaptor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(tokenCaptor.capture());
        assertThat(tokenCaptor.getValue().getFamilyId()).isEqualTo("family-id");
        assertThat(current.isRevoked()).isTrue();
        assertThat(replacement.value()).isNotEqualTo("current");
    }

    @Test
    void rotate_reusedTokenRevokesEntireFamily() {
        RefreshTokenService service = service();
        User user = new User("user@example.com", "{bcrypt}hash", "User", UserRole.USER);
        RefreshToken current = new RefreshToken(user, RefreshTokenService.hash("reused"), "family-id", NOW.plus(Duration.ofDays(1)), NOW);
        current.revoke(NOW.minusSeconds(1));
        when(refreshTokenRepository.findByTokenHashForUpdate(RefreshTokenService.hash("reused")))
                .thenReturn(Optional.of(current));

        assertThatThrownBy(() -> service.rotate("reused"))
                .isInstanceOf(InvalidRefreshTokenException.class);
        verify(refreshTokenRepository).revokeActiveFamily(eq("family-id"), eq(NOW));
    }

    private RefreshTokenService service() {
        AuthProperties properties = new AuthProperties(
                "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=",
                "phraseforge-test", Duration.ofMinutes(15), Duration.ofDays(30), false);
        return new RefreshTokenService(refreshTokenRepository, properties, Clock.fixed(NOW, ZoneOffset.UTC));
    }
}
