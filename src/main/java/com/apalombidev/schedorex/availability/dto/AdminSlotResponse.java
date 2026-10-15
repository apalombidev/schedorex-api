package com.apalombidev.schedorex.availability.dto;

import com.apalombidev.schedorex.booking.BookingStatus;

import java.time.Instant;
import java.util.UUID;

public record AdminSlotResponse(UUID id, Instant slotStart, Instant slotEnd, boolean isBlocked,
		BookingStatus bookingStatus, UUID bookingId) {
}