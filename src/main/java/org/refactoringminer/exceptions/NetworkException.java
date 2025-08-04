package org.refactoringminer.exceptions;

public class NetworkException extends RuntimeException {

    private static final String DEFAULT_MESSAGE = "❌ Error in connecting. Please retry.";

    public NetworkException() {
        super(DEFAULT_MESSAGE);
    }
	public NetworkException(String msg){
		super(msg);
	}

	public NetworkException(Throwable cause) {
		super(DEFAULT_MESSAGE, cause);
	}

	public NetworkException(String message, Throwable cause) {
		super(message, cause);
	}
}
