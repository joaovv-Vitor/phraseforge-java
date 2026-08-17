package com.phraseforge.phraseforge_api.author.dto;

import java.time.Instant;

public record AuthorResponse(
        Long id,
        String name,
        String slug,
        Integer birthYear,
        Integer deathYear,
        String biography,
        long phraseCount,
        Instant createdAt,
        Instant updatedAt) {
}
