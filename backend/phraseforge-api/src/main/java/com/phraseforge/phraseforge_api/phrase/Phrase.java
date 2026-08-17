package com.phraseforge.phraseforge_api.phrase;

import com.phraseforge.phraseforge_api.author.Author;
import com.phraseforge.phraseforge_api.common.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Temporary minimal Phrase so Author (Task 4) can compile and be validated
 * against the phrases table. Replaced by the full Phrase entity in Task 8.
 */
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

    public Author getAuthor() {
        return author;
    }
}
