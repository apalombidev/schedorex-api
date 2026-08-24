package com.apalombidev.schedorex.booking.dto;

import java.time.Instant;
import java.util.UUID;

import com.apalombidev.schedorex.booking.Booking;
import com.apalombidev.schedorex.booking.BookingStatus;

public record BookingResponse(

		UUID id, String clientName, String clientEmail, String clientNotes, BookingStatus status, Instant createdAt,
		Instant cancelledAt) {

	public static BookingResponse fromEntity(Booking entity) {
		return new BookingResponse(entity.getId(), entity.getClientName(), entity.getClientEmail(),
				entity.getClientNotes(), entity.getStatus(), entity.getCreatedAt(), entity.getCancelledAt());
	}

}
