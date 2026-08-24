package com.apalombidev.schedorex.booking;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface BookingRepository extends JpaRepository<Booking,UUID>{
	
	Optional<Booking> findByCancellationToken(UUID cancellationToken);

}
