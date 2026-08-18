package com.phraseforge.phraseforge_api.user;

import com.phraseforge.phraseforge_api.support.DatabaseFixtureCleanup;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class UserRepositoryTest extends DatabaseFixtureCleanup {

    @Autowired
    private UserRepository userRepository;

    @Test
    void findsUserAndRoleByPersistedValues() {
        userRepository.save(new User("admin@example.com", "{bcrypt}hash", "Admin", UserRole.ADMIN));

        assertThat(userRepository.findByEmail("admin@example.com")).isPresent();
        assertThat(userRepository.existsByEmail("admin@example.com")).isTrue();
        assertThat(userRepository.existsByRole(UserRole.ADMIN)).isTrue();
        assertThat(userRepository.existsByRole(UserRole.USER)).isFalse();
    }
}
