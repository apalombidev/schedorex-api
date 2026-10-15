package com.apalombidev.schedorex.service;

import com.apalombidev.schedorex.service.dto.ServiceResponse;
import com.apalombidev.schedorex.service.dto.ServiceSetupRequest;

import lombok.RequiredArgsConstructor;

import java.util.Locale;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ServiceSetupService {

	private final ProfessionalServiceRepository serviceRepository;

	@Transactional(readOnly = true)
	public ServiceResponse getService() {
		return ServiceResponse.fromEntity(getServiceEntityOrThrow());
	}

	@Transactional
	public ServiceResponse setupOrUpdate(ServiceSetupRequest request) {
		ProfessionalService entity = serviceRepository.findAll().stream().findFirst()
				.orElseGet(ProfessionalService::new);

		entity.setProfessionalName(request.professionalName());
		entity.setServiceName(request.serviceName());
		entity.setDurationMinutes(request.durationMinutes());
		entity.setPriceCents(request.priceCents());
		entity.setCurrency(request.currency());
		entity.setTimezone(request.timezone());
		entity.setLanguage(request.language());
		entity.setCancellationDeadlineHours(request.cancellationDeadlineHours());
		entity.setStripeEnabled(request.stripeEnabled());

		ProfessionalService saved = serviceRepository.save(entity);
		return ServiceResponse.fromEntity(saved);
	}

	@Transactional(readOnly = true)
	public ProfessionalService getServiceEntityOrThrow() {
		return serviceRepository.findAll().stream().findFirst().orElseThrow(ServiceNotFoundException::new);
	}

	@Transactional(readOnly = true)
	public Locale getCurrentLocale() {
		return serviceRepository.findAll().stream().findFirst()
				.map(entity -> entity.getLanguage() == ServiceLanguage.EN ? Locale.ENGLISH : Locale.ITALIAN)
				.orElse(Locale.ENGLISH);
	}

}