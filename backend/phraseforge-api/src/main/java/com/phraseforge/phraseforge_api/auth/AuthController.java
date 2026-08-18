package com.phraseforge.phraseforge_api.auth;

import com.phraseforge.phraseforge_api.auth.dto.AuthResponse;
import com.phraseforge.phraseforge_api.auth.dto.LoginRequest;
import com.phraseforge.phraseforge_api.auth.dto.RegisterRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.time.Instant;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private static final String REFRESH_COOKIE = "phraseforge_refresh";
    private static final String REFRESH_COOKIE_PATH = "/api/v1/auth";

    private final AuthService authService;
    private final AuthProperties properties;

    public AuthController(AuthService authService, AuthProperties properties) {
        this.authService = authService;
        this.properties = properties;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return sessionResponse(authService.register(request), HttpStatus.CREATED);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return sessionResponse(authService.login(request), HttpStatus.OK);
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(
            @CookieValue(name = REFRESH_COOKIE, required = false) String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new InvalidRefreshTokenException();
        }
        return sessionResponse(authService.refresh(refreshToken), HttpStatus.OK);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @CookieValue(name = REFRESH_COOKIE, required = false) String refreshToken) {
        authService.logout(refreshToken);
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, expiredCookie().toString())
                .build();
    }

    private ResponseEntity<AuthResponse> sessionResponse(AuthService.AuthSession session, HttpStatus status) {
        return ResponseEntity.status(status)
                .header(HttpHeaders.SET_COOKIE, refreshCookie(session.refreshToken(), session.refreshTokenExpiresAt()).toString())
                .body(session.response());
    }

    private ResponseCookie refreshCookie(String value, Instant expiresAt) {
        Duration maxAge = Duration.between(Instant.now(), expiresAt);
        return ResponseCookie.from(REFRESH_COOKIE, value)
                .httpOnly(true)
                .secure(properties.cookieSecure())
                .sameSite("Strict")
                .path(REFRESH_COOKIE_PATH)
                .maxAge(maxAge.isNegative() ? Duration.ZERO : maxAge)
                .build();
    }

    private ResponseCookie expiredCookie() {
        return ResponseCookie.from(REFRESH_COOKIE, "")
                .httpOnly(true)
                .secure(properties.cookieSecure())
                .sameSite("Strict")
                .path(REFRESH_COOKIE_PATH)
                .maxAge(Duration.ZERO)
                .build();
    }
}
