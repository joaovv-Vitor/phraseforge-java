package com.phraseforge.phraseforge_api.favorite;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;

public interface FavoriteRepository extends JpaRepository<Favorite, Long> {

    boolean existsByUser_IdAndPhrase_Id(Long userId, Long phraseId);

    @EntityGraph(attributePaths = {"phrase", "phrase.author", "phrase.phraseCategories.category", "phrase.phraseTags.tag"})
    Page<Favorite> findByUser_IdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    @Query("select favorite.phrase.id from Favorite favorite where favorite.user.id = :userId and favorite.phrase.id in :phraseIds")
    Set<Long> findFavoritedPhraseIds(@Param("userId") Long userId, @Param("phraseIds") Collection<Long> phraseIds);

    @Modifying
    int deleteByUser_IdAndPhrase_Id(Long userId, Long phraseId);
}
