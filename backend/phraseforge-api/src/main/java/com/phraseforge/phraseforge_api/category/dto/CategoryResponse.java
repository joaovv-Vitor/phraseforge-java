package com.phraseforge.phraseforge_api.category.dto;

public record CategoryResponse(
        Long id,
        String name,
        String slug,
        String description,
        long phraseCount) {
}
