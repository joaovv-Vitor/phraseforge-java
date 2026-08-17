package com.phraseforge.phraseforge_api.author;

import com.phraseforge.phraseforge_api.common.AuditableEntity;
import com.phraseforge.phraseforge_api.phrase.Phrase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "authors")
public class Author extends AuditableEntity {

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "slug", nullable = false, length = 180)
    private String slug;

    @Column(name = "birth_year")
    @JdbcTypeCode(SqlTypes.SMALLINT)
    private Integer birthYear;

    @Column(name = "death_year")
    @JdbcTypeCode(SqlTypes.SMALLINT)
    private Integer deathYear;

    @Column(name = "biography", columnDefinition = "TEXT")
    private String biography;

    @OneToMany(mappedBy = "author")
    private List<Phrase> phrases = new ArrayList<>();

    protected Author() {
    }

    public Author(String name, String slug, Integer birthYear, Integer deathYear, String biography) {
        this.name = name;
        this.slug = slug;
        this.birthYear = birthYear;
        this.deathYear = deathYear;
        this.biography = biography;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public Integer getBirthYear() {
        return birthYear;
    }

    public void setBirthYear(Integer birthYear) {
        this.birthYear = birthYear;
    }

    public Integer getDeathYear() {
        return deathYear;
    }

    public void setDeathYear(Integer deathYear) {
        this.deathYear = deathYear;
    }

    public String getBiography() {
        return biography;
    }

    public void setBiography(String biography) {
        this.biography = biography;
    }

    public List<Phrase> getPhrases() {
        return phrases;
    }
}
