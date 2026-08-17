CREATE TABLE phrases (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    content     TEXT         NOT NULL,
    author_id   BIGINT       NOT NULL,
    year        SMALLINT     NULL,
    language    VARCHAR(10)  NOT NULL,
    source      VARCHAR(300) NULL,
    created_at  TIMESTAMP    NOT NULL,
    updated_at  TIMESTAMP    NOT NULL,
    CONSTRAINT pk_phrases PRIMARY KEY (id),
    CONSTRAINT fk_phrases_author FOREIGN KEY (author_id) REFERENCES authors (id)
);
CREATE INDEX idx_phrases_author_id ON phrases (author_id);
