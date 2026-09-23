package com.apalombidev.schedorex.availability;

import com.apalombidev.schedorex.exception.DomainException;

public class InvalidAvailabilityRangeException extends DomainException{
	public InvalidAvailabilityRangeException(String message) {
		super(message);
	}

}
