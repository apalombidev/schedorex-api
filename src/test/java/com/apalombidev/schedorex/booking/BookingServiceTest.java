package com.apalombidev.schedorex.booking;

import com.apalombidev.schedorex.availability.AvailabilitySlot;
import com.apalombidev.schedorex.availability.AvailabilitySlotRepository;
import com.apalombidev.schedorex.booking.dto.BookingRequest;
import com.apalombidev.schedorex.service.ProfessionalService;
import com.apalombidev.schedorex.service.ServiceSetupService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

	@Mock
	private BookingRepository bookingRepository;
	@Mock
	private AvailabilitySlotRepository slotRepository;
	@Mock
	private ServiceSetupService serviceSetupService;

	@InjectMocks
	private BookingService bookingService;

	private ProfessionalService service;
	private AvailabilitySlot slot;
	private UUID slotId;

	@BeforeEach
	void setUp() {
		slotId = UUID.randomUUID();
		service = ProfessionalService.builder().id(UUID.randomUUID()).stripeEnabled(false).build();
		slot = AvailabilitySlot.builder().id(slotId).service(service).slotStart(Instant.now().plusSeconds(3600))
				.slotEnd(Instant.now().plusSeconds(5400)).isBlocked(false).build();

		when(serviceSetupService.getServiceEntityOrThrow()).thenReturn(service);
		when(slotRepository.findById(slotId)).thenReturn(Optional.of(slot));
	}

	@Test
	void secondBookingOnSameSlotFailsWithSlotAlreadyBookedException() {
		BookingRequest request = new BookingRequest(slotId, "Mario Rossi", "mario@example.com", null);

		when(bookingRepository.saveAndFlush(any(Booking.class)))
				.thenReturn(Booking.builder().id(UUID.randomUUID()).availabilitySlot(slot).service(service).build())

				.thenThrow(new org.springframework.dao.DataIntegrityViolationException("uq_availability_slot_booking"));

		bookingService.createBooking(request);

		assertThatThrownBy(() -> bookingService.createBooking(request)).isInstanceOf(SlotAlreadyBookedException.class);
	}
}