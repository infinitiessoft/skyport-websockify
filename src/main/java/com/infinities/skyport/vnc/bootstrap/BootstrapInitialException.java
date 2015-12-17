package com.infinities.skyport.vnc.bootstrap;

public class BootstrapInitialException extends RuntimeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;


	public BootstrapInitialException() {
		super();
	}

	public BootstrapInitialException(String message) {
		super(message);
	}

	public BootstrapInitialException(String message, Throwable cause) {
		super(message, cause);
	}

	public BootstrapInitialException(Throwable cause) {
		super(cause);
	}

	protected BootstrapInitialException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

}
