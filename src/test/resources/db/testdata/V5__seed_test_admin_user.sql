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
-- $2a$/$2b$/$2y$ in lettura) — verificare comunque al primo run, è
-- un'assunzione basata sul comportamento storico di jBCrypt, non testata
-- contro la versione di Spring Security 7.x effettivamente in uso.
INSERT INTO admin_users (id, email, password_hash, created_at)
VALUES (
    gen_random_uuid(),
    'admin@test.local',
    '$2b$10$iOMxyJAaAlZmSzh.kLNQj.Fylyf8wgy7Y.QyCBoRMxy1XKdmo1yL.',
    now()
    ON CONFLICT (email) DO NOTHING;  -- idempotente: rieseguibile senza errori
);