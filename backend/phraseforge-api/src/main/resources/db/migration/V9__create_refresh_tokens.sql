CREATE TABLE refresh_tokens (
    id             BIGINT    NOT NULL AUTO_INCREMENT,
    user_id        BIGINT    NOT NULL,
    token_hash     CHAR(64)  NOT NULL,
    family_id      CHAR(36)  NOT NULL,
    expires_at     TIMESTAMP NOT NULL,
    created_at     TIMESTAMP NOT NULL,
    revoked_at     TIMESTAMP NULL,
    replaced_by_id BIGINT    NULL,
    CONSTRAINT pk_refresh_tokens PRIMARY KEY (id),
    CONSTRAINT uk_refresh_tokens_token_hash UNIQUE (token_hash),
    CONSTRAINT fk_refresh_tokens_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_refresh_tokens_replaced_by FOREIGN KEY (replaced_by_id) REFERENCES refresh_tokens (id)
);

CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens (user_id);
CREATE INDEX idx_refresh_tokens_family_id ON refresh_tokens (family_id);
CREATE INDEX idx_refresh_tokens_expires_at ON refresh_tokens (expires_at);
