package com.apalombidev.schedorex.auth.controller;

import com.apalombidev.schedorex.auth.AuthService;
import com.apalombidev.schedorex.auth.dto.LoginRequest;
import com.apalombidev.schedorex.auth.dto.LoginResponse;
import com.apalombidev.schedorex.config.JwtProperties;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.time.Instant;

/**
 * Login admin (v2/v8). Il JWT non compare mai nel body (LoginResponse,
 * Step 4/5): viene impostato qui come cookie httpOnly — questo controller
 * è l'unico punto della pipeline che scrive header HTTP, AuthService resta
 * ignaro di HTTP.
 *
 * ⚠️ Finché lo Step 8 non aggiunge il filtro di validazione JWT, il cookie
 * viene impostato correttamente ma non è verificato da nessuna parte —
 * /api/admin/** resta raggiungibile anche senza, per via della
 * SecurityFilterChain temporanea permissiva (vedi SecurityBeansConfig).
 *
 * cookieSecure esternalizzato (app.jwt.cookie-secure, default false): true
 * in produzione, promemoria per lo Step 8/application.yaml.
 */

@RestController
@RequestMapping("/api/admin/auth")
@RequiredArgsConstructor
public class AuthAdminController {

    private static final String COOKIE_NAME = "jwt";

    private final AuthService authService;
    private final JwtProperties jwtProperties;

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request, HttpServletResponse response) {
        AuthService.AuthResult result = authService.login(request);

        Duration maxAge = Duration.between(Instant.now(), result.response().expiresAt());

        ResponseCookie cookie = ResponseCookie.from(COOKIE_NAME, result.jwt())
                .httpOnly(true)
                .secure(jwtProperties.cookieSecure())
                .sameSite("Strict")
                .path("/api/admin")
                .maxAge(maxAge)
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        return result.response();
    }

    @PostMapping("/logout")
    public void logout(HttpServletResponse response) {
        ResponseCookie expiredCookie = ResponseCookie.from(COOKIE_NAME, "")
                .httpOnly(true)
                .secure(jwtProperties.cookieSecure())
                .sameSite("Strict")
                .path("/api/admin")
                .maxAge(0)
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, expiredCookie.toString());
        response.setStatus(HttpServletResponse.SC_NO_CONTENT);
    }
}