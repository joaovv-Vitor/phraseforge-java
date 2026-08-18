package com.phraseforge.phraseforge_api.auth;

import com.phraseforge.phraseforge_api.user.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

@Service
public class RefreshTokenService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int TOKEN_BYTES = 32;

    private final RefreshTokenRepository refreshTokenRepository;
    private final AuthProperties properties;
    private final Clock clock;

    @Autowired
    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository, AuthProperties properties) {
        this(refreshTokenRepository, properties, Clock.systemUTC());
    }

    RefreshTokenService(RefreshTokenRepository refreshTokenRepository, AuthProperties properties, Clock clock) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.properties = properties;
        this.clock = clock;
    }

    @Transactional
    public IssuedRefreshToken issue(User user) {
        return create(user, UUID.randomUUID().toString(), clock.instant()).issued();
    }

    @Transactional
    public IssuedRefreshToken rotate(String rawToken) {
        Instant now = clock.instant();
        RefreshToken current = refreshTokenRepository.findByTokenHashForUpdate(hash(rawToken))
                .orElseThrow(InvalidRefreshTokenException::new);

        if (current.isRevoked()) {
            refreshTokenRepository.revokeActiveFamily(current.getFamilyId(), now);
            throw new InvalidRefreshTokenException();
        }
        if (current.isExpired(now) || !current.getUser().isEnabled()) {
            current.revoke(now);
            refreshTokenRepository.revokeActiveFamily(current.getFamilyId(), now);
            throw new InvalidRefreshTokenException();
        }

        CreatedRefreshToken replacement = create(current.getUser(), current.getFamilyId(), now);
        current.replaceWith(replacement.entity(), now);
        return replacement.issued();
    }

    @Transactional
    public void revoke(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return;
        }
        refreshTokenRepository.findByTokenHashForUpdate(hash(rawToken))
                .ifPresent(token -> token.revoke(clock.instant()));
    }

    private CreatedRefreshToken create(User user, String familyId, Instant now) {
        String rawToken = randomToken();
        Instant expiresAt = now.plus(properties.refreshTokenTtl());
        RefreshToken token = new RefreshToken(user, hash(rawToken), familyId, expiresAt, now);
        RefreshToken saved = refreshTokenRepository.save(token);
        return new CreatedRefreshToken(saved, new IssuedRefreshToken(user, rawToken, expiresAt));
    }

    static String hash(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new InvalidRefreshTokenException();
        }
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is unavailable", ex);
        }
    }

    private static String randomToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private record CreatedRefreshToken(RefreshToken entity, IssuedRefreshToken issued) {
    }
}
