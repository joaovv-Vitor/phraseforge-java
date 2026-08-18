package com.phraseforge.phraseforge_api.user;

import com.phraseforge.phraseforge_api.auth.InvalidCredentialsException;
import com.phraseforge.phraseforge_api.exception.DuplicateResourceException;
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

    @Transactional
    public User register(String rawEmail, String rawDisplayName, String password) {
        String email = normalizeEmail(rawEmail);
        if (userRepository.existsByEmail(email)) {
            throw new DuplicateResourceException("Email is already registered");
        }
        PasswordPolicy.requireValid(password);
        String displayName = normalizeDisplayName(rawDisplayName);
        return userRepository.save(new User(email, passwordEncoder.encode(password), displayName, UserRole.USER));
    }

    @Transactional(readOnly = true)
    public User authenticate(String rawEmail, String password) {
        String email = normalizeEmail(rawEmail);
        return userRepository.findByEmail(email)
                .filter(User::isEnabled)
                .filter(user -> password != null && passwordEncoder.matches(password, user.getPasswordHash()))
                .orElseThrow(InvalidCredentialsException::new);
    }

    static String normalizeEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email must not be blank");
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private static String normalizeDisplayName(String displayName) {
        if (displayName == null || displayName.isBlank()) {
            throw new IllegalArgumentException("Display name must not be blank");
        }
        return displayName.trim();
    }
}
