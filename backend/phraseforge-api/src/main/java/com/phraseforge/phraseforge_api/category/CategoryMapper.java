package com.phraseforge.phraseforge_api.category;

import com.phraseforge.phraseforge_api.category.dto.CategoryResponse;
import com.phraseforge.phraseforge_api.category.dto.CategorySummaryResponse;
import org.springframework.stereotype.Component;

@Component
public class CategoryMapper {

    public CategorySummaryResponse toSummary(Category category, long phraseCount) {
        return new CategorySummaryResponse(
                category.getId(),
                category.getName(),
                category.getSlug(),
                phraseCount);
    }

    public CategoryResponse toResponse(Category category, long phraseCount) {
        return new CategoryResponse(
                category.getId(),
                category.getName(),
                category.getSlug(),
                category.getDescription(),
                phraseCount);
    }
}
