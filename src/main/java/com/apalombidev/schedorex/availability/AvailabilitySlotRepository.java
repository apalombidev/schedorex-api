package com.apalombidev.schedorex.availability;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.apalombidev.schedorex.booking.BookingStatus;

public interface AvailabilitySlotRepository extends JpaRepository<AvailabilitySlot, UUID> {
	
	
	@Query("""
	        SELECT s FROM AvailabilitySlot s
	        WHERE s.service.id = :serviceId
	          AND s.isBlocked = false
	          AND s.slotStart >= :from
	          AND s.slotStart < :to
	          AND NOT EXISTS (
	              SELECT 1 FROM Booking b
	              WHERE b.availabilitySlot = s
	                AND b.status IN :activeStatuses
	          )
	        ORDER BY s.slotStart ASC
	        """)
	List<AvailabilitySlot> findFreeSlots(
	        @Param("serviceId") UUID serviceId,
	        @Param("from") Instant from,
	        @Param("to") Instant to,
	        @Param("activeStatuses") List<BookingStatus> activeStatuses);	
	
    long countByService_IdAndSlotStartGreaterThanEqual(UUID serviceId, Instant from);
    
    boolean existsByService_IdAndSlotStart(UUID serviceId, Instant slotStart);

	

}
