package com.apalombidev.schedorex.availability.dto;

import com.apalombidev.schedorex.booking.BookingStatus;

import java.time.Instant;
import java.util.UUID;

/**
 * Vista admin di uno slot (Step 7 — rimandato da v8). A differenza di
 * AvailableSlotResponse (vista pubblica, solo id + orario), qui compaiono
 * anche isBlocked e lo stato dell'eventuale prenotazione — informazioni
 * che un cliente anonimo non deve vedere, ma che l'admin deve vedere per
 * gestire manualmente il calendario.
 *
 * bookingStatus/bookingId sono null se lo slot non ha una prenotazione
 * attiva (PENDING/CONFIRMED) — non un errore, è lo stato "slot libero".
 */
public record AdminSlotResponse(
        UUID id,
        Instant slotStart,
        Instant slotEnd,
        boolean isBlocked,
        BookingStatus bookingStatus,
        UUID bookingId
) {
}