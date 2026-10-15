package com.apalombidev.schedorex.booking.controller;

import com.apalombidev.schedorex.booking.BookingService;
import com.apalombidev.schedorex.booking.dto.BookingCreatedResponse;
import com.apalombidev.schedorex.booking.dto.BookingRequest;
import com.apalombidev.schedorex.booking.dto.BookingResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/public/bookings")
@RequiredArgsConstructor
public class BookingPublicController {

	private final BookingService bookingService;

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public BookingCreatedResponse createBooking(@Valid @RequestBody BookingRequest request) {
		return bookingService.createBooking(request);
	}

	/** Usato dalla pagina "dettaglio prenotazione" dietro al link della mail. */
	@GetMapping("/{token}")
	public BookingResponse getBooking(@PathVariable UUID token) {
		return bookingService.getBookingByCancellationToken(token);
	}

	@PostMapping("/{token}/cancel")
	public BookingResponse cancelBooking(@PathVariable UUID token) {
		return bookingService.cancelBooking(token);
	}
}