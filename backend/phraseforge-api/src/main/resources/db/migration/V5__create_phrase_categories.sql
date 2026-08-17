CREATE TABLE phrase_categories (
    id          BIGINT NOT NULL AUTO_INCREMENT,
    phrase_id   BIGINT NOT NULL,
    category_id BIGINT NOT NULL,
    CONSTRAINT pk_phrase_categories PRIMARY KEY (id),
    CONSTRAINT fk_phrase_categories_phrase FOREIGN KEY (phrase_id) REFERENCES phrases (id),
    CONSTRAINT fk_phrase_categories_category FOREIGN KEY (category_id) REFERENCES categories (id),
    CONSTRAINT uk_phrase_categories UNIQUE (phrase_id, category_id)
);
