package com.phraseforge.phraseforge_api.phrase;

import com.phraseforge.phraseforge_api.category.Category;
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
@Table(name = "phrase_categories",
        uniqueConstraints = @UniqueConstraint(name = "uk_phrase_categories", columnNames = {"phrase_id", "category_id"}))
public class PhraseCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "phrase_id", nullable = false)
    private Phrase phrase;

    @ManyToOne
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    protected PhraseCategory() {
    }

    public PhraseCategory(Phrase phrase, Category category) {
        this.phrase = phrase;
        this.category = category;
    }

    public Long getId() {
        return id;
    }

    public Phrase getPhrase() {
        return phrase;
    }

    public Category getCategory() {
        return category;
    }
}
