/*
 * Copyright (C) 2018, 2023 Thomas Wolf <twolf@apache.org> and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0 which is available at
 * https://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package org.eclipse.jgit.internal.transport.sshd;

import static java.text.MessageFormat.format;
import static org.eclipse.jgit.transport.SshConstants.NONE;
import static org.eclipse.jgit.transport.SshConstants.PKCS11_PROVIDER;
import static org.eclipse.jgit.transport.SshConstants.PKCS11_SLOT_LIST_INDEX;
import static org.eclipse.jgit.transport.SshConstants.PUBKEY_ACCEPTED_ALGORITHMS;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.sshd.agent.SshAgent;
import org.apache.sshd.agent.SshAgentFactory;
import org.apache.sshd.agent.SshAgentKeyConstraint;
import org.apache.sshd.client.auth.pubkey.KeyAgentIdentity;
import org.apache.sshd.client.auth.pubkey.PublicKeyIdentity;
import org.apache.sshd.client.auth.pubkey.UserAuthPublicKey;
import org.apache.sshd.client.auth.pubkey.UserAuthPublicKeyIterator;
import org.apache.sshd.client.config.hosts.HostConfigEntry;
import org.apache.sshd.client.session.ClientSession;
import org.apache.sshd.common.FactoryManager;
import org.apache.sshd.common.NamedFactory;
import org.apache.sshd.common.config.keys.AuthorizedKeyEntry;
import org.apache.sshd.common.config.keys.KeyUtils;
import org.apache.sshd.common.config.keys.PublicKeyEntryResolver;
import org.apache.sshd.common.config.keys.u2f.SecurityKeyPublicKey;
import org.apache.sshd.common.signature.Signature;
import org.apache.sshd.common.signature.SignatureFactoriesManager;
import org.apache.sshd.common.util.GenericUtils;
import org.eclipse.jgit.internal.transport.ssh.OpenSshConfigFile;
import org.eclipse.jgit.internal.transport.sshd.pkcs11.Pkcs11Provider;
import org.eclipse.jgit.transport.CredentialItem;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.SshConstants;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.jgit.util.FS;
import org.eclipse.jgit.util.StringUtils;

/**
 * Custom {@link UserAuthPublicKey} implementation for handling SSH config
 * PubkeyAcceptedAlgorithms and interaction with the SSH agent and PKCS11
 * providers.
 */
public class JGitPublicKeyAuthentication extends UserAuthPublicKey {

	private static final String LOG_FORMAT = "{}"; //$NON-NLS-1$

	private SshAgent agent;

	private HostConfigEntry hostConfig;

	private boolean addKeysToAgent;

	private boolean askBeforeAdding;

	private String skProvider;

	private SshAgentKeyConstraint[] constraints;

	JGitPublicKeyAuthentication(List<NamedFactory<Signature>> factories) {
		super(factories);
	}

	@Override
	public void init(ClientSession rawSession, String service)
			throws Exception {
		if (!(rawSession instanceof JGitClientSession)) {
			throw new IllegalStateException("Wrong session type: " //$NON-NLS-1$
					+ rawSession.getClass().getCanonicalName());
		}
		JGitClientSession session = (JGitClientSession) rawSession;
		hostConfig = session.getHostConfigEntry();
		// Set signature algorithms for public key authentication
		String pubkeyAlgos = hostConfig.getProperty(PUBKEY_ACCEPTED_ALGORITHMS);
		if (!StringUtils.isEmptyOrNull(pubkeyAlgos)) {
			List<String> signatures = session.getSignatureFactoriesNames();
			signatures = session.modifyAlgorithmList(signatures,
					session.getAllAvailableSignatureAlgorithms(), pubkeyAlgos,
					PUBKEY_ACCEPTED_ALGORITHMS);
			if (!signatures.isEmpty()) {
				if (log.isDebugEnabled()) {
					log.debug(PUBKEY_ACCEPTED_ALGORITHMS + ' ' + signatures);
				}
				setSignatureFactoriesNames(signatures);
				super.init(session, service);
				return;
			}
			log.warn(LOG_FORMAT, format(SshdText.get().configNoKnownAlgorithms,
					PUBKEY_ACCEPTED_ALGORITHMS, pubkeyAlgos));
		}
		// TODO: remove this once we're on an sshd version that has SSHD-1272
		// fixed
		List<NamedFactory<Signature>> localFactories = getSignatureFactories();
		if (localFactories == null || localFactories.isEmpty()) {
			setSignatureFactoriesNames(session.getSignatureFactoriesNames());
		}
		super.init(session, service);
	}

	@Override
	protected Iterator<PublicKeyIdentity> createPublicKeyIterator(
			ClientSession session, SignatureFactoriesManager manager)
			throws Exception {
		agent = getAgent(session);
		if (agent != null) {
			parseAddKeys(hostConfig);
			if (addKeysToAgent) {
				skProvider = hostConfig.getProperty(SshConstants.SECURITY_KEY_PROVIDER);
			}
		}
		return new KeyIterator(session, manager);
	}

	@Override
	protected PublicKeyIdentity resolveAttemptedPublicKeyIdentity(
			ClientSession session, String service) throws Exception {
		PublicKeyIdentity id = super.resolveAttemptedPublicKeyIdentity(session,
				service);
		if (addKeysToAgent && id != null && !(id instanceof KeyAgentIdentity)) {
			KeyPair key = id.getKeyIdentity();
			if (key != null && key.getPublic() != null
					&& key.getPrivate() != null) {
				// We've just successfully loaded a key that wasn't in the
				// agent. Add it to the agent.
				//
				// Keys are added after loading, as in OpenSSH. The alternative
				// might be to add a key only after (partially) successful
				// authentication?
				PublicKey pk = key.getPublic();
				String fingerprint = KeyUtils.getFingerPrint(pk);
				String keyType = KeyUtils.getKeyType(key);
				try {
					// Check that the key is not in the agent already.
					if (agentHasKey(pk)) {
						return id;
					}
					if (askBeforeAdding
							&& (session instanceof JGitClientSession)) {
						CredentialsProvider provider = ((JGitClientSession) session)
								.getCredentialsProvider();
						CredentialItem.YesNoType question = new CredentialItem.YesNoType(
								format(SshdText
										.get().pubkeyAuthAddKeyToAgentQuestion,
										keyType, fingerprint));
						boolean result = provider != null
								&& provider.supports(question)
								&& provider.get(getUri(), question);
						if (!result || !question.getValue()) {
							// Don't add the key.
							return id;
						}
					}
					SshAgentKeyConstraint[] rules = constraints;
					if (pk instanceof SecurityKeyPublicKey && !StringUtils.isEmptyOrNull(skProvider)) {
						rules = Arrays.copyOf(rules, rules.length + 1);
						rules[rules.length - 1] =
								new SshAgentKeyConstraint.FidoProviderExtension(skProvider);
					}
					// Unfortunately a comment associated with the key is lost
					// by Apache MINA sshd, and there is also no way to get the
					// original file name for keys loaded from a file. So add it
					// without comment.
					agent.addIdentity(key, null, rules);
				} catch (IOException e) {
					// Do not re-throw: we don't want authentication to fail if
					// we cannot add the key to the agent.
					log.error(LOG_FORMAT,
							format(SshdText.get().pubkeyAuthAddKeyToAgentError,
									keyType, fingerprint),
							e);
					// Note that as of Win32-OpenSSH 8.6 and Pageant 0.76,
					// neither can handle key constraints. Pageant fails
					// gracefully, not adding the key and returning
					// SSH_AGENT_FAILURE. Win32-OpenSSH closes the connection
					// without even returning a failure message, which violates
					// the SSH agent protocol and makes all subsequent requests
					// to the agent fail.
				}
			}
		}
		return id;
	}

	private boolean agentHasKey(PublicKey pk) throws IOException {
		Iterable<? extends Map.Entry<PublicKey, String>> ids = agent
				.getIdentities();
		if (ids == null) {
			return false;
		}
		Iterator<? extends Map.Entry<PublicKey, String>> iter = ids.iterator();
		while (iter.hasNext()) {
			if (KeyUtils.compareKeys(iter.next().getKey(), pk)) {
				return true;
			}
		}
		return false;
	}

	private URIish getUri() {
		String uri = SshConstants.SSH_SCHEME + "://"; //$NON-NLS-1$
		String userName = hostConfig.getUsername();
		if (!StringUtils.isEmptyOrNull(userName)) {
			uri += userName + '@';
		}
		uri += hostConfig.getHost();
		int port = hostConfig.getPort();
		if (port > 0 && port != SshConstants.SSH_DEFAULT_PORT) {
			uri += ":" + port; //$NON-NLS-1$
		}
		try {
			return new URIish(uri);
		} catch (URISyntaxException e) {
			log.error(e.getLocalizedMessage(), e);
		}
		return new URIish();
	}

	private SshAgent getAgent(ClientSession session) throws Exception {
		FactoryManager manager = Objects.requireNonNull(
				session.getFactoryManager(), "No session factory manager"); //$NON-NLS-1$
		SshAgentFactory factory = manager.getAgentFactory();
		if (factory == null) {
			return null;
		}
		return factory.createClient(session, manager);
	}

	private void parseAddKeys(HostConfigEntry config) {
		String value = config.getProperty(SshConstants.ADD_KEYS_TO_AGENT);
		if (StringUtils.isEmptyOrNull(value)) {
			addKeysToAgent = false;
			return;
		}
		String[] values = value.split(","); //$NON-NLS-1$
		List<SshAgentKeyConstraint> rules = new ArrayList<>(2);
		switch (values[0]) {
		case "yes": //$NON-NLS-1$
			addKeysToAgent = true;
			break;
		case "no": //$NON-NLS-1$
			addKeysToAgent = false;
			break;
		case "ask": //$NON-NLS-1$
			addKeysToAgent = true;
			askBeforeAdding = true;
			break;
		case "confirm": //$NON-NLS-1$
			addKeysToAgent = true;
			rules.add(SshAgentKeyConstraint.CONFIRM);
			if (values.length > 1) {
				int seconds = OpenSshConfigFile.timeSpec(values[1]);
				if (seconds > 0) {
					rules.add(new SshAgentKeyConstraint.LifeTime(seconds));
				}
			}
			break;
		default:
			int seconds = OpenSshConfigFile.timeSpec(values[0]);
			if (seconds > 0) {
				addKeysToAgent = true;
				rules.add(new SshAgentKeyConstraint.LifeTime(seconds));
			}
			break;
		}
		constraints = rules.toArray(new SshAgentKeyConstraint[0]);
	}

	@Override
	protected void releaseKeys() throws IOException {
		addKeysToAgent = false;
		askBeforeAdding = false;
		skProvider = null;
		constraints = null;
		try {
			if (agent != null) {
				try {
					agent.close();
				} finally {
					agent = null;
				}
			}
		} finally {
			super.releaseKeys();
		}
	}

	private class KeyIterator extends UserAuthPublicKeyIterator {

		public KeyIterator(ClientSession session,
				SignatureFactoriesManager manager)
				throws Exception {
			super(session, manager);
		}

		private List<PublicKey> getExplicitKeys(
				Collection<String> explicitFiles) {
			if (explicitFiles == null) {
				return null;
			}
			return explicitFiles.stream().map(s -> {
				try {
					Path p = Paths.get(s + ".pub"); //$NON-NLS-1$
					if (Files.isRegularFile(p, LinkOption.NOFOLLOW_LINKS)) {
						return AuthorizedKeyEntry.readAuthorizedKeys(p).get(0)
								.resolvePublicKey(null,
										PublicKeyEntryResolver.IGNORING);
					}
				} catch (InvalidPathException | IOException
						| GeneralSecurityException e) {
					log.warn("{}", //$NON-NLS-1$
							format(SshdText.get().cannotReadPublicKey, s), e);
				}
				return null;
			}).filter(Objects::nonNull).collect(Collectors.toList());
		}

		@Override
		protected Iterable<KeyAgentIdentity> initializeAgentIdentities(
				ClientSession session) throws IOException {
			Iterable<KeyAgentIdentity> allAgentKeys = getAgentIdentities();
			if (allAgentKeys == null) {
				return null;
			}
			Collection<PublicKey> identityFiles = identitiesOnly();
			if (GenericUtils.isEmpty(identityFiles)) {
				return allAgentKeys;
			}

			// Only consider agent or PKCS11 keys that match a known public key
			// file.
			return () -> new Iterator<>() {

				private final Iterator<KeyAgentIdentity> identities = allAgentKeys
						.iterator();

				private KeyAgentIdentity next;

				@Override
				public boolean hasNext() {
					while (next == null && identities.hasNext()) {
						KeyAgentIdentity val = identities.next();
						PublicKey pk = val.getKeyIdentity().getPublic();
						// This checks against all explicit keys for any agent
						// key, but since identityFiles.size() is typically 1,
						// it should be fine.
						if (identityFiles.stream()
								.anyMatch(k -> KeyUtils.compareKeys(k, pk))) {
							next = val;
							return true;
						}
						if (log.isTraceEnabled()) {
							log.trace(
									"Ignoring SSH agent or PKCS11 {} key not in explicit IdentityFile in SSH config: {}", //$NON-NLS-1$
									KeyUtils.getKeyType(pk),
									KeyUtils.getFingerPrint(pk));
						}
					}
					return next != null;
				}

				@Override
				public KeyAgentIdentity next() {
					if (!hasNext()) {
						throw new NoSuchElementException();
					}
					KeyAgentIdentity result = next;
					next = null;
					return result;
				}
			};
		}

		private Collection<PublicKey> identitiesOnly() {
			if (hostConfig != null && hostConfig.isIdentitiesOnly()) {
				return getExplicitKeys(hostConfig.getIdentities());
			}
			return Collections.emptyList();
		}

		private Iterable<KeyAgentIdentity> getAgentIdentities()
				throws IOException {
			Iterable<KeyAgentIdentity> pkcs11Keys = getPkcs11Keys();
			if (agent == null) {
				return pkcs11Keys;
			}
			Iterable<? extends Map.Entry<PublicKey, String>> agentKeys = agent
					.getIdentities();
			if (GenericUtils.isEmpty(agentKeys)) {
				return pkcs11Keys;
			}
			Iterable<KeyAgentIdentity> fromAgent = () -> new Iterator<>() {

				private final Iterator<? extends Map.Entry<PublicKey, String>> iter = agentKeys
						.iterator();

				@Override
				public boolean hasNext() {
					return iter.hasNext();
				}

				@Override
				public KeyAgentIdentity next() {
					Map.Entry<PublicKey, String> next = iter.next();
					return new KeyAgentIdentity(agent, next.getKey(),
							next.getValue());
				}
			};
			if (GenericUtils.isEmpty(pkcs11Keys)) {
				return fromAgent;
			}
			return () -> new Iterator<>() {

				private final Iterator<Iterator<KeyAgentIdentity>> keyIter = List
						.of(pkcs11Keys.iterator(), fromAgent.iterator())
						.iterator();

				private Iterator<KeyAgentIdentity> currentKeys;

				private Boolean hasElement;

				@Override
				public boolean hasNext() {
					if (hasElement != null) {
						return hasElement.booleanValue();
					}
					while (currentKeys == null || !currentKeys.hasNext()) {
						if (keyIter.hasNext()) {
							currentKeys = keyIter.next();
						} else {
							currentKeys = null;
							hasElement = Boolean.FALSE;
							return false;
						}
					}
					hasElement = Boolean.TRUE;
					return true;
				}

				@Override
				public KeyAgentIdentity next() {
					if (hasElement == null && !hasNext()
							|| !hasElement.booleanValue()) {
						throw new NoSuchElementException();
					}
					hasElement = null;
					KeyAgentIdentity result;
					try {
						result = currentKeys.next();
					} catch (NoSuchElementException e) {
						result = null;
					}
					return result;
				}
			};
		}

		private Iterable<KeyAgentIdentity> getPkcs11Keys() throws IOException {
			String value = hostConfig.getProperty(PKCS11_PROVIDER);
			if (StringUtils.isEmptyOrNull(value) || NONE.equals(value)) {
				return null;
			}
			if (value.startsWith("~/") //$NON-NLS-1$
					|| value.startsWith('~' + File.separator)) {
				value = new File(FS.DETECTED.userHome(), value.substring(2))
						.toString();
			}
			Path library = Paths.get(value);
			if (!library.isAbsolute()) {
				throw new IOException(format(SshdText.get().pkcs11NotAbsolute,
						hostConfig.getHost(), hostConfig.getHostName(),
						PKCS11_PROVIDER, value));
			}
			if (!Files.isRegularFile(library)) {
				throw new IOException(format(SshdText.get().pkcs11NonExisting,
						hostConfig.getHost(), hostConfig.getHostName(),
						PKCS11_PROVIDER, value));
			}
			try {
				int slotListIndex = OpenSshConfigFile.positive(
						hostConfig.getProperty(PKCS11_SLOT_LIST_INDEX));
				Pkcs11Provider provider = Pkcs11Provider.getProvider(library,
						slotListIndex);
				if (provider == null) {
					throw new UnsupportedOperationException();
				}
				Iterable<KeyAgentIdentity> pkcs11Identities = provider
						.getKeys(getSession());
				if (GenericUtils.isEmpty(pkcs11Identities)) {
					log.warn(LOG_FORMAT, format(SshdText.get().pkcs11NoKeys,
							hostConfig.getHost(), hostConfig.getHostName(),
							PKCS11_PROVIDER, value));
					return null;
				}
				return pkcs11Identities;
			} catch (UnsupportedOperationException e) {
				throw new UnsupportedOperationException(format(
						SshdText.get().pkcs11Unsupported, hostConfig.getHost(),
						hostConfig.getHostName(), PKCS11_PROVIDER, value), e);
			} catch (Exception e) {
				checkCancellation(e);
				throw new IOException(
						format(SshdText.get().pkcs11FailedInstantiation,
								hostConfig.getHost(), hostConfig.getHostName(),
								PKCS11_PROVIDER, value),
						e);
			}
		}

		private void checkCancellation(Throwable e) {
			Throwable t = e;
			while (t != null) {
				if (t instanceof AuthenticationCanceledException) {
					throw (AuthenticationCanceledException) t;
				}
				t = t.getCause();
			}
		}
	}
}
