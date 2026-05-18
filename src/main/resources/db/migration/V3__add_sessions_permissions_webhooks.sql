-- V3__add_sessions_permissions_webhooks.sql
-- Adiciona tabelas para sessões ativas e permissões finas.
-- ─────────────────────────────────────────────────────────────

-- Permissões finas por usuário
CREATE TABLE IF NOT EXISTS user_permissions (
    user_id     BIGINT       NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    permission  VARCHAR(100) NOT NULL,
    PRIMARY KEY (user_id, permission)
);
CREATE INDEX IF NOT EXISTS idx_user_permissions_user_id ON user_permissions(user_id);

-- Sessões ativas do usuário
CREATE TABLE IF NOT EXISTS user_sessions (
    id          BIGSERIAL     PRIMARY KEY,
    session_id  VARCHAR(36)   NOT NULL UNIQUE,
    user_id     BIGINT        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    ip_address  VARCHAR(50),
    user_agent  VARCHAR(1024),
    created_at  TIMESTAMP     NOT NULL DEFAULT NOW(),
    last_seen_at TIMESTAMP,
    expires_at  TIMESTAMP     NOT NULL,
    revoked     BOOLEAN       NOT NULL DEFAULT FALSE
);
CREATE INDEX IF NOT EXISTS idx_user_sessions_user_id ON user_sessions(user_id);
CREATE INDEX IF NOT EXISTS idx_user_sessions_session_id ON user_sessions(session_id);
