CREATE TABLE favorites (
    id         BIGINT    NOT NULL AUTO_INCREMENT,
    user_id    BIGINT    NOT NULL,
    phrase_id  BIGINT    NOT NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT pk_favorites PRIMARY KEY (id),
    CONSTRAINT uk_favorites_user_phrase UNIQUE (user_id, phrase_id),
    CONSTRAINT fk_favorites_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_favorites_phrase FOREIGN KEY (phrase_id) REFERENCES phrases (id) ON DELETE CASCADE
);

CREATE INDEX idx_favorites_user_created_at ON favorites (user_id, created_at);
