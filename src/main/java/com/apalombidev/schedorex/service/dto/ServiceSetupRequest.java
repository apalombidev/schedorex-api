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

        @NotBlank(message = "{validation.notblank}")
        @Size(max = 100, message = "{validation.size}")
        String professionalName,

        @NotBlank(message = "{validation.notblank}")
        @Size(max = 100, message = "{validation.size}")
        String serviceName,

        @NotNull(message = "{validation.notnull}")
        @Positive(message = "{validation.positive}")
        Integer durationMinutes,

        @NotNull(message = "{validation.notnull}")
        @Positive(message = "{validation.positive}")
        Integer priceCents,

        @NotNull(message = "{validation.notnull}")
        ServiceCurrency currency,

        @NotBlank(message = "{validation.notblank}")
        @Size(max = 50, message = "{validation.size}")
        String timezone,

        @NotNull(message = "{validation.notnull}")
        ServiceLanguage language,

        @NotNull(message = "{validation.notnull}")
        @Positive(message = "{validation.positive}")
        Integer cancellationDeadlineHours,

        @NotNull(message = "{validation.notnull}")
        Boolean stripeEnabled
) {
    public ServiceSetupRequest {
        if (timezone != null && !timezone.isBlank()) {
            try {
                ZoneId.of(timezone);
            } catch (DateTimeException e) {
                throw new InvalidTimezoneException(timezone);
            }
        }
    }
}