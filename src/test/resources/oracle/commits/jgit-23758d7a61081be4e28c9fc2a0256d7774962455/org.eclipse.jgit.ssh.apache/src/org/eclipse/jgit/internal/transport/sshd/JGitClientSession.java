/*
 * Copyright (C) 2018, 2021 Thomas Wolf <thomas.wolf@paranor.ch> and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0 which is available at
 * https://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package org.eclipse.jgit.internal.transport.sshd;

import static java.text.MessageFormat.format;
import static org.apache.sshd.core.CoreModuleProperties.MAX_IDENTIFICATION_SIZE;

import java.io.IOException;
import java.io.StreamCorruptedException;
import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;

import org.apache.sshd.client.ClientBuilder;
import org.apache.sshd.client.ClientFactoryManager;
import org.apache.sshd.client.config.hosts.HostConfigEntry;
import org.apache.sshd.client.keyverifier.ServerKeyVerifier;
import org.apache.sshd.client.session.ClientSessionImpl;
import org.apache.sshd.common.AttributeRepository;
import org.apache.sshd.common.FactoryManager;
import org.apache.sshd.common.NamedResource;
import org.apache.sshd.common.PropertyResolver;
import org.apache.sshd.common.config.keys.KeyUtils;
import org.apache.sshd.common.io.IoSession;
import org.apache.sshd.common.io.IoWriteFuture;
import org.apache.sshd.common.kex.BuiltinDHFactories;
import org.apache.sshd.common.kex.DHFactory;
import org.apache.sshd.common.kex.KeyExchangeFactory;
import org.apache.sshd.common.kex.extension.KexExtensionHandler;
import org.apache.sshd.common.kex.extension.KexExtensionHandler.AvailabilityPhase;
import org.apache.sshd.common.kex.extension.KexExtensions;
import org.apache.sshd.common.keyprovider.KeyPairProvider;
import org.apache.sshd.common.signature.BuiltinSignatures;
import org.apache.sshd.common.util.Readable;
import org.apache.sshd.common.util.buffer.Buffer;
import org.eclipse.jgit.errors.InvalidPatternException;
import org.eclipse.jgit.fnmatch.FileNameMatcher;
import org.eclipse.jgit.internal.transport.sshd.proxy.StatefulProxyConnector;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.SshConstants;
import org.eclipse.jgit.transport.sshd.KeyPasswordProvider;
import org.eclipse.jgit.util.StringUtils;

/**
 * A {@link org.apache.sshd.client.session.ClientSession ClientSession} that can
 * be associated with the {@link HostConfigEntry} the session was created for.
 * The {@link JGitSshClient} creates such sessions and sets this association.
 * <p>
 * Also provides for associating a JGit {@link CredentialsProvider} with a
 * session.
 * </p>
 */
public class JGitClientSession extends ClientSessionImpl {

	/**
	 * Attribute set by {@link JGitSshClient} to make the
	 * {@link KeyPasswordProvider} factory accessible via the session.
	 */
	public static final AttributeKey<Supplier<KeyPasswordProvider>> KEY_PASSWORD_PROVIDER_FACTORY = new AttributeKey<>();

	/**
	 * Default setting for the maximum number of bytes to read in the initial
	 * protocol version exchange. 64kb is what OpenSSH &lt; 8.0 read; OpenSSH
	 * 8.0 changed it to 8Mb, but that seems excessive for the purpose stated in
	 * RFC 4253. The Apache MINA sshd default in
	 * {@link org.apache.sshd.core.CoreModuleProperties#MAX_IDENTIFICATION_SIZE}
	 * is 16kb.
	 */
	private static final int DEFAULT_MAX_IDENTIFICATION_SIZE = 64 * 1024;

	private static final AttributeKey<Boolean> INITIAL_KEX_DONE = new AttributeKey<>();

	private HostConfigEntry hostConfig;

	private CredentialsProvider credentialsProvider;

	private volatile StatefulProxyConnector proxyHandler;

	/**
	 * @param manager
	 *            client factory manager
	 * @param session
	 *            the session
	 * @throws Exception
	 *             an error occurred
	 */
	public JGitClientSession(ClientFactoryManager manager, IoSession session)
			throws Exception {
		super(manager, session);
	}

	/**
	 * Retrieves the {@link HostConfigEntry} this session was created for.
	 *
	 * @return the {@link HostConfigEntry}, or {@code null} if none set
	 */
	public HostConfigEntry getHostConfigEntry() {
		return hostConfig;
	}

	/**
	 * Sets the {@link HostConfigEntry} this session was created for.
	 *
	 * @param hostConfig
	 *            the {@link HostConfigEntry}
	 */
	public void setHostConfigEntry(HostConfigEntry hostConfig) {
		this.hostConfig = hostConfig;
	}

	/**
	 * Sets the {@link CredentialsProvider} for this session.
	 *
	 * @param provider
	 *            to set
	 */
	public void setCredentialsProvider(CredentialsProvider provider) {
		credentialsProvider = provider;
	}

	/**
	 * Retrieves the {@link CredentialsProvider} set for this session.
	 *
	 * @return the provider, or {@code null} if none is set.
	 */
	public CredentialsProvider getCredentialsProvider() {
		return credentialsProvider;
	}

	/**
	 * Sets a {@link StatefulProxyConnector} to handle proxy connection
	 * protocols.
	 *
	 * @param handler
	 *            to set
	 */
	public void setProxyHandler(StatefulProxyConnector handler) {
		proxyHandler = handler;
	}

	@Override
	protected IoWriteFuture sendIdentification(String ident,
			List<String> extraLines) throws Exception {
		StatefulProxyConnector proxy = proxyHandler;
		if (proxy != null) {
			// We must not block here; the framework starts reading messages
			// from the peer only once the initial sendKexInit() following
			// this call to sendIdentification() has returned!
			proxy.runWhenDone(() -> {
				JGitClientSession.super.sendIdentification(ident, extraLines);
				return null;
			});
			// Called only from the ClientSessionImpl constructor, where the
			// return value is ignored.
			return null;
		}
		return super.sendIdentification(ident, extraLines);
	}

	@Override
	protected byte[] sendKexInit() throws Exception {
		StatefulProxyConnector proxy = proxyHandler;
		if (proxy != null) {
			// We must not block here; the framework starts reading messages
			// from the peer only once the initial sendKexInit() has
			// returned!
			proxy.runWhenDone(() -> {
				JGitClientSession.super.sendKexInit();
				return null;
			});
			// This is called only from the ClientSessionImpl
			// constructor, where the return value is ignored.
			return null;
		}
		return super.sendKexInit();
	}

	/**
	 * {@inheritDoc}
	 *
	 * As long as we're still setting up the proxy connection, diverts messages
	 * to the {@link StatefulProxyConnector}.
	 */
	@Override
	public void messageReceived(Readable buffer) throws Exception {
		StatefulProxyConnector proxy = proxyHandler;
		if (proxy != null) {
			proxy.messageReceived(getIoSession(), buffer);
		} else {
			super.messageReceived(buffer);
		}
	}

	Set<String> getAllAvailableSignatureAlgorithms() {
		Set<String> allAvailable = new HashSet<>();
		BuiltinSignatures.VALUES.forEach(s -> allAvailable.add(s.getName()));
		BuiltinSignatures.getRegisteredExtensions()
				.forEach(s -> allAvailable.add(s.getName()));
		return allAvailable;
	}

	private void setNewFactories(Collection<String> defaultFactories,
			Collection<String> finalFactories) {
		// If new factory names were added make sure we actually have factories
		// for them all.
		//
		// But add new ones at the end: we don't want to change the order for
		// pubkey auth, and any new ones added here were not included in the
		// default set for some reason, such as being deprecated or weak.
		//
		// The order for KEX is determined by the order in the proposal string,
		// but the order in pubkey auth is determined by the order in the
		// factory list (possibly overridden via ssh config
		// PubkeyAcceptedAlgorithms; see JGitPublicKeyAuthentication).
		Set<String> resultSet = new LinkedHashSet<>(defaultFactories);
		resultSet.addAll(finalFactories);
		setSignatureFactoriesNames(resultSet);
	}

	@Override
	protected String resolveAvailableSignaturesProposal(
			FactoryManager manager) {
		List<String> defaultSignatures = getSignatureFactoriesNames();
		HostConfigEntry config = resolveAttribute(
				JGitSshClient.HOST_CONFIG_ENTRY);
		String algorithms = config
				.getProperty(SshConstants.HOST_KEY_ALGORITHMS);
		if (!StringUtils.isEmptyOrNull(algorithms)) {
			List<String> result = modifyAlgorithmList(defaultSignatures,
					getAllAvailableSignatureAlgorithms(), algorithms,
					SshConstants.HOST_KEY_ALGORITHMS);
			if (!result.isEmpty()) {
				if (log.isDebugEnabled()) {
					log.debug(SshConstants.HOST_KEY_ALGORITHMS + ' ' + result);
				}
				setNewFactories(defaultSignatures, result);
				return String.join(",", result); //$NON-NLS-1$
			}
			log.warn(format(SshdText.get().configNoKnownAlgorithms,
					SshConstants.HOST_KEY_ALGORITHMS, algorithms));
		}
		// No HostKeyAlgorithms; using default -- change order to put existing
		// keys first.
		ServerKeyVerifier verifier = getServerKeyVerifier();
		if (verifier instanceof ServerKeyLookup) {
			SocketAddress remoteAddress = resolvePeerAddress(
					resolveAttribute(JGitSshClient.ORIGINAL_REMOTE_ADDRESS));
			List<PublicKey> allKnownKeys = ((ServerKeyLookup) verifier)
					.lookup(this, remoteAddress);
			Set<String> reordered = new LinkedHashSet<>();
			for (PublicKey key : allKnownKeys) {
				if (key != null) {
					String keyType = KeyUtils.getKeyType(key);
					if (keyType != null) {
						if (KeyPairProvider.SSH_RSA.equals(keyType)) {
							// Add all available signatures for ssh-rsa.
							reordered.add(KeyUtils.RSA_SHA512_KEY_TYPE_ALIAS);
							reordered.add(KeyUtils.RSA_SHA256_KEY_TYPE_ALIAS);
						}
						reordered.add(keyType);
					}
				}
			}
			reordered.addAll(defaultSignatures);
			if (log.isDebugEnabled()) {
				log.debug(SshConstants.HOST_KEY_ALGORITHMS + ' ' + reordered);
			}
			// Make sure we actually have factories for them all.
			if (reordered.size() > defaultSignatures.size()) {
				setNewFactories(defaultSignatures, reordered);
			}
			return String.join(",", reordered); //$NON-NLS-1$
		}
		if (log.isDebugEnabled()) {
			log.debug(
					SshConstants.HOST_KEY_ALGORITHMS + ' ' + defaultSignatures);
		}
		return String.join(",", defaultSignatures); //$NON-NLS-1$
	}

	private List<String> determineKexProposal() {
		List<KeyExchangeFactory> kexFactories = getKeyExchangeFactories();
		List<String> defaultKexMethods = NamedResource
				.getNameList(kexFactories);
		HostConfigEntry config = resolveAttribute(
				JGitSshClient.HOST_CONFIG_ENTRY);
		String algorithms = config.getProperty(SshConstants.KEX_ALGORITHMS);
		if (!StringUtils.isEmptyOrNull(algorithms)) {
			Set<String> allAvailable = new HashSet<>();
			BuiltinDHFactories.VALUES
					.forEach(s -> allAvailable.add(s.getName()));
			BuiltinDHFactories.getRegisteredExtensions()
					.forEach(s -> allAvailable.add(s.getName()));
			List<String> result = modifyAlgorithmList(defaultKexMethods,
					allAvailable, algorithms, SshConstants.KEX_ALGORITHMS);
			if (!result.isEmpty()) {
				// If new ones were added, update the installed factories
				Set<String> configuredKexMethods = new HashSet<>(
						defaultKexMethods);
				List<KeyExchangeFactory> newKexFactories = new ArrayList<>();
				result.forEach(name -> {
					if (!configuredKexMethods.contains(name)) {
						DHFactory factory = BuiltinDHFactories
								.resolveFactory(name);
						if (factory == null) {
							// Should not occur here
							if (log.isDebugEnabled()) {
								log.debug(
										"determineKexProposal({}) unknown KEX algorithm {} ignored", //$NON-NLS-1$
										this, name);
							}
						} else {
							newKexFactories
									.add(ClientBuilder.DH2KEX.apply(factory));
						}
					}
				});
				if (!newKexFactories.isEmpty()) {
					newKexFactories.addAll(kexFactories);
					setKeyExchangeFactories(newKexFactories);
				}
				return result;
			}
			log.warn(format(SshdText.get().configNoKnownAlgorithms,
					SshConstants.KEX_ALGORITHMS, algorithms));
		}
		return defaultKexMethods;
	}

	@Override
	protected String resolveSessionKexProposal(String hostKeyTypes)
			throws IOException {
		String kexMethods = String.join(",", determineKexProposal()); //$NON-NLS-1$
		Boolean isRekey = getAttribute(INITIAL_KEX_DONE);
		if (isRekey == null || !isRekey.booleanValue()) {
			// First time
			KexExtensionHandler extHandler = getKexExtensionHandler();
			if (extHandler != null && extHandler.isKexExtensionsAvailable(this,
					AvailabilityPhase.PROPOSAL)) {
				if (kexMethods.isEmpty()) {
					kexMethods = KexExtensions.CLIENT_KEX_EXTENSION;
				} else {
					kexMethods += ',' + KexExtensions.CLIENT_KEX_EXTENSION;
				}
			}
			setAttribute(INITIAL_KEX_DONE, Boolean.TRUE);
		}
		if (log.isDebugEnabled()) {
			log.debug(SshConstants.KEX_ALGORITHMS + ' ' + kexMethods);
		}
		return kexMethods;
	}

	/**
	 * Modifies a given algorithm list according to a list from the ssh config,
	 * including add ('+'), remove ('-') and reordering ('^') operators.
	 *
	 * @param defaultList
	 *            to modify
	 * @param allAvailable
	 *            all available values
	 * @param fromConfig
	 *            telling how to modify the {@code defaultList}, must not be
	 *            {@code null} or empty
	 * @param overrideKey
	 *            ssh config key; used for logging
	 * @return the modified list or {@code null} if {@code overrideKey} is not
	 *         set
	 */
	public List<String> modifyAlgorithmList(List<String> defaultList,
			Set<String> allAvailable, String fromConfig, String overrideKey) {
		Set<String> defaults = new LinkedHashSet<>();
		defaults.addAll(defaultList);
		switch (fromConfig.charAt(0)) {
		case '+':
			List<String> newSignatures = filteredList(allAvailable, overrideKey,
					fromConfig.substring(1));
			defaults.addAll(newSignatures);
			return new ArrayList<>(defaults);
		case '-':
			// This takes wildcard patterns!
			removeFromList(defaults, overrideKey, fromConfig.substring(1));
			return new ArrayList<>(defaults);
		case '^':
			// Specified entries go to the front of the default list
			List<String> allSignatures = filteredList(allAvailable, overrideKey,
					fromConfig.substring(1));
			Set<String> atFront = new HashSet<>(allSignatures);
			for (String sig : defaults) {
				if (!atFront.contains(sig)) {
					allSignatures.add(sig);
				}
			}
			return allSignatures;
		default:
			// Default is overridden -- only accept the ones for which we do
			// have an implementation.
			return filteredList(allAvailable, overrideKey, fromConfig);
		}
	}

	private void removeFromList(Set<String> current, String key,
			String patterns) {
		for (String toRemove : patterns.split("\\s*,\\s*")) { //$NON-NLS-1$
			if (toRemove.indexOf('*') < 0 && toRemove.indexOf('?') < 0) {
				current.remove(toRemove);
				continue;
			}
			try {
				FileNameMatcher matcher = new FileNameMatcher(toRemove, null);
				for (Iterator<String> i = current.iterator(); i.hasNext();) {
					matcher.reset();
					matcher.append(i.next());
					if (matcher.isMatch()) {
						i.remove();
					}
				}
			} catch (InvalidPatternException e) {
				log.warn(format(SshdText.get().configInvalidPattern, key,
						toRemove));
			}
		}
	}

	private List<String> filteredList(Set<String> known, String key,
			String values) {
		List<String> newNames = new ArrayList<>();
		for (String newValue : values.split("\\s*,\\s*")) { //$NON-NLS-1$
			if (known.contains(newValue)) {
				newNames.add(newValue);
			} else {
				log.warn(format(SshdText.get().configUnknownAlgorithm, this,
						newValue, key, values));
			}
		}
		return newNames;
	}

	/**
	 * Reads the RFC 4253, section 4.2 protocol version identification. The
	 * Apache MINA sshd default implementation checks for NUL bytes also in any
	 * preceding lines, whereas RFC 4253 requires such a check only for the
	 * actual identification string starting with "SSH-". Likewise, the 255
	 * character limit exists only for the identification string, not for the
	 * preceding lines. CR-LF handling is also relaxed.
	 *
	 * @param buffer
	 *            to read from
	 * @param server
	 *            whether we're an SSH server (should always be {@code false})
	 * @return the lines read, with the server identification line last, or
	 *         {@code null} if no identification line was found and more bytes
	 *         are needed
	 * @throws StreamCorruptedException
	 *             if the identification is malformed
	 * @see <a href="https://tools.ietf.org/html/rfc4253#section-4.2">RFC 4253,
	 *      section 4.2</a>
	 */
	@Override
	protected List<String> doReadIdentification(Buffer buffer, boolean server)
			throws StreamCorruptedException {
		if (server) {
			// Should never happen. No translation; internal bug.
			throw new IllegalStateException(
					"doReadIdentification of client called with server=true"); //$NON-NLS-1$
		}
		Integer maxIdentLength = MAX_IDENTIFICATION_SIZE.get(this).orElse(null);
		int maxIdentSize;
		if (maxIdentLength == null || maxIdentLength
				.intValue() < DEFAULT_MAX_IDENTIFICATION_SIZE) {
			maxIdentSize = DEFAULT_MAX_IDENTIFICATION_SIZE;
			MAX_IDENTIFICATION_SIZE.set(this, Integer.valueOf(maxIdentSize));
		} else {
			maxIdentSize = maxIdentLength.intValue();
		}
		int current = buffer.rpos();
		int end = current + buffer.available();
		if (current >= end) {
			return null;
		}
		byte[] raw = buffer.array();
		List<String> ident = new ArrayList<>();
		int start = current;
		boolean hasNul = false;
		for (int i = current; i < end; i++) {
			switch (raw[i]) {
			case 0:
				hasNul = true;
				break;
			case '\n':
				int eol = 1;
				if (i > start && raw[i - 1] == '\r') {
					eol++;
				}
				String line = new String(raw, start, i + 1 - eol - start,
						StandardCharsets.UTF_8);
				start = i + 1;
				if (log.isDebugEnabled()) {
					log.debug(format("doReadIdentification({0}) line: ", this) + //$NON-NLS-1$
							escapeControls(line));
				}
				ident.add(line);
				if (line.startsWith("SSH-")) { //$NON-NLS-1$
					if (hasNul) {
						throw new StreamCorruptedException(
								format(SshdText.get().serverIdWithNul,
										escapeControls(line)));
					}
					if (line.length() + eol > 255) {
						throw new StreamCorruptedException(
								format(SshdText.get().serverIdTooLong,
										escapeControls(line)));
					}
					buffer.rpos(start);
					return ident;
				}
				// If this were a server, we could throw an exception here: a
				// client is not supposed to send any extra lines before its
				// identification string.
				hasNul = false;
				break;
			default:
				break;
			}
			if (i - current + 1 >= maxIdentSize) {
				String msg = format(SshdText.get().serverIdNotReceived,
						Integer.toString(maxIdentSize));
				if (log.isDebugEnabled()) {
					log.debug(msg);
					log.debug(buffer.toHex());
				}
				throw new StreamCorruptedException(msg);
			}
		}
		// Need more data
		return null;
	}

	private static String escapeControls(String s) {
		StringBuilder b = new StringBuilder();
		int l = s.length();
		for (int i = 0; i < l; i++) {
			char ch = s.charAt(i);
			if (Character.isISOControl(ch)) {
				b.append(ch <= 0xF ? "\\u000" : "\\u00") //$NON-NLS-1$ //$NON-NLS-2$
						.append(Integer.toHexString(ch));
			} else {
				b.append(ch);
			}
		}
		return b.toString();
	}

	@Override
	public <T> T getAttribute(AttributeKey<T> key) {
		T value = super.getAttribute(key);
		if (value == null) {
			IoSession ioSession = getIoSession();
			if (ioSession != null) {
				Object obj = ioSession.getAttribute(AttributeRepository.class);
				if (obj instanceof AttributeRepository) {
					AttributeRepository sessionAttributes = (AttributeRepository) obj;
					value = sessionAttributes.resolveAttribute(key);
				}
			}
		}
		return value;
	}

	@Override
	public PropertyResolver getParentPropertyResolver() {
		IoSession ioSession = getIoSession();
		if (ioSession != null) {
			Object obj = ioSession.getAttribute(AttributeRepository.class);
			if (obj instanceof PropertyResolver) {
				return (PropertyResolver) obj;
			}
		}
		return super.getParentPropertyResolver();
	}

	/**
	 * An {@link AttributeRepository} that chains together two other attribute
	 * sources in a hierarchy.
	 */
	public static class ChainingAttributes implements AttributeRepository {

		private final AttributeRepository delegate;

		private final AttributeRepository parent;

		/**
		 * Create a new {@link ChainingAttributes} attribute source.
		 *
		 * @param self
		 *            to search for attributes first
		 * @param parent
		 *            to search for attributes if not found in {@code self}
		 */
		public ChainingAttributes(AttributeRepository self,
				AttributeRepository parent) {
			this.delegate = self;
			this.parent = parent;
		}

		@Override
		public int getAttributesCount() {
			return delegate.getAttributesCount();
		}

		@Override
		public <T> T getAttribute(AttributeKey<T> key) {
			return delegate.getAttribute(Objects.requireNonNull(key));
		}

		@Override
		public Collection<AttributeKey<?>> attributeKeys() {
			return delegate.attributeKeys();
		}

		@Override
		public <T> T resolveAttribute(AttributeKey<T> key) {
			T value = getAttribute(Objects.requireNonNull(key));
			if (value == null) {
				return parent.getAttribute(key);
			}
			return value;
		}
	}

	/**
	 * A {@link ChainingAttributes} repository that doubles as a
	 * {@link PropertyResolver}. The property map can be set via the attribute
	 * key {@link SessionAttributes#PROPERTIES}.
	 */
	public static class SessionAttributes extends ChainingAttributes
			implements PropertyResolver {

		/** Key for storing a map of properties in the attributes. */
		public static final AttributeKey<Map<String, Object>> PROPERTIES = new AttributeKey<>();

		private final PropertyResolver parentProperties;

		/**
		 * Creates a new {@link SessionAttributes} attribute and property
		 * source.
		 *
		 * @param self
		 *            to search for attributes first
		 * @param parent
		 *            to search for attributes if not found in {@code self}
		 * @param parentProperties
		 *            to search for properties if not found in {@code self}
		 */
		public SessionAttributes(AttributeRepository self,
				AttributeRepository parent, PropertyResolver parentProperties) {
			super(self, parent);
			this.parentProperties = parentProperties;
		}

		@Override
		public PropertyResolver getParentPropertyResolver() {
			return parentProperties;
		}

		@Override
		public Map<String, Object> getProperties() {
			Map<String, Object> props = getAttribute(PROPERTIES);
			return props == null ? Collections.emptyMap() : props;
		}
	}
}
