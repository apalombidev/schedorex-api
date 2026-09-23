package com.apalombidev.schedorex.booking;

import com.apalombidev.schedorex.exception.DomainException;

public class SlotBlockedException extends DomainException{
	public SlotBlockedException() {
        super("error.slot.blocked");
    }

}
