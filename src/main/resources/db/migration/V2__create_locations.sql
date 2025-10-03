CREATE TABLE locations
(
    id        BIGSERIAL PRIMARY KEY,
    name      VARCHAR        NOT NULL,
    user_id   BIGINT         NOT NULL,
    latitude  DOUBLE PRECISION NOT NULL,
    longitude DOUBLE PRECISION NOT NULL,
    FOREIGN KEY (user_id) REFERENCES users (id)
);