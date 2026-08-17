package com.phraseforge.phraseforge_api.phrase;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PhraseTagRepository extends JpaRepository<PhraseTag, Long> {

    @Modifying
    @Query("delete from PhraseTag pt where pt.tag.id = :tagId")
    void deleteByTagId(@Param("tagId") Long tagId);
}
