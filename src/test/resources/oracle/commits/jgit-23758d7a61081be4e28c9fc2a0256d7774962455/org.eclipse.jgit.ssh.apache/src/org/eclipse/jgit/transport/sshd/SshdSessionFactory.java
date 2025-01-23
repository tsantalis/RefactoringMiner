/*
 * Copyright (C) 2018, 2022 Thomas Wolf <thomas.wolf@paranor.ch> and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0 which is available at
 * https://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package org.eclipse.jgit.transport.sshd;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.security.KeyPair;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.apache.sshd.client.ClientBuilder;
import org.apache.sshd.client.SshClient;
import org.apache.sshd.client.auth.UserAuthFactory;
import org.apache.sshd.client.auth.keyboard.UserAuthKeyboardInteractiveFactory;
import org.apache.sshd.client.auth.password.UserAuthPasswordFactory;
import org.apache.sshd.client.config.hosts.HostConfigEntryResolver;
import org.apache.sshd.common.NamedFactory;
import org.apache.sshd.common.compression.BuiltinCompressions;
import org.apache.sshd.common.config.keys.FilePasswordProvider;
import org.apache.sshd.common.config.keys.loader.openssh.kdf.BCryptKdfOptions;
import org.apache.sshd.common.keyprovider.KeyIdentityProvider;
import org.apache.sshd.common.signature.BuiltinSignatures;
import org.apache.sshd.common.signature.Signature;
import org.eclipse.jgit.annotations.NonNull;
import org.eclipse.jgit.errors.TransportException;
import org.eclipse.jgit.internal.transport.ssh.OpenSshConfigFile;
import org.eclipse.jgit.internal.transport.sshd.CachingKeyPairProvider;
import org.eclipse.jgit.internal.transport.sshd.GssApiWithMicAuthFactory;
import org.eclipse.jgit.internal.transport.sshd.JGitPublicKeyAuthFactory;
import org.eclipse.jgit.internal.transport.sshd.JGitServerKeyVerifier;
import org.eclipse.jgit.internal.transport.sshd.JGitSshClient;
import org.eclipse.jgit.internal.transport.sshd.JGitSshConfig;
import org.eclipse.jgit.internal.transport.sshd.JGitUserInteraction;
import org.eclipse.jgit.internal.transport.sshd.OpenSshServerKeyDatabase;
import org.eclipse.jgit.internal.transport.sshd.PasswordProviderWrapper;
import org.eclipse.jgit.internal.transport.sshd.SshdText;
import org.eclipse.jgit.internal.transport.sshd.agent.JGitSshAgentFactory;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.SshConfigStore;
import org.eclipse.jgit.transport.SshConstants;
import org.eclipse.jgit.transport.SshSessionFactory;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.jgit.transport.sshd.agent.Connector;
import org.eclipse.jgit.transport.sshd.agent.ConnectorFactory;
import org.eclipse.jgit.util.FS;

/**
 * A {@link SshSessionFactory} that uses Apache MINA sshd. Classes from Apache
 * MINA sshd are kept private to avoid API evolution problems when Apache MINA
 * sshd interfaces change.
 *
 * @since 5.2
 */
public class SshdSessionFactory extends SshSessionFactory implements Closeable {

	private static final String MINA_SSHD = "mina-sshd"; //$NON-NLS-1$

	private final AtomicBoolean closing = new AtomicBoolean();

	private final Set<SshdSession> sessions = new HashSet<>();

	private final Map<Tuple, HostConfigEntryResolver> defaultHostConfigEntryResolver = new ConcurrentHashMap<>();

	private final Map<Tuple, ServerKeyDatabase> defaultServerKeyDatabase = new ConcurrentHashMap<>();

	private final Map<Tuple, Iterable<KeyPair>> defaultKeys = new ConcurrentHashMap<>();

	private final KeyCache keyCache;

	private final ProxyDataFactory proxies;

	private File sshDirectory;

	private File homeDirectory;

	/**
	 * Creates a new {@link SshdSessionFactory} without key cache and a
	 * {@link DefaultProxyDataFactory}.
	 */
	public SshdSessionFactory() {
		this(null, new DefaultProxyDataFactory());
	}

	/**
	 * Creates a new {@link SshdSessionFactory} using the given {@link KeyCache}
	 * and {@link ProxyDataFactory}. The {@code keyCache} is used for all
	 * sessions created through this session factory; cached keys are destroyed
	 * when the session factory is {@link #close() closed}.
	 * <p>
	 * Caching ssh keys in memory for an extended period of time is generally
	 * considered bad practice, but there may be circumstances where using a
	 * {@link KeyCache} is still the right choice, for instance to avoid that a
	 * user gets prompted several times for the same password for the same key.
	 * In general, however, it is preferable <em>not</em> to use a key cache but
	 * to use a {@link #createKeyPasswordProvider(CredentialsProvider)
	 * KeyPasswordProvider} that has access to some secure storage and can save
	 * and retrieve passwords from there without user interaction. Another
	 * approach is to use an SSH agent.
	 * </p>
	 * <p>
	 * Note that the underlying ssh library (Apache MINA sshd) may or may not
	 * keep ssh keys in memory for unspecified periods of time irrespective of
	 * the use of a {@link KeyCache}.
	 * </p>
	 * <p>
	 * By default, the factory uses the {@link java.util.ServiceLoader} to find
	 * a {@link ConnectorFactory} for creating a {@link Connector} to connect to
	 * a running SSH agent. If it finds one, the SSH agent is used in publickey
	 * authentication. If there is none, no SSH agent will ever be contacted.
	 * Note that one can define {@code IdentitiesOnly yes} for a host entry in
	 * the {@code ~/.ssh/config} file to bypass the SSH agent in any case.
	 * </p>
	 *
	 * @param keyCache
	 *            {@link KeyCache} to use for caching ssh keys, or {@code null}
	 *            to not use a key cache
	 * @param proxies
	 *            {@link ProxyDataFactory} to use, or {@code null} to not use a
	 *            proxy database (in which case connections through proxies will
	 *            not be possible)
	 */
	public SshdSessionFactory(KeyCache keyCache, ProxyDataFactory proxies) {
		super();
		this.keyCache = keyCache;
		this.proxies = proxies;
		// sshd limits the number of BCrypt KDF rounds to 255 by default.
		// Decrypting such a key takes about two seconds on my machine.
		// I consider this limit too low. The time increases linearly with the
		// number of rounds.
		BCryptKdfOptions.setMaxAllowedRounds(16384);
	}

	@Override
	public String getType() {
		return MINA_SSHD;
	}

	/** A simple general map key. */
	private static final class Tuple {
		private Object[] objects;

		public Tuple(Object[] objects) {
			this.objects = objects;
		}

		@Override
		public boolean equals(Object obj) {
			if (obj == this) {
				return true;
			}
			if (obj != null && obj.getClass() == Tuple.class) {
				Tuple other = (Tuple) obj;
				return Arrays.equals(objects, other.objects);
			}
			return false;
		}

		@Override
		public int hashCode() {
			return Arrays.hashCode(objects);
		}
	}

	// We can't really use a single client. Clients need to be stopped
	// properly, and we don't really know when to do that. Instead we use
	// a dedicated SshClient instance per session. We need a bit of caching to
	// avoid re-loading the ssh config and keys repeatedly.

	@Override
	public SshdSession getSession(URIish uri,
			CredentialsProvider credentialsProvider, FS fs, int tms)
			throws TransportException {
		SshdSession session = null;
		try {
			session = new SshdSession(uri, () -> {
				File home = getHomeDirectory();
				if (home == null) {
					// Always use the detected filesystem for the user home!
					// It makes no sense to have different "user home"
					// directories depending on what file system a repository
					// is.
					home = FS.DETECTED.userHome();
				}
				File sshDir = getSshDirectory();
				if (sshDir == null) {
					sshDir = new File(home, SshConstants.SSH_DIR);
				}
				HostConfigEntryResolver configFile = getHostConfigEntryResolver(
						home, sshDir);
				KeyIdentityProvider defaultKeysProvider = toKeyIdentityProvider(
						getDefaultKeys(sshDir));
				Supplier<KeyPasswordProvider> keyPasswordProvider = () -> createKeyPasswordProvider(
						credentialsProvider);
				SshClient client = ClientBuilder.builder()
						.factory(JGitSshClient::new)
						.filePasswordProvider(createFilePasswordProvider(
								keyPasswordProvider))
						.hostConfigEntryResolver(configFile)
						.serverKeyVerifier(new JGitServerKeyVerifier(
								getServerKeyDatabase(home, sshDir)))
						.signatureFactories(getSignatureFactories())
						.compressionFactories(
								new ArrayList<>(BuiltinCompressions.VALUES))
						.build();
				client.setUserInteraction(
						new JGitUserInteraction(credentialsProvider));
				client.setUserAuthFactories(getUserAuthFactories());
				client.setKeyIdentityProvider(defaultKeysProvider);
				ConnectorFactory connectors = getConnectorFactory();
				if (connectors != null) {
					client.setAgentFactory(
							new JGitSshAgentFactory(connectors, home));
				}
				// JGit-specific things:
				JGitSshClient jgitClient = (JGitSshClient) client;
				jgitClient.setKeyCache(getKeyCache());
				jgitClient.setCredentialsProvider(credentialsProvider);
				jgitClient.setProxyDatabase(proxies);
				jgitClient.setKeyPasswordProviderFactory(keyPasswordProvider);
				String defaultAuths = getDefaultPreferredAuthentications();
				if (defaultAuths != null) {
					jgitClient.setAttribute(
							JGitSshClient.PREFERRED_AUTHENTICATIONS,
							defaultAuths);
				}
				try {
					jgitClient.setAttribute(JGitSshClient.HOME_DIRECTORY,
							home.getAbsoluteFile().toPath());
				} catch (SecurityException | InvalidPathException e) {
					// Ignore
				}
				// Other things?
				return client;
			});
			session.addCloseListener(s -> unregister(s));
			register(session);
			session.connect(Duration.ofMillis(tms));
			return session;
		} catch (Exception e) {
			unregister(session);
			if (e instanceof TransportException) {
				throw (TransportException) e;
			}
			throw new TransportException(uri, e.getMessage(), e);
		}
	}

	@Override
	public void close() {
		closing.set(true);
		boolean cleanKeys = false;
		synchronized (this) {
			cleanKeys = sessions.isEmpty();
		}
		if (cleanKeys) {
			KeyCache cache = getKeyCache();
			if (cache != null) {
				cache.close();
			}
		}
	}

	private void register(SshdSession newSession) throws IOException {
		if (newSession == null) {
			return;
		}
		if (closing.get()) {
			throw new IOException(SshdText.get().sshClosingDown);
		}
		synchronized (this) {
			sessions.add(newSession);
		}
	}

	private void unregister(SshdSession oldSession) {
		boolean cleanKeys = false;
		synchronized (this) {
			sessions.remove(oldSession);
			cleanKeys = closing.get() && sessions.isEmpty();
		}
		if (cleanKeys) {
			KeyCache cache = getKeyCache();
			if (cache != null) {
				cache.close();
			}
		}
	}

	/**
	 * Set a global directory to use as the user's home directory
	 *
	 * @param homeDir
	 *            to use
	 */
	public void setHomeDirectory(@NonNull File homeDir) {
		if (homeDir.isAbsolute()) {
			homeDirectory = homeDir;
		} else {
			homeDirectory = homeDir.getAbsoluteFile();
		}
	}

	/**
	 * Retrieves the global user home directory
	 *
	 * @return the directory, or {@code null} if not set
	 */
	public File getHomeDirectory() {
		return homeDirectory;
	}

	/**
	 * Set a global directory to use as the .ssh directory
	 *
	 * @param sshDir
	 *            to use
	 */
	public void setSshDirectory(@NonNull File sshDir) {
		if (sshDir.isAbsolute()) {
			sshDirectory = sshDir;
		} else {
			sshDirectory = sshDir.getAbsoluteFile();
		}
	}

	/**
	 * Retrieves the global .ssh directory
	 *
	 * @return the directory, or {@code null} if not set
	 */
	public File getSshDirectory() {
		return sshDirectory;
	}

	/**
	 * Obtain a {@link HostConfigEntryResolver} to read the ssh config file and
	 * to determine host entries for connections.
	 *
	 * @param homeDir
	 *            home directory to use for ~ replacement
	 * @param sshDir
	 *            to use for looking for the config file
	 * @return the resolver
	 */
	@NonNull
	private HostConfigEntryResolver getHostConfigEntryResolver(
			@NonNull File homeDir, @NonNull File sshDir) {
		return defaultHostConfigEntryResolver.computeIfAbsent(
				new Tuple(new Object[] { homeDir, sshDir }),
				t -> new JGitSshConfig(createSshConfigStore(homeDir,
						getSshConfig(sshDir), getLocalUserName())));
	}

	/**
	 * Determines the ssh config file. The default implementation returns
	 * ~/.ssh/config. If the file does not exist and is created later it will be
	 * picked up. To not use a config file at all, return {@code null}.
	 *
	 * @param sshDir
	 *            representing ~/.ssh/
	 * @return the file (need not exist), or {@code null} if no config file
	 *         shall be used
	 * @since 5.5
	 */
	protected File getSshConfig(@NonNull File sshDir) {
		return new File(sshDir, SshConstants.CONFIG);
	}

	/**
	 * Obtains a {@link SshConfigStore}, or {@code null} if no SSH config is to
	 * be used. The default implementation returns {@code null} if
	 * {@code configFile == null} and otherwise an OpenSSH-compatible store
	 * reading host entries from the given file.
	 *
	 * @param homeDir
	 *            may be used for ~-replacements by the returned config store
	 * @param configFile
	 *            to use, or {@code null} if none
	 * @param localUserName
	 *            user name of the current user on the local OS
	 * @return A {@link SshConfigStore}, or {@code null} if none is to be used
	 *
	 * @since 5.8
	 */
	protected SshConfigStore createSshConfigStore(@NonNull File homeDir,
			File configFile, String localUserName) {
		return configFile == null ? null
				: new OpenSshConfigFile(homeDir, configFile, localUserName);
	}

	/**
	 * Obtains a {@link ServerKeyDatabase} to verify server host keys. The
	 * default implementation returns a {@link ServerKeyDatabase} that
	 * recognizes the two openssh standard files {@code ~/.ssh/known_hosts} and
	 * {@code ~/.ssh/known_hosts2} as well as any files configured via the
	 * {@code UserKnownHostsFile} option in the ssh config file.
	 *
	 * @param homeDir
	 *            home directory to use for ~ replacement
	 * @param sshDir
	 *            representing ~/.ssh/
	 * @return the {@link ServerKeyDatabase}
	 * @since 5.5
	 */
	@NonNull
	protected ServerKeyDatabase getServerKeyDatabase(@NonNull File homeDir,
			@NonNull File sshDir) {
		return defaultServerKeyDatabase.computeIfAbsent(
				new Tuple(new Object[] { homeDir, sshDir }),
				t -> createServerKeyDatabase(homeDir, sshDir));

	}

	/**
	 * Creates a {@link ServerKeyDatabase} to verify server host keys. The
	 * default implementation returns a {@link ServerKeyDatabase} that
	 * recognizes the two openssh standard files {@code ~/.ssh/known_hosts} and
	 * {@code ~/.ssh/known_hosts2} as well as any files configured via the
	 * {@code UserKnownHostsFile} option in the ssh config file.
	 *
	 * @param homeDir
	 *            home directory to use for ~ replacement
	 * @param sshDir
	 *            representing ~/.ssh/
	 * @return the {@link ServerKeyDatabase}
	 * @since 5.8
	 */
	@NonNull
	protected ServerKeyDatabase createServerKeyDatabase(@NonNull File homeDir,
			@NonNull File sshDir) {
		return new OpenSshServerKeyDatabase(true,
				getDefaultKnownHostsFiles(sshDir));
	}

	/**
	 * Gets a {@link ConnectorFactory}. If this returns {@code null}, SSH agents
	 * are not supported.
	 * <p>
	 * The default implementation uses {@link ConnectorFactory#getDefault()}
	 * </p>
	 *
	 * @return the factory, or {@code null} if no SSH agent support is desired
	 * @since 6.0
	 */
	protected ConnectorFactory getConnectorFactory() {
		return ConnectorFactory.getDefault();
	}

	/**
	 * Gets the list of default user known hosts files. The default returns
	 * ~/.ssh/known_hosts and ~/.ssh/known_hosts2. The ssh config
	 * {@code UserKnownHostsFile} overrides this default.
	 *
	 * @param sshDir
	 *            directory containing ssh configurations
	 * @return the possibly empty list of default known host file paths.
	 */
	@NonNull
	protected List<Path> getDefaultKnownHostsFiles(@NonNull File sshDir) {
		return Arrays.asList(sshDir.toPath().resolve(SshConstants.KNOWN_HOSTS),
				sshDir.toPath().resolve(SshConstants.KNOWN_HOSTS + '2'));
	}

	/**
	 * Determines the default keys. The default implementation will lazy load
	 * the {@link #getDefaultIdentities(File) default identity files}.
	 * <p>
	 * Subclasses may override and return an {@link Iterable} of whatever keys
	 * are appropriate. If the returned iterable lazily loads keys, it should be
	 * an instance of
	 * {@link org.apache.sshd.common.keyprovider.AbstractResourceKeyPairProvider
	 * AbstractResourceKeyPairProvider} so that the session can later pass it
	 * the {@link #createKeyPasswordProvider(CredentialsProvider) password
	 * provider} wrapped as a {@link FilePasswordProvider} via
	 * {@link org.apache.sshd.common.keyprovider.AbstractResourceKeyPairProvider#setPasswordFinder(FilePasswordProvider)
	 * AbstractResourceKeyPairProvider#setPasswordFinder(FilePasswordProvider)}
	 * so that encrypted, password-protected keys can be loaded.
	 * </p>
	 * <p>
	 * The default implementation uses exactly this mechanism; class
	 * {@link CachingKeyPairProvider} may serve as a model for a customized
	 * lazy-loading {@link Iterable} implementation
	 * </p>
	 * <p>
	 * If the {@link Iterable} returned has the keys already pre-loaded or
	 * otherwise doesn't need to decrypt encrypted keys, it can be any
	 * {@link Iterable}, for instance a simple {@link java.util.List List}.
	 * </p>
	 *
	 * @param sshDir
	 *            to look in for keys
	 * @return an {@link Iterable} over the default keys
	 * @since 5.3
	 */
	@NonNull
	protected Iterable<KeyPair> getDefaultKeys(@NonNull File sshDir) {
		List<Path> defaultIdentities = getDefaultIdentities(sshDir);
		return defaultKeys.computeIfAbsent(
				new Tuple(defaultIdentities.toArray(new Path[0])),
				t -> new CachingKeyPairProvider(defaultIdentities,
						getKeyCache()));
	}

	/**
	 * Converts an {@link Iterable} of {link KeyPair}s into a
	 * {@link KeyIdentityProvider}.
	 *
	 * @param keys
	 *            to provide via the returned {@link KeyIdentityProvider}
	 * @return a {@link KeyIdentityProvider} that provides the given
	 *         {@code keys}
	 */
	private KeyIdentityProvider toKeyIdentityProvider(Iterable<KeyPair> keys) {
		if (keys instanceof KeyIdentityProvider) {
			return (KeyIdentityProvider) keys;
		}
		return (session) -> keys;
	}

	/**
	 * Gets a list of default identities, i.e., private key files that shall
	 * always be tried for public key authentication. Typically those are
	 * ~/.ssh/id_dsa, ~/.ssh/id_rsa, and so on. The default implementation
	 * returns the files defined in {@link SshConstants#DEFAULT_IDENTITIES}.
	 *
	 * @param sshDir
	 *            the directory that represents ~/.ssh/
	 * @return a possibly empty list of paths containing default identities
	 *         (private keys)
	 */
	@NonNull
	protected List<Path> getDefaultIdentities(@NonNull File sshDir) {
		return Arrays
				.asList(SshConstants.DEFAULT_IDENTITIES).stream()
				.map(s -> new File(sshDir, s).toPath()).filter(Files::exists)
				.collect(Collectors.toList());
	}

	/**
	 * Obtains the {@link KeyCache} to use to cache loaded keys.
	 *
	 * @return the {@link KeyCache}, or {@code null} if none.
	 */
	protected final KeyCache getKeyCache() {
		return keyCache;
	}

	/**
	 * Creates a {@link KeyPasswordProvider} for a new session.
	 *
	 * @param provider
	 *            the {@link CredentialsProvider} to delegate to for user
	 *            interactions
	 * @return a new {@link KeyPasswordProvider}
	 */
	@NonNull
	protected KeyPasswordProvider createKeyPasswordProvider(
			CredentialsProvider provider) {
		return new IdentityPasswordProvider(provider);
	}

	/**
	 * Creates a {@link FilePasswordProvider} for a new session.
	 *
	 * @param providerFactory
	 *            providing the {@link KeyPasswordProvider} to delegate to
	 * @return a new {@link FilePasswordProvider}
	 */
	@NonNull
	private FilePasswordProvider createFilePasswordProvider(
			Supplier<KeyPasswordProvider> providerFactory) {
		return new PasswordProviderWrapper(providerFactory);
	}

	/**
	 * Gets the user authentication mechanisms (or rather, factories for them).
	 * By default this returns gssapi-with-mic, public-key, password, and
	 * keyboard-interactive, in that order. The order is only significant if the
	 * ssh config does <em>not</em> set {@code PreferredAuthentications}; if it
	 * is set, the order defined there will be taken.
	 *
	 * @return the non-empty list of factories.
	 */
	@NonNull
	private List<UserAuthFactory> getUserAuthFactories() {
		// About the order of password and keyboard-interactive, see upstream
		// bug https://issues.apache.org/jira/projects/SSHD/issues/SSHD-866 .
		// Password auth doesn't have this problem.
		return Collections.unmodifiableList(
				Arrays.asList(GssApiWithMicAuthFactory.INSTANCE,
						JGitPublicKeyAuthFactory.FACTORY,
						UserAuthPasswordFactory.INSTANCE,
						UserAuthKeyboardInteractiveFactory.INSTANCE));
	}

	/**
	 * Gets the list of default preferred authentication mechanisms. If
	 * {@code null} is returned the openssh default list will be in effect. If
	 * the ssh config defines {@code PreferredAuthentications} the value from
	 * the ssh config takes precedence.
	 *
	 * @return a comma-separated list of mechanism names, or {@code null} if
	 *         none
	 */
	protected String getDefaultPreferredAuthentications() {
		return null;
	}

	/**
	 * Apache MINA sshd 2.6.0 has removed DSA, DSA_CERT and RSA_CERT. We have to
	 * set it up explicitly to still allow users to connect with DSA keys.
	 *
	 * @return a list of supported signature factories
	 */
	@SuppressWarnings("deprecation")
	private static List<NamedFactory<Signature>> getSignatureFactories() {
		// @formatter:off
		return Arrays.asList(
				BuiltinSignatures.nistp256_cert,
				BuiltinSignatures.nistp384_cert,
				BuiltinSignatures.nistp521_cert,
				BuiltinSignatures.ed25519_cert,
				BuiltinSignatures.rsaSHA512_cert,
				BuiltinSignatures.rsaSHA256_cert,
				BuiltinSignatures.rsa_cert,
				BuiltinSignatures.nistp256,
				BuiltinSignatures.nistp384,
				BuiltinSignatures.nistp521,
				BuiltinSignatures.ed25519,
				BuiltinSignatures.sk_ecdsa_sha2_nistp256,
				BuiltinSignatures.sk_ssh_ed25519,
				BuiltinSignatures.rsaSHA512,
				BuiltinSignatures.rsaSHA256,
				BuiltinSignatures.rsa,
				BuiltinSignatures.dsa_cert,
				BuiltinSignatures.dsa);
		// @formatter:on
	}
}
