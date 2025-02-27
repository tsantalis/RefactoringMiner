/*
 * Copyright (C) 2018, 2020 Thomas Wolf <thomas.wolf@paranor.ch> and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0 which is available at
 * https://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package org.eclipse.jgit.junit.ssh;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.apache.sshd.common.config.keys.PublicKeyEntry;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.PushCommand;
import org.eclipse.jgit.api.ResetCommand.ResetType;
import org.eclipse.jgit.errors.UnsupportedCredentialItem;
import org.eclipse.jgit.junit.RepositoryTestCase;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.transport.CredentialItem;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.PushResult;
import org.eclipse.jgit.transport.RemoteRefUpdate;
import org.eclipse.jgit.transport.SshSessionFactory;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.jgit.util.FS;
import org.junit.After;

/**
 * Root class for ssh tests. Sets up the ssh test server. A set of pre-computed
 * keys for testing is provided in the bundle and can be used in test cases via
 * {@link #copyTestResource(String, File)}. These test key files names have four
 * components, separated by a single underscore: "id", the algorithm, the bits
 * (if variable), and the password if the private key is encrypted. For instance
 * "{@code id_ecdsa_384_testpass}" is an encrypted ECDSA-384 key. The passphrase
 * to decrypt is "testpass". The key "{@code id_ecdsa_384}" is the same but
 * unencrypted. All keys were generated and encrypted via ssh-keygen. Note that
 * DSA and ec25519 have no "bits" component. Available keys are listed in
 * {@link SshTestBase#KEY_RESOURCES}.
 */
public abstract class SshTestHarness extends RepositoryTestCase {

	protected static final String TEST_USER = "testuser";

	protected File sshDir;

	protected File privateKey1;

	protected File privateKey2;

	protected File publicKey1;

	/**
	 * @since 5.10
	 */
	protected File publicKey2;

	protected SshTestGitServer server;

	private SshSessionFactory factory;

	protected int testPort;

	protected File knownHosts;

	private File homeDir;

	@Override
	public void setUp() throws Exception {
		super.setUp();
		writeTrashFile("file.txt", "something");
		try (Git git = new Git(db)) {
			git.add().addFilepattern("file.txt").call();
			git.commit().setMessage("Initial commit").call();
		}
		mockSystemReader.setProperty("user.home",
				getTemporaryDirectory().getAbsolutePath());
		mockSystemReader.setProperty("HOME",
				getTemporaryDirectory().getAbsolutePath());
		homeDir = FS.DETECTED.userHome();
		FS.DETECTED.setUserHome(getTemporaryDirectory().getAbsoluteFile());
		sshDir = new File(getTemporaryDirectory(), ".ssh");
		assertTrue(sshDir.mkdir());
		File serverDir = new File(getTemporaryDirectory(), "srv");
		assertTrue(serverDir.mkdir());
		// Create two key pairs. Let's not call them "id_rsa".
		KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
		generator.initialize(2048);
		privateKey1 = new File(sshDir, "first_key");
		privateKey2 = new File(sshDir, "second_key");
		publicKey1 = createKeyPair(generator.generateKeyPair(), privateKey1);
		publicKey2 = createKeyPair(generator.generateKeyPair(), privateKey2);
		// Create a host key
		KeyPair hostKey = generator.generateKeyPair();
		// Start a server with our test user and the first key.
		server = new SshTestGitServer(TEST_USER, publicKey1.toPath(), db,
				hostKey);
		testPort = server.start();
		assertTrue(testPort > 0);
		knownHosts = new File(sshDir, "known_hosts");
		StringBuilder knownHostsLine = new StringBuilder();
		knownHostsLine.append("[localhost]:").append(testPort).append(' ');
		PublicKeyEntry.appendPublicKeyEntry(knownHostsLine,
				hostKey.getPublic());
		Files.write(knownHosts.toPath(),
				Collections.singleton(knownHostsLine.toString()));
		factory = createSessionFactory();
		SshSessionFactory.setInstance(factory);
	}

	private static File createKeyPair(KeyPair newKey, File privateKeyFile)
			throws Exception {
		// Write PKCS#8 PEM unencrypted. Both JSch and sshd can read that.
		PrivateKey privateKey = newKey.getPrivate();
		String format = privateKey.getFormat();
		if (!"PKCS#8".equalsIgnoreCase(format)) {
			throw new IOException("Cannot write " + privateKey.getAlgorithm()
					+ " key in " + format + " format");
		}
		try (BufferedWriter writer = Files.newBufferedWriter(
				privateKeyFile.toPath(), StandardCharsets.US_ASCII)) {
			writer.write("-----BEGIN PRIVATE KEY-----");
			writer.newLine();
			write(writer, privateKey.getEncoded(), 64);
			writer.write("-----END PRIVATE KEY-----");
			writer.newLine();
		}
		File publicKeyFile = new File(privateKeyFile.getParentFile(),
				privateKeyFile.getName() + ".pub");
		StringBuilder builder = new StringBuilder();
		PublicKeyEntry.appendPublicKeyEntry(builder, newKey.getPublic());
		builder.append(' ').append(TEST_USER);
		try (OutputStream out = new FileOutputStream(publicKeyFile)) {
			out.write(builder.toString().getBytes(StandardCharsets.US_ASCII));
		}
		return publicKeyFile;
	}

	private static void write(BufferedWriter out, byte[] bytes, int lineLength)
			throws IOException {
		String data = Base64.getEncoder().encodeToString(bytes);
		int last = data.length();
		for (int i = 0; i < last; i += lineLength) {
			if (i + lineLength <= last) {
				out.write(data.substring(i, i + lineLength));
			} else {
				out.write(data.substring(i));
			}
			out.newLine();
		}
		Arrays.fill(bytes, (byte) 0);
	}

	/**
	 * Creates a new known_hosts file with one entry for the given host and port
	 * taken from the given public key file.
	 *
	 * @param file
	 *            to write the known_hosts file to
	 * @param host
	 *            for the entry
	 * @param port
	 *            for the entry
	 * @param publicKey
	 *            to use
	 * @return the public-key part of the line
	 * @throws IOException
	 *             if an IO error occurred
	 */
	protected static String createKnownHostsFile(File file, String host,
			int port, File publicKey) throws IOException {
		List<String> lines = Files.readAllLines(publicKey.toPath(),
				StandardCharsets.UTF_8);
		assertEquals("Public key has too many lines", 1, lines.size());
		String pubKey = lines.get(0);
		// Strip off the comment.
		String[] parts = pubKey.split("\\s+");
		assertTrue("Unexpected key content",
				parts.length == 2 || parts.length == 3);
		String keyPart = parts[0] + ' ' + parts[1];
		String line = '[' + host + "]:" + port + ' ' + keyPart;
		Files.write(file.toPath(), Collections.singletonList(line));
		return keyPart;
	}

	/**
	 * Checks whether there is a line for the given host and port that also
	 * matches the given key part in the list of lines.
	 *
	 * @param host
	 *            to look for
	 * @param port
	 *            to look for
	 * @param keyPart
	 *            to look for
	 * @param lines
	 *            to look in
	 * @return {@code true} if found, {@code false} otherwise
	 */
	protected boolean hasHostKey(String host, int port, String keyPart,
			List<String> lines) {
		String h = '[' + host + "]:" + port;
		return lines.stream()
				.anyMatch(l -> l.contains(h) && l.contains(keyPart));
	}

	@After
	public void shutdownServer() throws Exception {
		if (server != null) {
			server.stop();
			server = null;
		}
		FS.DETECTED.setUserHome(homeDir);
		SshSessionFactory.setInstance(null);
		factory = null;
	}

	protected abstract SshSessionFactory createSessionFactory();

	protected SshSessionFactory getSessionFactory() {
		return factory;
	}

	protected abstract void installConfig(String... config);

	/**
	 * Copies a test data file contained in the test bundle to the given file.
	 * Equivalent to {@link #copyTestResource(Class, String, File)} with
	 * {@code SshTestHarness.class} as first parameter.
	 *
	 * @param resourceName
	 *            of the test resource to copy
	 * @param to
	 *            file to copy the resource to
	 * @throws IOException
	 *             if the resource cannot be copied
	 */
	protected void copyTestResource(String resourceName, File to)
			throws IOException {
		copyTestResource(SshTestHarness.class, resourceName, to);
	}

	/**
	 * Copies a test data file contained in the test bundle to the given file,
	 * using {@link Class#getResourceAsStream(String)} to get the test resource.
	 *
	 * @param loader
	 *            {@link Class} to use to load the resource
	 * @param resourceName
	 *            of the test resource to copy
	 * @param to
	 *            file to copy the resource to
	 * @throws IOException
	 *             if the resource cannot be copied
	 */
	protected void copyTestResource(Class<?> loader, String resourceName,
			File to) throws IOException {
		try (InputStream in = loader.getResourceAsStream(resourceName)) {
			Files.copy(in, to.toPath());
		}
	}

	protected File cloneWith(String uri, File to, CredentialsProvider provider,
			String... config) throws Exception {
		installConfig(config);
		CloneCommand clone = Git.cloneRepository().setCloneAllBranches(true)
				.setDirectory(to).setURI(uri);
		if (provider != null) {
			clone.setCredentialsProvider(provider);
		}
		try (Git git = clone.call()) {
			Repository repo = git.getRepository();
			assertNotNull(repo.resolve("master"));
			assertNotEquals(db.getWorkTree(),
					git.getRepository().getWorkTree());
			assertTrue(new File(git.getRepository().getWorkTree(), "file.txt")
					.exists());
			return repo.getWorkTree();
		}
	}

	protected void pushTo(File localClone) throws Exception {
		pushTo(null, localClone);
	}

	protected void pushTo(CredentialsProvider provider, File localClone)
			throws Exception {
		RevCommit commit;
		File newFile = null;
		try (Git git = Git.open(localClone)) {
			// Write a new file and modify a file.
			Repository local = git.getRepository();
			newFile = File.createTempFile("new", "sshtest",
					local.getWorkTree());
			write(newFile, "something new");
			File existingFile = new File(local.getWorkTree(), "file.txt");
			write(existingFile, "something else");
			git.add().addFilepattern("file.txt")
					.addFilepattern(newFile.getName())
					.call();
			commit = git.commit().setMessage("Local commit").call();
			// Push
			PushCommand push = git.push().setPushAll();
			if (provider != null) {
				push.setCredentialsProvider(provider);
			}
			Iterable<PushResult> results = push.call();
			for (PushResult result : results) {
				for (RemoteRefUpdate u : result.getRemoteUpdates()) {
					assertEquals(
							"Could not update " + u.getRemoteName() + ' '
									+ u.getMessage(),
							RemoteRefUpdate.Status.OK, u.getStatus());
				}
			}
		}
		// Now check "master" in the remote repo directly:
		assertEquals("Unexpected remote commit", commit, db.resolve("master"));
		assertEquals("Unexpected remote commit", commit,
				db.resolve(Constants.HEAD));
		File remoteFile = new File(db.getWorkTree(), newFile.getName());
		assertFalse("File should not exist on remote", remoteFile.exists());
		try (Git git = new Git(db)) {
			git.reset().setMode(ResetType.HARD).setRef(Constants.HEAD).call();
		}
		assertTrue("File does not exist on remote", remoteFile.exists());
		checkFile(remoteFile, "something new");
	}

	protected static class TestCredentialsProvider extends CredentialsProvider {

		private final List<String> stringStore;

		private final Iterator<String> strings;

		public TestCredentialsProvider(String... strings) {
			if (strings == null || strings.length == 0) {
				stringStore = Collections.emptyList();
			} else {
				stringStore = Arrays.asList(strings);
			}
			this.strings = stringStore.iterator();
		}

		@Override
		public boolean isInteractive() {
			return true;
		}

		@Override
		public boolean supports(CredentialItem... items) {
			return true;
		}

		@Override
		public boolean get(URIish uri, CredentialItem... items)
				throws UnsupportedCredentialItem {
			System.out.println("URI: " + uri);
			for (CredentialItem item : items) {
				System.out.println(item.getClass().getSimpleName() + ' '
						+ item.getPromptText());
			}
			logItems(uri, items);
			for (CredentialItem item : items) {
				if (item instanceof CredentialItem.InformationalMessage) {
					continue;
				}
				if (item instanceof CredentialItem.YesNoType) {
					((CredentialItem.YesNoType) item).setValue(true);
				} else if (item instanceof CredentialItem.CharArrayType) {
					if (strings.hasNext()) {
						((CredentialItem.CharArrayType) item)
								.setValue(strings.next().toCharArray());
					} else {
						return false;
					}
				} else if (item instanceof CredentialItem.StringType) {
					if (strings.hasNext()) {
						((CredentialItem.StringType) item)
								.setValue(strings.next());
					} else {
						return false;
					}
				} else {
					return false;
				}
			}
			return true;
		}

		private List<LogEntry> log = new ArrayList<>();

		private void logItems(URIish uri, CredentialItem... items) {
			log.add(new LogEntry(uri, Arrays.asList(items)));
		}

		public List<LogEntry> getLog() {
			return log;
		}
	}

	protected static class LogEntry {

		private URIish uri;

		private List<CredentialItem> items;

		public LogEntry(URIish uri, List<CredentialItem> items) {
			this.uri = uri;
			this.items = items;
		}

		public URIish getURIish() {
			return uri;
		}

		public List<CredentialItem> getItems() {
			return items;
		}
	}
}
