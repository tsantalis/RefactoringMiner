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

import java.io.IOException;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.PublicKey;
import java.security.Security;
import java.security.Signature;
import java.security.cert.Certificate;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import javax.security.auth.login.FailedLoginException;

import org.apache.sshd.agent.SshAgent;
import org.apache.sshd.agent.SshAgentKeyConstraint;
import org.apache.sshd.client.auth.pubkey.KeyAgentIdentity;
import org.apache.sshd.common.session.SessionContext;
import org.apache.sshd.common.signature.BuiltinSignatures;
import org.eclipse.jgit.annotations.NonNull;
import org.eclipse.jgit.transport.URIish;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Bridge for using a PKCS11 HSM (Hardware Security Module) for public-key
 * authentication.
 */
public class Pkcs11Provider {

	private static final Logger LOG = LoggerFactory
			.getLogger(Pkcs11Provider.class);

	/**
	 * A dummy agent; exists only because
	 * {@link KeyAgentIdentity#KeyAgentIdentity(SshAgent, PublicKey, String)} requires
	 * a non-{@code null} {@link SshAgent}.
	 */
	private static final SshAgent NULL_AGENT = new SshAgent() {

		@Override
		public boolean isOpen() {
			return true;
		}

		@Override
		public void close() throws IOException {
			// Nothing to do
		}

		@Override
		public Iterable<? extends Entry<PublicKey, String>> getIdentities()
				throws IOException {
			throw new UnsupportedOperationException();
		}

		@Override
		public Entry<String, byte[]> sign(SessionContext session, PublicKey key,
				String algo, byte[] data) throws IOException {
			throw new UnsupportedOperationException();
		}

		@Override
		public void addIdentity(KeyPair key, String comment,
				SshAgentKeyConstraint... constraints) throws IOException {
			throw new UnsupportedOperationException();
		}

		@Override
		public void removeIdentity(PublicKey key) throws IOException {
			throw new UnsupportedOperationException();
		}

		@Override
		public void removeAllIdentities() throws IOException {
			throw new UnsupportedOperationException();
		}

	};

	private static final Map<String, Pkcs11Provider> PROVIDERS = new ConcurrentHashMap<>();

	private static final AtomicInteger COUNT = new AtomicInteger();

	/**
	 * Creates a new {@link Pkcs11Provider}.
	 *
	 * @param library
	 *            {@link Path} to the library the SunPKCS11 provider shall use
	 * @param slotListIndex
	 *            index identifying the token; if &lt; 0, ignored and 0 is used
	 * @return a new {@link Pkcs11Provider}, or {@code null} if SunPKCS11 is not
	 *         available
	 * @throws IOException
	 *             if the configuration file cannot be created
	 * @throws java.security.ProviderException
	 *             if the Java {@link Provider} encounters a problem
	 * @throws UnsupportedOperationException
	 *             if PKCS#11 is unsupported
	 */
	public static Pkcs11Provider getProvider(@NonNull Path library,
			int slotListIndex) throws IOException {
		int slotIndex = slotListIndex < 0 ? 0 : slotListIndex;
		Path libPath = library.toAbsolutePath();
		String key = libPath.toString() + '/' + slotIndex;
		return PROVIDERS.computeIfAbsent(key, sharedLib -> {
			Provider pkcs11 = Security.getProvider("SunPKCS11"); //$NON-NLS-1$
			if (pkcs11 == null) {
				throw new UnsupportedOperationException();
			}
			// There must not be any spaces in the name.
			String name = libPath.getFileName().toString().replaceAll("\\s", //$NON-NLS-1$
					""); //$NON-NLS-1$
			name = "JGit-" + slotIndex + '-' + name; //$NON-NLS-1$
			// SunPKCS11 has a problem with paths containing multiple successive
			// spaces; it collapses them to a single space.
			//
			// However, it also performs property expansion on these paths.
			// (Seems to be an undocumented feature, though.) A reference like
			// ${xyz} is replaced by system property "xyz". Use that to work
			// around the rudimentary config parsing in SunPKCS11.
			String property = "pkcs11-" + COUNT.incrementAndGet() + '-' + name; //$NON-NLS-1$
			System.setProperty(property, libPath.toString());
			// Undocumented feature of the SunPKCS11 provider: if the parameter
			// to configure() starts with two dashes, it's not a file name but
			// the configuration directly.
			String config = "--" //$NON-NLS-1$
					+ "name = " + name + '\n' //$NON-NLS-1$
					+ "library = ${" + property + "}\n" //$NON-NLS-1$ //$NON-NLS-2$
					+ "slotListIndex = " + slotIndex + '\n'; //$NON-NLS-1$
			if (LOG.isDebugEnabled()) {
				LOG.debug(
						"{}: configuring provider with system property {}={} and config:{}{}", //$NON-NLS-1$
						name, property, libPath, System.lineSeparator(),
						config);
			}
			pkcs11 = pkcs11.configure(config);
			// Produce an RFC7512 URI. Empty path, module-path must be in
			// the query.
			String path = "pkcs11:?module-path=" + libPath; //$NON-NLS-1$
			if (slotListIndex > 0) {
				// RFC7512 has nothing for the slot list index; pretend it
				// was a vendor-specific query attribute.
				path += "&slot-list-index=" + slotListIndex; //$NON-NLS-1$
			}
			SecurityCallback callback = new SecurityCallback(
					new URIish().setPath(path));
			return new Pkcs11Provider(pkcs11, callback);
		});
	}

	private final Provider provider;

	private final SecurityCallback prompter;

	private final KeyStore.Builder builder;

	private KeyStore keys;

	private Pkcs11Provider(Provider pkcs11, SecurityCallback prompter) {
		this.provider = pkcs11;
		this.prompter = prompter;
		this.builder = KeyStore.Builder.newInstance("PKCS11", provider, //$NON-NLS-1$
				new KeyStore.CallbackHandlerProtection(prompter));
	}

	// Implementation note: With SoftHSM Java 11 asks for the PIN when the
	// KeyStore is loaded, i.e., when the token is accessed. softhsm2-util,
	// however, can list certificates and public keys without PIN entry, but
	// needs a PIN to also list private keys. So it appears that different
	// module libraries or possibly different KeyStore implementations may
	// prompt either when accessing the token, or only when we try to actually
	// sign something (i.e., when accessing a private key). It may also depend
	// on the token itself; some tokens require early log-in.
	//
	// Therefore we initialize the prompter in both cases, even if it may be
	// unused in one or the other operation.
	//
	// The price to pay is that sign() has to be synchronized, too, to avoid
	// that different sessions step on each other's toes in the prompter.

	private synchronized void load(SessionContext session)
			throws GeneralSecurityException, IOException {
		if (keys == null) {
			int numberOfPrompts = prompter.init(session);
			int attempt = 0;
			while (attempt < numberOfPrompts) {
				attempt++;
				try {
					if (LOG.isDebugEnabled()) {
						LOG.debug(
								"{}: Loading PKCS#11 KeyStore (attempt {})", //$NON-NLS-1$
								getName(), Integer.toString(attempt));
					}
					keys = builder.getKeyStore();
					prompter.passwordTried(null);
					return;
				} catch (GeneralSecurityException e) {
					if (!prompter.passwordTried(e) || attempt >= numberOfPrompts
							|| !isWrongPin(e)) {
						throw e;
					}
				}
			}
		}
	}

	synchronized byte[] sign(SessionContext session, String algorithm,
			String alias, byte[] data)
			throws GeneralSecurityException, IOException {
		int numberOfPrompts = prompter.init(session);
		int attempt = 0;
		while (attempt < numberOfPrompts) {
			attempt++;
			try {
				if (LOG.isDebugEnabled()) {
					LOG.debug(
							"{}: Signing with PKCS#11 key {}, algorithm {} (attempt {})", //$NON-NLS-1$
							getName(), alias, algorithm,
							Integer.toString(attempt));
				}
				Signature signer = Signature.getInstance(algorithm, provider);
				PrivateKey privKey = (PrivateKey) keys.getKey(alias, null);
				signer.initSign(privKey);
				signer.update(data);
				byte[] signature = signer.sign();
				prompter.passwordTried(null);
				return signature;
			} catch (GeneralSecurityException e) {
				if (!prompter.passwordTried(e) || attempt >= numberOfPrompts
						|| !isWrongPin(e)) {
					throw e;
				}
			}
		}
		return null;
	}

	private boolean isWrongPin(Throwable e) {
		Throwable t = e;
		while (t != null) {
			if (t instanceof FailedLoginException) {
				return true;
			}
			t = t.getCause();
		}
		return false;
	}

	/**
	 * Retrieves an identifying name of this {@link Pkcs11Provider}.
	 *
	 * @return the name
	 */
	public String getName() {
		return provider.getName();
	}

	/**
	 * Obtains the identities provided by the PKCS11 library.
	 *
	 * @param session
	 *            in which we to load the identities
	 * @return all the available identities
	 * @throws IOException
	 *             if keys cannot be accessed
	 * @throws GeneralSecurityException
	 *             if keys cannot be accessed
	 */
	public Iterable<KeyAgentIdentity> getKeys(SessionContext session)
			throws IOException, GeneralSecurityException {
		// Get all public keys from the KeyStore.
		load(session);
		List<KeyAgentIdentity> result = new ArrayList<>(2);
		Enumeration<String> aliases = keys.aliases();
		while (aliases.hasMoreElements()) {
			String alias = aliases.nextElement();
			Certificate certificate = keys.getCertificate(alias);
			if (certificate == null) {
				continue;
			}
			PublicKey pubKey = certificate.getPublicKey();
			if (pubKey == null) {
				// This should never happen
				if (LOG.isDebugEnabled()) {
					LOG.debug("{}: certificate {} has no public key??", //$NON-NLS-1$
							getName(), alias);
				}
				continue;
			}
			if (LOG.isDebugEnabled()) {
				if (certificate instanceof X509Certificate) {
					X509Certificate x509 = (X509Certificate) certificate;
					// OpenSSH does not seem to check certificate validity?
					String msg;
					try {
						x509.checkValidity();
						msg = "Certificate is valid"; //$NON-NLS-1$
					} catch (CertificateExpiredException
							| CertificateNotYetValidException e) {
						msg = "Certificate is INVALID"; //$NON-NLS-1$
					}
					// OpenSSh explicitly also considers private keys not
					// intended for signing, see
					// https://bugzilla.mindrot.org/show_bug.cgi?id=1736 .
					boolean[] usage = x509.getKeyUsage();
					if (usage != null) {
						// We have no access to the PKCS#11 flags on the key, so
						// report the certificate flag, if present.
						msg += ", signing " //$NON-NLS-1$
								+ (usage[0] ? "allowed" : "NOT allowed"); //$NON-NLS-1$ //$NON-NLS-2$
					}
					LOG.debug(
							"{}: Loaded X.509 certificate {}, key type {}. {}.", //$NON-NLS-1$
							getName(), alias, pubKey.getAlgorithm(), msg);
				} else {
					LOG.debug("{}: Loaded certificate {}, key type {}.", //$NON-NLS-1$
							getName(), alias, pubKey.getAlgorithm());
				}
			}
			result.add(new Pkcs11Identity(pubKey, alias));
		}
		return result;
	}

	// We use a KeyAgentIdentity because we want to hide the private key.
	//
	// JGit doesn't do Agent forwarding, so there will never be any reason to
	// add a PKCS11 key/token to an agent.
	private class Pkcs11Identity extends KeyAgentIdentity {

		Pkcs11Identity(PublicKey key, String alias) {
			super(NULL_AGENT, key, alias);
		}

		@Override
		public Entry<String, byte[]> sign(SessionContext session, String algo,
				byte[] data) throws Exception {
			// Find the built-in signature factory for the algorithm
			BuiltinSignatures factory = BuiltinSignatures.fromFactoryName(algo);
			// Get its Java signature algorithm name from that
			String javaSignatureName = factory.create().getAlgorithm();
			// We cannot use the Signature created by the factory -- we need a
			// provider-specific Signature instance.
			return new SimpleImmutableEntry<>(algo,
					Pkcs11Provider.this.sign(session, javaSignatureName,
							getComment(), data));
		}
	}
}
