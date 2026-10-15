package com.apalombidev.schedorex.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import com.apalombidev.schedorex.config.JwtProperties;

import javax.crypto.SecretKey;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Component
public class JwtService {

	private final SecretKey key;
	private final JwtProperties properties;

	public JwtService(JwtProperties properties) {
		this.properties = properties;
		this.key = Keys.hmacShaKeyFor(properties.secret().getBytes(StandardCharsets.UTF_8));
	}

	public GeneratedToken generateToken(String subject, List<String> roles) {
		Instant now = Instant.now();
		Instant expiresAt = now.plus(properties.expirationMinutes(), ChronoUnit.MINUTES);

		String jwt = Jwts.builder().subject(subject).claim("roles", roles).issuedAt(Date.from(now))
				.expiration(Date.from(expiresAt)).signWith(key).compact();

		return new GeneratedToken(jwt, expiresAt);
	}

	@SuppressWarnings("unchecked")
	public Optional<TokenPrincipal> validateToken(String token) {
		if (token == null || token.isBlank()) {
			return Optional.empty();
		}
		try {
			Jws<Claims> parsed = Jwts.parser().verifyWith(key).build().parseSignedClaims(token);
			Claims claims = parsed.getPayload();

			String subject = claims.getSubject();
			if (subject == null || subject.isBlank()) {
				return Optional.empty();
			}

			List<String> roles = claims.get("roles", List.class);
			if (roles == null) {
				roles = List.of();
			}

			return Optional.of(new TokenPrincipal(subject, roles));
		} catch (JwtException | IllegalArgumentException e) {
			return Optional.empty();
		}
	}

	public record GeneratedToken(String token, Instant expiresAt) {
	}

	public record TokenPrincipal(String subject, List<String> roles) {
	}
}