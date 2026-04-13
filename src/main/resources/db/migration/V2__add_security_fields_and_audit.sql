-- V2__add_security_fields_and_audit.sql
-- Adiciona campos de seguranca em users, tabela de reset de senha e auditoria
-- ─────────────────────────────────────────────────────────────
-- Campos de seguranca na tabela users
-- ─────────────────────────────────────────────────────────────
ALTER TABLE users
    ADD COLUMN IF NOT EXISTS last_login_at       TIMESTAMP,
    ADD COLUMN IF NOT EXISTS failed_attempts     INT          NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS locked_until        TIMESTAMP;
-- ─────────────────────────────────────────────────────────────
-- Tokens de recuperacao de senha
-- ─────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS password_reset_tokens (
    id          BIGSERIAL     PRIMARY KEY,
    user_id     BIGINT        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token       VARCHAR(255)  NOT NULL UNIQUE,
    expires_at  TIMESTAMP     NOT NULL,
    used        BOOLEAN       NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMP     NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_prt_token   ON password_reset_tokens(token);
CREATE INDEX IF NOT EXISTS idx_prt_user_id ON password_reset_tokens(user_id);
-- ─────────────────────────────────────────────────────────────
-- Log de auditoria
-- ─────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS audit_log (
    id          BIGSERIAL     PRIMARY KEY,
    user_id     BIGINT        REFERENCES users(id) ON DELETE SET NULL,
    action      VARCHAR(100)  NOT NULL,
    entity      VARCHAR(100),
    entity_id   BIGINT,
    detail      TEXT,
    ip_address  VARCHAR(50),
    created_at  TIMESTAMP     NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_audit_user_id   ON audit_log(user_id);
CREATE INDEX IF NOT EXISTS idx_audit_action    ON audit_log(action);
CREATE INDEX IF NOT EXISTS idx_audit_entity    ON audit_log(entity, entity_id);
CREATE INDEX IF NOT EXISTS idx_audit_created   ON audit_log(created_at);