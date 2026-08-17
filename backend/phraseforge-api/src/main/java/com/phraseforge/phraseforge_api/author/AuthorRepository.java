package com.phraseforge.phraseforge_api.author;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface AuthorRepository extends JpaRepository<Author, Long> {

    Optional<Author> findBySlug(String slug);

    Optional<Author> findByName(String name);

    boolean existsByName(String name);

    boolean existsBySlug(String slug);

    /**
     * Returns [authorId, phraseCount] pairs for all authors (0 for authors
     * without phrases). A single GROUP BY query avoids per-author N+1 counts.
     */
    @Query("select a.id, count(p) from Author a left join a.phrases p group by a.id")
    List<Object[]> findPhraseCounts();
}
