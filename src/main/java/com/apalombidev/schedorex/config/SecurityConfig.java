package com.apalombidev.schedorex.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.DefaultSecurityFilterChain;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.session.NullAuthenticatedSessionStrategy;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.session.SessionManagementFilter;

import com.apalombidev.schedorex.auth.JwtAuthenticationFilter;

import lombok.RequiredArgsConstructor;

/**
 * Configurazione di sicurezza definitiva (Step 8), sostituisce la
 * SecurityFilterChain temporanea permissiva introdotta allo Step 7.
 *
 * Regole per prefisso (v2/v8): /api/public/** e /api/admin/auth/login
 * sempre aperti; il resto di /api/admin/** richiede autenticazione JWT
 * (cookie httpOnly, validato da JwtAuthenticationFilter).
 *
 * STATELESS: nessuna sessione HTTP server-side, l'unico stato di
 * autenticazione è il JWT nel cookie, rivalidato a ogni richiesta.
 *
 * CSRF disabilitato: il cookie jwt è SameSite=Strict (AuthAdminController,
 * Step 7), che impedisce al browser di inviarlo su richieste cross-site —
 * il vettore classico di un attacco CSRF. Decisione esplicita, non
 * un'omissione: da rivalutare se in futuro il cookie dovesse diventare
 * SameSite=Lax/None per qualche integrazione cross-origin.
 */


@Configuration
@EnableConfigurationProperties(JwtProperties.class)
@RequiredArgsConstructor
public class SecurityConfig {
	
	private final JwtAuthenticationFilter jwtAuthenticationFilter;
	private final RestAuthenticationEntryPoint restAuthenticationEntryPoint;
	private final RestAccessDeniedHandler restAccessDeniedHandler;
	
	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}
	
	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
	    http
	        .csrf(csrf -> csrf
	                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
	                .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler())
	                .ignoringRequestMatchers("/api/public/**", "/api/admin/auth/login", "/api/admin/auth/logout")
	        )
	        .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
	        .exceptionHandling(ex -> ex
	                .authenticationEntryPoint(restAuthenticationEntryPoint)
	                .accessDeniedHandler(restAccessDeniedHandler)
	        )
	        .authorizeHttpRequests(auth -> auth
	                .requestMatchers("/api/public/**").permitAll()
	                .requestMatchers("/api/admin/auth/login").permitAll()
	                .requestMatchers("/api/admin/auth/logout").permitAll()
	                .requestMatchers("/api/admin/**").hasRole("ADMIN")
	                .anyRequest().denyAll()
	        )
	        .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
	        .addFilterAfter(new CsrfCookieFilter(), CsrfFilter.class);

	    DefaultSecurityFilterChain chain = (DefaultSecurityFilterChain) http.build();
	    // In uno setup completamente STATELESS non c'è nessuna sessione da gestire:
	    // SessionManagementFilter comunque scatta ad ogni richiesta (nessuna sessione
	    // su cui "ricordarsi" di averlo già fatto) e invoca CompositeSessionAuthenticationStrategy
	    // → CsrfAuthenticationStrategy, cancellando XSRF-TOKEN dopo ogni richiesta admin
	    // andata a buon fine. Rimosso esplicitamente: CsrfConfigurer lo aggiunge alla
	    // strategia comunque, non è disattivabile via .sessionAuthenticationStrategy(...).
	    chain.getFilters().removeIf(SessionManagementFilter.class::isInstance);
	    return chain;
	}
}
