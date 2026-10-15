package com.apalombidev.schedorex.service.dto;

public class InvalidTimezoneException extends IllegalArgumentException {

	private final String timezone;

	public InvalidTimezoneException(String timezone) {
		super("error.timezone.invalid"); // solo per log/stacktrace, mai esposto al client
		this.timezone = timezone;
	}

	public String getTimezone() {
		return timezone;
	}
}