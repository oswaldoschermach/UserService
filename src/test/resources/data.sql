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

-- Inserção de dados iniciais na tabela users
INSERT INTO users (username, email, password, full_name, role, active) VALUES
('admin', 'admin@email.com', '$2a$10$XDU7wRwo8BKNvDm1UZWNub2xvZ2lh', 'Administrador', 'ADMIN', true),
('user1', 'user1@email.com', '$2a$10$XDU7wRwo8BKNvDm1UZWNub2xvZ2lh', 'Usuário Normal', 'USER', true),
('inactive_user', 'inactive@email.com', '$2a$10$XDU7wRwo8BKNvDm1UZWNub2xvZ2lh', 'Usuário Inativo', 'USER', false);
