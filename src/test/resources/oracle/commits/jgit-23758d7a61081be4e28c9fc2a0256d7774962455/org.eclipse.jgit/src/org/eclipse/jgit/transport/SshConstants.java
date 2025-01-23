/*
 * Copyright (C) 2018, 2021 Thomas Wolf <thomas.wolf@paranor.ch> and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0 which is available at
 * https://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package org.eclipse.jgit.transport;

import org.eclipse.jgit.lib.Constants;

/**
 * Constants relating to ssh.
 *
 * @since 5.2
 */
@SuppressWarnings("nls")
public final class SshConstants {

	private SshConstants() {
		// No instances, please.
	}

	/** IANA assigned port number for ssh. */
	public static final int SSH_DEFAULT_PORT = 22;

	/** URI scheme for ssh. */
	public static final String SSH_SCHEME = "ssh";

	/** URI scheme for sftp. */
	public static final String SFTP_SCHEME = "sftp";

	/** Default name for a ssh directory. */
	public static final String SSH_DIR = ".ssh";

	/** Name of the ssh config file. */
	public static final String CONFIG = Constants.CONFIG;

	/** Default name of the user "known hosts" file. */
	public static final String KNOWN_HOSTS = "known_hosts";

	// Config file keys

	/**
	 * Property to control whether private keys are added to an SSH agent, if
	 * one is running, after having been loaded.
	 *
	 * @since 6.1
	 */
	public static final String ADD_KEYS_TO_AGENT = "AddKeysToAgent";

	/** Key in an ssh config file. */
	public static final String BATCH_MODE = "BatchMode";

	/** Key in an ssh config file. */
	public static final String CANONICAL_DOMAINS = "CanonicalDomains";

	/** Key in an ssh config file. */
	public static final String CERTIFICATE_FILE = "CertificateFile";

	/** Key in an ssh config file. */
	public static final String CIPHERS = "Ciphers";

	/** Key in an ssh config file. */
	public static final String COMPRESSION = "Compression";

	/** Key in an ssh config file. */
	public static final String CONNECTION_ATTEMPTS = "ConnectionAttempts";

	/**
	 * An OpenSSH time value for the connection timeout. In OpenSSH, this
	 * includes everything until the end of the initial key exchange; in JGit it
	 * covers only the underlying TCP connect.
	 *
	 * @since 6.1
	 */
	public static final String CONNECT_TIMEOUT = "ConnectTimeout";

	/** Key in an ssh config file. */
	public static final String CONTROL_PATH = "ControlPath";

	/** Key in an ssh config file. */
	public static final String GLOBAL_KNOWN_HOSTS_FILE = "GlobalKnownHostsFile";

	/**
	 * Key in an ssh config file.
	 *
	 * @since 5.5
	 */
	public static final String HASH_KNOWN_HOSTS = "HashKnownHosts";

	/** Key in an ssh config file. */
	public static final String HOST = "Host";

	/** Key in an ssh config file. */
	public static final String HOST_KEY_ALGORITHMS = "HostKeyAlgorithms";

	/** Key in an ssh config file. */
	public static final String HOST_NAME = "HostName";

	/** Key in an ssh config file. */
	public static final String IDENTITIES_ONLY = "IdentitiesOnly";

	/** Key in an ssh config file. */
	public static final String IDENTITY_AGENT = "IdentityAgent";

	/** Key in an ssh config file. */
	public static final String IDENTITY_FILE = "IdentityFile";

	/** Key in an ssh config file. */
	public static final String KEX_ALGORITHMS = "KexAlgorithms";

	/** Key in an ssh config file. */
	public static final String LOCAL_COMMAND = "LocalCommand";

	/** Key in an ssh config file. */
	public static final String LOCAL_FORWARD = "LocalForward";

	/** Key in an ssh config file. */
	public static final String MACS = "MACs";

	/** Key in an ssh config file. */
	public static final String NUMBER_OF_PASSWORD_PROMPTS = "NumberOfPasswordPrompts";

	/**
	 * Path to a shared library of a PKCS11 key provider, or "none".
	 * <p>
	 * If set and not "none", the provider's keys should be used.
	 * </p>
	 *
	 * @since 6.7
	 */
	public static final String PKCS11_PROVIDER = "PKCS11Provider";

	/**
	 * Non-standard JGit addition: specify the PKCS#11 slot list index of the
	 * token to use. A positive number; defaults to zero; ignored if negative
	 * (in which case zero is used, too).
	 *
	 * @since 6.7
	 */
	public static final String PKCS11_SLOT_LIST_INDEX = "PKCS11SlotListIndex";

	/** Key in an ssh config file. */
	public static final String PORT = "Port";

	/** Key in an ssh config file. */
	public static final String PREFERRED_AUTHENTICATIONS = "PreferredAuthentications";

	/**
	 * Key in an ssh config file; defines signature algorithms for public key
	 * authentication as a comma-separated list.
	 *
	 * @since 5.11.1
	 */
	public static final String PUBKEY_ACCEPTED_ALGORITHMS = "PubkeyAcceptedAlgorithms";

	/** Key in an ssh config file. */
	public static final String PROXY_COMMAND = "ProxyCommand";

	/**
	 * Comma-separated list of jump hosts, defining a jump host chain <em>in
	 * reverse order</em>. Each jump host is a SSH URI or "[user@]host[:port]".
	 * <p>
	 * Reverse order means: to connect {@literal A -> B -> target}, one can do
	 * in {@code ~/.ssh/config} either of:
	 * </p>
	 *
	 * <pre>
	 * Host target
	 *   ProxyJump B,A
	 * </pre>
	 * <p>
	 * <em>or</em>
	 * </p>
	 *
	 * <pre>
	 * Host target
	 *   ProxyJump B
	 *
	 * Host B
	 *   ProxyJump A
	 * </pre>
	 *
	 * @since 5.10
	 */
	public static final String PROXY_JUMP = "ProxyJump";

	/** Key in an ssh config file. */
	public static final String REMOTE_COMMAND = "RemoteCommand";

	/** Key in an ssh config file. */
	public static final String REMOTE_FORWARD = "RemoteForward";

	/**
	 * (Absolute) path to a middleware library the SSH agent shall use to load
	 * SK (U2F) keys.
	 *
	 * @since 6.1
	 */
	public static final String SECURITY_KEY_PROVIDER = "SecurityKeyProvider";

	/** Key in an ssh config file. */
	public static final String SEND_ENV = "SendEnv";

	/** Key in an ssh config file. */
	public static final String STRICT_HOST_KEY_CHECKING = "StrictHostKeyChecking";

	/** Key in an ssh config file. */
	public static final String USER = "User";

	/** Key in an ssh config file. */
	public static final String USER_KNOWN_HOSTS_FILE = "UserKnownHostsFile";

	// Values

	/** Flag value. */
	public static final String YES = "yes";

	/** Flag value. */
	public static final String ON = "on";

	/** Flag value. */
	public static final String TRUE = "true";

	/** Flag value. */
	public static final String NO = "no";

	/** Flag value. */
	public static final String OFF = "off";

	/** Flag value. */
	public static final String FALSE = "false";

	/**
	 * Property value. Some keys accept a special 'none' value to override and
	 * clear a setting otherwise contributed by another host entry, for instance
	 * {@link #PROXY_COMMAND} or {@link #PROXY_JUMP}. Example:
	 *
	 * <pre>
	 * Host bastion.example.org
	 *   ProxyJump none
	 *
	 * Host *.example.org
	 *   ProxyJump bastion.example.org
	 * </pre>
	 * <p>
	 * OpenSSH supports this since OpenSSH 7.8.
	 * </p>
	 *
	 * @since 6.0
	 */
	public static final String NONE = "none";

	// Default identity file names

	/** Name of the default RSA private identity file. */
	public static final String ID_RSA = "id_rsa";

	/** Name of the default DSA private identity file. */
	public static final String ID_DSA = "id_dsa";

	/** Name of the default ECDSA private identity file. */
	public static final String ID_ECDSA = "id_ecdsa";

	/** Name of the default ED25519 private identity file. */
	public static final String ID_ED25519 = "id_ed25519";

	/** All known default identity file names. */
	public static final String[] DEFAULT_IDENTITIES = { //
			ID_RSA, ID_DSA, ID_ECDSA, ID_ED25519
	};

	/**
	 * Name of the environment variable holding the Unix domain socket for
	 * communication with an SSH agent.
	 *
	 * @since 6.1
	 */
	public static final String ENV_SSH_AUTH_SOCKET = "SSH_AUTH_SOCK";
}
