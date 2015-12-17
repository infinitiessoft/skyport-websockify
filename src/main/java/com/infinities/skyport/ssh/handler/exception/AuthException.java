package com.infinities.skyport.ssh.handler.exception;

public class AuthException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private Throwable cause = null;


	public AuthException() {
		super();
	}

	public AuthException(String s) {
		super(s);
	}

	public AuthException(String s, Throwable e) {
		super(s);
		this.cause = e;
	}

	public Throwable getCause() {
		return this.cause;
	}
}
