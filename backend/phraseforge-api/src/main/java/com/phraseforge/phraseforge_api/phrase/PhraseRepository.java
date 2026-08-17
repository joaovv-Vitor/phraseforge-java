package com.phraseforge.phraseforge_api.phrase;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
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

    @EntityGraph(attributePaths = {"author", "phraseCategories.category", "phraseTags.tag"})
    Optional<Phrase> findWithDetailsById(Long id);

    /**
     * Listings eagerly fetch author + categories + tags so the paged JSON
     * does not trigger N+1 SELECTs. Overrides the inherited method.
     */
    @Override
    @EntityGraph(attributePaths = {"author", "phraseCategories.category", "phraseTags.tag"})
    Page<Phrase> findAll(Specification<Phrase> spec, Pageable pageable);
}
