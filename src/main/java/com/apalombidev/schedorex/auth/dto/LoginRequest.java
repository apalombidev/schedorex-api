package com.apalombidev.schedorex.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(

		@NotBlank(message = "{validation.notblank}") @Email(message = "{validation.email}") String email,

		@NotBlank(message = "{validation.notblank}") String password) {
}