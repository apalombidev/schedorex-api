package com.apalombidev.schedorex.availability.dto;

import java.time.Instant;
import java.util.UUID;

import com.apalombidev.schedorex.availability.AvailabilitySlot;

public record AvailableSlotResponse(

		UUID id, Instant slotStart, Instant slotEnd

) {

	public static AvailableSlotResponse fromEntity(AvailabilitySlot entity) {
		return new AvailableSlotResponse(entity.getId(), entity.getSlotStart(), entity.getSlotEnd());
	}

}
