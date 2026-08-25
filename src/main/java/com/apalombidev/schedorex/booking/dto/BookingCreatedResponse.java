package com.apalombidev.schedorex.booking.dto;

import java.time.Instant;
import java.util.UUID;

import com.apalombidev.schedorex.booking.Booking;
import com.apalombidev.schedorex.booking.BookingStatus;

public record BookingCreatedResponse(

		UUID id, String clientName, String clientEmail, String clientNotes, BookingStatus status,
		UUID cancellationToken, Instant slotStart, Instant slotEnd, Instant createdAt

) {

	public static BookingCreatedResponse fromEntity(Booking entity) {
		return new BookingCreatedResponse(entity.getId(), entity.getClientName(), entity.getClientEmail(),
				entity.getClientNotes(), entity.getStatus(), entity.getCancellationToken(),
				entity.getAvailabilitySlot().getSlotStart(), entity.getAvailabilitySlot().getSlotEnd(),
				entity.getCreatedAt());
	}

}
