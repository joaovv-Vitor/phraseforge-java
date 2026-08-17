package com.phraseforge.phraseforge_api.phrase;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PhraseCategoryRepository extends JpaRepository<PhraseCategory, Long> {

    @Query("select pc.category.id, count(pc) from PhraseCategory pc group by pc.category.id")
    List<Object[]> findPhraseCounts();

    @Modifying
    @Query("delete from PhraseCategory pc where pc.category.id = :categoryId")
    void deleteByCategoryId(@Param("categoryId") Long categoryId);
}
