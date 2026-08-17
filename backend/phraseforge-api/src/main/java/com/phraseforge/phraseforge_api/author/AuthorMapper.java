package com.phraseforge.phraseforge_api.author;

import com.phraseforge.phraseforge_api.author.dto.AuthorResponse;
import com.phraseforge.phraseforge_api.author.dto.AuthorSummaryResponse;
import org.springframework.stereotype.Component;

@Component
public class AuthorMapper {

    public AuthorSummaryResponse toSummary(Author author, long phraseCount) {
        return new AuthorSummaryResponse(
                author.getId(),
                author.getName(),
                author.getSlug(),
                author.getBirthYear(),
                author.getDeathYear(),
                phraseCount);
    }

    public AuthorResponse toResponse(Author author, long phraseCount) {
        return new AuthorResponse(
                author.getId(),
                author.getName(),
                author.getSlug(),
                author.getBirthYear(),
                author.getDeathYear(),
                author.getBiography(),
                phraseCount,
                author.getCreatedAt(),
                author.getUpdatedAt());
    }
}
