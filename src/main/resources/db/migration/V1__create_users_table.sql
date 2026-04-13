-- V1__create_users_table.sql
-- Criacao inicial da tabela de usuarios
CREATE TABLE IF NOT EXISTS users (
    id          BIGSERIAL        PRIMARY KEY,
    full_name   VARCHAR(255),
    username    VARCHAR(255)     NOT NULL UNIQUE,
    email       VARCHAR(255)     NOT NULL UNIQUE,
    password    VARCHAR(255)     NOT NULL,
    role        VARCHAR(20)      NOT NULL DEFAULT 'USER',
    active      BOOLEAN          NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP        NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP        NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_users_role CHECK (role IN ('USER', 'ADMIN', 'MODERATOR'))
);
CREATE INDEX IF NOT EXISTS idx_users_username ON users(username);
CREATE INDEX IF NOT EXISTS idx_users_email    ON users(email);
CREATE INDEX IF NOT EXISTS idx_users_fullname ON users(full_name);