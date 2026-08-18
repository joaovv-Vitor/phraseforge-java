package com.phraseforge.phraseforge_api.user;

import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AdminBootstrapRunnerTest {

    private final DefaultApplicationArguments args = new DefaultApplicationArguments();

    @Test
    void run_doesNothingWhenBootstrapIsNotConfigured() throws Exception {
        UserService userService = mock(UserService.class);
        AdminBootstrapRunner runner = new AdminBootstrapRunner(new AdminBootstrapProperties("", ""), userService);

        runner.run(args);

        verifyNoInteractions(userService);
    }

    @Test
    void run_rejectsPartialBootstrapConfiguration() {
        UserService userService = mock(UserService.class);
        AdminBootstrapRunner runner = new AdminBootstrapRunner(
                new AdminBootstrapProperties("admin@example.com", ""), userService);

        assertThatThrownBy(() -> runner.run(args))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("must be configured together");
        verifyNoInteractions(userService);
    }

    @Test
    void run_createsInitialAdministratorWhenFullyConfigured() throws Exception {
        UserService userService = mock(UserService.class);
        AdminBootstrapRunner runner = new AdminBootstrapRunner(
                new AdminBootstrapProperties("admin@example.com", "a-long-bootstrap-password"), userService);
        when(userService.createInitialAdministrator("admin@example.com", "a-long-bootstrap-password")).thenReturn(true);

        runner.run(args);

        verify(userService).createInitialAdministrator("admin@example.com", "a-long-bootstrap-password");
    }
}
