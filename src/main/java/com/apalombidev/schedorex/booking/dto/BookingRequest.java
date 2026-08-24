package com.apalombidev.schedorex.booking.dto;

import java.util.UUID;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record BookingRequest(

		@NotNull UUID AvailabilitySlotId,

		@NotBlank String clientName,

		@NotBlank @Email String clientEmail,

		@Size(max = 500) String clientNotes

) {

}
