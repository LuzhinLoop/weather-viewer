CREATE TABLE sessions
(
    id         UUID PRIMARY KEY,
    user_id    BIGINT                      NOT NULL,
    expires_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    FOREIGN KEY (user_id) REFERENCES users (id)
);