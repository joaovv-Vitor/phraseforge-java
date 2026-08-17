CREATE TABLE authors (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    name        VARCHAR(150) NOT NULL,
    slug        VARCHAR(180) NOT NULL,
    birth_year  SMALLINT     NULL,
    death_year  SMALLINT     NULL,
    biography   TEXT         NULL,
    created_at  TIMESTAMP    NOT NULL,
    updated_at  TIMESTAMP    NOT NULL,
    CONSTRAINT pk_authors PRIMARY KEY (id),
    CONSTRAINT uk_authors_slug UNIQUE (slug)
);
