/*
 * Copyright (C) 2014, Google Inc.
 * and other copyright owners as documented in the project's IP log.
 *
 * This program and the accompanying materials are made available
 * under the terms of the Eclipse Distribution License v1.0 which
 * accompanies this distribution, is reproduced below, and is
 * available at http://www.eclipse.org/org/documents/edl-v10.php
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or
 * without modification, are permitted provided that the following
 * conditions are met:
 *
 * - Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above
 *   copyright notice, this list of conditions and the following
 *   disclaimer in the documentation and/or other materials provided
 *   with the distribution.
 *
 * - Neither the name of the Eclipse Foundation, Inc. nor the
 *   names of its contributors may be used to endorse or promote
 *   products derived from this software without specific prior
 *   written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND
 * CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.eclipse.jgit.gitrepo;

import static org.eclipse.jgit.lib.Constants.CHARSET;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.InvalidRemoteException;
import org.eclipse.jgit.api.errors.RefNotFoundException;
import org.eclipse.jgit.junit.JGitTestUtil;
import org.eclipse.jgit.junit.RepositoryTestCase;
import org.eclipse.jgit.lib.BlobBasedConfig;
import org.eclipse.jgit.lib.Config;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.storage.file.FileBasedConfig;
import org.eclipse.jgit.util.FS;
import org.junit.Test;

public class RepoCommandTest extends RepositoryTestCase {

	private static final String BRANCH = "branch";
	private static final String TAG = "release";

	private Repository defaultDb;
	private Repository notDefaultDb;
	private Repository groupADb;
	private Repository groupBDb;

	private String rootUri;
	private String defaultUri;
	private String notDefaultUri;
	private String groupAUri;
	private String groupBUri;

	private ObjectId oldCommitId;

	@Override
	public void setUp() throws Exception {
		super.setUp();

		defaultDb = createWorkRepository();
		try (Git git = new Git(defaultDb)) {
			JGitTestUtil.writeTrashFile(defaultDb, "hello.txt", "branch world");
			git.add().addFilepattern("hello.txt").call();
			oldCommitId = git.commit().setMessage("Initial commit").call().getId();
			git.checkout().setName(BRANCH).setCreateBranch(true).call();
			git.checkout().setName("master").call();
			git.tag().setName(TAG).call();
			JGitTestUtil.writeTrashFile(defaultDb, "hello.txt", "master world");
			git.add().addFilepattern("hello.txt").call();
			git.commit().setMessage("Second commit").call();
			addRepoToClose(defaultDb);
		}

		notDefaultDb = createWorkRepository();
		try (Git git = new Git(notDefaultDb)) {
			JGitTestUtil.writeTrashFile(notDefaultDb, "world.txt", "hello");
			git.add().addFilepattern("world.txt").call();
			git.commit().setMessage("Initial commit").call();
			addRepoToClose(notDefaultDb);
		}

		groupADb = createWorkRepository();
		try (Git git = new Git(groupADb)) {
			JGitTestUtil.writeTrashFile(groupADb, "a.txt", "world");
			git.add().addFilepattern("a.txt").call();
			git.commit().setMessage("Initial commit").call();
			addRepoToClose(groupADb);
		}

		groupBDb = createWorkRepository();
		try (Git git = new Git(groupBDb)) {
			JGitTestUtil.writeTrashFile(groupBDb, "b.txt", "world");
			git.add().addFilepattern("b.txt").call();
			git.commit().setMessage("Initial commit").call();
			addRepoToClose(groupBDb);
		}

		resolveRelativeUris();
	}

	class IndexedRepos implements RepoCommand.RemoteReader {
		Map<String, Repository> uriRepoMap;
		IndexedRepos() {
			uriRepoMap = new HashMap<>();
		}

		void put(String u, Repository r) {
			uriRepoMap.put(u, r);
		}

		@Override
		public ObjectId sha1(String uri, String refname) throws GitAPIException {
			if (!uriRepoMap.containsKey(uri)) {
				return null;
			}

			Repository r = uriRepoMap.get(uri);
			try {
				Ref ref = r.findRef(refname);
				if (ref == null) return null;

				ref = r.getRefDatabase().peel(ref);
				ObjectId id = ref.getObjectId();
				return id;
			} catch (IOException e) {
				throw new InvalidRemoteException("", e);
			}
		}

		@Override
		public byte[] readFile(String uri, String refName, String path)
			throws GitAPIException, IOException {
			Repository repo = uriRepoMap.get(uri);

			String idStr = refName + ":" + path;
			ObjectId id = repo.resolve(idStr);
			if (id == null) {
				throw new RefNotFoundException(
					String.format("repo %s does not have %s", repo.toString(), idStr));
			}
			try (ObjectReader reader = repo.newObjectReader()) {
				return reader.open(id).getCachedBytes(Integer.MAX_VALUE);
			}
		}
	}

	private Repository cloneRepository(Repository repo, boolean bare)
			throws Exception {
		Repository r = Git.cloneRepository()
				.setURI(repo.getDirectory().toURI().toString())
				.setDirectory(createUniqueTestGitDir(true)).setBare(bare).call()
				.getRepository();
		if (bare) {
			assertTrue(r.isBare());
		} else {
			assertFalse(r.isBare());
		}
		return r;
	}

	@Test
	public void runTwiceIsNOP() throws Exception {
		try (Repository child = cloneRepository(groupADb, true);
				Repository dest = cloneRepository(db, true)) {
			StringBuilder xmlContent = new StringBuilder();
			xmlContent.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
					.append("<manifest>")
					.append("<remote name=\"remote1\" fetch=\"..\" />")
					.append("<default revision=\"master\" remote=\"remote1\" />")
					.append("<project path=\"base\" name=\"platform/base\" />")
					.append("</manifest>");
			RepoCommand cmd = new RepoCommand(dest);

			IndexedRepos repos = new IndexedRepos();
			repos.put("platform/base", child);

			RevCommit commit = cmd
					.setInputStream(new ByteArrayInputStream(
							xmlContent.toString().getBytes(CHARSET)))
					.setRemoteReader(repos).setURI("platform/")
					.setTargetURI("platform/superproject")
					.setRecordRemoteBranch(true).setRecordSubmoduleLabels(true)
					.call();

			String firstIdStr = commit.getId().name() + ":" + ".gitmodules";
			commit = new RepoCommand(dest)
					.setInputStream(new ByteArrayInputStream(
							xmlContent.toString().getBytes(CHARSET)))
					.setRemoteReader(repos).setURI("platform/")
					.setTargetURI("platform/superproject")
					.setRecordRemoteBranch(true).setRecordSubmoduleLabels(true)
					.call();
			String idStr = commit.getId().name() + ":" + ".gitmodules";
			assertEquals(firstIdStr, idStr);
		}
	}

	@Test
	public void androidSetup() throws Exception {
		try (Repository child = cloneRepository(groupADb, true);
				Repository dest = cloneRepository(db, true)) {
			StringBuilder xmlContent = new StringBuilder();
			xmlContent.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
					.append("<manifest>")
					.append("<remote name=\"remote1\" fetch=\"..\" />")
					.append("<default revision=\"master\" remote=\"remote1\" />")
					.append("<project path=\"base\" name=\"platform/base\" />")
					.append("</manifest>");
			RepoCommand cmd = new RepoCommand(dest);

			IndexedRepos repos = new IndexedRepos();
			repos.put("platform/base", child);

			RevCommit commit = cmd
					.setInputStream(new ByteArrayInputStream(
							xmlContent.toString().getBytes(CHARSET)))
					.setRemoteReader(repos).setURI("platform/")
					.setTargetURI("platform/superproject")
					.setRecordRemoteBranch(true).setRecordSubmoduleLabels(true)
					.call();

			String idStr = commit.getId().name() + ":" + ".gitmodules";
			ObjectId modId = dest.resolve(idStr);

			try (ObjectReader reader = dest.newObjectReader()) {
				byte[] bytes = reader.open(modId)
						.getCachedBytes(Integer.MAX_VALUE);
				Config base = new Config();
				BlobBasedConfig cfg = new BlobBasedConfig(base, bytes);
				String subUrl = cfg.getString("submodule", "base", "url");
				assertEquals(subUrl, "../base");
			}
		}
	}

	@Test
	public void recordUnreachableRemotes() throws Exception {
		StringBuilder xmlContent = new StringBuilder();
		xmlContent.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
			.append("<manifest>")
			.append("<remote name=\"remote1\" fetch=\"https://host.com/\" />")
			.append("<default revision=\"master\" remote=\"remote1\" />")
			.append("<project path=\"base\" name=\"platform/base\" />")
			.append("</manifest>");

		try (Repository dest = cloneRepository(db, true)) {
			RevCommit commit = new RepoCommand(dest)
					.setInputStream(new ByteArrayInputStream(
							xmlContent.toString().getBytes(CHARSET)))
					.setRemoteReader(new IndexedRepos()).setURI("platform/")
					.setTargetURI("platform/superproject")
					.setRecordRemoteBranch(true).setIgnoreRemoteFailures(true)
					.setRecordSubmoduleLabels(true).call();

			String idStr = commit.getId().name() + ":" + ".gitmodules";
			ObjectId modId = dest.resolve(idStr);

			try (ObjectReader reader = dest.newObjectReader()) {
				byte[] bytes = reader.open(modId)
						.getCachedBytes(Integer.MAX_VALUE);
				Config base = new Config();
				BlobBasedConfig cfg = new BlobBasedConfig(base, bytes);
				String subUrl = cfg.getString("submodule", "base", "url");
				assertEquals(subUrl, "https://host.com/platform/base");
			}
		}
	}

	@Test
	public void gerritSetup() throws Exception {
		try (Repository child = cloneRepository(groupADb, true);
				Repository dest = cloneRepository(db, true)) {
			StringBuilder xmlContent = new StringBuilder();
			xmlContent.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
					.append("<manifest>")
					.append("<remote name=\"remote1\" fetch=\".\" />")
					.append("<default revision=\"master\" remote=\"remote1\" />")
					.append("<project path=\"plugins/cookbook\" name=\"plugins/cookbook\" />")
					.append("</manifest>");
			RepoCommand cmd = new RepoCommand(dest);

			IndexedRepos repos = new IndexedRepos();
			repos.put("plugins/cookbook", child);

			RevCommit commit = cmd
					.setInputStream(new ByteArrayInputStream(
							xmlContent.toString().getBytes(CHARSET)))
					.setRemoteReader(repos).setURI("").setTargetURI("gerrit")
					.setRecordRemoteBranch(true).setRecordSubmoduleLabels(true)
					.call();

			String idStr = commit.getId().name() + ":" + ".gitmodules";
			ObjectId modId = dest.resolve(idStr);

			try (ObjectReader reader = dest.newObjectReader()) {
				byte[] bytes = reader.open(modId)
						.getCachedBytes(Integer.MAX_VALUE);
				Config base = new Config();
				BlobBasedConfig cfg = new BlobBasedConfig(base, bytes);
				String subUrl = cfg.getString("submodule", "plugins/cookbook",
						"url");
				assertEquals(subUrl, "../plugins/cookbook");
			}
		}
	}

	@Test
	public void absoluteRemoteURL() throws Exception {
		try (Repository child = cloneRepository(groupADb, true);
				Repository dest = cloneRepository(db, true)) {
			String abs = "https://chromium.googlesource.com";
			String repoUrl = "https://chromium.googlesource.com/chromium/src";
			boolean fetchSlash = false;
			boolean baseSlash = false;
			do {
				do {
					String fetchUrl = fetchSlash ? abs + "/" : abs;
					String baseUrl = baseSlash ? abs + "/" : abs;

					StringBuilder xmlContent = new StringBuilder();
					xmlContent.append(
							"<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
							.append("<manifest>")
							.append("<remote name=\"origin\" fetch=\""
									+ fetchUrl + "\" />")
							.append("<default revision=\"master\" remote=\"origin\" />")
							.append("<project path=\"src\" name=\"chromium/src\" />")
							.append("</manifest>");
					RepoCommand cmd = new RepoCommand(dest);

					IndexedRepos repos = new IndexedRepos();
					repos.put(repoUrl, child);

					RevCommit commit = cmd
							.setInputStream(new ByteArrayInputStream(
									xmlContent.toString().getBytes(CHARSET)))
							.setRemoteReader(repos).setURI(baseUrl)
							.setTargetURI("gerrit").setRecordRemoteBranch(true)
							.setRecordSubmoduleLabels(true).call();

					String idStr = commit.getId().name() + ":" + ".gitmodules";
					ObjectId modId = dest.resolve(idStr);

					try (ObjectReader reader = dest.newObjectReader()) {
						byte[] bytes = reader.open(modId)
								.getCachedBytes(Integer.MAX_VALUE);
						Config base = new Config();
						BlobBasedConfig cfg = new BlobBasedConfig(base, bytes);
						String subUrl = cfg.getString("submodule", "src",
								"url");
						assertEquals(
								"https://chromium.googlesource.com/chromium/src",
								subUrl);
					}
					fetchSlash = !fetchSlash;
				} while (fetchSlash);
				baseSlash = !baseSlash;
			} while (baseSlash);
		}
	}

	@Test
	public void absoluteRemoteURLAbsoluteTargetURL() throws Exception {
		try (Repository child = cloneRepository(groupADb, true);
				Repository dest = cloneRepository(db, true)) {
			String abs = "https://chromium.googlesource.com";
			String repoUrl = "https://chromium.googlesource.com/chromium/src";
			boolean fetchSlash = false;
			boolean baseSlash = false;
			do {
				do {
					String fetchUrl = fetchSlash ? abs + "/" : abs;
					String baseUrl = baseSlash ? abs + "/" : abs;

					StringBuilder xmlContent = new StringBuilder();
					xmlContent.append(
							"<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
							.append("<manifest>")
							.append("<remote name=\"origin\" fetch=\""
									+ fetchUrl + "\" />")
							.append("<default revision=\"master\" remote=\"origin\" />")
							.append("<project path=\"src\" name=\"chromium/src\" />")
							.append("</manifest>");
					RepoCommand cmd = new RepoCommand(dest);

					IndexedRepos repos = new IndexedRepos();
					repos.put(repoUrl, child);

					RevCommit commit = cmd
							.setInputStream(new ByteArrayInputStream(
									xmlContent.toString().getBytes(CHARSET)))
							.setRemoteReader(repos).setURI(baseUrl)
							.setTargetURI(abs + "/superproject")
							.setRecordRemoteBranch(true)
							.setRecordSubmoduleLabels(true).call();

					String idStr = commit.getId().name() + ":" + ".gitmodules";
					ObjectId modId = dest.resolve(idStr);

					try (ObjectReader reader = dest.newObjectReader()) {
						byte[] bytes = reader.open(modId)
								.getCachedBytes(Integer.MAX_VALUE);
						Config base = new Config();
						BlobBasedConfig cfg = new BlobBasedConfig(base, bytes);
						String subUrl = cfg.getString("submodule", "src",
								"url");
						assertEquals("../chromium/src", subUrl);
					}
					fetchSlash = !fetchSlash;
				} while (fetchSlash);
				baseSlash = !baseSlash;
			} while (baseSlash);
		}
	}

	@Test
	public void testAddRepoManifest() throws Exception {
		StringBuilder xmlContent = new StringBuilder();
		xmlContent.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
			.append("<manifest>")
			.append("<remote name=\"remote1\" fetch=\".\" />")
			.append("<default revision=\"master\" remote=\"remote1\" />")
			.append("<project path=\"foo\" name=\"")
			.append(defaultUri)
			.append("\" />")
			.append("</manifest>");
		writeTrashFile("manifest.xml", xmlContent.toString());
		RepoCommand command = new RepoCommand(db);
		command.setPath(db.getWorkTree().getAbsolutePath() + "/manifest.xml")
			.setURI(rootUri)
			.call();
		File hello = new File(db.getWorkTree(), "foo/hello.txt");
		assertTrue("submodule should be checked out", hello.exists());
		try (BufferedReader reader = new BufferedReader(
				new FileReader(hello))) {
			String content = reader.readLine();
			assertEquals("submodule content should be as expected",
					"master world", content);
		}
	}

	@Test
	public void testRepoManifestGroups() throws Exception {
		StringBuilder xmlContent = new StringBuilder();
		xmlContent.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
			.append("<manifest>")
			.append("<remote name=\"remote1\" fetch=\".\" />")
			.append("<default revision=\"master\" remote=\"remote1\" />")
			.append("<project path=\"foo\" name=\"")
			.append(defaultUri)
			.append("\" groups=\"a,test\" />")
			.append("<project path=\"bar\" name=\"")
			.append(notDefaultUri)
			.append("\" groups=\"notdefault\" />")
			.append("<project path=\"a\" name=\"")
			.append(groupAUri)
			.append("\" groups=\"a\" />")
			.append("<project path=\"b\" name=\"")
			.append(groupBUri)
			.append("\" groups=\"b\" />")
			.append("</manifest>");

		// default should have foo, a & b
		Repository localDb = createWorkRepository();
		JGitTestUtil.writeTrashFile(
				localDb, "manifest.xml", xmlContent.toString());
		RepoCommand command = new RepoCommand(localDb);
		command
			.setPath(localDb.getWorkTree().getAbsolutePath() + "/manifest.xml")
			.setURI(rootUri)
			.call();
		File file = new File(localDb.getWorkTree(), "foo/hello.txt");
		assertTrue("default should have foo", file.exists());
		file = new File(localDb.getWorkTree(), "bar/world.txt");
		assertFalse("default shouldn't have bar", file.exists());
		file = new File(localDb.getWorkTree(), "a/a.txt");
		assertTrue("default should have a", file.exists());
		file = new File(localDb.getWorkTree(), "b/b.txt");
		assertTrue("default should have b", file.exists());

		// all,-a should have bar & b
		localDb = createWorkRepository();
		JGitTestUtil.writeTrashFile(
				localDb, "manifest.xml", xmlContent.toString());
		command = new RepoCommand(localDb);
		command
			.setPath(localDb.getWorkTree().getAbsolutePath() + "/manifest.xml")
			.setURI(rootUri)
			.setGroups("all,-a")
			.call();
		file = new File(localDb.getWorkTree(), "foo/hello.txt");
		assertFalse("\"all,-a\" shouldn't have foo", file.exists());
		file = new File(localDb.getWorkTree(), "bar/world.txt");
		assertTrue("\"all,-a\" should have bar", file.exists());
		file = new File(localDb.getWorkTree(), "a/a.txt");
		assertFalse("\"all,-a\" shuoldn't have a", file.exists());
		file = new File(localDb.getWorkTree(), "b/b.txt");
		assertTrue("\"all,-a\" should have b", file.exists());
	}

	@Test
	public void testRepoManifestCopyFile() throws Exception {
		Repository localDb = createWorkRepository();
		StringBuilder xmlContent = new StringBuilder();
		xmlContent.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
			.append("<manifest>")
			.append("<remote name=\"remote1\" fetch=\".\" />")
			.append("<default revision=\"master\" remote=\"remote1\" />")
			.append("<project path=\"foo\" name=\"")
			.append(defaultUri)
			.append("\">")
			.append("<copyfile src=\"hello.txt\" dest=\"Hello\" />")
			.append("</project>")
			.append("</manifest>");
		JGitTestUtil.writeTrashFile(
				localDb, "manifest.xml", xmlContent.toString());
		RepoCommand command = new RepoCommand(localDb);
		command
			.setPath(localDb.getWorkTree().getAbsolutePath() + "/manifest.xml")
			.setURI(rootUri)
			.call();
		// The original file should exist
		File hello = new File(localDb.getWorkTree(), "foo/hello.txt");
		assertTrue("The original file should exist", hello.exists());
		try (BufferedReader reader = new BufferedReader(
				new FileReader(hello))) {
			String content = reader.readLine();
			assertEquals("The original file should have expected content",
					"master world", content);
		}
		// The dest file should also exist
		hello = new File(localDb.getWorkTree(), "Hello");
		assertTrue("The destination file should exist", hello.exists());
		try (BufferedReader reader = new BufferedReader(
				new FileReader(hello))) {
			String content = reader.readLine();
			assertEquals("The destination file should have expected content",
					"master world", content);
		}
	}

	@Test
	public void testBareRepo() throws Exception {
		Repository remoteDb = createBareRepository();
		Repository tempDb = createWorkRepository();

		StringBuilder xmlContent = new StringBuilder();
		xmlContent.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
				.append("<manifest>")
				.append("<remote name=\"remote1\" fetch=\".\" />")
				.append("<default revision=\"master\" remote=\"remote1\" />")
				.append("<project path=\"foo\" name=\"").append(defaultUri)
				.append("\" />").append("</manifest>");
		JGitTestUtil.writeTrashFile(tempDb, "manifest.xml",
				xmlContent.toString());
		RepoCommand command = new RepoCommand(remoteDb);
		command.setPath(
				tempDb.getWorkTree().getAbsolutePath() + "/manifest.xml")
				.setURI(rootUri).call();
		// Clone it
		File directory = createTempDirectory("testBareRepo");
		try (Repository localDb = Git.cloneRepository().setDirectory(directory)
				.setURI(remoteDb.getDirectory().toURI().toString()).call()
				.getRepository()) {
			// The .gitmodules file should exist
			File gitmodules = new File(localDb.getWorkTree(), ".gitmodules");
			assertTrue("The .gitmodules file should exist",
					gitmodules.exists());
			// The first line of .gitmodules file should be expected
			try (BufferedReader reader = new BufferedReader(
					new FileReader(gitmodules))) {
				String content = reader.readLine();
				assertEquals(
						"The first line of .gitmodules file should be as expected",
						"[submodule \"foo\"]", content);
			}
			// The gitlink should be the same as remote head sha1
			String gitlink = localDb.resolve(Constants.HEAD + ":foo").name();
			String remote = defaultDb.resolve(Constants.HEAD).name();
			assertEquals("The gitlink should be the same as remote head",
					remote, gitlink);
		}
	}

	@Test
	public void testRevision() throws Exception {
		StringBuilder xmlContent = new StringBuilder();
		xmlContent.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
			.append("<manifest>")
			.append("<remote name=\"remote1\" fetch=\".\" />")
			.append("<default revision=\"master\" remote=\"remote1\" />")
			.append("<project path=\"foo\" name=\"")
			.append(defaultUri)
			.append("\" revision=\"")
			.append(oldCommitId.name())
			.append("\" />")
			.append("</manifest>");
		writeTrashFile("manifest.xml", xmlContent.toString());
		RepoCommand command = new RepoCommand(db);
		command.setPath(db.getWorkTree().getAbsolutePath() + "/manifest.xml")
			.setURI(rootUri)
			.call();
		File hello = new File(db.getWorkTree(), "foo/hello.txt");
		try (BufferedReader reader = new BufferedReader(
				new FileReader(hello))) {
			String content = reader.readLine();
			assertEquals("submodule content should be as expected",
					"branch world", content);
		}
	}

	@Test
	public void testRevisionBranch() throws Exception {
		StringBuilder xmlContent = new StringBuilder();
		xmlContent.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
			.append("<manifest>")
			.append("<remote name=\"remote1\" fetch=\".\" />")
			.append("<default revision=\"")
			.append(BRANCH)
			.append("\" remote=\"remote1\" />")
			.append("<project path=\"foo\" name=\"")
			.append(defaultUri)
			.append("\" />")
			.append("</manifest>");
		writeTrashFile("manifest.xml", xmlContent.toString());
		RepoCommand command = new RepoCommand(db);
		command.setPath(db.getWorkTree().getAbsolutePath() + "/manifest.xml")
			.setURI(rootUri)
			.call();
		File hello = new File(db.getWorkTree(), "foo/hello.txt");
		try (BufferedReader reader = new BufferedReader(
				new FileReader(hello))) {
			String content = reader.readLine();
			assertEquals("submodule content should be as expected",
					"branch world", content);
		}
	}

	@Test
	public void testRevisionTag() throws Exception {
		StringBuilder xmlContent = new StringBuilder();
		xmlContent.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
			.append("<manifest>")
			.append("<remote name=\"remote1\" fetch=\".\" />")
			.append("<default revision=\"master\" remote=\"remote1\" />")
			.append("<project path=\"foo\" name=\"")
			.append(defaultUri)
			.append("\" revision=\"")
			.append(TAG)
			.append("\" />")
			.append("</manifest>");
		writeTrashFile("manifest.xml", xmlContent.toString());
		RepoCommand command = new RepoCommand(db);
		command.setPath(db.getWorkTree().getAbsolutePath() + "/manifest.xml")
			.setURI(rootUri)
			.call();
		File hello = new File(db.getWorkTree(), "foo/hello.txt");
		try (BufferedReader reader = new BufferedReader(
				new FileReader(hello))) {
			String content = reader.readLine();
			assertEquals("submodule content should be as expected",
					"branch world", content);
		}
	}

	@Test
	public void testRevisionBare() throws Exception {
		Repository remoteDb = createBareRepository();
		Repository tempDb = createWorkRepository();

		StringBuilder xmlContent = new StringBuilder();
		xmlContent.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
				.append("<manifest>")
				.append("<remote name=\"remote1\" fetch=\".\" />")
				.append("<default revision=\"").append(BRANCH)
				.append("\" remote=\"remote1\" />")
				.append("<project path=\"foo\" name=\"").append(defaultUri)
				.append("\" />").append("</manifest>");
		JGitTestUtil.writeTrashFile(tempDb, "manifest.xml",
				xmlContent.toString());
		RepoCommand command = new RepoCommand(remoteDb);
		command.setPath(
				tempDb.getWorkTree().getAbsolutePath() + "/manifest.xml")
				.setURI(rootUri).call();
		// Clone it
		File directory = createTempDirectory("testRevisionBare");
		try (Repository localDb = Git.cloneRepository().setDirectory(directory)
				.setURI(remoteDb.getDirectory().toURI().toString()).call()
				.getRepository()) {
			// The gitlink should be the same as oldCommitId
			String gitlink = localDb.resolve(Constants.HEAD + ":foo").name();
			assertEquals("The gitlink is same as remote head",
					oldCommitId.name(), gitlink);
		}
	}

	@Test
	public void testCopyFileBare() throws Exception {
		Repository remoteDb = createBareRepository();
		Repository tempDb = createWorkRepository();

		StringBuilder xmlContent = new StringBuilder();
		xmlContent.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
				.append("<manifest>")
				.append("<remote name=\"remote1\" fetch=\".\" />")
				.append("<default revision=\"master\" remote=\"remote1\" />")
				.append("<project path=\"foo\" name=\"").append(defaultUri)
				.append("\" revision=\"").append(BRANCH).append("\" >")
				.append("<copyfile src=\"hello.txt\" dest=\"Hello\" />")
				.append("<copyfile src=\"hello.txt\" dest=\"foo/Hello\" />")
				.append("</project>").append("</manifest>");
		JGitTestUtil.writeTrashFile(tempDb, "manifest.xml",
				xmlContent.toString());
		RepoCommand command = new RepoCommand(remoteDb);
		command.setPath(
				tempDb.getWorkTree().getAbsolutePath() + "/manifest.xml")
				.setURI(rootUri).call();
		// Clone it
		File directory = createTempDirectory("testCopyFileBare");
		try (Repository localDb = Git.cloneRepository().setDirectory(directory)
				.setURI(remoteDb.getDirectory().toURI().toString()).call()
				.getRepository()) {
			// The Hello file should exist
			File hello = new File(localDb.getWorkTree(), "Hello");
			assertTrue("The Hello file should exist", hello.exists());
			// The foo/Hello file should be skipped.
			File foohello = new File(localDb.getWorkTree(), "foo/Hello");
			assertFalse("The foo/Hello file should be skipped",
					foohello.exists());
			// The content of Hello file should be expected
			try (BufferedReader reader = new BufferedReader(
					new FileReader(hello))) {
				String content = reader.readLine();
				assertEquals("The Hello file should have expected content",
						"branch world", content);
			}
		}
	}

	@Test
	public void testReplaceManifestBare() throws Exception {
		Repository remoteDb = createBareRepository();
		Repository tempDb = createWorkRepository();

		StringBuilder xmlContent = new StringBuilder();
		xmlContent.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
				.append("<manifest>")
				.append("<remote name=\"remote1\" fetch=\".\" />")
				.append("<default revision=\"master\" remote=\"remote1\" />")
				.append("<project path=\"foo\" name=\"").append(defaultUri)
				.append("\" revision=\"").append(BRANCH).append("\" >")
				.append("<copyfile src=\"hello.txt\" dest=\"Hello\" />")
				.append("</project>").append("</manifest>");
		JGitTestUtil.writeTrashFile(tempDb, "old.xml", xmlContent.toString());
		RepoCommand command = new RepoCommand(remoteDb);
		command.setPath(tempDb.getWorkTree().getAbsolutePath() + "/old.xml")
				.setURI(rootUri).call();
		xmlContent = new StringBuilder();
		xmlContent.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
				.append("<manifest>")
				.append("<remote name=\"remote1\" fetch=\".\" />")
				.append("<default revision=\"master\" remote=\"remote1\" />")
				.append("<project path=\"bar\" name=\"").append(defaultUri)
				.append("\" revision=\"").append(BRANCH).append("\" >")
				.append("<copyfile src=\"hello.txt\" dest=\"Hello.txt\" />")
				.append("</project>").append("</manifest>");
		JGitTestUtil.writeTrashFile(tempDb, "new.xml", xmlContent.toString());
		command = new RepoCommand(remoteDb);
		command.setPath(tempDb.getWorkTree().getAbsolutePath() + "/new.xml")
				.setURI(rootUri).call();
		// Clone it
		File directory = createTempDirectory("testReplaceManifestBare");
		File dotmodules;
		try (Repository localDb = Git.cloneRepository().setDirectory(directory)
				.setURI(remoteDb.getDirectory().toURI().toString()).call()
				.getRepository()) {
			// The Hello file should not exist
			File hello = new File(localDb.getWorkTree(), "Hello");
			assertFalse("The Hello file shouldn't exist", hello.exists());
			// The Hello.txt file should exist
			File hellotxt = new File(localDb.getWorkTree(), "Hello.txt");
			assertTrue("The Hello.txt file should exist", hellotxt.exists());
			dotmodules = new File(localDb.getWorkTree(),
					Constants.DOT_GIT_MODULES);
		}
		// The .gitmodules file should have 'submodule "bar"' and shouldn't
		// have
		// 'submodule "foo"' lines.
		try (BufferedReader reader = new BufferedReader(
				new FileReader(dotmodules))) {
			boolean foo = false;
			boolean bar = false;
			while (true) {
				String line = reader.readLine();
				if (line == null)
					break;
				if (line.contains("submodule \"foo\""))
					foo = true;
				if (line.contains("submodule \"bar\""))
					bar = true;
			}
			assertTrue("The bar submodule should exist", bar);
			assertFalse("The foo submodule shouldn't exist", foo);
		}
	}

	@Test
	public void testRemoveOverlappingBare() throws Exception {
		Repository remoteDb = createBareRepository();
		Repository tempDb = createWorkRepository();

		StringBuilder xmlContent = new StringBuilder();
		xmlContent.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
				.append("<manifest>")
				.append("<remote name=\"remote1\" fetch=\".\" />")
				.append("<default revision=\"master\" remote=\"remote1\" />")
				.append("<project path=\"foo/bar\" name=\"").append(groupBUri)
				.append("\" />").append("<project path=\"a\" name=\"")
				.append(groupAUri).append("\" />")
				.append("<project path=\"foo\" name=\"").append(defaultUri)
				.append("\" />").append("</manifest>");
		JGitTestUtil.writeTrashFile(tempDb, "manifest.xml",
				xmlContent.toString());
		RepoCommand command = new RepoCommand(remoteDb);
		command.setPath(
				tempDb.getWorkTree().getAbsolutePath() + "/manifest.xml")
				.setURI(rootUri).call();
		// Clone it
		File directory = createTempDirectory("testRemoveOverlappingBare");
		File dotmodules;
		try (Repository localDb = Git.cloneRepository().setDirectory(directory)
				.setURI(remoteDb.getDirectory().toURI().toString()).call()
				.getRepository()) {
			dotmodules = new File(localDb.getWorkTree(),
				Constants.DOT_GIT_MODULES);
		}

		// The .gitmodules file should have 'submodule "foo"' and shouldn't
		// have
		// 'submodule "foo/bar"' lines.
		try (BufferedReader reader = new BufferedReader(
				new FileReader(dotmodules))) {
			boolean foo = false;
			boolean foobar = false;
			boolean a = false;
			while (true) {
				String line = reader.readLine();
				if (line == null)
					break;
				if (line.contains("submodule \"foo\""))
					foo = true;
				if (line.contains("submodule \"foo/bar\""))
					foobar = true;
				if (line.contains("submodule \"a\""))
					a = true;
			}
			assertTrue("The foo submodule should exist", foo);
			assertFalse("The foo/bar submodule shouldn't exist", foobar);
			assertTrue("The a submodule should exist", a);
		}
	}

	@Test
	public void testIncludeTag() throws Exception {
		Repository localDb = createWorkRepository();
		Repository tempDb = createWorkRepository();

		StringBuilder xmlContent = new StringBuilder();
		xmlContent.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
			.append("<manifest>")
			.append("<include name=\"_include.xml\" />")
			.append("<default revision=\"master\" remote=\"remote1\" />")
			.append("</manifest>");
		JGitTestUtil.writeTrashFile(
				tempDb, "manifest.xml", xmlContent.toString());

		xmlContent = new StringBuilder();
		xmlContent.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
			.append("<manifest>")
			.append("<remote name=\"remote1\" fetch=\".\" />")
			.append("<default revision=\"master\" remote=\"remote1\" />")
			.append("<project path=\"foo\" name=\"")
			.append(defaultUri)
			.append("\" />")
			.append("</manifest>");
		JGitTestUtil.writeTrashFile(
				tempDb, "_include.xml", xmlContent.toString());

		RepoCommand command = new RepoCommand(localDb);
		command
			.setPath(tempDb.getWorkTree().getAbsolutePath() + "/manifest.xml")
			.setURI(rootUri)
			.call();
		File hello = new File(localDb.getWorkTree(), "foo/hello.txt");
		assertTrue("submodule should be checked out", hello.exists());
		try (BufferedReader reader = new BufferedReader(
				new FileReader(hello))) {
			String content = reader.readLine();
			assertEquals("submodule content should be as expected",
					"master world", content);
		}
	}
	@Test
	public void testRemoteAlias() throws Exception {
		StringBuilder xmlContent = new StringBuilder();
		xmlContent.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
			.append("<manifest>")
			.append("<remote name=\"remote1\" fetch=\".\" alias=\"remote2\" />")
			.append("<default revision=\"master\" remote=\"remote2\" />")
			.append("<project path=\"foo\" name=\"")
			.append(defaultUri)
			.append("\" />")
			.append("</manifest>");

		Repository localDb = createWorkRepository();
		JGitTestUtil.writeTrashFile(
				localDb, "manifest.xml", xmlContent.toString());
		RepoCommand command = new RepoCommand(localDb);
		command
			.setPath(localDb.getWorkTree().getAbsolutePath() + "/manifest.xml")
			.setURI(rootUri)
			.call();
		File file = new File(localDb.getWorkTree(), "foo/hello.txt");
		assertTrue("We should have foo", file.exists());
	}

	@Test
	public void testTargetBranch() throws Exception {
		Repository remoteDb1 = createBareRepository();
		Repository remoteDb2 = createBareRepository();
		Repository tempDb = createWorkRepository();

		StringBuilder xmlContent = new StringBuilder();
		xmlContent.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
				.append("<manifest>")
				.append("<remote name=\"remote1\" fetch=\".\" />")
				.append("<default revision=\"master\" remote=\"remote1\" />")
				.append("<project path=\"foo\" name=\"").append(defaultUri)
				.append("\" />").append("</manifest>");
		JGitTestUtil.writeTrashFile(tempDb, "manifest.xml",
				xmlContent.toString());
		RepoCommand command = new RepoCommand(remoteDb1);
		command.setPath(
				tempDb.getWorkTree().getAbsolutePath() + "/manifest.xml")
				.setURI(rootUri).setTargetBranch("test").call();
		ObjectId branchId = remoteDb1
				.resolve(Constants.R_HEADS + "test^{tree}");
		command = new RepoCommand(remoteDb2);
		command.setPath(
				tempDb.getWorkTree().getAbsolutePath() + "/manifest.xml")
				.setURI(rootUri).call();
		ObjectId defaultId = remoteDb2.resolve(Constants.HEAD + "^{tree}");
		assertEquals(
				"The tree id of branch db and default db should be the same",
				branchId, defaultId);
	}

	@Test
	public void testRecordRemoteBranch() throws Exception {
		Repository remoteDb = createBareRepository();
		Repository tempDb = createWorkRepository();

		StringBuilder xmlContent = new StringBuilder();
		xmlContent.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
				.append("<manifest>")
				.append("<remote name=\"remote1\" fetch=\".\" />")
				.append("<default revision=\"master\" remote=\"remote1\" />")
				.append("<project path=\"with-branch\" ")
				.append("revision=\"master\" ").append("name=\"")
				.append(notDefaultUri).append("\" />")
				.append("<project path=\"with-long-branch\" ")
				.append("revision=\"refs/heads/master\" ").append("name=\"")
				.append(defaultUri).append("\" />").append("</manifest>");
		JGitTestUtil.writeTrashFile(tempDb, "manifest.xml",
				xmlContent.toString());

		RepoCommand command = new RepoCommand(remoteDb);
		command.setPath(
				tempDb.getWorkTree().getAbsolutePath() + "/manifest.xml")
				.setURI(rootUri).setRecordRemoteBranch(true).call();
		// Clone it
		File directory = createTempDirectory("testBareRepo");
		try (Repository localDb = Git.cloneRepository().setDirectory(directory)
				.setURI(remoteDb.getDirectory().toURI().toString()).call()
				.getRepository();) {
			// The .gitmodules file should exist
			File gitmodules = new File(localDb.getWorkTree(), ".gitmodules");
			assertTrue("The .gitmodules file should exist",
					gitmodules.exists());
			FileBasedConfig c = new FileBasedConfig(gitmodules, FS.DETECTED);
			c.load();
			assertEquals(
					"Recording remote branches should work for short branch descriptions",
					"master",
					c.getString("submodule", "with-branch", "branch"));
			assertEquals(
					"Recording remote branches should work for full ref specs",
					"refs/heads/master",
					c.getString("submodule", "with-long-branch", "branch"));
		}
	}


	@Test
	public void testRecordSubmoduleLabels() throws Exception {
		Repository remoteDb = createBareRepository();
		Repository tempDb = createWorkRepository();

		StringBuilder xmlContent = new StringBuilder();
		xmlContent.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
				.append("<manifest>")
				.append("<remote name=\"remote1\" fetch=\".\" />")
				.append("<default revision=\"master\" remote=\"remote1\" />")
				.append("<project path=\"test\" ")
				.append("revision=\"master\" ").append("name=\"")
				.append(notDefaultUri).append("\" ")
				.append("groups=\"a1,a2\" />").append("</manifest>");
		JGitTestUtil.writeTrashFile(tempDb, "manifest.xml",
				xmlContent.toString());

		RepoCommand command = new RepoCommand(remoteDb);
		command.setPath(
				tempDb.getWorkTree().getAbsolutePath() + "/manifest.xml")
				.setURI(rootUri).setRecordSubmoduleLabels(true).call();
		// Clone it
		File directory = createTempDirectory("testBareRepo");
		try (Repository localDb = Git.cloneRepository().setDirectory(directory)
				.setURI(remoteDb.getDirectory().toURI().toString()).call()
				.getRepository();) {
			// The .gitattributes file should exist
			File gitattributes = new File(localDb.getWorkTree(),
					".gitattributes");
			assertTrue("The .gitattributes file should exist",
					gitattributes.exists());
			try (BufferedReader reader = new BufferedReader(
					new FileReader(gitattributes));) {
				String content = reader.readLine();
				assertEquals(".gitattributes content should be as expected",
						"/test a1 a2", content);
			}
		}
	}

	@Test
	public void testRecordShallowRecommendation() throws Exception {
		Repository remoteDb = createBareRepository();
		Repository tempDb = createWorkRepository();

		StringBuilder xmlContent = new StringBuilder();
		xmlContent.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
				.append("<manifest>")
				.append("<remote name=\"remote1\" fetch=\".\" />")
				.append("<default revision=\"master\" remote=\"remote1\" />")
				.append("<project path=\"shallow-please\" ").append("name=\"")
				.append(defaultUri).append("\" ").append("clone-depth=\"1\" />")
				.append("<project path=\"non-shallow\" ").append("name=\"")
				.append(defaultUri).append("\" />").append("</manifest>");
		JGitTestUtil.writeTrashFile(tempDb, "manifest.xml",
				xmlContent.toString());

		RepoCommand command = new RepoCommand(remoteDb);
		command.setPath(
				tempDb.getWorkTree().getAbsolutePath() + "/manifest.xml")
				.setURI(rootUri).setRecommendShallow(true).call();
		// Clone it
		File directory = createTempDirectory("testBareRepo");
		try (Repository localDb = Git.cloneRepository().setDirectory(directory)
				.setURI(remoteDb.getDirectory().toURI().toString()).call()
				.getRepository();) {
			// The .gitmodules file should exist
			File gitmodules = new File(localDb.getWorkTree(), ".gitmodules");
			assertTrue("The .gitmodules file should exist",
					gitmodules.exists());
			FileBasedConfig c = new FileBasedConfig(gitmodules, FS.DETECTED);
			c.load();
			assertEquals("Recording shallow configuration should work", "true",
					c.getString("submodule", "shallow-please", "shallow"));
			assertNull("Recording non shallow configuration should work",
					c.getString("submodule", "non-shallow", "shallow"));
		}
	}

	@Test
	public void testRemoteRevision() throws Exception {
		StringBuilder xmlContent = new StringBuilder();
		xmlContent.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
			.append("<manifest>")
			.append("<remote name=\"remote1\" fetch=\".\" />")
			.append("<remote name=\"remote2\" fetch=\".\" revision=\"")
			.append(BRANCH)
			.append("\" />")
			.append("<default remote=\"remote1\" revision=\"master\" />")
			.append("<project path=\"foo\" remote=\"remote2\" name=\"")
			.append(defaultUri)
			.append("\" />")
			.append("</manifest>");
		writeTrashFile("manifest.xml", xmlContent.toString());
		RepoCommand command = new RepoCommand(db);
		command.setPath(db.getWorkTree().getAbsolutePath() + "/manifest.xml")
			.setURI(rootUri)
			.call();
		File hello = new File(db.getWorkTree(), "foo/hello.txt");
		try (BufferedReader reader = new BufferedReader(
				new FileReader(hello))) {
			String content = reader.readLine();
			assertEquals("submodule content should be as expected",
					"branch world", content);
		}
	}

	@Test
	public void testDefaultRemoteRevision() throws Exception {
		StringBuilder xmlContent = new StringBuilder();
		xmlContent.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
			.append("<manifest>")
			.append("<remote name=\"remote1\" fetch=\".\" revision=\"")
			.append(BRANCH)
			.append("\" />")
			.append("<default remote=\"remote1\" />")
			.append("<project path=\"foo\" name=\"")
			.append(defaultUri)
			.append("\" />")
			.append("</manifest>");
		writeTrashFile("manifest.xml", xmlContent.toString());
		RepoCommand command = new RepoCommand(db);
		command.setPath(db.getWorkTree().getAbsolutePath() + "/manifest.xml")
			.setURI(rootUri)
			.call();
		File hello = new File(db.getWorkTree(), "foo/hello.txt");
		try (BufferedReader reader = new BufferedReader(
				new FileReader(hello))) {
			String content = reader.readLine();
			assertEquals("submodule content should be as expected",
					"branch world", content);
		}
	}

	private void resolveRelativeUris() {
		// Find the longest common prefix ends with "/" as rootUri.
		defaultUri = defaultDb.getDirectory().toURI().toString();
		notDefaultUri = notDefaultDb.getDirectory().toURI().toString();
		groupAUri = groupADb.getDirectory().toURI().toString();
		groupBUri = groupBDb.getDirectory().toURI().toString();
		int start = 0;
		while (start <= defaultUri.length()) {
			int newStart = defaultUri.indexOf('/', start + 1);
			String prefix = defaultUri.substring(0, newStart);
			if (!notDefaultUri.startsWith(prefix) ||
					!groupAUri.startsWith(prefix) ||
					!groupBUri.startsWith(prefix)) {
				start++;
				rootUri = defaultUri.substring(0, start) + "manifest";
				defaultUri = defaultUri.substring(start);
				notDefaultUri = notDefaultUri.substring(start);
				groupAUri = groupAUri.substring(start);
				groupBUri = groupBUri.substring(start);
				return;
			}
			start = newStart;
		}
	}

	void testRelative(String a, String b, String want) {
		String got = RepoCommand.relativize(URI.create(a), URI.create(b)).toString();

		if (!got.equals(want)) {
			fail(String.format("relative('%s', '%s') = '%s', want '%s'", a, b, got, want));
		}
	}

	@Test
	public void relative() {
		testRelative("a/b/", "a/", "../");
		// Normalization:
		testRelative("a/p/..//b/", "a/", "../");
		testRelative("a/b", "a/", "");
		testRelative("a/", "a/b/", "b/");
		testRelative("a/", "a/b", "b");
		testRelative("/a/b/c", "/b/c", "../../b/c");
		testRelative("/abc", "bcd", "bcd");
		testRelative("abc", "def", "def");
		testRelative("abc", "/bcd", "/bcd");
		testRelative("http://a", "a/b", "a/b");
		testRelative("http://base.com/a/", "http://child.com/a/b", "http://child.com/a/b");
		testRelative("http://base.com/a/", "http://base.com/a/b", "b");
	}
}
