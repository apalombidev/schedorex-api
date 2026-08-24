package com.apalombidev.schedorex.service.dto;

import java.util.UUID;

import com.apalombidev.schedorex.service.ProfessionalService;
import com.apalombidev.schedorex.service.ServiceCurrency;
import com.apalombidev.schedorex.service.ServiceLanguage;

public record ServiceResponse(

		UUID id, String professionalName, String serviceName, Integer durationMinutes, Integer priceCents,
		ServiceCurrency currency, String timezone, ServiceLanguage language, Integer cancellationDeadlineHours,
		Boolean stripeEnabled) {

	public static ServiceResponse fromEntity(ProfessionalService entity) {
		return new ServiceResponse(entity.getId(), entity.getProfessionalName(), entity.getServiceName(),
				entity.getDurationMinutes(), entity.getPriceCents(), entity.getCurrency(), entity.getTimezone(),
				entity.getLanguage(), entity.getCancellationDeadlineHours(), entity.getStripeEnabled());
	}

}
