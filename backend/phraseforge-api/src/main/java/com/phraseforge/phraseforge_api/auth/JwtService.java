package com.phraseforge.phraseforge_api.auth;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.nimbusds.jose.proc.SecurityContext;
import com.phraseforge.phraseforge_api.user.User;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

@Service
public class JwtService {

    private static final int MIN_SECRET_BYTES = 32;

    private final AuthProperties properties;
    private final JwtEncoder jwtEncoder;
    private final JwtDecoder jwtDecoder;
    private final Clock clock;

    @Autowired
    public JwtService(AuthProperties properties) {
        this(properties, Clock.systemUTC());
    }

    JwtService(AuthProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
        SecretKey secretKey = secretKey(properties.jwtSecret());
        this.jwtEncoder = new NimbusJwtEncoder(new ImmutableSecret<SecurityContext>(secretKey));
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(secretKey)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(properties.issuer()));
        this.jwtDecoder = decoder;
    }

    public AccessToken issue(User user) {
        Instant issuedAt = clock.instant();
        Instant expiresAt = issuedAt.plus(properties.accessTokenTtl());
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(properties.issuer())
                .subject(user.getId().toString())
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .id(UUID.randomUUID().toString())
                .claim("role", user.getRole().name())
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).type("JWT").build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new AccessToken(token, expiresAt);
    }

    public Jwt decode(String token) {
        return jwtDecoder.decode(token);
    }

    public JwtDecoder decoder() {
        return jwtDecoder;
    }

    private static SecretKey secretKey(String encodedSecret) {
        if (encodedSecret == null || encodedSecret.isBlank()) {
            throw new IllegalStateException("JWT_SECRET must be configured");
        }
        byte[] bytes;
        try {
            bytes = Base64.getDecoder().decode(encodedSecret);
        } catch (IllegalArgumentException ex) {
            throw new IllegalStateException("JWT_SECRET must be Base64 encoded", ex);
        }
        if (bytes.length < MIN_SECRET_BYTES) {
            throw new IllegalStateException("JWT_SECRET must decode to at least 32 bytes");
        }
        return new SecretKeySpec(bytes, "HmacSHA256");
    }
}
