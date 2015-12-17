package com.infinities.skyport.ssh.handler.exception;

public class KeyExchangeException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private Throwable cause = null;


	public KeyExchangeException() {
		super();
	}

	public KeyExchangeException(String s) {
		super(s);
	}

	public KeyExchangeException(String s, Throwable e) {
		super(s);
		this.cause = e;
	}

	public Throwable getCause() {
		return this.cause;
	}
}
