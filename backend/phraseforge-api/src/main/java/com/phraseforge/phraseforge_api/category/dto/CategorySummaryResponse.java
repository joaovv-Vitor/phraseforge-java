package com.phraseforge.phraseforge_api.category.dto;

public record CategorySummaryResponse(
        Long id,
        String name,
        String slug,
        long phraseCount) {
}
