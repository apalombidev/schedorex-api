package com.apalombidev.schedorex.exception;

public abstract class DomainException extends RuntimeException {

	private final String messageKey;
	private final transient Object[] args;

	protected DomainException(String messageKey, Object... args) {
		super(messageKey);
		this.messageKey = messageKey;
		this.args = args;
	}

	public String getMessageKey() {
		return messageKey;
	}

	public Object[] getArgs() {
		return args;
	}
}