/*
 * Copyright (C) 2018, 2022 Thomas Wolf <thomas.wolf@paranor.ch> and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0 which is available at
 * https://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package org.eclipse.jgit.internal.transport.sshd;

import org.eclipse.jgit.nls.NLS;
import org.eclipse.jgit.nls.TranslationBundle;

/**
 * Externalized text messages for localization.
 */
public final class SshdText extends TranslationBundle {

	/**
	 * Get an instance of this translation bundle.
	 *
	 * @return an instance of this translation bundle
	 */
	public static SshdText get() {
		return NLS.getBundleFor(SshdText.class);
	}

	// @formatter:off
	/***/ public String authenticationCanceled;
	/***/ public String authenticationOnClosedSession;
	/***/ public String authGssApiAttempt;
	/***/ public String authGssApiExhausted;
	/***/ public String authGssApiFailure;
	/***/ public String authGssApiNotTried;
	/***/ public String authGssApiPartialSuccess;
	/***/ public String authPasswordAttempt;
	/***/ public String authPasswordChangeAttempt;
	/***/ public String authPasswordExhausted;
	/***/ public String authPasswordFailure;
	/***/ public String authPasswordNotTried;
	/***/ public String authPasswordPartialSuccess;
	/***/ public String authPubkeyAttempt;
	/***/ public String authPubkeyAttemptAgent;
	/***/ public String authPubkeyExhausted;
	/***/ public String authPubkeyFailure;
	/***/ public String authPubkeyNoKeys;
	/***/ public String authPubkeyPartialSuccess;
	/***/ public String closeListenerFailed;
	/***/ public String cannotReadPublicKey;
	/***/ public String configInvalidPath;
	/***/ public String configInvalidPattern;
	/***/ public String configInvalidPositive;
	/***/ public String configInvalidProxyJump;
	/***/ public String configNoKnownAlgorithms;
	/***/ public String configProxyJumpNotSsh;
	/***/ public String configProxyJumpWithPath;
	/***/ public String configUnknownAlgorithm;
	/***/ public String ftpCloseFailed;
	/***/ public String gssapiFailure;
	/***/ public String gssapiInitFailure;
	/***/ public String gssapiUnexpectedMechanism;
	/***/ public String gssapiUnexpectedMessage;
	/***/ public String identityFileCannotDecrypt;
	/***/ public String identityFileNoKey;
	/***/ public String identityFileMultipleKeys;
	/***/ public String identityFileNotFound;
	/***/ public String identityFileUnsupportedFormat;
	/***/ public String invalidSignatureAlgorithm;
	/***/ public String kexServerKeyInvalid;
	/***/ public String keyEncryptedMsg;
	/***/ public String keyEncryptedPrompt;
	/***/ public String keyEncryptedRetry;
	/***/ public String keyLoadFailed;
	/***/ public String knownHostsCouldNotUpdate;
	/***/ public String knownHostsFileLockedUpdate;
	/***/ public String knownHostsFileReadFailed;
	/***/ public String knownHostsInvalidLine;
	/***/ public String knownHostsInvalidPath;
	/***/ public String knownHostsKeyFingerprints;
	/***/ public String knownHostsModifiedKeyAcceptPrompt;
	/***/ public String knownHostsModifiedKeyDenyMsg;
	/***/ public String knownHostsModifiedKeyStorePrompt;
	/***/ public String knownHostsModifiedKeyWarning;
	/***/ public String knownHostsRevokedKeyMsg;
	/***/ public String knownHostsUnknownKeyMsg;
	/***/ public String knownHostsUnknownKeyPrompt;
	/***/ public String knownHostsUnknownKeyType;
	/***/ public String knownHostsUserAskCreationMsg;
	/***/ public String knownHostsUserAskCreationPrompt;
	/***/ public String loginDenied;
	/***/ public String passwordPrompt;
	/***/ public String proxyCannotAuthenticate;
	/***/ public String proxyHttpFailure;
	/***/ public String proxyHttpInvalidUserName;
	/***/ public String proxyHttpUnexpectedReply;
	/***/ public String proxyHttpUnspecifiedFailureReason;
	/***/ public String proxyJumpAbort;
	/***/ public String proxyPasswordPrompt;
	/***/ public String proxySocksAuthenticationFailed;
	/***/ public String proxySocksFailureForbidden;
	/***/ public String proxySocksFailureGeneral;
	/***/ public String proxySocksFailureHostUnreachable;
	/***/ public String proxySocksFailureNetworkUnreachable;
	/***/ public String proxySocksFailureRefused;
	/***/ public String proxySocksFailureTTL;
	/***/ public String proxySocksFailureUnspecified;
	/***/ public String proxySocksFailureUnsupportedAddress;
	/***/ public String proxySocksFailureUnsupportedCommand;
	/***/ public String proxySocksGssApiFailure;
	/***/ public String proxySocksGssApiMessageTooShort;
	/***/ public String proxySocksGssApiUnknownMessage;
	/***/ public String proxySocksGssApiVersionMismatch;
	/***/ public String proxySocksNoRemoteHostName;
	/***/ public String proxySocksPasswordTooLong;
	/***/ public String proxySocksUnexpectedMessage;
	/***/ public String proxySocksUnexpectedVersion;
	/***/ public String proxySocksUsernameTooLong;
	/***/ public String pubkeyAuthAddKeyToAgentError;
	/***/ public String pubkeyAuthAddKeyToAgentQuestion;
	/***/ public String pubkeyAuthWrongCommand;
	/***/ public String pubkeyAuthWrongKey;
	/***/ public String pubkeyAuthWrongSignatureAlgorithm;
	/***/ public String serverIdNotReceived;
	/***/ public String serverIdTooLong;
	/***/ public String serverIdWithNul;
	/***/ public String sessionCloseFailed;
	/***/ public String sessionWithoutUsername;
	/***/ public String sshAgentEdDSAFormatError;
	/***/ public String sshAgentPayloadLengthError;
	/***/ public String sshAgentReplyLengthError;
	/***/ public String sshAgentReplyUnexpected;
	/***/ public String sshAgentShortReadBuffer;
	/***/ public String sshAgentUnknownKey;
	/***/ public String sshAgentWrongKeyLength;
	/***/ public String sshAgentWrongNumberOfKeys;
	/***/ public String sshClosingDown;
	/***/ public String sshCommandTimeout;
	/***/ public String sshProcessStillRunning;
	/***/ public String sshProxySessionCloseFailed;
	/***/ public String unknownProxyProtocol;

}
