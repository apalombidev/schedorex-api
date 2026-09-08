package com.apalombidev.schedorex.availability;

import com.apalombidev.schedorex.availability.dto.AdminSlotResponse;
import com.apalombidev.schedorex.availability.dto.AvailabilityGenerationRequest;
import com.apalombidev.schedorex.availability.dto.AvailableSlotResponse;
import com.apalombidev.schedorex.booking.Booking;
import com.apalombidev.schedorex.booking.BookingRepository;
import com.apalombidev.schedorex.booking.BookingStatus;
import com.apalombidev.schedorex.dashboard.dto.DashboardSummaryResponse;
import com.apalombidev.schedorex.service.ProfessionalService;
import com.apalombidev.schedorex.service.ServiceSetupService;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Genera slot di disponibilità in blocco (pannello admin "Genera
 * disponibilità", v2) e risponde alla query pubblica "slot liberi".
 */
@Service
@RequiredArgsConstructor
public class AvailabilityService {

    private final AvailabilitySlotRepository slotRepository;
    private final ServiceSetupService serviceSetupService;
    private final BookingRepository bookingRepository;

    /**
     * Ritorna un GenerationResult (record interno, non ancora un DTO REST —
     * rimandato allo Step 7 in v8) con il conteggio di slot creati/scartati.
     */
    @Transactional
    public GenerationResult generateSlots(AvailabilityGenerationRequest request) {
        validateRequest(request);

        ProfessionalService service = serviceSetupService.getServiceEntityOrThrow();
        ZoneId zone = ZoneId.of(service.getTimezone());
        int durationMinutes = service.getDurationMinutes();

        Set<DayOfWeek> activeDays = EnumSet.copyOf(request.activeDays());

        List<AvailabilitySlot> toCreate = new ArrayList<>();
        int skipped = 0;

        for (LocalDate date = request.startDate(); !date.isAfter(request.endDate()); date = date.plusDays(1)) {
            if (!activeDays.contains(date.getDayOfWeek())) {
                continue;
            }

            LocalTime cursor = request.openingTime();
            while (!cursor.plusMinutes(durationMinutes).isAfter(request.closingTime())) {
                LocalTime slotEndTime = cursor.plusMinutes(durationMinutes);

                if (overlapsLunchBreak(cursor, slotEndTime, request)) {
                    cursor = slotEndTime;
                    continue;
                }

                Instant slotStart = LocalDateTime.of(date, cursor).atZone(zone).toInstant();
                Instant slotEnd = LocalDateTime.of(date, slotEndTime).atZone(zone).toInstant();

                if (slotRepository.existsByService_IdAndSlotStart(service.getId(), slotStart)) {
                    // Rigenerazione idempotente (v2): slot già esistente, scartato senza errore.
                    skipped++;
                } else {
                    toCreate.add(AvailabilitySlot.builder()
                            .service(service)
                            .slotStart(slotStart)
                            .slotEnd(slotEnd)
                            .isBlocked(false)
                            .build());
                }

                cursor = slotEndTime;
            }
        }

        List<AvailabilitySlot> saved = slotRepository.saveAll(toCreate);
        return new GenerationResult(saved.size(), skipped);
    }

    /**
     * Query pubblica "slot liberi per servizio in un range di date"
     * (Step 9, punto 3 del flusso end-to-end). Delega interamente a
     * findFreeSlots() (Step 3/v6, JPQL con NOT EXISTS), passando gli status
     * attivi come parametro bind — mai come letterali (bug già risolto in v6).
     */
    @Transactional(readOnly = true)
    public List<AvailableSlotResponse> getAvailableSlots(Instant from, Instant to) {
        ProfessionalService service = serviceSetupService.getServiceEntityOrThrow();
        return slotRepository.findFreeSlots(service.getId(), from, to, BookingStatus.activeStatuses())
                .stream()
                .map(AvailableSlotResponse::fromEntity)
                .toList();
    }

    private boolean overlapsLunchBreak(LocalTime slotStart, LocalTime slotEnd, AvailabilityGenerationRequest request) {
        if (request.lunchBreakStart() == null || request.lunchBreakEnd() == null) {
            return false;
        }
        return slotStart.isBefore(request.lunchBreakEnd()) && slotEnd.isAfter(request.lunchBreakStart());
    }

    /**
     * Validazione incrociata rimandata dallo Step 4 (v8, promemoria Step 5):
     * endDate >= startDate, openingTime < closingTime, pausa pranzo (se
     * presente) contenuta nell'orario di apertura.
     */
    private void validateRequest(AvailabilityGenerationRequest request) {
        if (request.endDate().isBefore(request.startDate())) {
            throw new InvalidAvailabilityRangeException("La data di fine non può precedere la data di inizio.");
        }
        if (!request.openingTime().isBefore(request.closingTime())) {
            throw new InvalidAvailabilityRangeException("L'orario di apertura deve precedere l'orario di chiusura.");
        }
        if (request.lunchBreakStart() != null && request.lunchBreakEnd() != null) {
            if (!request.lunchBreakStart().isBefore(request.lunchBreakEnd())) {
                throw new InvalidAvailabilityRangeException("L'inizio della pausa pranzo deve precedere la fine.");
            }
            if (request.lunchBreakStart().isBefore(request.openingTime())
                    || request.lunchBreakEnd().isAfter(request.closingTime())) {
                throw new InvalidAvailabilityRangeException(
                        "La pausa pranzo deve essere contenuta nell'orario di apertura.");
            }
        }
    }
    
    
    /** Non è un DTO REST: il conteggio viene già calcolato qui per lo Step 7. */
    public record GenerationResult(int created, int skipped) {}
    
    /**
     * Vista admin degli slot in un range, con stato prenotazione se presente
     * (Step 7 — rimandato da v8). Nessuna esclusione degli slot bloccati: a
     * differenza di getAvailableSlots() (vista pubblica), qui l'admin deve
     * vedere tutto, incluso ciò che ha bloccato manualmente.
     */
    @Transactional(readOnly = true)
    public List<AdminSlotResponse> getAdminSlots(Instant from, Instant to) {
        ProfessionalService service = serviceSetupService.getServiceEntityOrThrow();
        return slotRepository.findAdminSlotsWithBookingStatus(
                        service.getId(), from, to, BookingStatus.activeStatuses())
                .stream()
                .map(row -> {
                    AvailabilitySlot slot = (AvailabilitySlot) row[0];
                    Booking booking = (Booking) row[1];
                    return new AdminSlotResponse(
                            slot.getId(),
                            slot.getSlotStart(),
                            slot.getSlotEnd(),
                            Boolean.TRUE.equals(slot.isBlocked()),
                            booking != null ? booking.getStatus() : null,
                            booking != null ? booking.getId() : null);
                })
                .toList();
    }
  

    /**
     * Blocco/sblocco manuale di uno slot (pannello admin, v2: "pausa, ferie").
     * Non tocca eventuali prenotazioni esistenti: blocca solo la disponibilità
     * per NUOVE prenotazioni (BookingService.createBooking() controlla
     * isBlocked, Step 5).
     */
    @Transactional
    public AdminSlotResponse setSlotBlocked(UUID slotId, boolean blocked) {
        AvailabilitySlot slot = slotRepository.findById(slotId)
                .orElseThrow(() -> new SlotNotFoundException("Slot non trovato."));
        slot.setBlocked(blocked);
        AvailabilitySlot saved = slotRepository.save(slot);

        Booking activeBooking = bookingRepository
                .findFirstByAvailabilitySlot_IdAndStatusIn(saved.getId(), BookingStatus.activeStatuses())
                .orElse(null);

        return new AdminSlotResponse(
                saved.getId(),
                saved.getSlotStart(),
                saved.getSlotEnd(),
                Boolean.TRUE.equals(saved.isBlocked()),
                activeBooking != null ? activeBooking.getStatus() : null,
                activeBooking != null ? activeBooking.getId() : null);
    }

    /**
     * Riepilogo per il banner dashboard (v2: "meno di 7 giorni di slot futuri
     * generati"). lowAvailabilityWarning = true se non esiste nessuno slot
     * futuro, oppure se lo slot futuro più lontano è entro 7 giorni da ora.
     */
    @Transactional(readOnly = true)
    public DashboardSummaryResponse getDashboardSummary() {
        ProfessionalService service = serviceSetupService.getServiceEntityOrThrow();
        Instant now = Instant.now();
        Instant latest = slotRepository.findLatestSlotStart(service.getId()).orElse(null);
        boolean warning = (latest == null) || latest.isBefore(now.plus(Duration.ofDays(7)));
        return new DashboardSummaryResponse(warning, latest, now);
    }
    
    
    
    
}