package com.apalombidev.schedorex.booking;

public class SlotAlreadyBookedException extends RuntimeException {
	public SlotAlreadyBookedException(String message) {
		super(message);
	}

}
