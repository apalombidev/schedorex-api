package com.apalombidev.schedorex.auth;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;

/**
 * Generazione (e, dallo Step 8, validazione) del JWT admin. Isolata in una
 * classe propria — non dentro AuthService — perché il filtro di sicurezza
 * dello Step 8 dovrà validare lo stesso token su ogni richiesta a
 * /api/admin/**: la logica di firma/parsing va condivisa, non duplicata.
 *
 * generateToken() ritorna un GeneratedToken (token + expiresAt) invece del
 * solo String: AuthService deve costruire LoginResponse.expiresAt con lo
 * stesso istante effettivamente firmato nel JWT, non uno ricalcolato a
 * parte con un secondo Instant.now().
 *
 * Secret fornito via variabile d'ambiente JWT_SECRET (stesso pattern di
 * DB_* in application.yaml, Step 0), minimo 256 bit per HS256. Non ancora
 * aggiunto ad application.yaml — promemoria per lo Step 8.
 */
@Component
public class JwtService {

    private final SecretKey key;
    private final long expirationMinutes;

    public JwtService(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.expiration-minutes:60}") long expirationMinutes) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes());
        this.expirationMinutes = expirationMinutes;
    }

    public GeneratedToken generateToken(String subjectEmail) {
        Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds(expirationMinutes * 60);

        String jwt = Jwts.builder()
                .subject(subjectEmail)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                .signWith(key)
                .compact();

        return new GeneratedToken(jwt, expiresAt);
    }

    public record GeneratedToken(String token, Instant expiresAt) {}
}