CREATE TABLE users
(
    id       BIGSERIAL PRIMARY KEY,
    login    VARCHAR(25) UNIQUE NOT NULL,
    password VARCHAR(60)        NOT NULL
);