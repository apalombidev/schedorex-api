package com.apalombidev.schedorex.booking;

import com.apalombidev.schedorex.exception.DomainException;

public class CancellationDeadlinePassedException extends DomainException {
	public CancellationDeadlinePassedException() {
		super("error.cancellation.deadline_passed");
	}

}
