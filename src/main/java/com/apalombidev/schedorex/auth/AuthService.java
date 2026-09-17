package com.apalombidev.schedorex.auth;

import com.apalombidev.schedorex.auth.dto.LoginRequest;
import com.apalombidev.schedorex.auth.dto.LoginResponse;

import lombok.RequiredArgsConstructor;

import java.util.List;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Login dell'unico admin_user per istanza (v2 — nessun account cliente).
 * login() ritorna un AuthResult che separa il body pubblico (LoginResponse,
 * senza JWT nel body — v7) dal token: il controller (Step 8) imposterà il
 * JWT come cookie httpOnly via Set-Cookie, non lo serializzerà.
 *
 * LoginResponse.expiresAt (confermato da step4-dto-mapping-codice.md) viene
 * letto dallo stesso GeneratedToken prodotto da JwtService, non ricalcolato
 * qui: evita che i due Instant (quello firmato nel JWT e quello nel body)
 * possano disallinearsi per un arrotondamento o una chiamata a Instant.now()
 * ripetuta a pochi millisecondi di distanza.
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final AdminUserRepository adminUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Transactional(readOnly = true)
    public AuthResult login(LoginRequest request) {
        AdminUser admin = adminUserRepository.findByEmail(request.email())
                .orElseThrow(() -> new InvalidCredentialsException("Credenziali non valide."));

        if (!passwordEncoder.matches(request.password(), admin.getPasswordHash())) {
            throw new InvalidCredentialsException("Credenziali non valide.");
        }

        JwtService.GeneratedToken generated = jwtService.generateToken(admin.getEmail(), List.of("ADMIN"));
        LoginResponse response = new LoginResponse(admin.getEmail(), generated.expiresAt());
        return new AuthResult(response, generated.token());
    }

    public record AuthResult(LoginResponse response, String jwt) {}
}