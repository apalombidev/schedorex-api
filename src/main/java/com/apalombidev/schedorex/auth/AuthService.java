package com.apalombidev.schedorex.auth;

import com.apalombidev.schedorex.auth.dto.LoginRequest;
import com.apalombidev.schedorex.auth.dto.LoginResponse;

import lombok.RequiredArgsConstructor;

import java.util.List;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

	private final AdminUserRepository adminUserRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtService jwtService;

	@Transactional(readOnly = true)
	public AuthResult login(LoginRequest request) {
		AdminUser admin = adminUserRepository.findByEmail(request.email())
				.orElseThrow(InvalidCredentialsException::new);

		if (!passwordEncoder.matches(request.password(), admin.getPasswordHash())) {
			throw new InvalidCredentialsException();
		}

		JwtService.GeneratedToken generated = jwtService.generateToken(admin.getEmail(), List.of("ADMIN"));
		LoginResponse response = new LoginResponse(admin.getEmail(), generated.expiresAt());
		return new AuthResult(response, generated.token());
	}

	public record AuthResult(LoginResponse response, String jwt) {
	}
}