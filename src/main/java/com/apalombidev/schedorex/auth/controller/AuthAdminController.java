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

		ResponseCookie cookie = ResponseCookie.from(COOKIE_NAME, result.jwt()).httpOnly(true)
				.secure(jwtProperties.cookieSecure()).sameSite("Strict").path("/api/admin").maxAge(maxAge).build();

		response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

		return result.response();
	}

	@PostMapping("/logout")
	public void logout(HttpServletResponse response) {
		ResponseCookie expiredCookie = ResponseCookie.from(COOKIE_NAME, "").httpOnly(true)
				.secure(jwtProperties.cookieSecure()).sameSite("Strict").path("/api/admin").maxAge(0).build();

		response.addHeader(HttpHeaders.SET_COOKIE, expiredCookie.toString());
		response.setStatus(HttpServletResponse.SC_NO_CONTENT);
	}
}