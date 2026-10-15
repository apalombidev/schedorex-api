package com.apalombidev.schedorex.booking;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface BookingRepository extends JpaRepository<Booking, UUID> {

	Optional<Booking> findByCancellationToken(UUID cancellationToken);

	Optional<Booking> findByStripeCheckoutSessionId(String stripeCheckoutSessionId);

	List<Booking> findByStatusAndStripeCheckoutSessionIdIsNotNull(BookingStatus pending);

	Optional<Booking> findFirstByAvailabilitySlot_IdAndStatusIn(UUID availabilitySlotId,
			List<BookingStatus> activeStatuses);

}
