package com.phraseforge.phraseforge_api.phrase;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface PhraseRepository
        extends JpaRepository<Phrase, Long>, JpaSpecificationExecutor<Phrase> {

    boolean existsByContentAndAuthor_Id(String content, Long authorId);

    /**
     * Duplicate check for updates: true if another phrase (id != excludeId)
     * has the same content under the same author.
     */
    boolean existsByContentAndAuthor_IdAndIdNot(String content, Long authorId, Long excludeId);

    /**
     * Loads a phrase with its author, categories, and tags in one query
     * (avoids N+1 when serializing the detail view).
     */
    @EntityGraph(attributePaths = {"author", "phraseCategories.category", "phraseTags.tag"})
    Optional<Phrase> findWithDetailsById(Long id);
}
