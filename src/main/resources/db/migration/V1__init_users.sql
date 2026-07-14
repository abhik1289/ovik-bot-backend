-- V1__init_users.sql
-- Persistent user records for Google-authenticated accounts.

CREATE TABLE IF NOT EXISTS users (
    id          UUID         PRIMARY KEY,
    google_id   VARCHAR(128) NOT NULL,
    email       VARCHAR(320) NOT NULL,
    name        VARCHAR(256),
    picture     VARCHAR(1024),
    role        VARCHAR(32)  NOT NULL DEFAULT 'USER',
    provider    VARCHAR(32)  NOT NULL DEFAULT 'GOOGLE',
    created_at  TIMESTAMP    NOT NULL,
    updated_at  TIMESTAMP    NOT NULL
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_users_google_id ON users (google_id);
CREATE UNIQUE INDEX IF NOT EXISTS idx_users_email     ON users (email);
