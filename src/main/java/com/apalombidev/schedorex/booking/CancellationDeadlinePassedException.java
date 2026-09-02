package com.apalombidev.schedorex.booking;

public class CancellationDeadlinePassedException extends RuntimeException{
	public CancellationDeadlinePassedException(String message) {
		super(message);
	}

}
