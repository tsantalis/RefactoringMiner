/*
 * Copyright (C) 2016, 2022 Christian Halstrick <christian.halstrick@sap.com> and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0 which is available at
 * https://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package org.eclipse.jgit.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.MergeResult;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.attributes.FilterCommand;
import org.eclipse.jgit.attributes.FilterCommandFactory;
import org.eclipse.jgit.attributes.FilterCommandRegistry;
import org.eclipse.jgit.junit.RepositoryTestCase;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.RefUpdate;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.revwalk.RevCommit;
import org.junit.Before;
import org.junit.Test;

public class FilterCommandsTest extends RepositoryTestCase {
	private Git git;

	RevCommit initialCommit;

	RevCommit secondCommit;

	class TestCommandFactory implements FilterCommandFactory {
		private int prefix;

		public TestCommandFactory(int prefix) {
			this.prefix = prefix;
		}

		@Override
		public FilterCommand create(Repository repo, InputStream in,
				final OutputStream out) {
			FilterCommand cmd = new FilterCommand(in, out) {

				@Override
				public int run() throws IOException {
					int b = in.read();
					if (b == -1) {
						in.close();
						out.close();
						return b;
					}
					out.write(prefix);
					out.write(b);
					return 1;
				}
			};
			return cmd;
		}
	}

	@Override
	@Before
	public void setUp() throws Exception {
		super.setUp();
		git = new Git(db);
		// commit something
		writeTrashFile("Test.txt", "Hello world");
		git.add().addFilepattern("Test.txt").call();
		initialCommit = git.commit().setMessage("Initial commit").call();

		// create a master branch and switch to it
		git.branchCreate().setName("test").call();
		RefUpdate rup = db.updateRef(Constants.HEAD);
		rup.link("refs/heads/test");

		// commit something on the test branch
		writeTrashFile("Test.txt", "Some change");
		git.add().addFilepattern("Test.txt").call();
		secondCommit = git.commit().setMessage("Second commit").call();
	}

	@Override
	public void tearDown() throws Exception {
		Set<String> existingFilters = new HashSet<>(
				FilterCommandRegistry.getRegisteredFilterCommands());
		existingFilters.forEach(FilterCommandRegistry::unregister);
		super.tearDown();
	}

	@Test
	public void testBuiltinCleanFilter()
			throws IOException, GitAPIException {
		String builtinCommandName = "jgit://builtin/test/clean";
		FilterCommandRegistry.register(builtinCommandName,
				new TestCommandFactory('c'));
		StoredConfig config = git.getRepository().getConfig();
		config.setString("filter", "test", "clean", builtinCommandName);
		config.save();

		writeTrashFile(".gitattributes", "*.txt filter=test");
		git.add().addFilepattern(".gitattributes").call();
		git.commit().setMessage("add filter").call();

		writeTrashFile("Test.txt", "Hello again");
		git.add().addFilepattern("Test.txt").call();
		assertEquals(
				"[.gitattributes, mode:100644, content:*.txt filter=test][Test.txt, mode:100644, content:cHceclclcoc cacgcacicn]",
				indexState(CONTENT));

		writeTrashFile("Test.bin", "Hello again");
		git.add().addFilepattern("Test.bin").call();
		assertEquals(
				"[.gitattributes, mode:100644, content:*.txt filter=test][Test.bin, mode:100644, content:Hello again][Test.txt, mode:100644, content:cHceclclcoc cacgcacicn]",
				indexState(CONTENT));

		config.setString("filter", "test", "clean", null);
		config.save();

		git.add().addFilepattern("Test.txt").call();
		assertEquals(
				"[.gitattributes, mode:100644, content:*.txt filter=test][Test.bin, mode:100644, content:Hello again][Test.txt, mode:100644, content:Hello again]",
				indexState(CONTENT));

		config.setString("filter", "test", "clean", null);
		config.save();
	}

	@Test
	public void testBuiltinSmudgeFilter() throws IOException, GitAPIException {
		String builtinCommandName = "jgit://builtin/test/smudge";
		FilterCommandRegistry.register(builtinCommandName,
				new TestCommandFactory('s'));
		StoredConfig config = git.getRepository().getConfig();
		config.setString("filter", "test", "smudge", builtinCommandName);
		config.save();

		writeTrashFile(".gitattributes", "*.txt filter=test");
		git.add().addFilepattern(".gitattributes").call();
		git.commit().setMessage("add filter").call();

		writeTrashFile("Test.txt", "Hello again");
		git.add().addFilepattern("Test.txt").call();
		assertEquals(
				"[.gitattributes, mode:100644, content:*.txt filter=test][Test.txt, mode:100644, content:Hello again]",
				indexState(CONTENT));
		assertEquals("Hello again", read("Test.txt"));
		deleteTrashFile("Test.txt");
		git.checkout().addPath("Test.txt").call();
		assertEquals("sHseslslsos sasgsasisn", read("Test.txt"));

		writeTrashFile("Test.bin", "Hello again");
		git.add().addFilepattern("Test.bin").call();
		assertEquals(
				"[.gitattributes, mode:100644, content:*.txt filter=test][Test.bin, mode:100644, content:Hello again][Test.txt, mode:100644, content:Hello again]",
				indexState(CONTENT));
		deleteTrashFile("Test.bin");
		git.checkout().addPath("Test.bin").call();
		assertEquals("Hello again", read("Test.bin"));

		config.setString("filter", "test", "clean", null);
		config.save();

		git.add().addFilepattern("Test.txt").call();
		assertEquals(
				"[.gitattributes, mode:100644, content:*.txt filter=test][Test.bin, mode:100644, content:Hello again][Test.txt, mode:100644, content:sHseslslsos sasgsasisn]",
				indexState(CONTENT));

		config.setString("filter", "test", "clean", null);
		config.save();
	}

	@Test
	public void testBuiltinCleanAndSmudgeFilter() throws IOException, GitAPIException {
		String builtinCommandPrefix = "jgit://builtin/test/";
		FilterCommandRegistry.register(builtinCommandPrefix + "smudge",
				new TestCommandFactory('s'));
		FilterCommandRegistry.register(builtinCommandPrefix + "clean",
				new TestCommandFactory('c'));
		StoredConfig config = git.getRepository().getConfig();
		config.setString("filter", "test", "smudge", builtinCommandPrefix+"smudge");
		config.setString("filter", "test", "clean",
				builtinCommandPrefix + "clean");
		config.save();

		writeTrashFile(".gitattributes", "*.txt filter=test");
		git.add().addFilepattern(".gitattributes").call();
		git.commit().setMessage("add filter").call();

		writeTrashFile("Test.txt", "Hello again");
		git.add().addFilepattern("Test.txt").call();
		assertEquals(
				"[.gitattributes, mode:100644, content:*.txt filter=test][Test.txt, mode:100644, content:cHceclclcoc cacgcacicn]",
				indexState(CONTENT));
		assertEquals("Hello again", read("Test.txt"));
		deleteTrashFile("Test.txt");
		git.checkout().addPath("Test.txt").call();
		assertEquals("scsHscsescslscslscsoscs scsascsgscsascsiscsn",
				read("Test.txt"));

		writeTrashFile("Test.bin", "Hello again");
		git.add().addFilepattern("Test.bin").call();
		assertEquals(
				"[.gitattributes, mode:100644, content:*.txt filter=test][Test.bin, mode:100644, content:Hello again][Test.txt, mode:100644, content:cHceclclcoc cacgcacicn]",
				indexState(CONTENT));
		deleteTrashFile("Test.bin");
		git.checkout().addPath("Test.bin").call();
		assertEquals("Hello again", read("Test.bin"));

		config.setString("filter", "test", "clean", null);
		config.save();

		git.add().addFilepattern("Test.txt").call();
		assertEquals(
				"[.gitattributes, mode:100644, content:*.txt filter=test][Test.bin, mode:100644, content:Hello again][Test.txt, mode:100644, content:scsHscsescslscslscsoscs scsascsgscsascsiscsn]",
				indexState(CONTENT));

		config.setString("filter", "test", "clean", null);
		config.save();
	}

	@Test
	public void testBranchSwitch() throws Exception {
		String builtinCommandPrefix = "jgit://builtin/test/";
		FilterCommandRegistry.register(builtinCommandPrefix + "smudge",
				new TestCommandFactory('s'));
		FilterCommandRegistry.register(builtinCommandPrefix + "clean",
				new TestCommandFactory('c'));
		StoredConfig config = git.getRepository().getConfig();
		config.setString("filter", "test", "smudge",
				builtinCommandPrefix + "smudge");
		config.setString("filter", "test", "clean",
				builtinCommandPrefix + "clean");
		config.save();
		// We're on the test branch
		File aFile = writeTrashFile("a.txt", "a");
		writeTrashFile(".gitattributes", "a.txt filter=test");
		File cFile = writeTrashFile("cc/c.txt", "C");
		writeTrashFile("cc/.gitattributes", "c.txt filter=test");
		git.add().addFilepattern(".").call();
		git.commit().setMessage("On test").call();
		git.checkout().setName("master").call();
		git.branchCreate().setName("other").call();
		git.checkout().setName("other").call();
		writeTrashFile("b.txt", "b");
		writeTrashFile(".gitattributes", "b.txt filter=test");
		git.add().addFilepattern(".").call();
		git.commit().setMessage("On other").call();
		git.checkout().setName("test").call();
		checkFile(aFile, "scsa");
		checkFile(cFile, "scsC");
	}

	@Test
	public void testCheckoutSingleFile() throws Exception {
		String builtinCommandPrefix = "jgit://builtin/test/";
		FilterCommandRegistry.register(builtinCommandPrefix + "smudge",
				new TestCommandFactory('s'));
		FilterCommandRegistry.register(builtinCommandPrefix + "clean",
				new TestCommandFactory('c'));
		StoredConfig config = git.getRepository().getConfig();
		config.setString("filter", "test", "smudge",
				builtinCommandPrefix + "smudge");
		config.setString("filter", "test", "clean",
				builtinCommandPrefix + "clean");
		config.save();
		// We're on the test branch
		File aFile = writeTrashFile("a.txt", "a");
		File attributes = writeTrashFile(".gitattributes", "a.txt filter=test");
		git.add().addFilepattern(".").call();
		git.commit().setMessage("On test").call();
		git.checkout().setName("master").call();
		git.branchCreate().setName("other").call();
		git.checkout().setName("other").call();
		writeTrashFile("b.txt", "b");
		writeTrashFile(".gitattributes", "b.txt filter=test");
		git.add().addFilepattern(".").call();
		git.commit().setMessage("On other").call();
		git.checkout().setName("master").call();
		assertFalse(aFile.exists());
		assertFalse(attributes.exists());
		git.checkout().setStartPoint("test").addPath("a.txt").call();
		checkFile(aFile, "scsa");
	}

	@Test
	public void testCheckoutSingleFile2() throws Exception {
		String builtinCommandPrefix = "jgit://builtin/test/";
		FilterCommandRegistry.register(builtinCommandPrefix + "smudge",
				new TestCommandFactory('s'));
		FilterCommandRegistry.register(builtinCommandPrefix + "clean",
				new TestCommandFactory('c'));
		StoredConfig config = git.getRepository().getConfig();
		config.setString("filter", "test", "smudge",
				builtinCommandPrefix + "smudge");
		config.setString("filter", "test", "clean",
				builtinCommandPrefix + "clean");
		config.save();
		// We're on the test branch
		File aFile = writeTrashFile("a.txt", "a");
		File attributes = writeTrashFile(".gitattributes", "a.txt filter=test");
		git.add().addFilepattern(".").call();
		git.commit().setMessage("On test").call();
		git.checkout().setName("master").call();
		git.branchCreate().setName("other").call();
		git.checkout().setName("other").call();
		writeTrashFile("b.txt", "b");
		writeTrashFile(".gitattributes", "b.txt filter=test");
		git.add().addFilepattern(".").call();
		git.commit().setMessage("On other").call();
		git.checkout().setName("master").call();
		assertFalse(aFile.exists());
		assertFalse(attributes.exists());
		writeTrashFile(".gitattributes", "");
		git.checkout().setStartPoint("test").addPath("a.txt").call();
		checkFile(aFile, "scsa");
	}

	@Test
	public void testMerge() throws Exception {
		String builtinCommandPrefix = "jgit://builtin/test/";
		FilterCommandRegistry.register(builtinCommandPrefix + "smudge",
				new TestCommandFactory('s'));
		FilterCommandRegistry.register(builtinCommandPrefix + "clean",
				new TestCommandFactory('c'));
		StoredConfig config = git.getRepository().getConfig();
		config.setString("filter", "test", "smudge",
				builtinCommandPrefix + "smudge");
		config.setString("filter", "test", "clean",
				builtinCommandPrefix + "clean");
		config.save();
		// We're on the test branch. Set up two branches that are expected to
		// merge cleanly.
		File aFile = writeTrashFile("a.txt", "a");
		writeTrashFile(".gitattributes", "a.txt filter=test");
		git.add().addFilepattern(".").call();
		RevCommit aCommit = git.commit().setMessage("On test").call();
		git.checkout().setName("master").call();
		assertFalse(aFile.exists());
		git.branchCreate().setName("other").call();
		git.checkout().setName("other").call();
		writeTrashFile("b/b.txt", "b");
		writeTrashFile("b/.gitattributes", "b.txt filter=test");
		git.add().addFilepattern(".").call();
		git.commit().setMessage("On other").call();
		MergeResult result = git.merge().include(aCommit).call();
		assertEquals(MergeResult.MergeStatus.MERGED, result.getMergeStatus());
		checkFile(aFile, "scsa");
	}

}
