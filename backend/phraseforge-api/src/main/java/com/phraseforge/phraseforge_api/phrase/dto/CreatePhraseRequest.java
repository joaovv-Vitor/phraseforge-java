package com.phraseforge.phraseforge_api.phrase.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.HashSet;
import java.util.Set;

public record CreatePhraseRequest(
        @NotBlank(message = "Content is required")
        @Size(max = 10000, message = "Content must be at most 10000 characters")
        String content,

        @NotNull(message = "Author is required")
        Long authorId,

        @Min(value = -10000, message = "Year is out of range")
        @Max(value = 10000, message = "Year is out of range")
        Integer year,

        @NotBlank(message = "Language is required")
        @Size(max = 10, message = "Language must be at most 10 characters")
        @Pattern(regexp = "^[a-z]{2}(-[A-Z]{2})?$", message = "Language must be an ISO 639-1 code")
        String language,

        @Size(max = 300, message = "Source must be at most 300 characters")
        String source,

        Set<Long> categoryIds,

        Set<Long> tagIds) {

    public CreatePhraseRequest {
        categoryIds = categoryIds == null ? new HashSet<>() : categoryIds;
        tagIds = tagIds == null ? new HashSet<>() : tagIds;
    }
}
