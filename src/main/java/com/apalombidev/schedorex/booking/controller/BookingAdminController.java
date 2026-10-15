package com.apalombidev.schedorex.booking.controller;

import com.apalombidev.schedorex.booking.BookingService;
import com.apalombidev.schedorex.booking.dto.BookingResponse;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin/bookings")
@RequiredArgsConstructor
public class BookingAdminController {

	private final BookingService bookingService;

	@PostMapping("/{bookingId}/cancel")
	public BookingResponse cancelBookingAsAdmin(@PathVariable UUID bookingId) {
		return bookingService.cancelBookingAsAdmin(bookingId);
	}
}