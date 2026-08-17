package com.phraseforge.phraseforge_api.phrase;

import com.phraseforge.phraseforge_api.author.Author;
import com.phraseforge.phraseforge_api.category.Category;
import com.phraseforge.phraseforge_api.phrase.dto.AuthorRef;
import com.phraseforge.phraseforge_api.phrase.dto.CategoryRef;
import com.phraseforge.phraseforge_api.phrase.dto.PhraseResponse;
import com.phraseforge.phraseforge_api.phrase.dto.PhraseSummaryResponse;
import com.phraseforge.phraseforge_api.phrase.dto.TagRef;
import com.phraseforge.phraseforge_api.tag.Tag;
import org.springframework.stereotype.Component;

@Component
public class PhraseMapper {

    public PhraseSummaryResponse toSummary(Phrase phrase) {
        return new PhraseSummaryResponse(
                phrase.getId(),
                phrase.getContent(),
                phrase.getYear(),
                phrase.getLanguage(),
                phrase.getSource(),
                authorRef(phrase.getAuthor()),
                phrase.getCategories().stream().map(this::categoryRef).toList(),
                phrase.getTags().stream().map(this::tagRef).toList(),
                phrase.getCreatedAt());
    }

    public PhraseResponse toResponse(Phrase phrase) {
        return new PhraseResponse(
                phrase.getId(),
                phrase.getContent(),
                phrase.getYear(),
                phrase.getLanguage(),
                phrase.getSource(),
                authorRef(phrase.getAuthor()),
                phrase.getCategories().stream().map(this::categoryRef).toList(),
                phrase.getTags().stream().map(this::tagRef).toList(),
                phrase.getCreatedAt(),
                phrase.getUpdatedAt());
    }

    private AuthorRef authorRef(Author author) {
        return new AuthorRef(author.getId(), author.getName(), author.getSlug());
    }

    private CategoryRef categoryRef(Category category) {
        return new CategoryRef(category.getId(), category.getName(), category.getSlug());
    }

    private TagRef tagRef(Tag tag) {
        return new TagRef(tag.getId(), tag.getName());
    }
}
