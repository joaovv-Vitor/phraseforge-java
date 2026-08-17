package com.phraseforge.phraseforge_api.phrase;

import com.phraseforge.phraseforge_api.author.Author;
import com.phraseforge.phraseforge_api.category.Category;
import com.phraseforge.phraseforge_api.common.AuditableEntity;
import com.phraseforge.phraseforge_api.tag.Tag;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "phrases")
public class Phrase extends AuditableEntity {

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id", nullable = false)
    private Author author;

    @Column(name = "year")
    @JdbcTypeCode(SqlTypes.SMALLINT)
    private Integer year;

    @Column(name = "language", nullable = false, length = 10)
    private String language;

    @Column(name = "source", length = 300)
    private String source;

    @OneToMany(mappedBy = "phrase", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PhraseCategory> phraseCategories = new ArrayList<>();

    @OneToMany(mappedBy = "phrase", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PhraseTag> phraseTags = new ArrayList<>();

    protected Phrase() {
    }

    public Phrase(String content, Author author, Integer year, String language, String source) {
        this.content = content;
        this.author = author;
        this.year = year;
        this.language = language;
        this.source = source;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Author getAuthor() {
        return author;
    }

    public void setAuthor(Author author) {
        this.author = author;
    }

    public Integer getYear() {
        return year;
    }

    public void setYear(Integer year) {
        this.year = year;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public List<PhraseCategory> getPhraseCategories() {
        return phraseCategories;
    }

    public List<PhraseTag> getPhraseTags() {
        return phraseTags;
    }

    public List<Category> getCategories() {
        return phraseCategories.stream().map(PhraseCategory::getCategory).toList();
    }

    public List<Tag> getTags() {
        return phraseTags.stream().map(PhraseTag::getTag).toList();
    }
}
