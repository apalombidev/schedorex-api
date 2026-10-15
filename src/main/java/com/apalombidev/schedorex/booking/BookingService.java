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

@Service
@RequiredArgsConstructor
public class BookingService {

	private final BookingRepository bookingRepository;
	private final AvailabilitySlotRepository slotRepository;
	private final ServiceSetupService serviceSetupService;

	@Transactional
	public BookingCreatedResponse createBooking(BookingRequest request) {
		ProfessionalService service = serviceSetupService.getServiceEntityOrThrow();

		AvailabilitySlot slot = slotRepository.findById(request.availabilitySlotId())
				.orElseThrow(SlotNotFoundException::new);

		if (Boolean.TRUE.equals(slot.isBlocked())) {
			throw new SlotBlockedException();
		}

		BookingStatus initialStatus = Boolean.TRUE.equals(service.getStripeEnabled()) ? BookingStatus.PENDING
				: BookingStatus.CONFIRMED;

		Booking booking = Booking.builder().service(service).availabilitySlot(slot).clientName(request.clientName())
				.clientEmail(request.clientEmail()).clientNotes(request.clientNotes()).status(initialStatus).build();

		Booking saved;
		try {
			saved = bookingRepository.saveAndFlush(booking);
		} catch (DataIntegrityViolationException e) {
			throw new SlotAlreadyBookedException();
		}

		return BookingCreatedResponse.fromEntity(saved);
	}

	@Transactional(readOnly = true)
	public BookingResponse getBookingByCancellationToken(UUID token) {
		Booking booking = bookingRepository.findByCancellationToken(token)
				.orElseThrow(InvalidCancellationTokenException::new);
		return BookingResponse.fromEntity(booking);
	}

	@Transactional
	public BookingResponse cancelBooking(UUID token) {
		Booking booking = bookingRepository.findByCancellationToken(token)
				.orElseThrow(InvalidCancellationTokenException::new);

		ProfessionalService service = booking.getService();
		Instant deadline = booking.getAvailabilitySlot().getSlotStart()
				.minusSeconds(service.getCancellationDeadlineHours() * 3600L);

		if (Instant.now().isAfter(deadline)) {
			throw new CancellationDeadlinePassedException();
		}

		booking.setStatus(BookingStatus.CANCELLED);
		booking.setCancelledAt(Instant.now());
		Booking saved = bookingRepository.save(booking);
		return BookingResponse.fromEntity(saved);
	}

	@Transactional
	public BookingResponse cancelBookingAsAdmin(UUID bookingId) {
		Booking booking = bookingRepository.findById(bookingId)
				.orElseThrow(() -> new InvalidCancellationTokenException("error.booking.not_found"));

		booking.setStatus(BookingStatus.CANCELLED);
		booking.setCancelledAt(Instant.now());
		Booking saved = bookingRepository.save(booking);
		return BookingResponse.fromEntity(saved);
	}

	@Transactional(readOnly = true)
	public List<Booking> findPendingBookingsWithStripeSession() {
		return bookingRepository.findByStatusAndStripeCheckoutSessionIdIsNotNull(BookingStatus.PENDING);
	}

	@Transactional
	public BookingResponse confirmPayment(Booking booking) {
		booking.setStatus(BookingStatus.CONFIRMED);
		booking.setStripePaymentStatus("paid");
		Booking saved = bookingRepository.save(booking);
		return BookingResponse.fromEntity(saved);
	}
}