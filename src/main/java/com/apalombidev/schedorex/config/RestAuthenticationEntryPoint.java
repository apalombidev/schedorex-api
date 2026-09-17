package com.apalombidev.schedorex.config;

import java.io.IOException;
import java.time.Instant;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import com.apalombidev.schedorex.exception.ErrorResponse;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import tools.jackson.databind.ObjectMapper;

/**
 * Risposta 401 per richieste non autenticate su risorse protette. Senza
 * questa classe, Spring Security risponde di default con 403 (nessun
 * AuthenticationEntryPoint configurato) — il checkpoint v2 richiede 401.
 */

@Component
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {
	
	private final ObjectMapper objectMapper;
	
	public RestAuthenticationEntryPoint(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}
	
	@Override
	public void commence(HttpServletRequest request, HttpServletResponse response,
			AuthenticationException authException) throws IOException, ServletException {
		response.setStatus(HttpStatus.UNAUTHORIZED.value());
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		
		ErrorResponse body = new ErrorResponse(
				Instant.now(),
				HttpStatus.UNAUTHORIZED.value(),
				"Unauthorized",
				"Autenticazione richiesta: cookie assente, non valido o scaduto",
				null
		);
		
		response.getWriter().write(objectMapper.writeValueAsString(body));
	}

}
