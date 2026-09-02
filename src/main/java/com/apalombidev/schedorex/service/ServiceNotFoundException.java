package com.apalombidev.schedorex.service;

public class ServiceNotFoundException extends RuntimeException {
	public ServiceNotFoundException(String message) {
		super(message);
	}
}
