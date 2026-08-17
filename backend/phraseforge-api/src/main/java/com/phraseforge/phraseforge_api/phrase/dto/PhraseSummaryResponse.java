package com.phraseforge.phraseforge_api.phrase.dto;

import java.time.Instant;
import java.util.List;

public record PhraseSummaryResponse(
        Long id,
        String content,
        Integer year,
        String language,
        String source,
        AuthorRef author,
        List<CategoryRef> categories,
        List<TagRef> tags,
        Instant createdAt) {
}
