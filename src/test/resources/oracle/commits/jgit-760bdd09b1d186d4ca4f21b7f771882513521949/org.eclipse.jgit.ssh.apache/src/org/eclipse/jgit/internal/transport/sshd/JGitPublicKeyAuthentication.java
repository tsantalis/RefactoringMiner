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
import static org.eclipse.jgit.transport.SshConstants.PUBKEY_ACCEPTED_ALGORITHMS;

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
import org.eclipse.jgit.internal.transport.ssh.OpenSshConfigFile;
import org.eclipse.jgit.transport.CredentialItem;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.SshConstants;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.jgit.util.StringUtils;

/**
 * Custom {@link UserAuthPublicKey} implementation for handling SSH config
 * PubkeyAcceptedAlgorithms and interaction with the SSH agent.
 */
public class JGitPublicKeyAuthentication extends UserAuthPublicKey {

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
			log.warn(format(SshdText.get().configNoKnownAlgorithms,
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
					log.error(
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

		private Iterable<? extends Map.Entry<PublicKey, String>> agentKeys;

		// If non-null, all the public keys from explicitly given key files. Any
		// agent key not matching one of these public keys will be ignored in
		// getIdentities().
		private Collection<PublicKey> identityFiles;

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
					log.warn(format(SshdText.get().cannotReadPublicKey, s), e);
				}
				return null;
			}).filter(Objects::nonNull).collect(Collectors.toList());
		}

		@Override
		protected Iterable<KeyAgentIdentity> initializeAgentIdentities(
				ClientSession session) throws IOException {
			if (agent == null) {
				return null;
			}
			agentKeys = agent.getIdentities();
			if (hostConfig != null && hostConfig.isIdentitiesOnly()) {
				identityFiles = getExplicitKeys(hostConfig.getIdentities());
			}
			return () -> new Iterator<>() {

				private final Iterator<? extends Map.Entry<PublicKey, String>> iter = agentKeys
						.iterator();

				private Map.Entry<PublicKey, String> next;

				@Override
				public boolean hasNext() {
					while (next == null && iter.hasNext()) {
						Map.Entry<PublicKey, String> val = iter.next();
						PublicKey pk = val.getKey();
						// This checks against all explicit keys for any agent
						// key, but since identityFiles.size() is typically 1,
						// it should be fine.
						if (identityFiles == null || identityFiles.stream()
								.anyMatch(k -> KeyUtils.compareKeys(k, pk))) {
							next = val;
							return true;
						}
						if (log.isTraceEnabled()) {
							log.trace(
									"Ignoring SSH agent {} key not in explicit IdentityFile in SSH config: {}", //$NON-NLS-1$
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
					KeyAgentIdentity result = new KeyAgentIdentity(agent,
							next.getKey(), next.getValue());
					next = null;
					return result;
				}
			};
		}
	}
}
