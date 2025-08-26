package org.refactoringminer.exceptions;

public class OAuthTokenNotFoundException extends NetworkException {
	private static final String DEFAULT_MESSAGE =
			"❌ Missing OAuth token. Please set the environment variable 'OAuthToken' with your personal access token.";

	public OAuthTokenNotFoundException() {
		super(DEFAULT_MESSAGE);
	}

	public OAuthTokenNotFoundException(Exception e) {
		super(DEFAULT_MESSAGE, e);
	}
}
