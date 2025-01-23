/*
 * Copyright (C) 2010, Stefan Lay <stefan.lay@sap.com>
 * Copyright (C) 2010, Christian Halstrick <christian.halstrick@sap.com> and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0 which is available at
 * https://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package org.eclipse.jgit.api;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.eclipse.jgit.util.FileUtils.RECURSIVE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeTrue;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.eclipse.jgit.api.ResetCommand.ResetType;
import org.eclipse.jgit.api.errors.FilterFailedException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.NoFilepatternException;
import org.eclipse.jgit.attributes.FilterCommandRegistry;
import org.eclipse.jgit.dircache.DirCache;
import org.eclipse.jgit.dircache.DirCacheBuilder;
import org.eclipse.jgit.dircache.DirCacheEntry;
import org.eclipse.jgit.junit.JGitTestUtil;
import org.eclipse.jgit.junit.RepositoryTestCase;
import org.eclipse.jgit.lfs.BuiltinLFS;
import org.eclipse.jgit.lib.ConfigConstants;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.CoreConfig.SymLinks;
import org.eclipse.jgit.lib.FileMode;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectInserter;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.WorkingTreeOptions;
import org.eclipse.jgit.util.FS;
import org.eclipse.jgit.util.FileUtils;
import org.junit.Test;
import org.junit.experimental.theories.DataPoints;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.runner.RunWith;

@RunWith(Theories.class)
public class AddCommandTest extends RepositoryTestCase {
	@DataPoints
	public static boolean[] sleepBeforeAddOptions = { true, false };


	@Override
	public void setUp() throws Exception {
		BuiltinLFS.register();
		super.setUp();
	}

	@Test
	public void testAddNothing() throws GitAPIException {
		try (Git git = new Git(db)) {
			git.add().call();
			fail("Expected IllegalArgumentException");
		} catch (NoFilepatternException e) {
			// expected
		}

	}

	@Test
	public void testAddNonExistingSingleFile() throws GitAPIException {
		try (Git git = new Git(db)) {
			DirCache dc = git.add().addFilepattern("a.txt").call();
			assertEquals(0, dc.getEntryCount());
		}
	}

	@Test
	public void testAddExistingSingleFile() throws IOException, GitAPIException {
		File file = new File(db.getWorkTree(), "a.txt");
		FileUtils.createNewFile(file);
		try (PrintWriter writer = new PrintWriter(file, UTF_8.name())) {
			writer.print("content");
		}

		try (Git git = new Git(db)) {
			git.add().addFilepattern("a.txt").call();

			assertEquals(
					"[a.txt, mode:100644, content:content]",
					indexState(CONTENT));
		}
	}

	@Test
	public void testAddLink() throws IOException, GitAPIException {
		assumeTrue(db.getFS().supportsSymlinks());
		try (Git git = new Git(db)) {
			writeTrashFile("a.txt", "a");
			File link = new File(db.getWorkTree(), "link");
			db.getFS().createSymLink(link, "a.txt");
			git.add().addFilepattern(".").call();
			assertEquals(
					"[a.txt, mode:100644, content:a][link, mode:120000, content:a.txt]",
					indexState(CONTENT));
			git.commit().setMessage("link").call();
			StoredConfig config = db.getConfig();
			config.setEnum(ConfigConstants.CONFIG_CORE_SECTION, null,
					ConfigConstants.CONFIG_KEY_SYMLINKS, SymLinks.FALSE);
			config.save();
			Files.delete(link.toPath());
			git.reset().setMode(ResetType.HARD).call();
			assertTrue(Files.isRegularFile(link.toPath()));
			assertEquals(
					"[a.txt, mode:100644, content:a][link, mode:120000, content:a.txt]",
					indexState(CONTENT));
			writeTrashFile("link", "b.txt");
			git.add().addFilepattern("link").call();
			assertEquals(
					"[a.txt, mode:100644, content:a][link, mode:120000, content:b.txt]",
					indexState(CONTENT));
			config.setEnum(ConfigConstants.CONFIG_CORE_SECTION, null,
					ConfigConstants.CONFIG_KEY_SYMLINKS, SymLinks.TRUE);
			config.save();
			git.add().addFilepattern("link").call();
			assertEquals(
					"[a.txt, mode:100644, content:a][link, mode:100644, content:b.txt]",
					indexState(CONTENT));
		}
	}

	@Test
	public void testCleanFilter() throws IOException, GitAPIException {
		writeTrashFile(".gitattributes", "*.txt filter=tstFilter");
		writeTrashFile("src/a.tmp", "foo");
		// Caution: we need a trailing '\n' since sed on mac always appends
		// linefeeds if missing
		writeTrashFile("src/a.txt", "foo\n");
		File script = writeTempFile("sed s/o/e/g");

		try (Git git = new Git(db)) {
			StoredConfig config = git.getRepository().getConfig();
			config.setString("filter", "tstFilter", "clean",
					"sh " + slashify(script.getPath()));
			config.save();

			git.add().addFilepattern("src/a.txt").addFilepattern("src/a.tmp")
					.call();

			assertEquals(
					"[src/a.tmp, mode:100644, content:foo][src/a.txt, mode:100644, content:fee\n]",
					indexState(CONTENT));
		}
	}

	@Theory
	public void testBuiltinFilters(boolean sleepBeforeAdd)
			throws IOException,
			GitAPIException, InterruptedException {
		writeTrashFile(".gitattributes", "*.txt filter=lfs");
		writeTrashFile("src/a.tmp", "foo");
		// Caution: we need a trailing '\n' since sed on mac always appends
		// linefeeds if missing
		File script = writeTempFile("sed s/o/e/g");
		File f = writeTrashFile("src/a.txt", "foo\n");

		try (Git git = new Git(db)) {
			if (!sleepBeforeAdd) {
				fsTick(f);
			}
			git.add().addFilepattern(".gitattributes").call();
			StoredConfig config = git.getRepository().getConfig();
			config.setString("filter", "lfs", "clean",
					"sh " + slashify(script.getPath()));
			config.setString("filter", "lfs", "smudge",
					"sh " + slashify(script.getPath()));
			config.setBoolean("filter", "lfs", "useJGitBuiltin", true);
			config.save();

			if (!sleepBeforeAdd) {
				fsTick(f);
			}
			git.add().addFilepattern("src/a.txt").addFilepattern("src/a.tmp")
					.addFilepattern(".gitattributes").call();

			assertEquals(
					"[.gitattributes, mode:100644, content:*.txt filter=lfs][src/a.tmp, mode:100644, content:foo][src/a.txt, mode:100644, content:version https://git-lfs.github.com/spec/v1\noid sha256:b5bb9d8014a0f9b1d61e21e796d78dccdf1352f23cd32812f4850b878ae4944c\nsize 4\n]",
					indexState(CONTENT));

			RevCommit c1 = git.commit().setMessage("c1").call();
			assertTrue(git.status().call().isClean());
			f = writeTrashFile("src/a.txt", "foobar\n");
			if (!sleepBeforeAdd) {
				fsTick(f);
			}
			git.add().addFilepattern("src/a.txt").call();
			git.commit().setMessage("c2").call();
			assertTrue(git.status().call().isClean());
			assertEquals(
					"[.gitattributes, mode:100644, content:*.txt filter=lfs][src/a.tmp, mode:100644, content:foo][src/a.txt, mode:100644, content:version https://git-lfs.github.com/spec/v1\noid sha256:aec070645fe53ee3b3763059376134f058cc337247c978add178b6ccdfb0019f\nsize 7\n]",
					indexState(CONTENT));
			assertEquals("foobar\n", read("src/a.txt"));
			git.checkout().setName(c1.getName()).call();
			assertEquals(
					"[.gitattributes, mode:100644, content:*.txt filter=lfs][src/a.tmp, mode:100644, content:foo][src/a.txt, mode:100644, content:version https://git-lfs.github.com/spec/v1\noid sha256:b5bb9d8014a0f9b1d61e21e796d78dccdf1352f23cd32812f4850b878ae4944c\nsize 4\n]",
					indexState(CONTENT));
			assertEquals(
					"foo\n", read("src/a.txt"));
		}
	}

	@Theory
	public void testBuiltinCleanFilter(boolean sleepBeforeAdd)
			throws IOException, GitAPIException, InterruptedException {
		writeTrashFile(".gitattributes", "*.txt filter=lfs");
		writeTrashFile("src/a.tmp", "foo");
		// Caution: we need a trailing '\n' since sed on mac always appends
		// linefeeds if missing
		File script = writeTempFile("sed s/o/e/g");
		File f = writeTrashFile("src/a.txt", "foo\n");

		// unregister the smudge filter. Only clean filter should be builtin
		FilterCommandRegistry.unregister(
				org.eclipse.jgit.lib.Constants.BUILTIN_FILTER_PREFIX
						+ "lfs/smudge");

		try (Git git = new Git(db)) {
			if (!sleepBeforeAdd) {
				fsTick(f);
			}
			git.add().addFilepattern(".gitattributes").call();
			StoredConfig config = git.getRepository().getConfig();
			config.setString("filter", "lfs", "clean",
					"sh " + slashify(script.getPath()));
			config.setString("filter", "lfs", "smudge",
					"sh " + slashify(script.getPath()));
			config.setBoolean("filter", "lfs", "useJGitBuiltin", true);
			config.save();

			if (!sleepBeforeAdd) {
				fsTick(f);
			}
			git.add().addFilepattern("src/a.txt").addFilepattern("src/a.tmp")
					.addFilepattern(".gitattributes").call();

			assertEquals(
					"[.gitattributes, mode:100644, content:*.txt filter=lfs][src/a.tmp, mode:100644, content:foo][src/a.txt, mode:100644, content:version https://git-lfs.github.com/spec/v1\noid sha256:b5bb9d8014a0f9b1d61e21e796d78dccdf1352f23cd32812f4850b878ae4944c\nsize 4\n]",
					indexState(CONTENT));

			RevCommit c1 = git.commit().setMessage("c1").call();
			assertTrue(git.status().call().isClean());
			f = writeTrashFile("src/a.txt", "foobar\n");
			if (!sleepBeforeAdd) {
				fsTick(f);
			}
			git.add().addFilepattern("src/a.txt").call();
			git.commit().setMessage("c2").call();
			assertTrue(git.status().call().isClean());
			assertEquals(
					"[.gitattributes, mode:100644, content:*.txt filter=lfs][src/a.tmp, mode:100644, content:foo][src/a.txt, mode:100644, content:version https://git-lfs.github.com/spec/v1\noid sha256:aec070645fe53ee3b3763059376134f058cc337247c978add178b6ccdfb0019f\nsize 7\n]",
					indexState(CONTENT));
			assertEquals("foobar\n", read("src/a.txt"));
			git.checkout().setName(c1.getName()).call();
			assertEquals(
					"[.gitattributes, mode:100644, content:*.txt filter=lfs][src/a.tmp, mode:100644, content:foo][src/a.txt, mode:100644, content:version https://git-lfs.github.com/spec/v1\noid sha256:b5bb9d8014a0f9b1d61e21e796d78dccdf1352f23cd32812f4850b878ae4944c\nsize 4\n]",
					indexState(CONTENT));
			// due to lfs clean filter but dummy smudge filter we expect strange
			// content. The smudge filter converts from real content to pointer
			// file content (starting with "version ") but the smudge filter
			// replaces 'o' by 'e' which results in a text starting with
			// "versien "
			assertEquals(
					"versien https://git-lfs.github.cem/spec/v1\neid sha256:b5bb9d8014a0f9b1d61e21e796d78dccdf1352f23cd32812f4850b878ae4944c\nsize 4\n",
					read("src/a.txt"));
		}
	}

	@Test
	public void testAttributesWithTreeWalkFilter()
			throws IOException, GitAPIException {
		writeTrashFile(".gitattributes", "*.txt filter=lfs");
		writeTrashFile("src/a.tmp", "foo");
		writeTrashFile("src/a.txt", "foo\n");
		File script = writeTempFile("sed s/o/e/g");

		try (Git git = new Git(db)) {
			StoredConfig config = git.getRepository().getConfig();
			config.setString("filter", "lfs", "clean",
					"sh " + slashify(script.getPath()));
			config.save();

			git.add().addFilepattern(".gitattributes").call();
			git.commit().setMessage("attr").call();
			git.add().addFilepattern("src/a.txt").addFilepattern("src/a.tmp")
					.addFilepattern(".gitattributes").call();
			git.commit().setMessage("c1").call();
			assertTrue(git.status().call().isClean());
		}
	}

	@Test
	public void testAttributesConflictingMatch() throws Exception {
		writeTrashFile(".gitattributes", "foo/** crlf=input\n*.jar binary");
		writeTrashFile("foo/bar.jar", "\r\n");
		// We end up with attributes [binary -diff -merge -text crlf=input].
		// crlf should have no effect when -text is present.
		try (Git git = new Git(db)) {
			git.add().addFilepattern(".").call();
			assertEquals(
					"[.gitattributes, mode:100644, content:foo/** crlf=input\n*.jar binary]"
							+ "[foo/bar.jar, mode:100644, content:\r\n]",
					indexState(CONTENT));
		}
	}

	@Test
	public void testCleanFilterEnvironment()
			throws IOException, GitAPIException {
		writeTrashFile(".gitattributes", "*.txt filter=tstFilter");
		writeTrashFile("src/a.txt", "foo");
		File script = writeTempFile("echo $GIT_DIR; echo 1 >xyz");

		try (Git git = new Git(db)) {
			StoredConfig config = git.getRepository().getConfig();
			config.setString("filter", "tstFilter", "clean",
					"sh " + slashify(script.getPath()));
			config.save();
			git.add().addFilepattern("src/a.txt").call();

			String gitDir = db.getDirectory().getAbsolutePath();
			assertEquals("[src/a.txt, mode:100644, content:" + gitDir
					+ "\n]", indexState(CONTENT));
			assertTrue(new File(db.getWorkTree(), "xyz").exists());
		}
	}

	@Test
	public void testMultipleCleanFilter() throws IOException, GitAPIException {
		writeTrashFile(".gitattributes",
				"*.txt filter=tstFilter\n*.tmp filter=tstFilter2");
		// Caution: we need a trailing '\n' since sed on mac always appends
		// linefeeds if missing
		writeTrashFile("src/a.tmp", "foo\n");
		writeTrashFile("src/a.txt", "foo\n");
		File script = writeTempFile("sed s/o/e/g");
		File script2 = writeTempFile("sed s/f/x/g");

		try (Git git = new Git(db)) {
			StoredConfig config = git.getRepository().getConfig();
			config.setString("filter", "tstFilter", "clean",
					"sh " + slashify(script.getPath()));
			config.setString("filter", "tstFilter2", "clean",
					"sh " + slashify(script2.getPath()));
			config.save();

			git.add().addFilepattern("src/a.txt").addFilepattern("src/a.tmp")
					.call();

			assertEquals(
					"[src/a.tmp, mode:100644, content:xoo\n][src/a.txt, mode:100644, content:fee\n]",
					indexState(CONTENT));

			// TODO: multiple clean filters for one file???
		}
	}

	/**
	 * The path of an added file name contains ';' and afterwards malicious
	 * commands. Make sure when calling filter commands to properly escape the
	 * filenames
	 *
	 * @throws IOException
	 * @throws GitAPIException
	 */
	@Test
	public void testCommandInjection() throws IOException, GitAPIException {
		// Caution: we need a trailing '\n' since sed on mac always appends
		// linefeeds if missing
		writeTrashFile("; echo virus", "foo\n");
		File script = writeTempFile("sed s/o/e/g");

		try (Git git = new Git(db)) {
			StoredConfig config = git.getRepository().getConfig();
			config.setString("filter", "tstFilter", "clean",
					"sh " + slashify(script.getPath()) + " %f");
			writeTrashFile(".gitattributes", "* filter=tstFilter");

			git.add().addFilepattern("; echo virus").call();
			// Without proper escaping the content would be "feovirus". The sed
			// command and the "echo virus" would contribute to the content
			assertEquals("[; echo virus, mode:100644, content:fee\n]",
					indexState(CONTENT));
		}
	}

	@Test
	public void testBadCleanFilter() throws IOException, GitAPIException {
		writeTrashFile("a.txt", "foo");
		File script = writeTempFile("sedfoo s/o/e/g");

		try (Git git = new Git(db)) {
			StoredConfig config = git.getRepository().getConfig();
			config.setString("filter", "tstFilter", "clean",
					"sh " + script.getPath());
			config.save();
			writeTrashFile(".gitattributes", "*.txt filter=tstFilter");

			try {
				git.add().addFilepattern("a.txt").call();
				fail("Didn't received the expected exception");
			} catch (FilterFailedException e) {
				assertEquals(127, e.getReturnCode());
			}
		}
	}

	@Test
	public void testBadCleanFilter2() throws IOException, GitAPIException {
		writeTrashFile("a.txt", "foo");
		File script = writeTempFile("sed s/o/e/g");

		try (Git git = new Git(db)) {
			StoredConfig config = git.getRepository().getConfig();
			config.setString("filter", "tstFilter", "clean",
					"shfoo " + script.getPath());
			config.save();
			writeTrashFile(".gitattributes", "*.txt filter=tstFilter");

			try {
				git.add().addFilepattern("a.txt").call();
				fail("Didn't received the expected exception");
			} catch (FilterFailedException e) {
				assertEquals(127, e.getReturnCode());
			}
		}
	}

	@Test
	public void testCleanFilterReturning12() throws IOException,
			GitAPIException {
		writeTrashFile("a.txt", "foo");
		File script = writeTempFile("exit 12");

		try (Git git = new Git(db)) {
			StoredConfig config = git.getRepository().getConfig();
			config.setString("filter", "tstFilter", "clean",
					"sh " + slashify(script.getPath()));
			config.save();
			writeTrashFile(".gitattributes", "*.txt filter=tstFilter");

			try {
				git.add().addFilepattern("a.txt").call();
				fail("Didn't received the expected exception");
			} catch (FilterFailedException e) {
				assertEquals(12, e.getReturnCode());
			}
		}
	}

	@Test
	public void testNotApplicableFilter() throws IOException, GitAPIException {
		writeTrashFile("a.txt", "foo");
		File script = writeTempFile("sed s/o/e/g");

		try (Git git = new Git(db)) {
			StoredConfig config = git.getRepository().getConfig();
			config.setString("filter", "tstFilter", "something",
					"sh " + script.getPath());
			config.save();
			writeTrashFile(".gitattributes", "*.txt filter=tstFilter");

			git.add().addFilepattern("a.txt").call();

			assertEquals("[a.txt, mode:100644, content:foo]",
					indexState(CONTENT));
		}
	}

	private File writeTempFile(String body) throws IOException {
		File f = File.createTempFile("AddCommandTest_", "");
		JGitTestUtil.write(f, body);
		return f;
	}

	@Test
	public void testAddExistingSingleSmallFileWithNewLine() throws IOException,
			GitAPIException {
		File file = new File(db.getWorkTree(), "a.txt");
		FileUtils.createNewFile(file);
		try (PrintWriter writer = new PrintWriter(file, UTF_8.name())) {
			writer.print("row1\r\nrow2");
		}

		try (Git git = new Git(db)) {
			db.getConfig().setString("core", null, "autocrlf", "false");
			git.add().addFilepattern("a.txt").call();
			assertEquals("[a.txt, mode:100644, content:row1\r\nrow2]",
					indexState(CONTENT));
			db.getConfig().setString("core", null, "autocrlf", "true");
			git.add().addFilepattern("a.txt").call();
			assertEquals("[a.txt, mode:100644, content:row1\r\nrow2]",
					indexState(CONTENT));
			db.getConfig().setString("core", null, "autocrlf", "input");
			git.add().addFilepattern("a.txt").call();
			assertEquals("[a.txt, mode:100644, content:row1\r\nrow2]",
					indexState(CONTENT));
		}
	}

	@Test
	public void testAddExistingSingleMediumSizeFileWithNewLine()
			throws IOException, GitAPIException {
		File file = new File(db.getWorkTree(), "a.txt");
		FileUtils.createNewFile(file);
		StringBuilder data = new StringBuilder();
		for (int i = 0; i < 1000; ++i) {
			data.append("row1\r\nrow2");
		}
		String crData = data.toString();
		try (PrintWriter writer = new PrintWriter(file, UTF_8.name())) {
			writer.print(crData);
		}
		try (Git git = new Git(db)) {
			db.getConfig().setString("core", null, "autocrlf", "false");
			git.add().addFilepattern("a.txt").call();
			assertEquals("[a.txt, mode:100644, content:" + crData + "]",
					indexState(CONTENT));
			db.getConfig().setString("core", null, "autocrlf", "true");
			git.add().addFilepattern("a.txt").call();
			assertEquals("[a.txt, mode:100644, content:" + crData + "]",
					indexState(CONTENT));
			db.getConfig().setString("core", null, "autocrlf", "input");
			git.add().addFilepattern("a.txt").call();
			assertEquals("[a.txt, mode:100644, content:" + crData + "]",
					indexState(CONTENT));
		}
	}

	@Test
	public void testAddExistingSingleBinaryFile() throws IOException,
			GitAPIException {
		File file = new File(db.getWorkTree(), "a.txt");
		FileUtils.createNewFile(file);
		try (PrintWriter writer = new PrintWriter(file, UTF_8.name())) {
			writer.print("row1\r\nrow2\u0000");
		}

		try (Git git = new Git(db)) {
			db.getConfig().setString("core", null, "autocrlf", "false");
			git.add().addFilepattern("a.txt").call();
			assertEquals("[a.txt, mode:100644, content:row1\r\nrow2\u0000]",
					indexState(CONTENT));
			db.getConfig().setString("core", null, "autocrlf", "true");
			git.add().addFilepattern("a.txt").call();
			assertEquals("[a.txt, mode:100644, content:row1\r\nrow2\u0000]",
					indexState(CONTENT));
			db.getConfig().setString("core", null, "autocrlf", "input");
			git.add().addFilepattern("a.txt").call();
			assertEquals("[a.txt, mode:100644, content:row1\r\nrow2\u0000]",
					indexState(CONTENT));
		}
	}

	@Test
	public void testAddExistingSingleFileInSubDir() throws IOException,
			GitAPIException {
		FileUtils.mkdir(new File(db.getWorkTree(), "sub"));
		File file = new File(db.getWorkTree(), "sub/a.txt");
		FileUtils.createNewFile(file);
		try (PrintWriter writer = new PrintWriter(file, UTF_8.name())) {
			writer.print("content");
		}

		try (Git git = new Git(db)) {
			git.add().addFilepattern("sub/a.txt").call();

			assertEquals(
					"[sub/a.txt, mode:100644, content:content]",
					indexState(CONTENT));
		}
	}

	@Test
	public void testAddExistingSingleFileTwice() throws IOException,
			GitAPIException {
		File file = new File(db.getWorkTree(), "a.txt");
		FileUtils.createNewFile(file);
		try (PrintWriter writer = new PrintWriter(file, UTF_8.name())) {
			writer.print("content");
		}

		try (Git git = new Git(db)) {
			DirCache dc = git.add().addFilepattern("a.txt").call();

			dc.getEntry(0).getObjectId();

			try (PrintWriter writer = new PrintWriter(file, UTF_8.name())) {
				writer.print("other content");
			}

			dc = git.add().addFilepattern("a.txt").call();

			assertEquals(
					"[a.txt, mode:100644, content:other content]",
					indexState(CONTENT));
		}
	}

	@Test
	public void testAddExistingSingleFileTwiceWithCommit() throws Exception {
		File file = new File(db.getWorkTree(), "a.txt");
		FileUtils.createNewFile(file);
		try (PrintWriter writer = new PrintWriter(file, UTF_8.name())) {
			writer.print("content");
		}

		try (Git git = new Git(db)) {
			DirCache dc = git.add().addFilepattern("a.txt").call();

			dc.getEntry(0).getObjectId();

			git.commit().setMessage("commit a.txt").call();

			try (PrintWriter writer = new PrintWriter(file, UTF_8.name())) {
				writer.print("other content");
			}

			dc = git.add().addFilepattern("a.txt").call();

			assertEquals(
					"[a.txt, mode:100644, content:other content]",
					indexState(CONTENT));
		}
	}

	@Test
	public void testAddRemovedFile() throws Exception {
		File file = new File(db.getWorkTree(), "a.txt");
		FileUtils.createNewFile(file);
		try (PrintWriter writer = new PrintWriter(file, UTF_8.name())) {
			writer.print("content");
		}

		try (Git git = new Git(db)) {
			DirCache dc = git.add().addFilepattern("a.txt").call();

			dc.getEntry(0).getObjectId();
			FileUtils.delete(file);

			// is supposed to do nothing
			dc = git.add().addFilepattern("a.txt").call();

			assertEquals(
					"[a.txt, mode:100644, content:content]",
					indexState(CONTENT));
		}
	}

	@Test
	public void testAddRemovedCommittedFile() throws Exception {
		File file = new File(db.getWorkTree(), "a.txt");
		FileUtils.createNewFile(file);
		try (PrintWriter writer = new PrintWriter(file, UTF_8.name())) {
			writer.print("content");
		}

		try (Git git = new Git(db)) {
			DirCache dc = git.add().addFilepattern("a.txt").call();

			git.commit().setMessage("commit a.txt").call();

			dc.getEntry(0).getObjectId();
			FileUtils.delete(file);

			// is supposed to do nothing
			dc = git.add().addFilepattern("a.txt").call();

			assertEquals(
					"[a.txt, mode:100644, content:content]",
					indexState(CONTENT));
		}
	}

	@Test
	public void testAddWithConflicts() throws Exception {
		// prepare conflict

		File file = new File(db.getWorkTree(), "a.txt");
		FileUtils.createNewFile(file);
		try (PrintWriter writer = new PrintWriter(file, UTF_8.name())) {
			writer.print("content");
		}

		File file2 = new File(db.getWorkTree(), "b.txt");
		FileUtils.createNewFile(file2);
		try (PrintWriter writer = new PrintWriter(file2, UTF_8.name())) {
			writer.print("content b");
		}

		DirCache dc = db.lockDirCache();
		try (ObjectInserter newObjectInserter = db.newObjectInserter()) {
			DirCacheBuilder builder = dc.builder();

			addEntryToBuilder("b.txt", file2, newObjectInserter, builder, 0);
			addEntryToBuilder("a.txt", file, newObjectInserter, builder, 1);

			try (PrintWriter writer = new PrintWriter(file, UTF_8.name())) {
				writer.print("other content");
			}
			addEntryToBuilder("a.txt", file, newObjectInserter, builder, 3);

			try (PrintWriter writer = new PrintWriter(file, UTF_8.name())) {
				writer.print("our content");
			}
			addEntryToBuilder("a.txt", file, newObjectInserter, builder, 2)
					.getObjectId();

			builder.commit();
		}
		assertEquals(
				"[a.txt, mode:100644, stage:1, content:content]" +
				"[a.txt, mode:100644, stage:2, content:our content]" +
				"[a.txt, mode:100644, stage:3, content:other content]" +
				"[b.txt, mode:100644, content:content b]",
				indexState(CONTENT));

		// now the test begins

		try (Git git = new Git(db)) {
			dc = git.add().addFilepattern("a.txt").call();

			assertEquals(
					"[a.txt, mode:100644, content:our content]" +
					"[b.txt, mode:100644, content:content b]",
					indexState(CONTENT));
		}
	}

	@Test
	public void testAddTwoFiles() throws Exception  {
		File file = new File(db.getWorkTree(), "a.txt");
		FileUtils.createNewFile(file);
		try (PrintWriter writer = new PrintWriter(file, UTF_8.name())) {
			writer.print("content");
		}

		File file2 = new File(db.getWorkTree(), "b.txt");
		FileUtils.createNewFile(file2);
		try (PrintWriter writer = new PrintWriter(file2, UTF_8.name())) {
			writer.print("content b");
		}

		try (Git git = new Git(db)) {
			git.add().addFilepattern("a.txt").addFilepattern("b.txt").call();
			assertEquals(
					"[a.txt, mode:100644, content:content]" +
					"[b.txt, mode:100644, content:content b]",
					indexState(CONTENT));
		}
	}

	@Test
	public void testAddFolder() throws Exception  {
		FileUtils.mkdir(new File(db.getWorkTree(), "sub"));
		File file = new File(db.getWorkTree(), "sub/a.txt");
		FileUtils.createNewFile(file);
		try (PrintWriter writer = new PrintWriter(file, UTF_8.name())) {
			writer.print("content");
		}

		File file2 = new File(db.getWorkTree(), "sub/b.txt");
		FileUtils.createNewFile(file2);
		try (PrintWriter writer = new PrintWriter(file2, UTF_8.name())) {
			writer.print("content b");
		}

		try (Git git = new Git(db)) {
			git.add().addFilepattern("sub").call();
			assertEquals(
					"[sub/a.txt, mode:100644, content:content]" +
					"[sub/b.txt, mode:100644, content:content b]",
					indexState(CONTENT));
		}
	}

	@Test
	public void testAddIgnoredFile() throws Exception  {
		FileUtils.mkdir(new File(db.getWorkTree(), "sub"));
		File file = new File(db.getWorkTree(), "sub/a.txt");
		FileUtils.createNewFile(file);
		try (PrintWriter writer = new PrintWriter(file, UTF_8.name())) {
			writer.print("content");
		}

		File ignoreFile = new File(db.getWorkTree(), ".gitignore");
		FileUtils.createNewFile(ignoreFile);
		try (PrintWriter writer = new PrintWriter(ignoreFile, UTF_8.name())) {
			writer.print("sub/b.txt");
		}

		File file2 = new File(db.getWorkTree(), "sub/b.txt");
		FileUtils.createNewFile(file2);
		try (PrintWriter writer = new PrintWriter(file2, UTF_8.name())) {
			writer.print("content b");
		}

		try (Git git = new Git(db)) {
			git.add().addFilepattern("sub").call();

			assertEquals(
					"[sub/a.txt, mode:100644, content:content]",
					indexState(CONTENT));
		}
	}

	@Test
	public void testAddWholeRepo() throws Exception {
		FileUtils.mkdir(new File(db.getWorkTree(), "sub"));
		File file = new File(db.getWorkTree(), "sub/a.txt");
		FileUtils.createNewFile(file);
		try (PrintWriter writer = new PrintWriter(file, UTF_8.name())) {
			writer.print("content");
		}

		File file2 = new File(db.getWorkTree(), "sub/b.txt");
		FileUtils.createNewFile(file2);
		try (PrintWriter writer = new PrintWriter(file2, UTF_8.name())) {
			writer.print("content b");
		}

		try (Git git = new Git(db)) {
			git.add().addFilepattern(".").call();
			assertEquals(
					"[sub/a.txt, mode:100644, content:content]" +
					"[sub/b.txt, mode:100644, content:content b]",
					indexState(CONTENT));
		}
	}

	@Test
	public void testAddAllNoRenormalize() throws Exception {
		final int nOfFiles = 1000;
		final int filesPerDir = nOfFiles / 10;
		final int fileSizeInBytes = 10_000;
		assertTrue(nOfFiles > 0);
		assertTrue(filesPerDir > 0);
		File dir = null;
		File lastFile = null;
		for (int i = 0; i < nOfFiles; i++) {
			if (i % filesPerDir == 0) {
				dir = new File(db.getWorkTree(), "dir" + (i / filesPerDir));
				FileUtils.mkdir(dir);
			}
			lastFile = new File(dir, "file" + i);
			try (OutputStream out = new BufferedOutputStream(
					new FileOutputStream(lastFile))) {
				for (int b = 0; b < fileSizeInBytes; b++) {
					out.write('a' + (b % 26));
					if (((b + 1) % 70) == 0) {
						out.write('\n');
					}
				}
			}
		}
		// Help null pointer analysis.
		assert lastFile != null;
		// Wait a bit. If entries are "racily clean", we'll recompute
		// hashes from the disk files, and then the second add is also slow.
		// We want to test the normal case.
		fsTick(lastFile);
		try (Git git = new Git(db)) {
			long start = System.nanoTime();
			git.add().addFilepattern(".").call();
			long initialElapsed = System.nanoTime() - start;
			assertEquals("Unexpected number on index entries", nOfFiles,
					db.readDirCache().getEntryCount());
			start = System.nanoTime();
			git.add().addFilepattern(".").setRenormalize(false).call();
			long secondElapsed = System.nanoTime() - start;
			assertEquals("Unexpected number on index entries", nOfFiles,
					db.readDirCache().getEntryCount());
			// Fail the test if the second add all was not significantly faster.
			// A factor of 4 is rather generous. The speed-up depends on the
			// file system and OS caching and is hard to predict.
			assertTrue(
					"Second add all was too slow; initial took "
							+ TimeUnit.NANOSECONDS.toMillis(initialElapsed)
							+ ", second took "
							+ TimeUnit.NANOSECONDS.toMillis(secondElapsed),
					secondElapsed * 4 <= initialElapsed);
			// Change one file. The index should be updated even if
			// renormalize==false. It doesn't matter what kind of change we do.
			final String newData = "Hello";
			Files.writeString(lastFile.toPath(), newData);
			git.add().addFilepattern(".").setRenormalize(false).call();
			DirCache dc = db.readDirCache();
			DirCacheEntry e = dc.getEntry(lastFile.getParentFile().getName()
					+ '/' + lastFile.getName());
			String blob = new String(db
					.open(e.getObjectId(), Constants.OBJ_BLOB).getCachedBytes(),
					UTF_8);
			assertEquals("Unexpected index content", newData, blob);
		}
	}

	// the same three cases as in testAddWithParameterUpdate
	// file a exists in workdir and in index -> added
	// file b exists not in workdir but in index -> unchanged
	// file c exists in workdir but not in index -> added
	@Test
	public void testAddWithoutParameterUpdate() throws Exception {
		FileUtils.mkdir(new File(db.getWorkTree(), "sub"));
		File file = new File(db.getWorkTree(), "sub/a.txt");
		FileUtils.createNewFile(file);
		try (PrintWriter writer = new PrintWriter(file, UTF_8.name())) {
			writer.print("content");
		}

		File file2 = new File(db.getWorkTree(), "sub/b.txt");
		FileUtils.createNewFile(file2);
		try (PrintWriter writer = new PrintWriter(file2, UTF_8.name())) {
			writer.print("content b");
		}

		try (Git git = new Git(db)) {
			git.add().addFilepattern("sub").call();

			assertEquals(
					"[sub/a.txt, mode:100644, content:content]" +
					"[sub/b.txt, mode:100644, content:content b]",
					indexState(CONTENT));

			git.commit().setMessage("commit").call();

			// new unstaged file sub/c.txt
			File file3 = new File(db.getWorkTree(), "sub/c.txt");
			FileUtils.createNewFile(file3);
			try (PrintWriter writer = new PrintWriter(file3, UTF_8.name())) {
				writer.print("content c");
			}

			// file sub/a.txt is modified
			try (PrintWriter writer = new PrintWriter(file, UTF_8.name())) {
				writer.print("modified content");
			}

			// file sub/b.txt is deleted
			FileUtils.delete(file2);

			git.add().addFilepattern("sub").call();
			// change in sub/a.txt is staged
			// deletion of sub/b.txt is not staged
			// sub/c.txt is staged
			assertEquals(
					"[sub/a.txt, mode:100644, content:modified content]" +
					"[sub/b.txt, mode:100644, content:content b]" +
					"[sub/c.txt, mode:100644, content:content c]",
					indexState(CONTENT));
		}
	}

	// file a exists in workdir and in index -> added
	// file b exists not in workdir but in index -> deleted
	// file c exists in workdir but not in index -> unchanged
	@Test
	public void testAddWithParameterUpdate() throws Exception {
		FileUtils.mkdir(new File(db.getWorkTree(), "sub"));
		File file = new File(db.getWorkTree(), "sub/a.txt");
		FileUtils.createNewFile(file);
		try (PrintWriter writer = new PrintWriter(file, UTF_8.name())) {
			writer.print("content");
		}

		File file2 = new File(db.getWorkTree(), "sub/b.txt");
		FileUtils.createNewFile(file2);
		try (PrintWriter writer = new PrintWriter(file2, UTF_8.name())) {
			writer.print("content b");
		}

		try (Git git = new Git(db)) {
			git.add().addFilepattern("sub").call();

			assertEquals(
					"[sub/a.txt, mode:100644, content:content]" +
					"[sub/b.txt, mode:100644, content:content b]",
					indexState(CONTENT));

			git.commit().setMessage("commit").call();

			// new unstaged file sub/c.txt
			File file3 = new File(db.getWorkTree(), "sub/c.txt");
			FileUtils.createNewFile(file3);
			try (PrintWriter writer = new PrintWriter(file3, UTF_8.name())) {
				writer.print("content c");
			}

			// file sub/a.txt is modified
			try (PrintWriter writer = new PrintWriter(file, UTF_8.name())) {
				writer.print("modified content");
			}

			FileUtils.delete(file2);

			// change in sub/a.txt is staged
			// deletion of sub/b.txt is staged
			// sub/c.txt is not staged
			git.add().addFilepattern("sub").setUpdate(true).call();
			// change in sub/a.txt is staged
			assertEquals(
					"[sub/a.txt, mode:100644, content:modified content]",
					indexState(CONTENT));
		}
	}

	@Test
	public void testAssumeUnchanged() throws Exception {
		try (Git git = new Git(db)) {
			String path = "a.txt";
			writeTrashFile(path, "content");
			git.add().addFilepattern(path).call();
			String path2 = "b.txt";
			writeTrashFile(path2, "content");
			git.add().addFilepattern(path2).call();
			git.commit().setMessage("commit").call();
			assertEquals("[a.txt, mode:100644, content:"
					+ "content, assume-unchanged:false]"
					+ "[b.txt, mode:100644, content:content, "
					+ "assume-unchanged:false]", indexState(CONTENT
					| ASSUME_UNCHANGED));
			assumeUnchanged(path2);
			assertEquals("[a.txt, mode:100644, content:content, "
					+ "assume-unchanged:false][b.txt, mode:100644, "
					+ "content:content, assume-unchanged:true]", indexState(CONTENT
					| ASSUME_UNCHANGED));
			writeTrashFile(path, "more content");
			writeTrashFile(path2, "more content");

			git.add().addFilepattern(".").call();

			assertEquals("[a.txt, mode:100644, content:more content,"
					+ " assume-unchanged:false][b.txt, mode:100644,"
					+ " content:content, assume-unchanged:true]",
					indexState(CONTENT
					| ASSUME_UNCHANGED));
		}
	}

	@Test
	public void testReplaceFileWithDirectory()
			throws IOException, NoFilepatternException, GitAPIException {
		try (Git git = new Git(db)) {
			writeTrashFile("df", "before replacement");
			git.add().addFilepattern("df").call();
			assertEquals("[df, mode:100644, content:before replacement]",
					indexState(CONTENT));
			FileUtils.delete(new File(db.getWorkTree(), "df"));
			writeTrashFile("df/f", "after replacement");
			git.add().addFilepattern("df").call();
			assertEquals("[df/f, mode:100644, content:after replacement]",
					indexState(CONTENT));
		}
	}

	@Test
	public void testReplaceDirectoryWithFile()
			throws IOException, NoFilepatternException, GitAPIException {
		try (Git git = new Git(db)) {
			writeTrashFile("df/f", "before replacement");
			git.add().addFilepattern("df").call();
			assertEquals("[df/f, mode:100644, content:before replacement]",
					indexState(CONTENT));
			FileUtils.delete(new File(db.getWorkTree(), "df"), RECURSIVE);
			writeTrashFile("df", "after replacement");
			git.add().addFilepattern("df").call();
			assertEquals("[df, mode:100644, content:after replacement]",
					indexState(CONTENT));
		}
	}

	@Test
	public void testReplaceFileByPartOfDirectory()
			throws IOException, NoFilepatternException, GitAPIException {
		try (Git git = new Git(db)) {
			writeTrashFile("src/main", "df", "before replacement");
			writeTrashFile("src/main", "z", "z");
			writeTrashFile("z", "z2");
			git.add().addFilepattern("src/main/df")
				.addFilepattern("src/main/z")
				.addFilepattern("z")
				.call();
			assertEquals(
					"[src/main/df, mode:100644, content:before replacement]" +
					"[src/main/z, mode:100644, content:z]" +
					"[z, mode:100644, content:z2]",
					indexState(CONTENT));
			FileUtils.delete(new File(db.getWorkTree(), "src/main/df"));
			writeTrashFile("src/main/df", "a", "after replacement");
			writeTrashFile("src/main/df", "b", "unrelated file");
			git.add().addFilepattern("src/main/df/a").call();
			assertEquals(
					"[src/main/df/a, mode:100644, content:after replacement]" +
					"[src/main/z, mode:100644, content:z]" +
					"[z, mode:100644, content:z2]",
					indexState(CONTENT));
		}
	}

	@Test
	public void testReplaceDirectoryConflictsWithFile()
			throws IOException, NoFilepatternException, GitAPIException {
		DirCache dc = db.lockDirCache();
		try (ObjectInserter oi = db.newObjectInserter()) {
			DirCacheBuilder builder = dc.builder();
			File f = writeTrashFile("a", "df", "content");
			addEntryToBuilder("a", f, oi, builder, 1);

			f = writeTrashFile("a", "df", "other content");
			addEntryToBuilder("a/df", f, oi, builder, 3);

			f = writeTrashFile("a", "df", "our content");
			addEntryToBuilder("a/df", f, oi, builder, 2);

			f = writeTrashFile("z", "z");
			addEntryToBuilder("z", f, oi, builder, 0);
			builder.commit();
		}
		assertEquals(
				"[a, mode:100644, stage:1, content:content]" +
				"[a/df, mode:100644, stage:2, content:our content]" +
				"[a/df, mode:100644, stage:3, content:other content]" +
				"[z, mode:100644, content:z]",
				indexState(CONTENT));

		try (Git git = new Git(db)) {
			FileUtils.delete(new File(db.getWorkTree(), "a"), RECURSIVE);
			writeTrashFile("a", "merged");
			git.add().addFilepattern("a").call();
			assertEquals("[a, mode:100644, content:merged]" +
					"[z, mode:100644, content:z]",
					indexState(CONTENT));
		}
	}

	@Test
	public void testExecutableRetention() throws Exception {
		StoredConfig config = db.getConfig();
		config.setBoolean(ConfigConstants.CONFIG_CORE_SECTION, null,
				ConfigConstants.CONFIG_KEY_FILEMODE, true);
		config.save();

		FS executableFs = new FS() {

			@Override
			public boolean supportsExecute() {
				return true;
			}

			@Override
			public boolean setExecute(File f, boolean canExec) {
				return true;
			}

			@Override
			public ProcessBuilder runInShell(String cmd, String[] args) {
				return null;
			}

			@Override
			public boolean retryFailedLockFileCommit() {
				return false;
			}

			@Override
			public FS newInstance() {
				return this;
			}

			@Override
			protected File discoverGitExe() {
				return null;
			}

			@Override
			public boolean canExecute(File f) {
				try {
					return read(f).startsWith("binary:");
				} catch (IOException e) {
					return false;
				}
			}

			@Override
			public boolean isCaseSensitive() {
				return false;
			}
		};

		String path = "a.txt";
		String path2 = "a.sh";
		writeTrashFile(path, "content");
		writeTrashFile(path2, "binary: content");
		try (Git git = Git.open(db.getDirectory(), executableFs)) {
			git.add().addFilepattern(path).addFilepattern(path2).call();
			RevCommit commit1 = git.commit().setMessage("commit").call();
			try (TreeWalk walk = new TreeWalk(db)) {
				walk.addTree(commit1.getTree());
				walk.next();
				assertEquals(path2, walk.getPathString());
				assertEquals(FileMode.EXECUTABLE_FILE, walk.getFileMode(0));
				walk.next();
				assertEquals(path, walk.getPathString());
				assertEquals(FileMode.REGULAR_FILE, walk.getFileMode(0));
			}
		}
		config = db.getConfig();
		config.setBoolean(ConfigConstants.CONFIG_CORE_SECTION, null,
				ConfigConstants.CONFIG_KEY_FILEMODE, false);
		config.save();

		writeTrashFile(path2, "content2");
		writeTrashFile(path, "binary: content2");
		try (Git git2 = Git.open(db.getDirectory(), executableFs)) {
			git2.add().addFilepattern(path).addFilepattern(path2).call();
			RevCommit commit2 = git2.commit().setMessage("commit2").call();
			try (TreeWalk walk = new TreeWalk(db)) {
				walk.addTree(commit2.getTree());
				walk.next();
				assertEquals(path2, walk.getPathString());
				assertEquals(FileMode.EXECUTABLE_FILE, walk.getFileMode(0));
				walk.next();
				assertEquals(path, walk.getPathString());
				assertEquals(FileMode.REGULAR_FILE, walk.getFileMode(0));
			}
		}
	}

	@Test
	public void testAddGitlink() throws Exception {
		createNestedRepo("git-link-dir");
		try (Git git = new Git(db)) {
			git.add().addFilepattern("git-link-dir").call();

			assertEquals(
					"[git-link-dir, mode:160000]",
					indexState(0));
			Set<String> untrackedFiles = git.status().call().getUntracked();
			assert (untrackedFiles.isEmpty());
		}

	}

	@Test
	public void testAddSubrepoWithDirNoGitlinks() throws Exception {
		createNestedRepo("nested-repo");

		// Set DIR_NO_GITLINKS
		StoredConfig config = db.getConfig();
		config.setBoolean(ConfigConstants.CONFIG_CORE_SECTION, null,
				ConfigConstants.CONFIG_KEY_DIRNOGITLINKS, true);
		config.save();

		assert (db.getConfig().get(WorkingTreeOptions.KEY).isDirNoGitLinks());

		try (Git git = new Git(db)) {
			git.add().addFilepattern("nested-repo").call();

			assertEquals(
					"[nested-repo/README1.md, mode:100644]" +
							"[nested-repo/README2.md, mode:100644]",
					indexState(0));
		}

		// Turn off DIR_NO_GITLINKS, ensure nested-repo is still treated as
		// a normal directory
		// Set DIR_NO_GITLINKS
		config.setBoolean(ConfigConstants.CONFIG_CORE_SECTION, null,
				ConfigConstants.CONFIG_KEY_DIRNOGITLINKS, false);
		config.save();

		writeTrashFile("nested-repo", "README3.md", "content");

		try (Git git = new Git(db)) {
			git.add().addFilepattern("nested-repo").call();

			assertEquals(
					"[nested-repo/README1.md, mode:100644]" +
							"[nested-repo/README2.md, mode:100644]" +
							"[nested-repo/README3.md, mode:100644]",
					indexState(0));
		}
	}

	@Test
	public void testAddGitlinkDoesNotChange() throws Exception {
		createNestedRepo("nested-repo");

		try (Git git = new Git(db)) {
			git.add().addFilepattern("nested-repo").call();

			assertEquals(
					"[nested-repo, mode:160000]",
					indexState(0));
		}

		// Set DIR_NO_GITLINKS
		StoredConfig config = db.getConfig();
		config.setBoolean(ConfigConstants.CONFIG_CORE_SECTION, null,
				ConfigConstants.CONFIG_KEY_DIRNOGITLINKS, true);
		config.save();

		assertTrue(
				db.getConfig().get(WorkingTreeOptions.KEY).isDirNoGitLinks());

		try (Git git = new Git(db)) {
			git.add().addFilepattern("nested-repo").call();
			// with gitlinks ignored, we treat this as a normal directory
			assertEquals(
					"[nested-repo/README1.md, mode:100644][nested-repo/README2.md, mode:100644]",
					indexState(0));
		}
	}

	private static DirCacheEntry addEntryToBuilder(String path, File file,
			ObjectInserter newObjectInserter, DirCacheBuilder builder, int stage)
			throws IOException {
		ObjectId id;
		try (FileInputStream inputStream = new FileInputStream(file)) {
			id = newObjectInserter.insert(
				Constants.OBJ_BLOB, file.length(), inputStream);
		}
		DirCacheEntry entry = new DirCacheEntry(path, stage);
		entry.setObjectId(id);
		entry.setFileMode(FileMode.REGULAR_FILE);
		entry.setLastModified(FS.DETECTED.lastModifiedInstant(file));
		entry.setLength((int) file.length());

		builder.add(entry);
		return entry;
	}

	private void assumeUnchanged(String path) throws IOException {
		final DirCache dirc = db.lockDirCache();
		final DirCacheEntry ent = dirc.getEntry(path);
		if (ent != null)
			ent.setAssumeValid(true);
		dirc.write();
		if (!dirc.commit())
			throw new IOException("could not commit");
	}

	private void createNestedRepo(String path) throws IOException {
		File gitLinkDir = new File(db.getWorkTree(), path);
		FileUtils.mkdir(gitLinkDir);

		FileRepositoryBuilder nestedBuilder = new FileRepositoryBuilder();
		nestedBuilder.setWorkTree(gitLinkDir);

		try (Repository nestedRepo = nestedBuilder.build()) {
			nestedRepo.create();

			writeTrashFile(path, "README1.md", "content");
			writeTrashFile(path, "README2.md", "content");

			// Commit these changes in the subrepo
			try (Git git = new Git(nestedRepo)) {
				git.add().addFilepattern(".").call();
				git.commit().setMessage("subrepo commit").call();
			} catch (GitAPIException e) {
				throw new RuntimeException(e);
			}
		}
	}
}
