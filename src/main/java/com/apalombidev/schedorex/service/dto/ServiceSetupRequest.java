package com.apalombidev.schedorex.service.dto;

import com.apalombidev.schedorex.service.ServiceCurrency;
import com.apalombidev.schedorex.service.ServiceLanguage;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ServiceSetupRequest(

		@NotBlank String professionalName,

		@NotBlank String serviceName,

		@NotNull @Positive Integer durationMinutes,

		@NotNull @Positive Integer priceCents,

		@NotNull ServiceCurrency currency,

		@NotBlank String timezone,

		@NotNull ServiceLanguage language,

		@NotNull @Positive Integer cancellationDeadlineHours,

		@NotNull Boolean StripeEnabled

) {
}
