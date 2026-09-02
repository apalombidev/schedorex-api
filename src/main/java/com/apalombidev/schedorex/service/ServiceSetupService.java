package com.apalombidev.schedorex.service;

import com.apalombidev.schedorex.service.dto.ServiceResponse;
import com.apalombidev.schedorex.service.dto.ServiceSetupRequest;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Gestisce il servizio/professionista, che nello scope attuale (v2) è un
 * singleton: un'istanza self-hosted ha zero o un solo record in `services`.
 * ServiceSetupRequest non ha id (v7): setupOrUpdate() fa quindi da upsert.
 *
 * getServiceEntityOrThrow() è il punto d'accesso usato da AvailabilityService
 * e BookingService per ottenere l'entità completa (serve timezone,
 * durationMinutes, cancellationDeadlineHours, stripeEnabled), non il DTO.
 */
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
        ProfessionalService entity = serviceRepository.findAll().stream()
                .findFirst()
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
        return serviceRepository.findAll().stream()
                .findFirst()
                .orElseThrow(() -> new ServiceNotFoundException(
                        "Nessun servizio configurato. Completare il setup iniziale prima di procedere."));
    }
}