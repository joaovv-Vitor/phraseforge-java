package com.phraseforge.phraseforge_api.tag;

import com.phraseforge.phraseforge_api.common.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "tags")
public class Tag extends AuditableEntity {

    @Column(name = "name", nullable = false, length = 50)
    private String name;

    protected Tag() {
    }

    public Tag(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
