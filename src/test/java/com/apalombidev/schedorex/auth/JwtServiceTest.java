package com.apalombidev.schedorex.auth;

import com.apalombidev.schedorex.config.JwtProperties;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

	private static final String SECRET = "questo-e-un-secret-di-test-lungo-abbastanza-per-hs256";

	@Test
	void tokenWithoutSubjectReturnsEmptyNotException() {
		JwtProperties properties = new JwtProperties(SECRET, 60, false);
		JwtService jwtService = new JwtService(properties);

		SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
		String tokenWithoutSubject = Jwts.builder().claim("roles", List.of("ADMIN")).issuedAt(Date.from(Instant.now()))
				.expiration(Date.from(Instant.now().plusSeconds(3600))).signWith(key).compact();

		Optional<JwtService.TokenPrincipal> result = jwtService.validateToken(tokenWithoutSubject);

		assertThat(result).isEmpty();
	}

	@Test
	void validTokenRoundTripsSubjectAndRoles() {
		JwtProperties properties = new JwtProperties(SECRET, 60, false);
		JwtService jwtService = new JwtService(properties);

		String token = jwtService.generateToken("admin@example.com", List.of("ADMIN")).token();
		Optional<JwtService.TokenPrincipal> result = jwtService.validateToken(token);

		assertThat(result).isPresent();
		assertThat(result.get().subject()).isEqualTo("admin@example.com");
		assertThat(result.get().roles()).containsExactly("ADMIN");
	}
}
