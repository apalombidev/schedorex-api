package com.apalombidev.schedorex.auth;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * ⚠️ TEST ONE-OFF — non fa parte della suite di regressione del progetto.
 * Scopo: creare un AdminUser reale e persistente nel DB locale di sviluppo,
 * per poter testare via Postman gli endpoint /api/admin/auth/login e
 * /api/admin/auth/logout con credenziali vere.
 *
 * Deliberatamente SENZA @Transactional: a differenza di
 * SecurityIntegrationTest (dove il rollback automatico è corretto, quel
 * test non deve lasciare tracce), qui vogliamo che la riga sopravviva
 * all'esecuzione del test e resti nel DB per l'uso successivo con Postman.
 *
 * Idempotente: se l'admin esiste già (stessa email), non fa nulla invece
 * di fallire sul vincolo UNIQUE(email) — puoi rilanciarlo senza rischio.
 *
 * DA ELIMINARE dal progetto una volta creato l'admin: non è pensato per
 * restare nella codebase né per girare in CI/altri ambienti.
 */
@SpringBootTest
class AdminUserSeedTest {

    private static final String SEED_EMAIL = "admin@localhost.dev";
    private static final String SEED_PASSWORD = "LocalDevPassword123!";

    @Autowired
    private AdminUserRepository adminUserRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void seedLocalAdminUserIfNotPresent() {
        boolean alreadyExists = adminUserRepository.findByEmail(SEED_EMAIL).isPresent();

        if (alreadyExists) {
            System.out.println("Admin '" + SEED_EMAIL + "' già presente nel DB, nessuna azione.");
            return;
        }

        AdminUser admin = AdminUser.builder()
                .email(SEED_EMAIL)
                .passwordHash(passwordEncoder.encode(SEED_PASSWORD))
                .build();

        adminUserRepository.saveAndFlush(admin);

        System.out.println("Admin creato con successo:");
        System.out.println("  email:    " + SEED_EMAIL);
        System.out.println("  password: " + SEED_PASSWORD + "  (in chiaro, solo per questo output — usala in Postman)");
    }
}