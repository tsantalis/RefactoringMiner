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

import static java.text.MessageFormat.format;
import static org.apache.sshd.core.CoreModuleProperties.PASSWORD_PROMPTS;
import static org.apache.sshd.core.CoreModuleProperties.PREFERRED_AUTHS;
import static org.eclipse.jgit.internal.transport.ssh.OpenSshConfigFile.positive;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.SocketAddress;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.apache.sshd.client.SshClient;
import org.apache.sshd.client.config.hosts.HostConfigEntry;
import org.apache.sshd.client.future.ConnectFuture;
import org.apache.sshd.client.future.DefaultConnectFuture;
import org.apache.sshd.client.session.ClientSessionImpl;
import org.apache.sshd.client.session.SessionFactory;
import org.apache.sshd.common.AttributeRepository;
import org.apache.sshd.common.config.keys.FilePasswordProvider;
import org.apache.sshd.common.future.SshFutureListener;
import org.apache.sshd.common.io.IoConnectFuture;
import org.apache.sshd.common.io.IoSession;
import org.apache.sshd.common.keyprovider.AbstractResourceKeyPairProvider;
import org.apache.sshd.common.keyprovider.KeyIdentityProvider;
import org.apache.sshd.common.session.SessionContext;
import org.apache.sshd.common.session.helpers.AbstractSession;
import org.apache.sshd.common.util.ValidateUtils;
import org.apache.sshd.common.util.net.SshdSocketAddress;
import org.eclipse.jgit.internal.transport.sshd.JGitClientSession.ChainingAttributes;
import org.eclipse.jgit.internal.transport.sshd.JGitClientSession.SessionAttributes;
import org.eclipse.jgit.internal.transport.sshd.proxy.HttpClientConnector;
import org.eclipse.jgit.internal.transport.sshd.proxy.Socks5ClientConnector;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.SshConstants;
import org.eclipse.jgit.transport.sshd.KeyCache;
import org.eclipse.jgit.transport.sshd.KeyPasswordProvider;
import org.eclipse.jgit.transport.sshd.ProxyData;
import org.eclipse.jgit.transport.sshd.ProxyDataFactory;
import org.eclipse.jgit.util.StringUtils;

/**
 * Customized {@link SshClient} for JGit. It creates specialized
 * {@link JGitClientSession}s that know about the {@link HostConfigEntry} they
 * were created for, and it loads all KeyPair identities lazily.
 */
public class JGitSshClient extends SshClient {

	/**
	 * We need access to this during the constructor of the ClientSession,
	 * before setConnectAddress() can have been called. So we have to remember
	 * it in an attribute on the SshClient, from where we can then retrieve it.
	 */
	static final AttributeKey<HostConfigEntry> HOST_CONFIG_ENTRY = new AttributeKey<>();

	static final AttributeKey<InetSocketAddress> ORIGINAL_REMOTE_ADDRESS = new AttributeKey<>();

	/**
	 * An attribute key for the comma-separated list of default preferred
	 * authentication mechanisms.
	 */
	public static final AttributeKey<String> PREFERRED_AUTHENTICATIONS = new AttributeKey<>();

	/**
	 * An attribute key for the home directory.
	 */
	public static final AttributeKey<Path> HOME_DIRECTORY = new AttributeKey<>();

	/**
	 * An attribute key for storing an alternate local address to connect to if
	 * a local forward from a ProxyJump ssh config is present. If set,
	 * {@link #connect(HostConfigEntry, AttributeRepository, SocketAddress)}
	 * will not connect to the address obtained from the {@link HostConfigEntry}
	 * but to the address stored in this key (which is assumed to forward the
	 * {@code HostConfigEntry} address).
	 */
	public static final AttributeKey<SshdSocketAddress> LOCAL_FORWARD_ADDRESS = new AttributeKey<>();

	private KeyCache keyCache;

	private CredentialsProvider credentialsProvider;

	private Supplier<KeyPasswordProvider> keyPasswordProviderFactory;

	private ProxyDataFactory proxyDatabase;

	@Override
	protected SessionFactory createSessionFactory() {
		// Override the parent's default
		return new JGitSessionFactory(this);
	}

	@Override
	public ConnectFuture connect(HostConfigEntry hostConfig,
			AttributeRepository context, SocketAddress localAddress)
			throws IOException {
		if (connector == null) {
			throw new IllegalStateException("SshClient not started."); //$NON-NLS-1$
		}
		Objects.requireNonNull(hostConfig, "No host configuration"); //$NON-NLS-1$
		String originalHost = ValidateUtils.checkNotNullAndNotEmpty(
				hostConfig.getHostName(), "No target host"); //$NON-NLS-1$
		int originalPort = hostConfig.getPort();
		ValidateUtils.checkTrue(originalPort > 0, "Invalid port: %d", //$NON-NLS-1$
				originalPort);
		InetSocketAddress originalAddress = new InetSocketAddress(originalHost,
				originalPort);
		InetSocketAddress targetAddress = originalAddress;
		String userName = hostConfig.getUsername();
		String id = userName + '@' + originalAddress;
		AttributeRepository attributes = chain(context, this);
		SshdSocketAddress localForward = attributes
				.resolveAttribute(LOCAL_FORWARD_ADDRESS);
		if (localForward != null) {
			targetAddress = new InetSocketAddress(localForward.getHostName(),
					localForward.getPort());
			id += '/' + targetAddress.toString();
		}
		ConnectFuture connectFuture = new DefaultConnectFuture(id, null);
		SshFutureListener<IoConnectFuture> listener = createConnectCompletionListener(
				connectFuture, userName, originalAddress, hostConfig);
		attributes = sessionAttributes(attributes, hostConfig, originalAddress);
		// Proxy support
		if (localForward == null) {
			ProxyData proxy = getProxyData(targetAddress);
			if (proxy != null) {
				targetAddress = configureProxy(proxy, targetAddress);
				proxy.clearPassword();
			}
		}
		connector.connect(targetAddress, attributes, localAddress)
				.addListener(listener);
		return connectFuture;
	}

	private AttributeRepository chain(AttributeRepository self,
			AttributeRepository parent) {
		if (self == null) {
			return Objects.requireNonNull(parent);
		}
		if (parent == null || parent == self) {
			return self;
		}
		return new ChainingAttributes(self, parent);
	}

	private AttributeRepository sessionAttributes(AttributeRepository parent,
			HostConfigEntry hostConfig, InetSocketAddress originalAddress) {
		// sshd needs some entries from the host config already in the
		// constructor of the session. Put those into a dedicated
		// AttributeRepository for the new session where it will find them.
		// We can set the host config only once the session object has been
		// created.
		Map<AttributeKey<?>, Object> data = new HashMap<>();
		data.put(HOST_CONFIG_ENTRY, hostConfig);
		data.put(ORIGINAL_REMOTE_ADDRESS, originalAddress);
		data.put(TARGET_SERVER, new SshdSocketAddress(originalAddress));
		String preferredAuths = hostConfig.getProperty(
				SshConstants.PREFERRED_AUTHENTICATIONS,
				resolveAttribute(PREFERRED_AUTHENTICATIONS));
		if (!StringUtils.isEmptyOrNull(preferredAuths)) {
			data.put(SessionAttributes.PROPERTIES,
					Collections.singletonMap(
							PREFERRED_AUTHS.getName(),
							preferredAuths));
		}
		return new SessionAttributes(
				AttributeRepository.ofAttributesMap(data),
				parent, this);
	}

	private ProxyData getProxyData(InetSocketAddress remoteAddress) {
		ProxyDataFactory factory = getProxyDatabase();
		return factory == null ? null : factory.get(remoteAddress);
	}

	private InetSocketAddress configureProxy(ProxyData proxyData,
			InetSocketAddress remoteAddress) {
		Proxy proxy = proxyData.getProxy();
		if (proxy.type() == Proxy.Type.DIRECT
				|| !(proxy.address() instanceof InetSocketAddress)) {
			return remoteAddress;
		}
		InetSocketAddress address = (InetSocketAddress) proxy.address();
		if (address.isUnresolved()) {
			address = new InetSocketAddress(address.getHostName(),
					address.getPort());
		}
		switch (proxy.type()) {
		case HTTP:
			setClientProxyConnector(
					new HttpClientConnector(address, remoteAddress,
							proxyData.getUser(), proxyData.getPassword()));
			return address;
		case SOCKS:
			setClientProxyConnector(
					new Socks5ClientConnector(address, remoteAddress,
							proxyData.getUser(), proxyData.getPassword()));
			return address;
		default:
			log.warn(format(SshdText.get().unknownProxyProtocol,
					proxy.type().name()));
			return remoteAddress;
		}
	}

	private SshFutureListener<IoConnectFuture> createConnectCompletionListener(
			ConnectFuture connectFuture, String username,
			InetSocketAddress address, HostConfigEntry hostConfig) {
		return new SshFutureListener<>() {

			@Override
			public void operationComplete(IoConnectFuture future) {
				if (future.isCanceled()) {
					connectFuture.cancel();
					return;
				}
				Throwable t = future.getException();
				if (t != null) {
					connectFuture.setException(t);
					return;
				}
				IoSession ioSession = future.getSession();
				try {
					JGitClientSession session = createSession(ioSession,
							username, address, hostConfig);
					connectFuture.setSession(session);
				} catch (RuntimeException e) {
					connectFuture.setException(e);
					ioSession.close(true);
				}
			}

			@Override
			public String toString() {
				return "JGitSshClient$ConnectCompletionListener[" + username //$NON-NLS-1$
						+ '@' + address + ']';
			}
		};
	}

	private JGitClientSession createSession(IoSession ioSession,
			String username, InetSocketAddress address,
			HostConfigEntry hostConfig) {
		AbstractSession rawSession = AbstractSession.getSession(ioSession);
		if (!(rawSession instanceof JGitClientSession)) {
			throw new IllegalStateException("Wrong session type: " //$NON-NLS-1$
					+ rawSession.getClass().getCanonicalName());
		}
		JGitClientSession session = (JGitClientSession) rawSession;
		session.setUsername(username);
		session.setConnectAddress(address);
		session.setHostConfigEntry(hostConfig);
		if (session.getCredentialsProvider() == null) {
			session.setCredentialsProvider(getCredentialsProvider());
		}
		int numberOfPasswordPrompts = getNumberOfPasswordPrompts(hostConfig);
		PASSWORD_PROMPTS.set(session, Integer.valueOf(numberOfPasswordPrompts));
		session.setAttribute(JGitClientSession.KEY_PASSWORD_PROVIDER_FACTORY,
				getKeyPasswordProviderFactory());
		List<Path> identities = hostConfig.getIdentities().stream()
				.map(s -> {
					try {
						return Paths.get(s);
					} catch (InvalidPathException e) {
						log.warn(format(SshdText.get().configInvalidPath,
								SshConstants.IDENTITY_FILE, s), e);
						return null;
					}
				}).filter(p -> p != null && Files.exists(p))
				.collect(Collectors.toList());
		CachingKeyPairProvider ourConfiguredKeysProvider = new CachingKeyPairProvider(
				identities, keyCache);
		FilePasswordProvider passwordProvider = getFilePasswordProvider();
		ourConfiguredKeysProvider.setPasswordFinder(passwordProvider);
		if (hostConfig.isIdentitiesOnly()) {
			session.setKeyIdentityProvider(ourConfiguredKeysProvider);
		} else {
			KeyIdentityProvider defaultKeysProvider = getKeyIdentityProvider();
			if (defaultKeysProvider instanceof AbstractResourceKeyPairProvider<?>) {
				((AbstractResourceKeyPairProvider<?>) defaultKeysProvider)
						.setPasswordFinder(passwordProvider);
			}
			KeyIdentityProvider combinedProvider = new CombinedKeyIdentityProvider(
					ourConfiguredKeysProvider, defaultKeysProvider);
			session.setKeyIdentityProvider(combinedProvider);
		}
		return session;
	}

	private int getNumberOfPasswordPrompts(HostConfigEntry hostConfig) {
		String prompts = hostConfig
				.getProperty(SshConstants.NUMBER_OF_PASSWORD_PROMPTS);
		if (prompts != null) {
			prompts = prompts.trim();
			int value = positive(prompts);
			if (value > 0) {
				return value;
			}
			log.warn(format(SshdText.get().configInvalidPositive,
					SshConstants.NUMBER_OF_PASSWORD_PROMPTS, prompts));
		}
		return PASSWORD_PROMPTS.getRequiredDefault().intValue();
	}

	/**
	 * Set a cache for loaded keys. Newly discovered keys will be added when
	 * IdentityFile host entries from the ssh config file are used during
	 * session authentication.
	 *
	 * @param cache
	 *            to use
	 */
	public void setKeyCache(KeyCache cache) {
		keyCache = cache;
	}

	/**
	 * Sets a {@link ProxyDataFactory} for connecting through proxies.
	 *
	 * @param factory
	 *            to use, or {@code null} if proxying is not desired or
	 *            supported
	 */
	public void setProxyDatabase(ProxyDataFactory factory) {
		proxyDatabase = factory;
	}

	/**
	 * Retrieves the {@link ProxyDataFactory}.
	 *
	 * @return the factory, or {@code null} if none is set
	 */
	protected ProxyDataFactory getProxyDatabase() {
		return proxyDatabase;
	}

	/**
	 * Sets the {@link CredentialsProvider} for this client.
	 *
	 * @param provider
	 *            to set
	 */
	public void setCredentialsProvider(CredentialsProvider provider) {
		credentialsProvider = provider;
	}

	/**
	 * Retrieves the {@link CredentialsProvider} set for this client.
	 *
	 * @return the provider, or {@code null} if none is set.
	 */
	public CredentialsProvider getCredentialsProvider() {
		return credentialsProvider;
	}

	/**
	 * Sets a supplier for a {@link KeyPasswordProvider} for this client.
	 *
	 * @param factory
	 *            to set
	 */
	public void setKeyPasswordProviderFactory(
			Supplier<KeyPasswordProvider> factory) {
		keyPasswordProviderFactory = factory;
	}

	/**
	 * Retrieves the {@link KeyPasswordProvider} factory of this client.
	 *
	 * @return a factory to create {@link KeyPasswordProvider}s
	 */
	public Supplier<KeyPasswordProvider> getKeyPasswordProviderFactory() {
		return keyPasswordProviderFactory;
	}

	/**
	 * A {@link SessionFactory} to create our own specialized
	 * {@link JGitClientSession}s.
	 */
	private static class JGitSessionFactory extends SessionFactory {

		public JGitSessionFactory(JGitSshClient client) {
			super(client);
		}

		@Override
		protected ClientSessionImpl doCreateSession(IoSession ioSession)
				throws Exception {
			return new JGitClientSession(getClient(), ioSession);
		}
	}

	/**
	 * A {@link KeyIdentityProvider} that iterates over the {@link Iterable}s
	 * returned by other {@link KeyIdentityProvider}s.
	 */
	private static class CombinedKeyIdentityProvider
			implements KeyIdentityProvider {

		private final List<KeyIdentityProvider> providers;

		public CombinedKeyIdentityProvider(KeyIdentityProvider... providers) {
			this(Arrays.stream(providers).filter(Objects::nonNull)
					.collect(Collectors.toList()));
		}

		public CombinedKeyIdentityProvider(
				List<KeyIdentityProvider> providers) {
			this.providers = providers;
		}

		@Override
		public Iterable<KeyPair> loadKeys(SessionContext context) {
			return () -> new Iterator<>() {

				private Iterator<KeyIdentityProvider> factories = providers
						.iterator();
				private Iterator<KeyPair> current;

				private Boolean hasElement;

				@Override
				public boolean hasNext() {
					if (hasElement != null) {
						return hasElement.booleanValue();
					}
					while (current == null || !current.hasNext()) {
						if (factories.hasNext()) {
							try {
								current = factories.next().loadKeys(context)
										.iterator();
							} catch (IOException | GeneralSecurityException e) {
								throw new RuntimeException(e);
							}
						} else {
							current = null;
							hasElement = Boolean.FALSE;
							return false;
						}
					}
					hasElement = Boolean.TRUE;
					return true;
				}

				@Override
				public KeyPair next() {
					if ((hasElement == null && !hasNext())
							|| !hasElement.booleanValue()) {
						throw new NoSuchElementException();
					}
					hasElement = null;
					KeyPair result;
					try {
						result = current.next();
					} catch (NoSuchElementException e) {
						result = null;
					}
					return result;
				}

			};
		}
	}
}
