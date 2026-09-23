package com.apalombidev.schedorex.booking;

import com.apalombidev.schedorex.exception.DomainException;

public class InvalidCancellationTokenException extends DomainException{
	public InvalidCancellationTokenException() {
        super("error.cancellation.invalid_token");
    }
	
	public InvalidCancellationTokenException(String messageKey) {
        super(messageKey);
    }

}
