package com.phraseforge.phraseforge_api.author.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateAuthorRequest(
        @NotBlank(message = "Name is required")
        @Size(max = 150, message = "Name must be at most 150 characters")
        String name,

        @Min(value = -10000, message = "Birth year is out of range")
        @Max(value = 10000, message = "Birth year is out of range")
        Integer birthYear,

        @Min(value = -10000, message = "Death year is out of range")
        @Max(value = 10000, message = "Death year is out of range")
        Integer deathYear,

        @Size(max = 10000, message = "Biography must be at most 10000 characters")
        String biography) {
}
