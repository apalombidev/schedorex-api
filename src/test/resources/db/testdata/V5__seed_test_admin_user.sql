-- Seed dell'AdminUser per i test end-to-end (Step 9). Applicata SOLO quando
-- il profilo Spring attivo è 'test' (vedi application-test.yaml,
-- spring.flyway.locations) — mai in dev/produzione, dove Flyway legge solo
-- classpath:db/migration.
--
-- Credenziali di test (MAI usare fuori dai test):
--   email:    admin@test.local
--   password: admin123
--
-- Hash generato con BCryptPasswordEncoder standard (10 round). Il prefisso
-- $2b$ è compatibile con Spring Security BCryptPasswordEncoder (supporta
-- $2a$/$2b$/$2y$ in lettura) — verificato: login riuscito in EndToEndFlowTest
-- e SecurityEnforcementTest (Step 9).
--
-- Nessun ON CONFLICT necessario: eseguita da Testcontainers su un DB
-- effimero e sempre vuoto a ogni run, non su un DB persistente riusato
-- (a differenza dello script di seed manuale per il DB locale di sviluppo).
INSERT INTO admin_users (id, email, password_hash, created_at)
VALUES (
    gen_random_uuid(),
    'admin@localhost.dev',
    '$2b$10$iOMxyJAaAlZmSzh.kLNQj.Fylyf8wgy7Y.QyCBoRMxy1XKdmo1yL.',
    now()
);