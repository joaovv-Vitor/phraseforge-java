package com.phraseforge.phraseforge_api.phrase;

import com.phraseforge.phraseforge_api.tag.Tag;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "phrase_tags",
        uniqueConstraints = @UniqueConstraint(name = "uk_phrase_tags", columnNames = {"phrase_id", "tag_id"}))
public class PhraseTag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "phrase_id", nullable = false)
    private Phrase phrase;

    @ManyToOne
    @JoinColumn(name = "tag_id", nullable = false)
    private Tag tag;

    protected PhraseTag() {
    }

    public PhraseTag(Phrase phrase, Tag tag) {
        this.phrase = phrase;
        this.tag = tag;
    }

    public Long getId() {
        return id;
    }

    public Phrase getPhrase() {
        return phrase;
    }

    public Tag getTag() {
        return tag;
    }
}
