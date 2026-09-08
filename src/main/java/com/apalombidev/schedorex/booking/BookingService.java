package com.apalombidev.schedorex.booking;

import com.apalombidev.schedorex.availability.AvailabilitySlot;
import com.apalombidev.schedorex.availability.AvailabilitySlotRepository;
import com.apalombidev.schedorex.availability.SlotNotFoundException;
import com.apalombidev.schedorex.booking.dto.BookingCreatedResponse;
import com.apalombidev.schedorex.booking.dto.BookingRequest;
import com.apalombidev.schedorex.booking.dto.BookingResponse;
import com.apalombidev.schedorex.service.ProfessionalService;
import com.apalombidev.schedorex.service.ServiceSetupService;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * La parte più delicata dello Step 5 (v2): crea prenotazioni gestendo la
 * race condition sul vincolo UNIQUE(availability_slot_id), applica la
 * policy di cancellazione self-service (con override admin) e riceve la
 * conferma di pagamento dal flusso Stripe (Fase 4, non ancora collegato).
 */
@Service
@RequiredArgsConstructor
public class BookingService {

    private final BookingRepository bookingRepository;
    private final AvailabilitySlotRepository slotRepository;
    private final ServiceSetupService serviceSetupService;

    /**
     * Ritorna BookingCreatedResponse (non BookingResponse): unico punto in
     * cui il cancellationToken esce verso il client (v2/v7 — "mai in liste
     * successive").
     */
    @Transactional
    public BookingCreatedResponse createBooking(BookingRequest request) {
        ProfessionalService service = serviceSetupService.getServiceEntityOrThrow();

        AvailabilitySlot slot = slotRepository.findById(request.availabilitySlotId())
                .orElseThrow(() -> new SlotNotFoundException("Lo slot richiesto non esiste."));

        if (Boolean.TRUE.equals(slot.isBlocked())) {
            throw new SlotBlockedException("Lo slot richiesto non è disponibile.");
        }

        BookingStatus initialStatus = Boolean.TRUE.equals(service.getStripeEnabled())
                ? BookingStatus.PENDING
                : BookingStatus.CONFIRMED;

        Booking booking = Booking.builder()
                .service(service)
                .availabilitySlot(slot)
                .clientName(request.clientName())
                .clientEmail(request.clientEmail())
                .clientNotes(request.clientNotes())
                .status(initialStatus)
                .build();

        Booking saved;
        try {
            saved = bookingRepository.saveAndFlush(booking);
        } catch (DataIntegrityViolationException e) {
            // Vincolo UNIQUE(availability_slot_id) violato: un'altra richiesta
            // concorrente ha prenotato lo stesso slot nel frattempo (v2).
            throw new SlotAlreadyBookedException("Questo slot è appena stato preso, scegline un altro.");
        }

        return BookingCreatedResponse.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public BookingResponse getBookingByCancellationToken(UUID token) {
        Booking booking = bookingRepository.findByCancellationToken(token)
                .orElseThrow(() -> new InvalidCancellationTokenException("Link di cancellazione non valido."));
        return BookingResponse.fromEntity(booking);
    }

    /**
     * Cancellazione self-service via token (v2): rispetta la soglia
     * cancellation_deadline_hours configurata sul servizio.
     */
    @Transactional
    public BookingResponse cancelBooking(UUID token) {
        Booking booking = bookingRepository.findByCancellationToken(token)
                .orElseThrow(() -> new InvalidCancellationTokenException("Link di cancellazione non valido."));

        ProfessionalService service = booking.getService();
        Instant deadline = booking.getAvailabilitySlot().getSlotStart()
                .minusSeconds(service.getCancellationDeadlineHours() * 3600L);

        if (Instant.now().isAfter(deadline)) {
            throw new CancellationDeadlinePassedException(
                    "Non è più possibile cancellare questa prenotazione: la soglia oraria è stata superata.");
        }

        booking.setStatus(BookingStatus.CANCELLED);
        booking.setCancelledAt(Instant.now());
        Booking saved = bookingRepository.save(booking);
        return BookingResponse.fromEntity(saved);
    }

    /**
     * Override admin: nessun limite di soglia (v2 — "Admin: override sempre
     * disponibile, nessun limite"). Da collegare a un endpoint /api/admin/**
     * protetto da JWT allo Step 7/8.
     */
    @Transactional
    public BookingResponse cancelBookingAsAdmin(UUID bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new InvalidCancellationTokenException("Prenotazione non trovata."));

        booking.setStatus(BookingStatus.CANCELLED);
        booking.setCancelledAt(Instant.now());
        Booking saved = bookingRepository.save(booking);
        return BookingResponse.fromEntity(saved);
    }

    /**
     * Elenco delle prenotazioni PENDING con una sessione Stripe associata,
     * da passare al job di riconciliazione periodica (Fase 4, non ancora
     * implementata). Il repository (Step 3) espone
     * findByStatusAndStripeCheckoutSessionIdIsNotNull(BookingStatus) — un
     * metodo bulk, non un lookup per session id: il job scorrerà questa
     * lista e interrogherà Stripe una per una.
     */
    @Transactional(readOnly = true)
    public List<Booking> findPendingBookingsWithStripeSession() {
        return bookingRepository.findByStatusAndStripeCheckoutSessionIdIsNotNull(BookingStatus.PENDING);
    }

    /**
     * Conferma il pagamento per una prenotazione già risolta dal chiamante
     * (job di riconciliazione, o verifica sincrona al ritorno da Checkout —
     * in entrambi i casi il chiamante avrà già in mano l'entità Booking,
     * non solo il session id). Non fa lookup: separare "trovare la
     * prenotazione" da "confermarla" tiene questo metodo valido sia per il
     * flusso sincrono sia per quello bulk, qualunque sia il modo in cui la
     * Fase 4 deciderà di recuperare la Booking di partenza.
     */
    @Transactional
    public BookingResponse confirmPayment(Booking booking) {
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setStripePaymentStatus("paid");
        Booking saved = bookingRepository.save(booking);
        return BookingResponse.fromEntity(saved);
    }
}