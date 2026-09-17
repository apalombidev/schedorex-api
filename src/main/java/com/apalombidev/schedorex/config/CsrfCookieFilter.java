package com.apalombidev.schedorex.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;

/**
 * Forza la risoluzione "eager" del CsrfToken differito su ogni richiesta.
 *
 * Senza questo filtro, il cookie XSRF-TOKEN non viene MAI scritto: Spring
 * Security 6+/7 carica il CsrfToken in modo differito (deferred, per
 * mitigazione BREACH) — il token esiste come "supplier" nella request, ma
 * CookieCsrfTokenRepository.saveToken() scrive davvero il cookie solo
 * quando qualcosa chiama .getToken() su quel supplier. Nessun controller
 * di questa app lo fa mai spontaneamente (non ci sono form Thymeleaf che
 * lo risolverebbero automaticamente), quindi senza questo filtro il
 * client (Postman, o un futuro frontend SPA) non riceve mai il cookie da
 * cui leggere il valore per l'header X-XSRF-TOKEN.
 */
public class CsrfCookieFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                     @NonNull HttpServletResponse response,
                                     @NonNull FilterChain filterChain) throws ServletException, IOException {
        CsrfToken csrfToken = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
        if (csrfToken != null) {
            csrfToken.getToken();
        }
        filterChain.doFilter(request, response);
    }
}