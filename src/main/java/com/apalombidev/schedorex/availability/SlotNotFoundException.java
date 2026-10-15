package com.apalombidev.schedorex.availability;

import com.apalombidev.schedorex.exception.DomainException;

public class SlotNotFoundException extends DomainException {
	public SlotNotFoundException() {
		super("error.slot.not_found");
	}
}