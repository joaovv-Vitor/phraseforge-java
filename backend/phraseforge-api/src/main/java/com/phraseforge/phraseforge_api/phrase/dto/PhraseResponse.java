package com.phraseforge.phraseforge_api.phrase.dto;

import java.time.Instant;
import java.util.List;

public record PhraseResponse(
        Long id,
        String content,
        Integer year,
        String language,
        String source,
        AuthorRef author,
        List<CategoryRef> categories,
        List<TagRef> tags,
        Instant createdAt,
        Instant updatedAt,
        boolean favorited) {

    public PhraseResponse(
            Long id,
            String content,
            Integer year,
            String language,
            String source,
            AuthorRef author,
            List<CategoryRef> categories,
            List<TagRef> tags,
            Instant createdAt,
            Instant updatedAt) {
        this(id, content, year, language, source, author, categories, tags, createdAt, updatedAt, false);
    }
}
