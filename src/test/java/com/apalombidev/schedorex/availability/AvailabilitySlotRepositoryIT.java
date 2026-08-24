package com.apalombidev.schedorex.availability;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;

import com.apalombidev.schedorex.booking.Booking;
import com.apalombidev.schedorex.booking.BookingStatus;
import com.apalombidev.schedorex.service.ProfessionalService;
import com.apalombidev.schedorex.service.ServiceCurrency;
import com.apalombidev.schedorex.service.ServiceLanguage;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class AvailabilitySlotRepositoryIT {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private AvailabilitySlotRepository availabilitySlotRepository;

    @Test
    void findFreeSlots_escludeSlotBloccatiEPrenotati_restituisceSoloGliSlotVeramenteLiberi() {
        // 1. Salva un ProfessionalService (obiettivo del checkpoint)
        ProfessionalService service = ProfessionalService.builder()
                .professionalName("Mario Rossi")
                .serviceName("Consulenza")
                .durationMinutes(30)
                .priceCents(5000)
                .currency(ServiceCurrency.EUR)
                .timezone("Europe/Rome")
                .language(ServiceLanguage.IT)
                .cancellationDeadlineHours(24)
                .stripeEnabled(false)
                .build();
        entityManager.persist(service);

        Instant base = Instant.now().truncatedTo(ChronoUnit.HOURS).plus(1, ChronoUnit.DAYS);

        // 2. Genera slot: uno libero, uno bloccato manualmente, uno con booking attiva,
        //    uno con booking CANCELLED (deve comunque risultare libero)
        AvailabilitySlot slotLibero = persistSlot(service, base, false);
        AvailabilitySlot slotBloccato = persistSlot(service, base.plus(30, ChronoUnit.MINUTES), true);
        AvailabilitySlot slotPrenotato = persistSlot(service, base.plus(60, ChronoUnit.MINUTES), false);
        AvailabilitySlot slotConBookingCancellata = persistSlot(service, base.plus(90, ChronoUnit.MINUTES), false);

        persistBooking(service, slotPrenotato, BookingStatus.CONFIRMED);
        persistBooking(service, slotConBookingCancellata, BookingStatus.CANCELLED);

        entityManager.flush();
        entityManager.clear();

        // 3. Verifica che la query "slot liberi" funzioni
        List<AvailabilitySlot> liberi = availabilitySlotRepository.findFreeSlots(
                service.getId(),
                base.minus(1, ChronoUnit.HOURS),
                base.plus(1, ChronoUnit.DAYS),
                List.of(BookingStatus.PENDING, BookingStatus.CONFIRMED));

        assertThat(liberi)
                .extracting(AvailabilitySlot::getId)
                .containsExactlyInAnyOrder(slotLibero.getId(), slotConBookingCancellata.getId())
                .doesNotContain(slotBloccato.getId(), slotPrenotato.getId());
    }

    @Test
    void existsByServiceIdAndSlotStart_rilevaUnoSlotGiaEsistente_perLoSlotGenerator() {
        ProfessionalService service = ProfessionalService.builder()
                .professionalName("Anna Bianchi")
                .serviceName("Taglio")
                .durationMinutes(45)
                .priceCents(3000)
                .currency(ServiceCurrency.GBP)
                .timezone("Europe/London")
                .language(ServiceLanguage.EN)
                .cancellationDeadlineHours(12)
                .stripeEnabled(true)
                .build();
        entityManager.persist(service);

        Instant slotStart = Instant.now().truncatedTo(ChronoUnit.HOURS).plus(2, ChronoUnit.DAYS);
        persistSlot(service, slotStart, false);
        entityManager.flush();
        entityManager.clear();

        boolean esiste = availabilitySlotRepository.existsByService_IdAndSlotStart(service.getId(), slotStart);
        boolean nonEsiste = availabilitySlotRepository.existsByService_IdAndSlotStart(
                service.getId(), slotStart.plus(1, ChronoUnit.DAYS));

        assertThat(esiste).isTrue();
        assertThat(nonEsiste).isFalse();
    }

    private AvailabilitySlot persistSlot(ProfessionalService service, Instant slotStart, boolean isBlocked) {
        AvailabilitySlot slot = AvailabilitySlot.builder()
                .service(service)
                .slotStart(slotStart)
                .slotEnd(slotStart.plus(30, ChronoUnit.MINUTES))
                .isBlocked(isBlocked)
                .build();
        return entityManager.persist(slot);
    }

    private Booking persistBooking(ProfessionalService service, AvailabilitySlot slot, BookingStatus status) {
        Booking booking = Booking.builder()
                .service(service)
                .availabilitySlot(slot)
                .clientName("Cliente Test")
                .clientEmail("cliente-" + UUID.randomUUID() + "@example.com")
                .status(status)
                .build();
        return entityManager.persist(booking);
    }
}