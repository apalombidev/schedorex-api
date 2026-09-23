package com.apalombidev.schedorex.auth;

import com.apalombidev.schedorex.exception.DomainException;

public class InvalidCredentialsException extends DomainException{
	public InvalidCredentialsException() {
        super("error.credentials.invalid");
    }

}
