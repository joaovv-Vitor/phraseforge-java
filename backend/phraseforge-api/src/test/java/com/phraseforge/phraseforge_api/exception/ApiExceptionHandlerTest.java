package com.phraseforge.phraseforge_api.exception;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ApiExceptionHandlerTest {

    private final ApiExceptionHandler handler = new ApiExceptionHandler();

    @Test
    void notFound_mapsTo404() {
        var response = handler.handleNotFound(new ResourceNotFoundException("Author not found: 9"));

        assertThat(response.getStatusCode().value()).isEqualTo(404);
        assertThat(response.getBody().message()).isEqualTo("Author not found: 9");
        assertThat(response.getBody().status()).isEqualTo(404);
        assertThat(response.getBody().timestamp()).isNotNull();
    }

    @Test
    void conflict_mapsTo409() {
        var response = handler.handleConflict(new DuplicateResourceException("Tag already exists: x"));

        assertThat(response.getStatusCode().value()).isEqualTo(409);
        assertThat(response.getBody().status()).isEqualTo(409);
    }
}
