package com.apalombidev.schedorex.config;

import com.apalombidev.schedorex.exception.ErrorResponse;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.time.Instant;

/**
 * 403 per un'autenticazione valida ma priva del ruolo richiesto
 * (hasRole("ADMIN") fallito, o violazione CSRF — Spring Security instrada
 * anche i fallimenti CSRF qui). Prima di questo fix, con .authenticated()
 * al posto di .hasRole("ADMIN"), questa classe non aveva scenari reali da
 * gestire: ora ne ha almeno due, quindi non è più codice morto.
 */
@Component
public class RestAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    public RestAccessDeniedHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                        AccessDeniedException accessDeniedException) throws IOException {
        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        ErrorResponse body = new ErrorResponse(
                Instant.now(),
                HttpStatus.FORBIDDEN.value(),
                "Forbidden",
                "Non hai i permessi necessari per accedere a questa risorsa.",
                null
        );

        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}