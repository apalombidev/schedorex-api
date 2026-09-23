package com.apalombidev.schedorex.service;

import com.apalombidev.schedorex.exception.DomainException;

public class ServiceNotFoundException extends DomainException {
	public ServiceNotFoundException() {
		super("error.service.not_found");
	}
}
