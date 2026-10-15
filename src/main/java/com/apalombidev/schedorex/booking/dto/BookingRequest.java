package com.apalombidev.schedorex.booking.dto;

import java.util.UUID;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record BookingRequest(

		@NotNull(message = "{validation.notnull}") UUID availabilitySlotId,

		@NotBlank(message = "{validation.notblank}") @Size(max = 100, message = "{validation.size}") String clientName,

		@NotBlank(message = "{validation.notblank}") @Email(message = "{validation.email}") @Size(max = 100, message = "{validation.size}") String clientEmail,

		@Size(max = 500, message = "{validation.size}") String clientNotes) {
}
