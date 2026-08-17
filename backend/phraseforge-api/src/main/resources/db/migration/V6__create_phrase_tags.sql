CREATE TABLE phrase_tags (
    id        BIGINT NOT NULL AUTO_INCREMENT,
    phrase_id BIGINT NOT NULL,
    tag_id    BIGINT NOT NULL,
    CONSTRAINT pk_phrase_tags PRIMARY KEY (id),
    CONSTRAINT fk_phrase_tags_phrase FOREIGN KEY (phrase_id) REFERENCES phrases (id),
    CONSTRAINT fk_phrase_tags_tag FOREIGN KEY (tag_id) REFERENCES tags (id),
    CONSTRAINT uk_phrase_tags UNIQUE (phrase_id, tag_id)
);
