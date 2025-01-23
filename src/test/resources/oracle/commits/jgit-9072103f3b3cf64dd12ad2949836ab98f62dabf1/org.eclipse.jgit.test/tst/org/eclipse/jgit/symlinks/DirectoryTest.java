/*
 * Copyright (C) 2023 Thomas Wolf <twolf@apache.org> and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0 which is available at
 * https://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package org.eclipse.jgit.symlinks;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ResetCommand.ResetType;
import org.eclipse.jgit.junit.RepositoryTestCase;
import org.eclipse.jgit.junit.TestRepository;
import org.eclipse.jgit.lib.ConfigConstants;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.patch.Patch;
import org.eclipse.jgit.patch.PatchApplier;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.util.FS;
import org.eclipse.jgit.util.FileUtils;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class DirectoryTest extends RepositoryTestCase {

	@BeforeClass
	public static void checkPrecondition() throws Exception {
		Assume.assumeTrue(FS.DETECTED.supportsSymlinks());
		Path tempDir = Files.createTempDirectory("jgit");
		try {
			Path a = tempDir.resolve("a");
			Files.writeString(a, "test");
			Path b = tempDir.resolve("A");
			Assume.assumeTrue(Files.exists(b));
		} finally {
			FileUtils.delete(tempDir.toFile(),
					FileUtils.RECURSIVE | FileUtils.IGNORE_ERRORS);
		}
	}

	@Parameters(name = "core.symlinks={0}")
	public static Boolean[] parameters() {
		return new Boolean[] { Boolean.TRUE, Boolean.FALSE };
	}

	@Parameter(0)
	public boolean useSymlinks;

	private void checkFiles() throws Exception {
		File a = new File(trash, "a");
		assertTrue("a should be a directory",
				Files.isDirectory(a.toPath(), LinkOption.NOFOLLOW_LINKS));
		File b = new File(a, "b");
		assertTrue("a/b should exist", b.isFile());
		File x = new File(trash, "x");
		assertTrue("x should be a directory",
				Files.isDirectory(x.toPath(), LinkOption.NOFOLLOW_LINKS));
		File y = new File(x, "y");
		assertTrue("x/y should exist", y.isFile());
	}

	@Test
	public void testCheckout() throws Exception {
		StoredConfig config = db.getConfig();
		config.setBoolean(ConfigConstants.CONFIG_CORE_SECTION, null,
				ConfigConstants.CONFIG_KEY_SYMLINKS, useSymlinks);
		config.save();
		try (TestRepository<Repository> repo = new TestRepository<>(db)) {
			db.incrementOpen();
			// Create links directly in the git repo, then use a hard reset
			// to get them into the workspace.
			RevCommit base = repo.commit(
					repo.tree(
							repo.link("A", repo.blob(".git")),
							repo.file("a/b", repo.blob("test")),
							repo.file("x/y", repo.blob("test2"))));
			try (Git git = new Git(db)) {
				git.reset().setMode(ResetType.HARD).setRef(base.name()).call();
				File b = new File(new File(trash, ".git"), "b");
				assertFalse(".git/b should not exist", b.exists());
				checkFiles();
			}
		}
	}

	@Test
	public void testCheckout2() throws Exception {
		StoredConfig config = db.getConfig();
		config.setBoolean(ConfigConstants.CONFIG_CORE_SECTION, null,
				ConfigConstants.CONFIG_KEY_SYMLINKS, useSymlinks);
		config.save();
		try (TestRepository<Repository> repo = new TestRepository<>(db)) {
			db.incrementOpen();
			RevCommit base = repo.commit(
					repo.tree(
							repo.link("A/B", repo.blob("../.git")),
							repo.file("a/b/a/b", repo.blob("test")),
							repo.file("x/y", repo.blob("test2"))));
			try (Git git = new Git(db)) {
				boolean testFiles = true;
				try {
					git.reset().setMode(ResetType.HARD).setRef(base.name())
							.call();
				} catch (Exception e) {
					if (!useSymlinks) {
						// There is a file in the middle of the path where we'd
						// expect a directory. This case is not handled
						// anywhere. What would be a better reply than an IOE?
						testFiles = false;
					} else {
						throw e;
					}
				}
				File a = new File(new File(trash, ".git"), "a");
				assertFalse(".git/a should not exist", a.exists());
				if (testFiles) {
					a = new File(trash, "a");
					assertTrue("a should be a directory", Files.isDirectory(
							a.toPath(), LinkOption.NOFOLLOW_LINKS));
					File b = new File(a, "b");
					assertTrue("a/b should be a directory", Files.isDirectory(
							a.toPath(), LinkOption.NOFOLLOW_LINKS));
					a = new File(b, "a");
					assertTrue("a/b/a should be a directory", Files.isDirectory(
							a.toPath(), LinkOption.NOFOLLOW_LINKS));
					b = new File(a, "b");
					assertTrue("a/b/a/b should exist", b.isFile());
					File x = new File(trash, "x");
					assertTrue("x should be a directory", Files.isDirectory(
							x.toPath(), LinkOption.NOFOLLOW_LINKS));
					File y = new File(x, "y");
					assertTrue("x/y should exist", y.isFile());
				}
			}
		}
	}

	@Test
	public void testMerge() throws Exception {
		StoredConfig config = db.getConfig();
		config.setBoolean(ConfigConstants.CONFIG_CORE_SECTION, null,
				ConfigConstants.CONFIG_KEY_SYMLINKS, useSymlinks);
		config.save();
		try (TestRepository<Repository> repo = new TestRepository<>(db)) {
			db.incrementOpen();
			RevCommit base = repo.commit(
					repo.tree(repo.file("q", repo.blob("test"))));
			RevCommit side = repo.commit(
					repo.tree(
							repo.link("A", repo.blob(".git")),
							repo.file("a/b", repo.blob("test")),
							repo.file("x/y", repo.blob("test2"))));
			try (Git git = new Git(db)) {
				git.reset().setMode(ResetType.HARD).setRef(base.name()).call();
				git.merge().include(side)
						.setMessage("merged").call();
				File b = new File(new File(trash, ".git"), "b");
				assertFalse(".git/b should not exist", b.exists());
				checkFiles();
			}
		}
	}

	@Test
	public void testMerge2() throws Exception {
		StoredConfig config = db.getConfig();
		config.setBoolean(ConfigConstants.CONFIG_CORE_SECTION, null,
				ConfigConstants.CONFIG_KEY_SYMLINKS, useSymlinks);
		config.save();
		try (TestRepository<Repository> repo = new TestRepository<>(db)) {
			db.incrementOpen();
			RevCommit base = repo.commit(
					repo.tree(
							repo.file("q", repo.blob("test")),
							repo.link("A", repo.blob(".git"))));
			RevCommit side = repo.commit(
					repo.tree(
							repo.file("a/b", repo.blob("test")),
							repo.file("x/y", repo.blob("test2"))));
			try (Git git = new Git(db)) {
				git.reset().setMode(ResetType.HARD).setRef(base.name()).call();
				git.merge().include(side)
						.setMessage("merged").call();
				File b = new File(new File(trash, ".git"), "b");
				assertFalse(".git/b should not exist", b.exists());
				checkFiles();
			}
		}
	}

	@Test
	public void testApply() throws Exception {
		StoredConfig config = db.getConfig();
		config.setBoolean(ConfigConstants.CONFIG_CORE_SECTION, null,
				ConfigConstants.CONFIG_KEY_SYMLINKS, useSymlinks);
		config.save();
		// PatchApplier doesn't do symlinks yet.
		try (TestRepository<Repository> repo = new TestRepository<>(db)) {
			db.incrementOpen();
			RevCommit base = repo.commit(
					repo.tree(
							repo.file("x", repo.blob("test")),
							repo.link("A", repo.blob(".git"))));
			try (Git git = new Git(db)) {
				git.reset().setMode(ResetType.HARD).setRef(base.name()).call();
				Patch patch = new Patch();
				try (InputStream patchStream = this.getClass()
						.getResourceAsStream("dirtest.patch")) {
					patch.parse(patchStream);
				}
				boolean testFiles = true;
				try {
					PatchApplier.Result result = new PatchApplier(db)
							.applyPatch(patch);
					assertNotNull(result);
				} catch (IOException e) {
					if (!useSymlinks) {
						// There is a file there, so the patch won't apply.
						// Unclear whether an IOE is the correct response,
						// though. Probably some negative PatchApplier.Result is
						// more appropriate.
						testFiles = false;
					} else {
						throw e;
					}
				}
				File b = new File(new File(trash, ".git"), "b");
				assertFalse(".git/b should not exist", b.exists());
				if (testFiles) {
					File a = new File(trash, "a");
					assertTrue("a should be a directory",
							Files.isDirectory(a.toPath(), LinkOption.NOFOLLOW_LINKS));
					b = new File(a, "b");
					assertTrue("a/b should exist", b.isFile());
				}
			}
		}
	}
}
