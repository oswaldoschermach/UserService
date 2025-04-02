INSERT INTO users (id, username, email, password, full_name, role, active) VALUES
(1, 'admin', 'admin@email.com', '$2a$10$XDU7wRwo8BKNvDm1UZWNub2xvZ2lh', 'Administrador', 'ADMIN', true),
(2, 'user1', 'user1@email.com', '$2a$10$XDU7wRwo8BKNvDm1UZWNub2xvZ2lh', 'Usuário Normal', 'USER', true),
(3, 'inactive_user', 'inactive@email.com', '$2a$10$XDU7wRwo8BKNvDm1UZWNub2xvZ2lh', 'Usuário Inativo', 'USER', false);

INSERT INTO refresh_tokens (id, user_id, token, expiry_date) VALUES
('a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', 1, 'token_admin', CURRENT_TIMESTAMP + INTERVAL '7 days');