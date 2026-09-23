package com.apalombidev.schedorex.booking;

import com.apalombidev.schedorex.exception.DomainException;

public class SlotAlreadyBookedException extends DomainException {
	public SlotAlreadyBookedException() {
        super("error.slot.already_booked");
    }

}
