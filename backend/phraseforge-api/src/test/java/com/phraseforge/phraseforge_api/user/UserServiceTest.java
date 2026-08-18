package com.phraseforge.phraseforge_api.user;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    @Test
    void createInitialAdministrator_normalizesEmailAndEncodesPassword() {
        String password = "a-long-bootstrap-password";
        when(userRepository.existsByRole(UserRole.ADMIN)).thenReturn(false);
        when(passwordEncoder.encode(password)).thenReturn("{bcrypt}encoded");

        boolean created = userService.createInitialAdministrator(" Admin@Example.COM ", password);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        assertThat(created).isTrue();
        verify(userRepository).save(userCaptor.capture());
        User user = userCaptor.getValue();
        assertThat(user.getEmail()).isEqualTo("admin@example.com");
        assertThat(user.getPasswordHash()).isEqualTo("{bcrypt}encoded");
        assertThat(user.getDisplayName()).isEqualTo("Administrator");
        assertThat(user.getRole()).isEqualTo(UserRole.ADMIN);
        assertThat(user.isEnabled()).isTrue();
    }

    @Test
    void createInitialAdministrator_doesNothingWhenAdministratorAlreadyExists() {
        when(userRepository.existsByRole(UserRole.ADMIN)).thenReturn(true);

        boolean created = userService.createInitialAdministrator("admin@example.com", "a-long-bootstrap-password");

        assertThat(created).isFalse();
        verify(passwordEncoder, never()).encode(any());
        verify(userRepository, never()).save(any());
    }

    @Test
    void createInitialAdministrator_rejectsInvalidPassword() {
        when(userRepository.existsByRole(UserRole.ADMIN)).thenReturn(false);

        assertThatThrownBy(() -> userService.createInitialAdministrator("admin@example.com", "short"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("at least 12 characters");
        verify(passwordEncoder, never()).encode(any());
        verify(userRepository, never()).save(any());
    }

    @Test
    void createInitialAdministrator_rejectsPasswordAboveBcryptByteLimit() {
        when(userRepository.existsByRole(UserRole.ADMIN)).thenReturn(false);

        assertThatThrownBy(() -> userService.createInitialAdministrator("admin@example.com", "a".repeat(73)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("at most 72 UTF-8 bytes");
        verify(passwordEncoder, never()).encode(any());
        verify(userRepository, never()).save(any());
    }
}
