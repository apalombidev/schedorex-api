package com.apalombidev.schedorex.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

@ConfigurationProperties(prefix = "app.jwt")
@Validated
public record JwtProperties(
	
	@NotBlank String secret,
	@Positive long expirationMinutes,
	boolean cookieSecure
) {
}
