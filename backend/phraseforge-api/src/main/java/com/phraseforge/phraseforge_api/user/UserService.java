package com.phraseforge.phraseforge_api.user;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
public class UserService {

    private static final String INITIAL_ADMIN_DISPLAY_NAME = "Administrator";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Creates the initial administrator only when none exists. The operation is
     * intentionally idempotent so a restarted application never resets a password.
     */
    @Transactional
    public boolean createInitialAdministrator(String rawEmail, String password) {
        if (userRepository.existsByRole(UserRole.ADMIN)) {
            return false;
        }

        PasswordPolicy.requireValid(password);
        User user = new User(
                normalizeEmail(rawEmail),
                passwordEncoder.encode(password),
                INITIAL_ADMIN_DISPLAY_NAME,
                UserRole.ADMIN);
        userRepository.save(user);
        return true;
    }

    static String normalizeEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email must not be blank");
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
