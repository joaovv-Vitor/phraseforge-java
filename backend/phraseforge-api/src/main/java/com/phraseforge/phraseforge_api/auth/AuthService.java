package com.phraseforge.phraseforge_api.auth;

import com.phraseforge.phraseforge_api.auth.dto.AuthResponse;
import com.phraseforge.phraseforge_api.auth.dto.LoginRequest;
import com.phraseforge.phraseforge_api.auth.dto.RegisterRequest;
import com.phraseforge.phraseforge_api.auth.dto.UserResponse;
import com.phraseforge.phraseforge_api.user.User;
import com.phraseforge.phraseforge_api.user.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class AuthService {

    private final UserService userService;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final AuthProperties properties;

    public AuthService(
            UserService userService,
            JwtService jwtService,
            RefreshTokenService refreshTokenService,
            AuthProperties properties) {
        this.userService = userService;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
        this.properties = properties;
    }

    @Transactional
    public AuthSession register(RegisterRequest request) {
        return issueSession(userService.register(request.email(), request.displayName(), request.password()));
    }

    @Transactional
    public AuthSession login(LoginRequest request) {
        return issueSession(userService.authenticate(request.email(), request.password()));
    }

    @Transactional
    public AuthSession refresh(String refreshToken) {
        IssuedRefreshToken rotated = refreshTokenService.rotate(refreshToken);
        return issueSession(rotated.user(), rotated);
    }

    @Transactional
    public void logout(String refreshToken) {
        refreshTokenService.revoke(refreshToken);
    }

    private AuthSession issueSession(User user) {
        return issueSession(user, refreshTokenService.issue(user));
    }

    private AuthSession issueSession(User user, IssuedRefreshToken refreshToken) {
        AccessToken accessToken = jwtService.issue(user);
        AuthResponse response = new AuthResponse(
                accessToken.value(),
                properties.accessTokenTtl().toSeconds(),
                new UserResponse(user.getId(), user.getEmail(), user.getDisplayName(), user.getRole()));
        return new AuthSession(response, refreshToken.value(), refreshToken.expiresAt());
    }

    public record AuthSession(AuthResponse response, String refreshToken, Instant refreshTokenExpiresAt) {
    }
}
