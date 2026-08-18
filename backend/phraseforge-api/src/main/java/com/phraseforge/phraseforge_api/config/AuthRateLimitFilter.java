package com.phraseforge.phraseforge_api.config;

import com.phraseforge.phraseforge_api.exception.ApiError;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

@Component
public class AuthRateLimitFilter extends OncePerRequestFilter {

    private static final Set<String> LIMITED_PATHS = Set.of(
            "/api/v1/auth/login",
            "/api/v1/auth/register");
    private static final int MAX_TRACKED_CLIENTS = 10_000;

    private final int maxAttempts;
    private final Duration window;
    private final ObjectMapper objectMapper;
    private final ConcurrentHashMap<String, AttemptWindow> attempts = new ConcurrentHashMap<>();

    public AuthRateLimitFilter(
            @Value("${app.auth.rate-limit.max-attempts:10}") int maxAttempts,
            @Value("${app.auth.rate-limit.window:PT1M}") Duration window,
            ObjectMapper objectMapper) {
        if (maxAttempts < 1 || window.isZero() || window.isNegative()) {
            throw new IllegalArgumentException("Authentication rate limit must be positive");
        }
        this.maxAttempts = maxAttempts;
        this.window = window;
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !"POST".equals(request.getMethod()) || !LIMITED_PATHS.contains(request.getRequestURI());
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        Instant now = Instant.now();
        String key = request.getRemoteAddr() + ':' + request.getRequestURI();
        if (!attempts.containsKey(key) && attempts.size() >= MAX_TRACKED_CLIENTS) {
            attempts.entrySet().removeIf(entry -> !now.isBefore(entry.getValue().resetsAt()));
            if (attempts.size() >= MAX_TRACKED_CLIENTS) {
                writeRateLimit(response, window.toMillis());
                return;
            }
        }
        AtomicBoolean allowed = new AtomicBoolean(true);
        AtomicReference<Instant> resetsAt = new AtomicReference<>();

        attempts.compute(key, (_key, current) -> {
            if (current == null || !now.isBefore(current.resetsAt())) {
                AttemptWindow created = new AttemptWindow(1, now.plus(window));
                resetsAt.set(created.resetsAt());
                return created;
            }
            resetsAt.set(current.resetsAt());
            if (current.count() >= maxAttempts) {
                allowed.set(false);
                return current;
            }
            return new AttemptWindow(current.count() + 1, current.resetsAt());
        });

        if (!allowed.get()) {
            writeRateLimit(response, Duration.between(now, resetsAt.get()).toMillis());
            return;
        }

        if (attempts.size() > MAX_TRACKED_CLIENTS) {
            attempts.entrySet().removeIf(entry -> !now.isBefore(entry.getValue().resetsAt()));
        }
        filterChain.doFilter(request, response);
    }

    private void writeRateLimit(HttpServletResponse response, long retryAfterMillis) throws IOException {
        long retryAfterSeconds = Math.max(1, (retryAfterMillis + 999) / 1_000);
        response.setStatus(429);
        response.setHeader("Retry-After", Long.toString(retryAfterSeconds));
        response.setContentType("application/json");
        objectMapper.writeValue(
                response.getOutputStream(),
                ApiError.of(429, "RATE_LIMITED", "Too many authentication attempts"));
    }

    private record AttemptWindow(int count, Instant resetsAt) {
    }
}
