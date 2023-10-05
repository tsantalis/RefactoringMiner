/*
 * Copyright (C) 2014, Christian Halstrick <christian.halstrick@sap.com>
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

package org.eclipse.jgit.lib;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.Set;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.errors.NoWorkTreeException;
import org.eclipse.jgit.internal.storage.file.FileRepository;
import org.eclipse.jgit.junit.JGitTestUtil;
import org.eclipse.jgit.junit.RepositoryTestCase;
import org.eclipse.jgit.submodule.SubmoduleWalk.IgnoreSubmoduleMode;
import org.eclipse.jgit.treewalk.FileTreeIterator;
import org.junit.Before;
import org.junit.experimental.theories.DataPoints;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.runner.RunWith;

@RunWith(Theories.class)
public class IndexDiffSubmoduleTest extends RepositoryTestCase {
	/** a submodule repository inside a root repository */
	protected FileRepository submodule_db;

	/** Working directory of the submodule repository */
	protected File submodule_trash;

	@DataPoints
	public static IgnoreSubmoduleMode allModes[] = IgnoreSubmoduleMode.values();

	@Override
	@Before
	public void setUp() throws Exception {
		super.setUp();
		FileRepository submoduleStandalone = createWorkRepository();
		JGitTestUtil.writeTrashFile(submoduleStandalone, "fileInSubmodule",
				"submodule");
		Git submoduleStandaloneGit = Git.wrap(submoduleStandalone);
		submoduleStandaloneGit.add().addFilepattern("fileInSubmodule").call();
		submoduleStandaloneGit.commit().setMessage("add file to submodule")
				.call();

		submodule_db = (FileRepository) Git.wrap(db).submoduleAdd()
				.setPath("modules/submodule")
				.setURI(submoduleStandalone.getDirectory().toURI().toString())
				.call();
		submodule_trash = submodule_db.getWorkTree();
		addRepoToClose(submodule_db);
		writeTrashFile("fileInRoot", "root");
		Git rootGit = Git.wrap(db);
		rootGit.add().addFilepattern("fileInRoot").call();
		rootGit.commit().setMessage("add submodule and root file").call();
	}

	@Theory
	public void testInitiallyClean(IgnoreSubmoduleMode mode)
			throws IOException {
		IndexDiff indexDiff = new IndexDiff(db, Constants.HEAD,
				new FileTreeIterator(db));
		indexDiff.setIgnoreSubmoduleMode(mode);
		assertFalse(indexDiff.diff());
	}

	@Theory
	public void testDirtyRootWorktree(IgnoreSubmoduleMode mode)
			throws IOException {
		writeTrashFile("fileInRoot", "2");

		IndexDiff indexDiff = new IndexDiff(db, Constants.HEAD,
				new FileTreeIterator(db));
		indexDiff.setIgnoreSubmoduleMode(mode);
		assertTrue(indexDiff.diff());
	}

	private void assertDiff(IndexDiff indexDiff, IgnoreSubmoduleMode mode,
			IgnoreSubmoduleMode... expectedEmptyModes) throws IOException {
		boolean diffResult = indexDiff.diff();
		Set<String> submodulePaths = indexDiff
				.getPathsWithIndexMode(FileMode.GITLINK);
		boolean emptyExpected = false;
		for (IgnoreSubmoduleMode empty : expectedEmptyModes) {
			if (mode.equals(empty)) {
				emptyExpected = true;
				break;
			}
		}
		if (emptyExpected) {
			assertFalse("diff should be false with mode=" + mode,
					diffResult);
			assertEquals("should have no paths with FileMode.GITLINK", 0,
					submodulePaths.size());
		} else {
			assertTrue("diff should be true with mode=" + mode,
					diffResult);
			assertTrue("submodule path should have FileMode.GITLINK",
					submodulePaths.contains("modules/submodule"));
		}
	}

	@Theory
	public void testDirtySubmoduleWorktree(IgnoreSubmoduleMode mode)
			throws IOException {
		JGitTestUtil.writeTrashFile(submodule_db, "fileInSubmodule", "2");
		IndexDiff indexDiff = new IndexDiff(db, Constants.HEAD,
				new FileTreeIterator(db));
		indexDiff.setIgnoreSubmoduleMode(mode);
		assertDiff(indexDiff, mode, IgnoreSubmoduleMode.ALL,
				IgnoreSubmoduleMode.DIRTY);
	}

	@Theory
	public void testDirtySubmoduleHEAD(IgnoreSubmoduleMode mode)
			throws IOException, GitAPIException {
		JGitTestUtil.writeTrashFile(submodule_db, "fileInSubmodule", "2");
		Git submoduleGit = Git.wrap(submodule_db);
		submoduleGit.add().addFilepattern("fileInSubmodule").call();
		submoduleGit.commit().setMessage("Modified fileInSubmodule").call();

		IndexDiff indexDiff = new IndexDiff(db, Constants.HEAD,
				new FileTreeIterator(db));
		indexDiff.setIgnoreSubmoduleMode(mode);
		assertDiff(indexDiff, mode, IgnoreSubmoduleMode.ALL);
	}

	@Theory
	public void testDirtySubmoduleIndex(IgnoreSubmoduleMode mode)
			throws IOException, GitAPIException {
		JGitTestUtil.writeTrashFile(submodule_db, "fileInSubmodule", "2");
		Git submoduleGit = Git.wrap(submodule_db);
		submoduleGit.add().addFilepattern("fileInSubmodule").call();

		IndexDiff indexDiff = new IndexDiff(db, Constants.HEAD,
				new FileTreeIterator(db));
		indexDiff.setIgnoreSubmoduleMode(mode);
		assertDiff(indexDiff, mode, IgnoreSubmoduleMode.ALL,
				IgnoreSubmoduleMode.DIRTY);
	}

	@Theory
	public void testDirtySubmoduleIndexAndWorktree(IgnoreSubmoduleMode mode)
			throws IOException, GitAPIException, NoWorkTreeException {
		JGitTestUtil.writeTrashFile(submodule_db, "fileInSubmodule", "2");
		Git submoduleGit = Git.wrap(submodule_db);
		submoduleGit.add().addFilepattern("fileInSubmodule").call();
		JGitTestUtil.writeTrashFile(submodule_db, "fileInSubmodule", "3");

		IndexDiff indexDiff = new IndexDiff(db, Constants.HEAD,
				new FileTreeIterator(db));
		indexDiff.setIgnoreSubmoduleMode(mode);
		assertDiff(indexDiff, mode, IgnoreSubmoduleMode.ALL,
				IgnoreSubmoduleMode.DIRTY);
	}

	@Theory
	public void testDirtySubmoduleWorktreeUntracked(IgnoreSubmoduleMode mode)
			throws IOException {
		JGitTestUtil.writeTrashFile(submodule_db, "additionalFileInSubmodule",
				"2");
		IndexDiff indexDiff = new IndexDiff(db, Constants.HEAD,
				new FileTreeIterator(db));
		indexDiff.setIgnoreSubmoduleMode(mode);
		assertDiff(indexDiff, mode, IgnoreSubmoduleMode.ALL,
				IgnoreSubmoduleMode.DIRTY, IgnoreSubmoduleMode.UNTRACKED);
	}
}
