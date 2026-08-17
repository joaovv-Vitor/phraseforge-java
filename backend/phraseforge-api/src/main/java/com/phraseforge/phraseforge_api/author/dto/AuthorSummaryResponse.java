package com.phraseforge.phraseforge_api.author.dto;

public record AuthorSummaryResponse(
        Long id,
        String name,
        String slug,
        Integer birthYear,
        Integer deathYear,
        long phraseCount) {
}
