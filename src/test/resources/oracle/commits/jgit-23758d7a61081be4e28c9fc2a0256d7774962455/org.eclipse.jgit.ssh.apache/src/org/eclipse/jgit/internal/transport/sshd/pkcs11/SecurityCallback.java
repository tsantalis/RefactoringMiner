/*
 * Copyright (C) 2023 Thomas Wolf <twolf@apache.org> and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0 which is available at
 * https://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package org.eclipse.jgit.internal.transport.sshd.pkcs11;

import static java.text.MessageFormat.format;
import static org.apache.sshd.core.CoreModuleProperties.PASSWORD_PROMPTS;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.ChoiceCallback;
import javax.security.auth.callback.ConfirmationCallback;
import javax.security.auth.callback.LanguageCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.TextInputCallback;
import javax.security.auth.callback.TextOutputCallback;
import javax.security.auth.callback.UnsupportedCallbackException;

import org.apache.sshd.common.session.SessionContext;
import org.eclipse.jgit.internal.transport.sshd.AuthenticationCanceledException;
import org.eclipse.jgit.internal.transport.sshd.JGitClientSession;
import org.eclipse.jgit.internal.transport.sshd.SshdText;
import org.eclipse.jgit.transport.CredentialItem;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.jgit.transport.sshd.KeyPasswordProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A bridge to the JGit {@link CredentialsProvider}.
 */
public class SecurityCallback implements CallbackHandler {

	private static final Logger LOG = LoggerFactory
			.getLogger(SecurityCallback.class);

	private final URIish uri;

	private KeyPasswordProvider passwordProvider;

	private CredentialsProvider credentialsProvider;

	private int attempts = 0;

	/**
	 * Creates a new {@link SecurityCallback}.
	 *
	 * @param uri
	 *            {@link URIish} identifying the item the interaction is about
	 */
	public SecurityCallback(URIish uri) {
		this.uri = uri;
	}

	/**
	 * Initializes this {@link SecurityCallback} for the given session.
	 *
	 * @param session
	 *            {@link SessionContext} of the keystore access
	 * @return the number of PIN prompts to try to log-in to the token
	 */
	public int init(SessionContext session) {
		int numberOfAttempts = PASSWORD_PROMPTS.getRequired(session).intValue();
		Supplier<KeyPasswordProvider> factory = session
				.getAttribute(JGitClientSession.KEY_PASSWORD_PROVIDER_FACTORY);
		if (factory == null) {
			passwordProvider = null;
		} else {
			passwordProvider = factory.get();
			passwordProvider.setAttempts(numberOfAttempts);
		}
		attempts = 0;
		if (session instanceof JGitClientSession) {
			credentialsProvider = ((JGitClientSession) session)
					.getCredentialsProvider();
		} else {
			credentialsProvider = null;
		}
		return numberOfAttempts;
	}

	/**
	 * Tells this {@link SecurityCallback} that an attempt to load something
	 * from the key store has been made.
	 *
	 * @param error
	 *            an {@link Exception} that may have occurred, or {@code null}
	 *            on success
	 * @return whether to try once more
	 * @throws IOException
	 *             on errors
	 * @throws GeneralSecurityException
	 *             on errors
	 */
	public boolean passwordTried(Exception error)
			throws IOException, GeneralSecurityException {
		if (attempts > 0 && passwordProvider != null) {
			return passwordProvider.keyLoaded(uri, attempts, error);
		}
		return true;
	}

	@Override
	public void handle(Callback[] callbacks)
			throws IOException, UnsupportedCallbackException {
		if (callbacks.length == 1 && callbacks[0] instanceof PasswordCallback
				&& passwordProvider != null) {
			PasswordCallback p = (PasswordCallback) callbacks[0];
			char[] password = passwordProvider.getPassphrase(uri, attempts++);
			if (password == null || password.length == 0) {
				throw new AuthenticationCanceledException();
			}
			p.setPassword(password);
			Arrays.fill(password, '\0');
		} else {
			handleGeneral(callbacks);
		}
	}

	private void handleGeneral(Callback[] callbacks)
			throws UnsupportedCallbackException {
		List<CredentialItem> items = new ArrayList<>();
		List<Runnable> updaters = new ArrayList<>();
		for (int i = 0; i < callbacks.length; i++) {
			Callback c = callbacks[i];
			if (c instanceof TextOutputCallback) {
				TextOutputCallback t = (TextOutputCallback) c;
				String msg = getText(t.getMessageType(), t.getMessage());
				if (credentialsProvider == null) {
					LOG.warn("{}", format(SshdText.get().pkcs11GeneralMessage, //$NON-NLS-1$
							uri, msg));
				} else {
					CredentialItem.InformationalMessage item =
							new CredentialItem.InformationalMessage(msg);
					items.add(item);
				}
			} else if (c instanceof TextInputCallback) {
				if (credentialsProvider == null) {
					throw new UnsupportedOperationException(
							"No CredentialsProvider " + uri); //$NON-NLS-1$
				}
				TextInputCallback t = (TextInputCallback) c;
				CredentialItem.StringType item = new CredentialItem.StringType(
						t.getPrompt(), false);
				String defaultValue = t.getDefaultText();
				if (defaultValue != null) {
					item.setValue(defaultValue);
				}
				items.add(item);
				updaters.add(() -> t.setText(item.getValue()));
			} else if (c instanceof PasswordCallback) {
				if (credentialsProvider == null) {
					throw new UnsupportedOperationException(
							"No CredentialsProvider " + uri); //$NON-NLS-1$
				}
				// It appears that this is actually the only callback item we
				// get from the KeyStore when it asks for the PIN.
				PasswordCallback p = (PasswordCallback) c;
				CredentialItem.Password item = new CredentialItem.Password(
						p.getPrompt());
				items.add(item);
				updaters.add(() -> {
					char[] password = item.getValue();
					if (password == null || password.length == 0) {
						throw new AuthenticationCanceledException();
					}
					p.setPassword(password);
					item.clear();
				});
			} else if (c instanceof ConfirmationCallback) {
				if (credentialsProvider == null) {
					throw new UnsupportedOperationException(
							"No CredentialsProvider " + uri); //$NON-NLS-1$
				}
				// JGit has only limited support for this
				ConfirmationCallback conf = (ConfirmationCallback) c;
				int options = conf.getOptionType();
				int defaultOption = conf.getDefaultOption();
				CredentialItem.YesNoType item = new CredentialItem.YesNoType(
						getText(conf.getMessageType(), conf.getPrompt()));
				switch (options) {
				case ConfirmationCallback.YES_NO_OPTION:
					if (defaultOption == ConfirmationCallback.YES) {
						item.setValue(true);
					}
					updaters.add(() -> conf.setSelectedIndex(
							item.getValue() ? ConfirmationCallback.YES
									: ConfirmationCallback.NO));
					break;
				case ConfirmationCallback.OK_CANCEL_OPTION:
					if (defaultOption == ConfirmationCallback.OK) {
						item.setValue(true);
					}
					updaters.add(() -> conf.setSelectedIndex(
							item.getValue() ? ConfirmationCallback.OK
									: ConfirmationCallback.CANCEL));
					break;
				default:
					throw new UnsupportedCallbackException(c);
				}
				items.add(item);
			} else if (c instanceof ChoiceCallback) {
				// TODO: implement? Information for the prompt, and individual
				// YesNoItems for the choices? Might be better to hoist JGit
				// onto the CallbackHandler interface directly, or add support
				// for choices.
				throw new UnsupportedCallbackException(c);
			} else if (c instanceof LanguageCallback) {
				((LanguageCallback) c).setLocale(Locale.getDefault());
			} else {
				throw new UnsupportedCallbackException(c);
			}
		}
		if (!items.isEmpty()) {
			if (credentialsProvider.get(uri, items)) {
				updaters.forEach(Runnable::run);
			} else {
				throw new AuthenticationCanceledException();
			}
		}
	}

	private String getText(int messageType, String text) {
		if (messageType == TextOutputCallback.WARNING) {
			return format(SshdText.get().pkcs11Warning, text);
		} else if (messageType == TextOutputCallback.ERROR) {
			return format(SshdText.get().pkcs11Error, text);
		}
		return text;
	}
}
