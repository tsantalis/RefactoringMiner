/*
 * Copyright (C) 2010, Red Hat Inc. and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0 which is available at
 * https://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package org.eclipse.jgit.ignore;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.eclipse.jgit.junit.Assert.assertEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;

import org.eclipse.jgit.junit.RepositoryTestCase;
import org.eclipse.jgit.lib.FileMode;
import org.eclipse.jgit.treewalk.FileTreeIterator;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.WorkingTreeIterator;
import org.eclipse.jgit.util.FileUtils;
import org.eclipse.jgit.util.SystemReader;
import org.junit.After;
import org.junit.Test;

/**
 * Tests ignore node behavior on the local filesystem.
 */
public class IgnoreNodeTest extends RepositoryTestCase {
	private static final FileMode D = FileMode.TREE;

	private static final FileMode F = FileMode.REGULAR_FILE;

	private static final boolean ignored = true;

	private static final boolean tracked = false;

	private TreeWalk walk;

	@After
	public void closeWalk() {
		if (walk != null) {
			walk.close();
		}
	}

	@Test
	public void testSimpleRootGitIgnoreGlobalIgnore() throws IOException {
		writeIgnoreFile(".gitignore", "x");

		writeTrashFile("a/x/file", "");
		writeTrashFile("b/x", "");
		writeTrashFile("x/file", "");

		beginWalk();
		assertEntry(F, tracked, ".gitignore");
		assertEntry(D, tracked, "a");
		assertEntry(D, ignored, "a/x");
		assertEntry(F, ignored, "a/x/file");
		assertEntry(D, tracked, "b");
		assertEntry(F, ignored, "b/x");
		assertEntry(D, ignored, "x");
		assertEntry(F, ignored, "x/file");
		endWalk();
	}

	@Test
	public void testSimpleRootGitIgnoreGlobalDirIgnore() throws IOException {
		writeIgnoreFile(".gitignore", "x/");

		writeTrashFile("a/x/file", "");
		writeTrashFile("x/file", "");

		beginWalk();
		assertEntry(F, tracked, ".gitignore");
		assertEntry(D, tracked, "a");
		assertEntry(D, ignored, "a/x");
		assertEntry(F, ignored, "a/x/file");
		assertEntry(D, ignored, "x");
		assertEntry(F, ignored, "x/file");
		endWalk();
	}

	@Test
	public void testSimpleRootGitIgnoreWildMatcher() throws IOException {
		writeIgnoreFile(".gitignore", "**");

		writeTrashFile("a/x", "");
		writeTrashFile("y", "");

		beginWalk();
		assertEntry(F, ignored, ".gitignore");
		assertEntry(D, ignored, "a");
		assertEntry(F, ignored, "a/x");
		assertEntry(F, ignored, "y");
		endWalk();
	}

	@Test
	public void testSimpleRootGitIgnoreWildMatcherDirOnly() throws IOException {
		writeIgnoreFile(".gitignore", "**/");

		writeTrashFile("a/x", "");
		writeTrashFile("y", "");

		beginWalk();
		assertEntry(F, tracked, ".gitignore");
		assertEntry(D, ignored, "a");
		assertEntry(F, ignored, "a/x");
		assertEntry(F, tracked, "y");
		endWalk();
	}

	@Test
	public void testSimpleRootGitIgnoreGlobalNegation1() throws IOException {
		writeIgnoreFile(".gitignore", "*", "!x*");
		writeTrashFile("x1", "");
		writeTrashFile("a/x2", "");
		writeTrashFile("x3/y", "");

		beginWalk();
		assertEntry(F, ignored, ".gitignore");
		assertEntry(D, ignored, "a");
		assertEntry(F, ignored, "a/x2");
		assertEntry(F, tracked, "x1");
		assertEntry(D, tracked, "x3");
		assertEntry(F, ignored, "x3/y");
		endWalk();
	}

	@Test
	public void testSimpleRootGitIgnoreGlobalNegation2() throws IOException {
		writeIgnoreFile(".gitignore", "*", "!x*", "!/a");
		writeTrashFile("x1", "");
		writeTrashFile("a/x2", "");
		writeTrashFile("x3/y", "");

		beginWalk();
		assertEntry(F, ignored, ".gitignore");
		assertEntry(D, tracked, "a");
		assertEntry(F, tracked, "a/x2");
		assertEntry(F, tracked, "x1");
		assertEntry(D, tracked, "x3");
		assertEntry(F, ignored, "x3/y");
		endWalk();
	}

	@Test
	public void testSimpleRootGitIgnoreGlobalNegation3() throws IOException {
		writeIgnoreFile(".gitignore", "*", "!x*", "!x*/**");
		writeTrashFile("x1", "");
		writeTrashFile("a/x2", "");
		writeTrashFile("x3/y", "");

		beginWalk();
		assertEntry(F, ignored, ".gitignore");
		assertEntry(D, ignored, "a");
		assertEntry(F, ignored, "a/x2");
		assertEntry(F, tracked, "x1");
		assertEntry(D, tracked, "x3");
		assertEntry(F, tracked, "x3/y");
		endWalk();
	}

	@Test
	public void testSimpleRootGitIgnoreGlobalNegation4() throws IOException {
		writeIgnoreFile(".gitignore", "*", "!**/");
		writeTrashFile("x1", "");
		writeTrashFile("a/x2", "");
		writeTrashFile("x3/y", "");

		beginWalk();
		assertEntry(F, ignored, ".gitignore");
		assertEntry(D, tracked, "a");
		assertEntry(F, ignored, "a/x2");
		assertEntry(F, ignored, "x1");
		assertEntry(D, tracked, "x3");
		assertEntry(F, ignored, "x3/y");
		endWalk();
	}

	@Test
	public void testRules() throws IOException {
		writeIgnoreFile(".git/info/exclude", "*~", "/out");

		writeIgnoreFile(".gitignore", "*.o", "/config");
		writeTrashFile("config/secret", "");
		writeTrashFile("mylib.c", "");
		writeTrashFile("mylib.c~", "");
		writeTrashFile("mylib.o", "");

		writeTrashFile("out/object/foo.exe", "");
		writeIgnoreFile("src/config/.gitignore", "lex.out");
		writeTrashFile("src/config/lex.out", "");
		writeTrashFile("src/config/config.c", "");
		writeTrashFile("src/config/config.c~", "");
		writeTrashFile("src/config/old/lex.out", "");

		beginWalk();
		assertEntry(F, tracked, ".gitignore");
		assertEntry(D, ignored, "config");
		assertEntry(F, ignored, "config/secret");
		assertEntry(F, tracked, "mylib.c");
		assertEntry(F, ignored, "mylib.c~");
		assertEntry(F, ignored, "mylib.o");

		assertEntry(D, ignored, "out");
		assertEntry(D, ignored, "out/object");
		assertEntry(F, ignored, "out/object/foo.exe");

		assertEntry(D, tracked, "src");
		assertEntry(D, tracked, "src/config");
		assertEntry(F, tracked, "src/config/.gitignore");
		assertEntry(F, tracked, "src/config/config.c");
		assertEntry(F, ignored, "src/config/config.c~");
		assertEntry(F, ignored, "src/config/lex.out");
		assertEntry(D, tracked, "src/config/old");
		assertEntry(F, ignored, "src/config/old/lex.out");
		endWalk();
	}

	@Test
	public void testNegation() throws IOException {
		// ignore all *.o files and ignore all "d" directories
		writeIgnoreFile(".gitignore", "*.o", "d");

		// negate "ignore" for a/b/keep.o file only
		writeIgnoreFile("src/a/b/.gitignore", "!keep.o");
		writeTrashFile("src/a/b/keep.o", "");
		writeTrashFile("src/a/b/nothere.o", "");

		// negate "ignore" for "d"
		writeIgnoreFile("src/c/.gitignore", "!d");
		// negate "ignore" for c/d/keep.o file only
		writeIgnoreFile("src/c/d/.gitignore", "!keep.o");
		writeTrashFile("src/c/d/keep.o", "");
		writeTrashFile("src/c/d/nothere.o", "");

		beginWalk();
		assertEntry(F, tracked, ".gitignore");
		assertEntry(D, tracked, "src");
		assertEntry(D, tracked, "src/a");
		assertEntry(D, tracked, "src/a/b");
		assertEntry(F, tracked, "src/a/b/.gitignore");
		assertEntry(F, tracked, "src/a/b/keep.o");
		assertEntry(F, ignored, "src/a/b/nothere.o");

		assertEntry(D, tracked, "src/c");
		assertEntry(F, tracked, "src/c/.gitignore");
		assertEntry(D, tracked, "src/c/d");
		assertEntry(F, tracked, "src/c/d/.gitignore");
		assertEntry(F, tracked, "src/c/d/keep.o");
		// must be ignored: "!d" should not negate *both* "d" and *.o rules!
		assertEntry(F, ignored, "src/c/d/nothere.o");
		endWalk();
	}

	/*
	 * See https://bugs.eclipse.org/bugs/show_bug.cgi?id=407475
	 */
	@Test
	public void testNegateAllExceptJavaInSrc() throws IOException {
		// ignore all files except from src directory
		writeIgnoreFile(".gitignore", "/*", "!/src/");
		writeTrashFile("nothere.o", "");

		// ignore all files except java
		writeIgnoreFile("src/.gitignore", "*", "!*.java");

		writeTrashFile("src/keep.java", "");
		writeTrashFile("src/nothere.o", "");
		writeTrashFile("src/a/nothere.o", "");

		beginWalk();
		assertEntry(F, ignored, ".gitignore");
		assertEntry(F, ignored, "nothere.o");
		assertEntry(D, tracked, "src");
		assertEntry(F, ignored, "src/.gitignore");
		assertEntry(D, ignored, "src/a");
		assertEntry(F, ignored, "src/a/nothere.o");
		assertEntry(F, tracked, "src/keep.java");
		assertEntry(F, ignored, "src/nothere.o");
		endWalk();
	}

	/*
	 * See https://bugs.eclipse.org/bugs/show_bug.cgi?id=407475
	 */
	@Test
	public void testNegationAllExceptJavaInSrcAndExceptChildDirInSrc()
			throws IOException {
		// ignore all files except from src directory
		writeIgnoreFile(".gitignore", "/*", "!/src/");
		writeTrashFile("nothere.o", "");

		// ignore all files except java in src folder and all children folders.
		// Last ignore rule breaks old jgit via bug 407475
		writeIgnoreFile("src/.gitignore", "*", "!*.java", "!*/");

		writeTrashFile("src/keep.java", "");
		writeTrashFile("src/nothere.o", "");
		writeTrashFile("src/a/keep.java", "");
		writeTrashFile("src/a/keep.o", "");

		beginWalk();
		assertEntry(F, ignored, ".gitignore");
		assertEntry(F, ignored, "nothere.o");
		assertEntry(D, tracked, "src");
		assertEntry(F, ignored, "src/.gitignore");
		assertEntry(D, tracked, "src/a");
		assertEntry(F, tracked, "src/a/keep.java");
		assertEntry(F, ignored, "src/a/keep.o");
		assertEntry(F, tracked, "src/keep.java");
		assertEntry(F, ignored, "src/nothere.o");
		endWalk();
	}

	/*
	 * See https://bugs.eclipse.org/bugs/show_bug.cgi?id=448094
	 */
	@Test
	public void testRepeatedNegation() throws IOException {
		writeIgnoreFile(".gitignore", "e", "!e", "e", "!e", "e");

		writeTrashFile("e/nothere.o", "");

		beginWalk();
		assertEntry(F, tracked, ".gitignore");
		assertEntry(D, ignored, "e");
		assertEntry(F, ignored, "e/nothere.o");
		endWalk();
	}

	/*
	 * See https://bugs.eclipse.org/bugs/show_bug.cgi?id=448094
	 */
	@Test
	public void testRepeatedNegationInDifferentFiles1() throws IOException {
		writeIgnoreFile(".gitignore", "*.o", "e");

		writeIgnoreFile("e/.gitignore", "!e");
		writeTrashFile("e/nothere.o", "");

		beginWalk();
		assertEntry(F, tracked, ".gitignore");
		assertEntry(D, ignored, "e");
		assertEntry(F, ignored, "e/.gitignore");
		assertEntry(F, ignored, "e/nothere.o");
		endWalk();
	}

	/*
	 * See https://bugs.eclipse.org/bugs/show_bug.cgi?id=448094
	 */
	@Test
	public void testRepeatedNegationInDifferentFiles2() throws IOException {
		writeIgnoreFile(".gitignore", "*.o", "e");

		writeIgnoreFile("a/.gitignore", "!e");
		writeTrashFile("a/e/nothere.o", "");

		beginWalk();
		assertEntry(F, tracked, ".gitignore");
		assertEntry(D, tracked, "a");
		assertEntry(F, tracked, "a/.gitignore");
		assertEntry(D, tracked, "a/e");
		assertEntry(F, ignored, "a/e/nothere.o");
		endWalk();
	}

	/*
	 * See https://bugs.eclipse.org/bugs/show_bug.cgi?id=448094
	 */
	@Test
	public void testRepeatedNegationInDifferentFiles3() throws IOException {
		writeIgnoreFile(".gitignore", "*.o");

		writeIgnoreFile("a/.gitignore", "e");
		writeIgnoreFile("a/b/.gitignore", "!e");
		writeTrashFile("a/b/e/nothere.o", "");

		beginWalk();
		assertEntry(F, tracked, ".gitignore");
		assertEntry(D, tracked, "a");
		assertEntry(F, tracked, "a/.gitignore");
		assertEntry(D, tracked, "a/b");
		assertEntry(F, tracked, "a/b/.gitignore");
		assertEntry(D, tracked, "a/b/e");
		assertEntry(F, ignored, "a/b/e/nothere.o");
		endWalk();
	}

	@Test
	public void testRepeatedNegationInDifferentFiles4() throws IOException {
		writeIgnoreFile(".gitignore", "*.o");

		writeIgnoreFile("a/.gitignore", "e");
		// Rules are never empty: WorkingTreeIterator optimizes empty rules away
		// paranoia check in case this optimization will be removed
		writeIgnoreFile("a/b/.gitignore", "#");
		writeIgnoreFile("a/b/c/.gitignore", "!e");
		writeTrashFile("a/b/c/e/nothere.o", "");

		beginWalk();
		assertEntry(F, tracked, ".gitignore");
		assertEntry(D, tracked, "a");
		assertEntry(F, tracked, "a/.gitignore");
		assertEntry(D, tracked, "a/b");
		assertEntry(F, tracked, "a/b/.gitignore");
		assertEntry(D, tracked, "a/b/c");
		assertEntry(F, tracked, "a/b/c/.gitignore");
		assertEntry(D, tracked, "a/b/c/e");
		assertEntry(F, ignored, "a/b/c/e/nothere.o");
		endWalk();
	}

	@Test
	public void testRepeatedNegationInDifferentFiles5() throws IOException {
		writeIgnoreFile(".gitignore", "e");
		writeIgnoreFile("a/.gitignore", "e");
		writeIgnoreFile("a/b/.gitignore", "!e");
		writeTrashFile("a/b/e/nothere.o", "");

		beginWalk();
		assertEntry(F, tracked, ".gitignore");
		assertEntry(D, tracked, "a");
		assertEntry(F, tracked, "a/.gitignore");
		assertEntry(D, tracked, "a/b");
		assertEntry(F, tracked, "a/b/.gitignore");
		assertEntry(D, tracked, "a/b/e");
		assertEntry(F, tracked, "a/b/e/nothere.o");
		endWalk();
	}

	@Test
	public void testIneffectiveNegationDifferentLevels1() throws IOException {
		writeIgnoreFile(".gitignore", "a/b/e/", "!a/b/e/*");
		writeTrashFile("a/b/e/nothere.o", "");

		beginWalk();
		assertEntry(F, tracked, ".gitignore");
		assertEntry(D, tracked, "a");
		assertEntry(D, tracked, "a/b");
		assertEntry(D, ignored, "a/b/e");
		assertEntry(F, ignored, "a/b/e/nothere.o");
		endWalk();
	}

	@Test
	public void testIneffectiveNegationDifferentLevels2() throws IOException {
		writeIgnoreFile(".gitignore", "a/b/e/");
		writeIgnoreFile("a/.gitignore", "!b/e/*");
		writeTrashFile("a/b/e/nothere.o", "");

		beginWalk();
		assertEntry(F, tracked, ".gitignore");
		assertEntry(D, tracked, "a");
		assertEntry(F, tracked, "a/.gitignore");
		assertEntry(D, tracked, "a/b");
		assertEntry(D, ignored, "a/b/e");
		assertEntry(F, ignored, "a/b/e/nothere.o");
		endWalk();
	}

	@Test
	public void testIneffectiveNegationDifferentLevels3() throws IOException {
		writeIgnoreFile(".gitignore", "a/b/e/");
		writeIgnoreFile("a/b/.gitignore", "!e/*");
		writeTrashFile("a/b/e/nothere.o", "");

		beginWalk();
		assertEntry(F, tracked, ".gitignore");
		assertEntry(D, tracked, "a");
		assertEntry(D, tracked, "a/b");
		assertEntry(F, tracked, "a/b/.gitignore");
		assertEntry(D, ignored, "a/b/e");
		assertEntry(F, ignored, "a/b/e/nothere.o");
		endWalk();
	}

	@Test
	public void testIneffectiveNegationDifferentLevels4() throws IOException {
		writeIgnoreFile(".gitignore", "a/b/e/");
		writeIgnoreFile("a/b/e/.gitignore", "!*");
		writeTrashFile("a/b/e/nothere.o", "");

		beginWalk();
		assertEntry(F, tracked, ".gitignore");
		assertEntry(D, tracked, "a");
		assertEntry(D, tracked, "a/b");
		assertEntry(D, ignored, "a/b/e");
		assertEntry(F, ignored, "a/b/e/.gitignore");
		assertEntry(F, ignored, "a/b/e/nothere.o");
		endWalk();
	}

	@Test
	public void testIneffectiveNegationDifferentLevels5() throws IOException {
		writeIgnoreFile("a/.gitignore", "b/e/");
		writeIgnoreFile("a/b/.gitignore", "!e/*");
		writeTrashFile("a/b/e/nothere.o", "");

		beginWalk();
		assertEntry(D, tracked, "a");
		assertEntry(F, tracked, "a/.gitignore");
		assertEntry(D, tracked, "a/b");
		assertEntry(F, tracked, "a/b/.gitignore");
		assertEntry(D, ignored, "a/b/e");
		assertEntry(F, ignored, "a/b/e/nothere.o");
		endWalk();
	}

	@Test
	public void testEmptyIgnoreRules() throws IOException {
		IgnoreNode node = new IgnoreNode();
		node.parse(writeToString("", "#", "!", "[[=a=]]"));
		assertEquals(new ArrayList<>(), node.getRules());
		node.parse(writeToString(" ", " / "));
		assertEquals(2, node.getRules().size());
	}

	@Test
	public void testSlashOnlyMatchesDirectory() throws IOException {
		writeIgnoreFile(".gitignore", "out/");
		writeTrashFile("out", "");

		beginWalk();
		assertEntry(F, tracked, ".gitignore");
		assertEntry(F, tracked, "out");

		FileUtils.delete(new File(trash, "out"));
		writeTrashFile("out/foo", "");

		beginWalk();
		assertEntry(F, tracked, ".gitignore");
		assertEntry(D, ignored, "out");
		assertEntry(F, ignored, "out/foo");
		endWalk();
	}

	@Test
	public void testSlashMatchesDirectory() throws IOException {
		writeIgnoreFile(".gitignore", "out2/");

		writeTrashFile("out1/out1", "");
		writeTrashFile("out1/out2", "");
		writeTrashFile("out2/out1", "");
		writeTrashFile("out2/out2", "");

		beginWalk();
		assertEntry(F, tracked, ".gitignore");
		assertEntry(D, tracked, "out1");
		assertEntry(F, tracked, "out1/out1");
		assertEntry(F, tracked, "out1/out2");
		assertEntry(D, ignored, "out2");
		assertEntry(F, ignored, "out2/out1");
		assertEntry(F, ignored, "out2/out2");
		endWalk();
	}

	@Test
	public void testWildcardWithSlashMatchesDirectory() throws IOException {
		writeIgnoreFile(".gitignore", "out2*/");

		writeTrashFile("out1/out1.txt", "");
		writeTrashFile("out1/out2", "");
		writeTrashFile("out1/out2.txt", "");
		writeTrashFile("out1/out2x/a", "");
		writeTrashFile("out2/out1.txt", "");
		writeTrashFile("out2/out2.txt", "");
		writeTrashFile("out2x/out1.txt", "");
		writeTrashFile("out2x/out2.txt", "");

		beginWalk();
		assertEntry(F, tracked, ".gitignore");
		assertEntry(D, tracked, "out1");
		assertEntry(F, tracked, "out1/out1.txt");
		assertEntry(F, tracked, "out1/out2");
		assertEntry(F, tracked, "out1/out2.txt");
		assertEntry(D, ignored, "out1/out2x");
		assertEntry(F, ignored, "out1/out2x/a");
		assertEntry(D, ignored, "out2");
		assertEntry(F, ignored, "out2/out1.txt");
		assertEntry(F, ignored, "out2/out2.txt");
		assertEntry(D, ignored, "out2x");
		assertEntry(F, ignored, "out2x/out1.txt");
		assertEntry(F, ignored, "out2x/out2.txt");
		endWalk();
	}

	@Test
	public void testWithSlashDoesNotMatchInSubDirectory() throws IOException {
		writeIgnoreFile(".gitignore", "a/b");
		writeTrashFile("a/a", "");
		writeTrashFile("a/b", "");
		writeTrashFile("src/a/a", "");
		writeTrashFile("src/a/b", "");

		beginWalk();
		assertEntry(F, tracked, ".gitignore");
		assertEntry(D, tracked, "a");
		assertEntry(F, tracked, "a/a");
		assertEntry(F, ignored, "a/b");
		assertEntry(D, tracked, "src");
		assertEntry(D, tracked, "src/a");
		assertEntry(F, tracked, "src/a/a");
		assertEntry(F, tracked, "src/a/b");
		endWalk();
	}

	@Test
	public void testNoPatterns() throws IOException {
		writeIgnoreFile(".gitignore", "", " ", "# comment", "/");
		writeTrashFile("a/a", "");

		beginWalk();
		assertEntry(F, tracked, ".gitignore");
		assertEntry(D, tracked, "a");
		assertEntry(F, tracked, "a/a");
		endWalk();
	}

	@Test
	public void testLeadingSpaces() throws IOException {
		writeTrashFile("  a/  a", "");
		writeTrashFile("  a/ a", "");
		writeTrashFile("  a/a", "");
		writeTrashFile(" a/  a", "");
		writeTrashFile(" a/ a", "");
		writeTrashFile(" a/a", "");
		writeIgnoreFile(".gitignore", " a", "  a");
		writeTrashFile("a/  a", "");
		writeTrashFile("a/ a", "");
		writeTrashFile("a/a", "");

		beginWalk();
		assertEntry(D, ignored, "  a");
		assertEntry(F, ignored, "  a/  a");
		assertEntry(F, ignored, "  a/ a");
		assertEntry(F, ignored, "  a/a");
		assertEntry(D, ignored, " a");
		assertEntry(F, ignored, " a/  a");
		assertEntry(F, ignored, " a/ a");
		assertEntry(F, ignored, " a/a");
		assertEntry(F, tracked, ".gitignore");
		assertEntry(D, tracked, "a");
		assertEntry(F, ignored, "a/  a");
		assertEntry(F, ignored, "a/ a");
		assertEntry(F, tracked, "a/a");
		endWalk();
	}

	@Test
	public void testTrailingSpaces() throws IOException {
		// Windows can't create files with trailing spaces
		// If this assumption fails the test is halted and ignored.
		org.junit.Assume.assumeFalse(SystemReader.getInstance().isWindows());
		writeTrashFile("a  /a", "");
		writeTrashFile("a  /a ", "");
		writeTrashFile("a  /a  ", "");
		writeTrashFile("a /a", "");
		writeTrashFile("a /a ", "");
		writeTrashFile("a /a  ", "");
		writeTrashFile("a/a", "");
		writeTrashFile("a/a ", "");
		writeTrashFile("a/a  ", "");
		writeTrashFile("b/c", "");

		writeIgnoreFile(".gitignore", "a\\ ", "a \\ ", "b/ ");

		beginWalk();
		assertEntry(F, tracked, ".gitignore");
		assertEntry(D, ignored, "a  ");
		assertEntry(F, ignored, "a  /a");
		assertEntry(F, ignored, "a  /a ");
		assertEntry(F, ignored, "a  /a  ");
		assertEntry(D, ignored, "a ");
		assertEntry(F, ignored, "a /a");
		assertEntry(F, ignored, "a /a ");
		assertEntry(F, ignored, "a /a  ");
		assertEntry(D, tracked, "a");
		assertEntry(F, tracked, "a/a");
		assertEntry(F, ignored, "a/a ");
		assertEntry(F, ignored, "a/a  ");
		assertEntry(D, ignored, "b");
		assertEntry(F, ignored, "b/c");
		endWalk();
	}

	@Test
	public void testToString() throws Exception {
		assertEquals(Arrays.asList("").toString(), new IgnoreNode().toString());
		assertEquals(Arrays.asList("hello").toString(),
				new IgnoreNode(Arrays.asList(new FastIgnoreRule("hello")))
						.toString());
	}

	private void beginWalk() {
		walk = new TreeWalk(db);
		FileTreeIterator iter = new FileTreeIterator(db);
		iter.setWalkIgnoredDirectories(true);
		walk.addTree(iter);
	}

	private void endWalk() throws IOException {
		assertFalse("Not all files tested", walk.next());
	}

	private void assertEntry(FileMode type, boolean entryIgnored,
			String pathName) throws IOException {
		assertTrue("walk has entry", walk.next());
		assertEquals(pathName, walk.getPathString());
		assertEquals(type, walk.getFileMode(0));

		WorkingTreeIterator itr = walk.getTree(0, WorkingTreeIterator.class);
		assertNotNull("has tree", itr);
		assertEquals("is ignored", entryIgnored, itr.isEntryIgnored());
		if (D.equals(type))
			walk.enterSubtree();
	}

	private void writeIgnoreFile(String name, String... rules)
			throws IOException {
		StringBuilder data = new StringBuilder();
		for (String line : rules)
			data.append(line + "\n");
		writeTrashFile(name, data.toString());
	}

	private InputStream writeToString(String... rules) {
		StringBuilder data = new StringBuilder();
		for (String line : rules) {
			data.append(line + "\n");
		}
		return new ByteArrayInputStream(data.toString().getBytes(UTF_8));
	}
}
