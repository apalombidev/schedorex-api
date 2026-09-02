package com.apalombidev.schedorex.booking;

import java.util.List;

public enum BookingStatus {
	
PENDING, 
CONFIRMED, 
CANCELLED;


	public static List<BookingStatus> activeStatuses() {
	

	return List.of(PENDING, CONFIRMED);
	}
}