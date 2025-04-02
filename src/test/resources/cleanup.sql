-- Verifica se a tabela refresh_tokens existe antes de tentar truncá-la
DO $$ 
BEGIN
    IF EXISTS (SELECT 1 FROM pg_tables WHERE schemaname = 'public' AND tablename = 'refresh_tokens') THEN
        TRUNCATE TABLE refresh_tokens CASCADE;
    END IF;
END $$;

-- Limpa a tabela users
TRUNCATE TABLE users CASCADE;
