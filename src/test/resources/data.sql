-- Criação da tabela users
CREATE TABLE IF NOT EXISTS users (
    id SERIAL PRIMARY KEY,
    username VARCHAR(255) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    full_name VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL,
    active BOOLEAN NOT NULL
);

-- Criação da tabela refresh_tokens
CREATE TABLE IF NOT EXISTS refresh_tokens (
    id UUID PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token VARCHAR(255) NOT NULL,
    expiry_date TIMESTAMP NOT NULL
);

-- Inserção de dados iniciais na tabela users
INSERT INTO users (username, email, password, full_name, role, active) VALUES
('admin', 'admin@email.com', '$2a$10$XDU7wRwo8BKNvDm1UZWNub2xvZ2lh', 'Administrador', 'ADMIN', true),
('user1', 'user1@email.com', '$2a$10$XDU7wRwo8BKNvDm1UZWNub2xvZ2lh', 'Usuário Normal', 'USER', true),
('inactive_user', 'inactive@email.com', '$2a$10$XDU7wRwo8BKNvDm1UZWNub2xvZ2lh', 'Usuário Inativo', 'USER', false);

-- Inserção de dados iniciais na tabela refresh_tokens
INSERT INTO refresh_tokens (id, user_id, token, expiry_date) VALUES
('a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', 1, 'token_admin', CURRENT_TIMESTAMP + INTERVAL '7 days');
