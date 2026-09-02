package com.apalombidev.schedorex.service.dto;

import java.time.DateTimeException;
import java.time.ZoneId;

import com.apalombidev.schedorex.service.ServiceCurrency;
import com.apalombidev.schedorex.service.ServiceLanguage;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record ServiceSetupRequest(

		@NotBlank @Size(max = 100)String professionalName,

		@NotBlank  @Size(max = 100) String serviceName,

		@NotNull @Positive Integer durationMinutes,

		@NotNull @Positive Integer priceCents,

		@NotNull ServiceCurrency currency,

		@NotBlank @Size(max = 50) String timezone,

		@NotNull ServiceLanguage language,

		@NotNull @Positive Integer cancellationDeadlineHours,

		@NotNull Boolean stripeEnabled

) {
	
	public ServiceSetupRequest {
        if (timezone != null && !timezone.isBlank()) {
            try {
                ZoneId.of(timezone);
            } catch (DateTimeException e) {
                throw new IllegalArgumentException("Fuso orario non valido: " + timezone, e);
            }
        }
    }
}
