/*
 * Copyright (C) 2010, Stefan Lay <stefan.lay@sap.com>
 * Copyright (C) 2010-2012, Christian Halstrick <christian.halstrick@sap.com>
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
package org.eclipse.jgit.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.util.Iterator;

import org.eclipse.jgit.api.MergeCommand.FastForwardMode;
import org.eclipse.jgit.api.MergeResult.MergeStatus;
import org.eclipse.jgit.api.errors.InvalidMergeHeadsException;
import org.eclipse.jgit.junit.RepositoryTestCase;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.RepositoryState;
import org.eclipse.jgit.merge.MergeStrategy;
import org.eclipse.jgit.merge.ResolveMerger.MergeFailureReason;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.util.FS;
import org.eclipse.jgit.util.FileUtils;
import org.eclipse.jgit.util.GitDateFormatter;
import org.eclipse.jgit.util.GitDateFormatter.Format;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.theories.DataPoints;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.runner.RunWith;

@RunWith(Theories.class)
public class MergeCommandTest extends RepositoryTestCase {

	public static @DataPoints
	MergeStrategy[] mergeStrategies = MergeStrategy.get();

	private GitDateFormatter dateFormatter;

	@Override
	@Before
	public void setUp() throws Exception {
		super.setUp();
		dateFormatter = new GitDateFormatter(Format.DEFAULT);
	}

	@Test
	public void testMergeInItself() throws Exception {
		Git git = new Git(db);
		git.commit().setMessage("initial commit").call();

		MergeResult result = git.merge().include(db.getRef(Constants.HEAD)).call();
		assertEquals(MergeResult.MergeStatus.ALREADY_UP_TO_DATE, result.getMergeStatus());
		// no reflog entry written by merge
		assertEquals("commit (initial): initial commit",
				db
				.getReflogReader(Constants.HEAD).getLastEntry().getComment());
		assertEquals("commit (initial): initial commit",
				db
				.getReflogReader(db.getBranch()).getLastEntry().getComment());
	}

	@Test
	public void testAlreadyUpToDate() throws Exception {
		Git git = new Git(db);
		RevCommit first = git.commit().setMessage("initial commit").call();
		createBranch(first, "refs/heads/branch1");

		RevCommit second = git.commit().setMessage("second commit").call();
		MergeResult result = git.merge().include(db.getRef("refs/heads/branch1")).call();
		assertEquals(MergeResult.MergeStatus.ALREADY_UP_TO_DATE, result.getMergeStatus());
		assertEquals(second, result.getNewHead());
		// no reflog entry written by merge
		assertEquals("commit: second commit", db
				.getReflogReader(Constants.HEAD).getLastEntry().getComment());
		assertEquals("commit: second commit", db
				.getReflogReader(db.getBranch()).getLastEntry().getComment());
	}

	@Test
	public void testFastForward() throws Exception {
		Git git = new Git(db);
		RevCommit first = git.commit().setMessage("initial commit").call();
		createBranch(first, "refs/heads/branch1");

		RevCommit second = git.commit().setMessage("second commit").call();

		checkoutBranch("refs/heads/branch1");

		MergeResult result = git.merge().include(db.getRef(Constants.MASTER)).call();

		assertEquals(MergeResult.MergeStatus.FAST_FORWARD, result.getMergeStatus());
		assertEquals(second, result.getNewHead());
		assertEquals("merge refs/heads/master: Fast-forward",
				db.getReflogReader(Constants.HEAD).getLastEntry().getComment());
		assertEquals("merge refs/heads/master: Fast-forward",
				db.getReflogReader(db.getBranch()).getLastEntry().getComment());
	}

	@Test
	public void testFastForwardNoCommit() throws Exception {
		Git git = new Git(db);
		RevCommit first = git.commit().setMessage("initial commit").call();
		createBranch(first, "refs/heads/branch1");

		RevCommit second = git.commit().setMessage("second commit").call();

		checkoutBranch("refs/heads/branch1");

		MergeResult result = git.merge().include(db.getRef(Constants.MASTER))
				.setCommit(false).call();

		assertEquals(MergeResult.MergeStatus.FAST_FORWARD,
				result.getMergeStatus());
		assertEquals(second, result.getNewHead());
		assertEquals("merge refs/heads/master: Fast-forward", db
				.getReflogReader(Constants.HEAD).getLastEntry().getComment());
		assertEquals("merge refs/heads/master: Fast-forward", db
				.getReflogReader(db.getBranch()).getLastEntry().getComment());
	}

	@Test
	public void testFastForwardWithFiles() throws Exception {
		Git git = new Git(db);

		writeTrashFile("file1", "file1");
		git.add().addFilepattern("file1").call();
		RevCommit first = git.commit().setMessage("initial commit").call();

		assertTrue(new File(db.getWorkTree(), "file1").exists());
		createBranch(first, "refs/heads/branch1");

		writeTrashFile("file2", "file2");
		git.add().addFilepattern("file2").call();
		RevCommit second = git.commit().setMessage("second commit").call();
		assertTrue(new File(db.getWorkTree(), "file2").exists());

		checkoutBranch("refs/heads/branch1");
		assertFalse(new File(db.getWorkTree(), "file2").exists());

		MergeResult result = git.merge().include(db.getRef(Constants.MASTER)).call();

		assertTrue(new File(db.getWorkTree(), "file1").exists());
		assertTrue(new File(db.getWorkTree(), "file2").exists());
		assertEquals(MergeResult.MergeStatus.FAST_FORWARD, result.getMergeStatus());
		assertEquals(second, result.getNewHead());
		assertEquals("merge refs/heads/master: Fast-forward",
				db.getReflogReader(Constants.HEAD).getLastEntry().getComment());
		assertEquals("merge refs/heads/master: Fast-forward",
				db.getReflogReader(db.getBranch()).getLastEntry().getComment());
	}

	@Test
	public void testMultipleHeads() throws Exception {
		Git git = new Git(db);

		writeTrashFile("file1", "file1");
		git.add().addFilepattern("file1").call();
		RevCommit first = git.commit().setMessage("initial commit").call();
		createBranch(first, "refs/heads/branch1");

		writeTrashFile("file2", "file2");
		git.add().addFilepattern("file2").call();
		RevCommit second = git.commit().setMessage("second commit").call();

		writeTrashFile("file3", "file3");
		git.add().addFilepattern("file3").call();
		git.commit().setMessage("third commit").call();

		checkoutBranch("refs/heads/branch1");
		assertFalse(new File(db.getWorkTree(), "file2").exists());
		assertFalse(new File(db.getWorkTree(), "file3").exists());

		MergeCommand merge = git.merge();
		merge.include(second.getId());
		merge.include(db.getRef(Constants.MASTER));
		try {
			merge.call();
			fail("Expected exception not thrown when merging multiple heads");
		} catch (InvalidMergeHeadsException e) {
			// expected this exception
		}
	}

	@Theory
	public void testMergeSuccessAllStrategies(MergeStrategy mergeStrategy)
			throws Exception {
		Git git = new Git(db);

		RevCommit first = git.commit().setMessage("first").call();
		createBranch(first, "refs/heads/side");

		writeTrashFile("a", "a");
		git.add().addFilepattern("a").call();
		git.commit().setMessage("second").call();

		checkoutBranch("refs/heads/side");
		writeTrashFile("b", "b");
		git.add().addFilepattern("b").call();
		git.commit().setMessage("third").call();

		MergeResult result = git.merge().setStrategy(mergeStrategy)
				.include(db.getRef(Constants.MASTER)).call();
		assertEquals(MergeStatus.MERGED, result.getMergeStatus());
		assertEquals(
				"merge refs/heads/master: Merge made by "
						+ mergeStrategy.getName() + ".",
				db.getReflogReader(Constants.HEAD).getLastEntry().getComment());
		assertEquals(
				"merge refs/heads/master: Merge made by "
						+ mergeStrategy.getName() + ".",
				db.getReflogReader(db.getBranch()).getLastEntry().getComment());
	}

	@Theory
	public void testMergeSuccessAllStrategiesNoCommit(
			MergeStrategy mergeStrategy) throws Exception {
		Git git = new Git(db);

		RevCommit first = git.commit().setMessage("first").call();
		createBranch(first, "refs/heads/side");

		writeTrashFile("a", "a");
		git.add().addFilepattern("a").call();
		git.commit().setMessage("second").call();

		checkoutBranch("refs/heads/side");
		writeTrashFile("b", "b");
		git.add().addFilepattern("b").call();
		RevCommit thirdCommit = git.commit().setMessage("third").call();

		MergeResult result = git.merge().setStrategy(mergeStrategy)
				.setCommit(false)
				.include(db.getRef(Constants.MASTER)).call();
		assertEquals(MergeStatus.MERGED_NOT_COMMITTED, result.getMergeStatus());
		assertEquals(db.getRef(Constants.HEAD).getTarget().getObjectId(),
				thirdCommit.getId());
	}

	@Test
	public void testContentMerge() throws Exception {
		Git git = new Git(db);

		writeTrashFile("a", "1\na\n3\n");
		writeTrashFile("b", "1\nb\n3\n");
		writeTrashFile("c/c/c", "1\nc\n3\n");
		git.add().addFilepattern("a").addFilepattern("b")
				.addFilepattern("c/c/c").call();
		RevCommit initialCommit = git.commit().setMessage("initial").call();

		createBranch(initialCommit, "refs/heads/side");
		checkoutBranch("refs/heads/side");

		writeTrashFile("a", "1\na(side)\n3\n");
		writeTrashFile("b", "1\nb(side)\n3\n");
		git.add().addFilepattern("a").addFilepattern("b").call();
		RevCommit secondCommit = git.commit().setMessage("side").call();

		assertEquals("1\nb(side)\n3\n", read(new File(db.getWorkTree(), "b")));
		checkoutBranch("refs/heads/master");
		assertEquals("1\nb\n3\n", read(new File(db.getWorkTree(), "b")));

		writeTrashFile("a", "1\na(main)\n3\n");
		writeTrashFile("c/c/c", "1\nc(main)\n3\n");
		git.add().addFilepattern("a").addFilepattern("c/c/c").call();
		git.commit().setMessage("main").call();

		MergeResult result = git.merge().include(secondCommit.getId())
				.setStrategy(MergeStrategy.RESOLVE).call();
		assertEquals(MergeStatus.CONFLICTING, result.getMergeStatus());

		assertEquals(
				"1\n<<<<<<< HEAD\na(main)\n=======\na(side)\n>>>>>>> 86503e7e397465588cc267b65d778538bffccb83\n3\n",
				read(new File(db.getWorkTree(), "a")));
		assertEquals("1\nb(side)\n3\n", read(new File(db.getWorkTree(), "b")));
		assertEquals("1\nc(main)\n3\n",
				read(new File(db.getWorkTree(), "c/c/c")));

		assertEquals(1, result.getConflicts().size());
		assertEquals(3, result.getConflicts().get("a")[0].length);

		assertEquals(RepositoryState.MERGING, db.getRepositoryState());
	}

	@Test
	public void testMergeMessage() throws Exception {
		Git git = new Git(db);

		writeTrashFile("a", "1\na\n3\n");
		git.add().addFilepattern("a").call();
		RevCommit initialCommit = git.commit().setMessage("initial").call();

		createBranch(initialCommit, "refs/heads/side");
		checkoutBranch("refs/heads/side");

		writeTrashFile("a", "1\na(side)\n3\n");
		git.add().addFilepattern("a").call();
		git.commit().setMessage("side").call();

		checkoutBranch("refs/heads/master");

		writeTrashFile("a", "1\na(main)\n3\n");
		git.add().addFilepattern("a").call();
		git.commit().setMessage("main").call();

		Ref sideBranch = db.getRef("side");

		git.merge().include(sideBranch)
				.setStrategy(MergeStrategy.RESOLVE).call();

		assertEquals("Merge branch 'side'\n\nConflicts:\n\ta\n",
				db.readMergeCommitMsg());

	}

	@Test
	public void testMergeNonVersionedPaths() throws Exception {
		Git git = new Git(db);

		writeTrashFile("a", "1\na\n3\n");
		writeTrashFile("b", "1\nb\n3\n");
		writeTrashFile("c/c/c", "1\nc\n3\n");
		git.add().addFilepattern("a").addFilepattern("b")
				.addFilepattern("c/c/c").call();
		RevCommit initialCommit = git.commit().setMessage("initial").call();

		createBranch(initialCommit, "refs/heads/side");
		checkoutBranch("refs/heads/side");

		writeTrashFile("a", "1\na(side)\n3\n");
		writeTrashFile("b", "1\nb(side)\n3\n");
		git.add().addFilepattern("a").addFilepattern("b").call();
		RevCommit secondCommit = git.commit().setMessage("side").call();

		assertEquals("1\nb(side)\n3\n", read(new File(db.getWorkTree(), "b")));
		checkoutBranch("refs/heads/master");
		assertEquals("1\nb\n3\n", read(new File(db.getWorkTree(), "b")));

		writeTrashFile("a", "1\na(main)\n3\n");
		writeTrashFile("c/c/c", "1\nc(main)\n3\n");
		git.add().addFilepattern("a").addFilepattern("c/c/c").call();
		git.commit().setMessage("main").call();

		writeTrashFile("d", "1\nd\n3\n");
		assertTrue(new File(db.getWorkTree(), "e").mkdir());

		MergeResult result = git.merge().include(secondCommit.getId())
				.setStrategy(MergeStrategy.RESOLVE).call();
		assertEquals(MergeStatus.CONFLICTING, result.getMergeStatus());

		assertEquals(
				"1\n<<<<<<< HEAD\na(main)\n=======\na(side)\n>>>>>>> 86503e7e397465588cc267b65d778538bffccb83\n3\n",
				read(new File(db.getWorkTree(), "a")));
		assertEquals("1\nb(side)\n3\n", read(new File(db.getWorkTree(), "b")));
		assertEquals("1\nc(main)\n3\n",
				read(new File(db.getWorkTree(), "c/c/c")));
		assertEquals("1\nd\n3\n", read(new File(db.getWorkTree(), "d")));
		File dir = new File(db.getWorkTree(), "e");
		assertTrue(dir.isDirectory());

		assertEquals(1, result.getConflicts().size());
		assertEquals(3, result.getConflicts().get("a")[0].length);

		assertEquals(RepositoryState.MERGING, db.getRepositoryState());
	}

	@Test
	public void testMultipleCreations() throws Exception {
		Git git = new Git(db);

		writeTrashFile("a", "1\na\n3\n");
		git.add().addFilepattern("a").call();
		RevCommit initialCommit = git.commit().setMessage("initial").call();

		createBranch(initialCommit, "refs/heads/side");
		checkoutBranch("refs/heads/side");

		writeTrashFile("b", "1\nb(side)\n3\n");
		git.add().addFilepattern("b").call();
		RevCommit secondCommit = git.commit().setMessage("side").call();

		checkoutBranch("refs/heads/master");

		writeTrashFile("b", "1\nb(main)\n3\n");
		git.add().addFilepattern("b").call();
		git.commit().setMessage("main").call();

		MergeResult result = git.merge().include(secondCommit.getId())
				.setStrategy(MergeStrategy.RESOLVE).call();
		assertEquals(MergeStatus.CONFLICTING, result.getMergeStatus());
	}

	@Test
	public void testMultipleCreationsSameContent() throws Exception {
		Git git = new Git(db);

		writeTrashFile("a", "1\na\n3\n");
		git.add().addFilepattern("a").call();
		RevCommit initialCommit = git.commit().setMessage("initial").call();

		createBranch(initialCommit, "refs/heads/side");
		checkoutBranch("refs/heads/side");

		writeTrashFile("b", "1\nb(1)\n3\n");
		git.add().addFilepattern("b").call();
		RevCommit secondCommit = git.commit().setMessage("side").call();

		checkoutBranch("refs/heads/master");

		writeTrashFile("b", "1\nb(1)\n3\n");
		git.add().addFilepattern("b").call();
		git.commit().setMessage("main").call();

		MergeResult result = git.merge().include(secondCommit.getId())
				.setStrategy(MergeStrategy.RESOLVE).call();
		assertEquals(MergeStatus.MERGED, result.getMergeStatus());
		assertEquals("1\nb(1)\n3\n", read(new File(db.getWorkTree(), "b")));
		assertEquals("merge " + secondCommit.getId().getName()
				+ ": Merge made by resolve.", db
				.getReflogReader(Constants.HEAD)
				.getLastEntry().getComment());
		assertEquals("merge " + secondCommit.getId().getName()
				+ ": Merge made by resolve.", db
				.getReflogReader(db.getBranch())
				.getLastEntry().getComment());
	}

	@Test
	public void testSuccessfulContentMerge() throws Exception {
		Git git = new Git(db);

		writeTrashFile("a", "1\na\n3\n");
		writeTrashFile("b", "1\nb\n3\n");
		writeTrashFile("c/c/c", "1\nc\n3\n");
		git.add().addFilepattern("a").addFilepattern("b")
				.addFilepattern("c/c/c").call();
		RevCommit initialCommit = git.commit().setMessage("initial").call();

		createBranch(initialCommit, "refs/heads/side");
		checkoutBranch("refs/heads/side");

		writeTrashFile("a", "1(side)\na\n3\n");
		writeTrashFile("b", "1\nb(side)\n3\n");
		git.add().addFilepattern("a").addFilepattern("b").call();
		RevCommit secondCommit = git.commit().setMessage("side").call();

		assertEquals("1\nb(side)\n3\n", read(new File(db.getWorkTree(), "b")));
		checkoutBranch("refs/heads/master");
		assertEquals("1\nb\n3\n", read(new File(db.getWorkTree(), "b")));

		writeTrashFile("a", "1\na\n3(main)\n");
		writeTrashFile("c/c/c", "1\nc(main)\n3\n");
		git.add().addFilepattern("a").addFilepattern("c/c/c").call();
		RevCommit thirdCommit = git.commit().setMessage("main").call();

		MergeResult result = git.merge().include(secondCommit.getId())
				.setStrategy(MergeStrategy.RESOLVE).call();
		assertEquals(MergeStatus.MERGED, result.getMergeStatus());

		assertEquals("1(side)\na\n3(main)\n", read(new File(db.getWorkTree(),
				"a")));
		assertEquals("1\nb(side)\n3\n", read(new File(db.getWorkTree(), "b")));
		assertEquals("1\nc(main)\n3\n", read(new File(db.getWorkTree(),
				"c/c/c")));

		assertEquals(null, result.getConflicts());

		assertEquals(2, result.getMergedCommits().length);
		assertEquals(thirdCommit, result.getMergedCommits()[0]);
		assertEquals(secondCommit, result.getMergedCommits()[1]);

		Iterator<RevCommit> it = git.log().call().iterator();
		RevCommit newHead = it.next();
		assertEquals(newHead, result.getNewHead());
		assertEquals(2, newHead.getParentCount());
		assertEquals(thirdCommit, newHead.getParent(0));
		assertEquals(secondCommit, newHead.getParent(1));
		assertEquals(
				"Merge commit '3fa334456d236a92db020289fe0bf481d91777b4'",
				newHead.getFullMessage());
		// @TODO fix me
		assertEquals(RepositoryState.SAFE, db.getRepositoryState());
		// test index state
	}

	@Test
	public void testSuccessfulContentMergeNoCommit() throws Exception {
		Git git = new Git(db);

		writeTrashFile("a", "1\na\n3\n");
		writeTrashFile("b", "1\nb\n3\n");
		writeTrashFile("c/c/c", "1\nc\n3\n");
		git.add().addFilepattern("a").addFilepattern("b")
				.addFilepattern("c/c/c").call();
		RevCommit initialCommit = git.commit().setMessage("initial").call();

		createBranch(initialCommit, "refs/heads/side");
		checkoutBranch("refs/heads/side");

		writeTrashFile("a", "1(side)\na\n3\n");
		writeTrashFile("b", "1\nb(side)\n3\n");
		git.add().addFilepattern("a").addFilepattern("b").call();
		RevCommit secondCommit = git.commit().setMessage("side").call();

		assertEquals("1\nb(side)\n3\n", read(new File(db.getWorkTree(), "b")));
		checkoutBranch("refs/heads/master");
		assertEquals("1\nb\n3\n", read(new File(db.getWorkTree(), "b")));

		writeTrashFile("a", "1\na\n3(main)\n");
		writeTrashFile("c/c/c", "1\nc(main)\n3\n");
		git.add().addFilepattern("a").addFilepattern("c/c/c").call();
		RevCommit thirdCommit = git.commit().setMessage("main").call();

		MergeResult result = git.merge().include(secondCommit.getId())
				.setCommit(false)
				.setStrategy(MergeStrategy.RESOLVE).call();
		assertEquals(MergeStatus.MERGED_NOT_COMMITTED, result.getMergeStatus());
		assertEquals(db.getRef(Constants.HEAD).getTarget().getObjectId(),
				thirdCommit.getId());

		assertEquals("1(side)\na\n3(main)\n", read(new File(db.getWorkTree(),
				"a")));
		assertEquals("1\nb(side)\n3\n", read(new File(db.getWorkTree(), "b")));
		assertEquals("1\nc(main)\n3\n",
				read(new File(db.getWorkTree(), "c/c/c")));

		assertEquals(null, result.getConflicts());

		assertEquals(2, result.getMergedCommits().length);
		assertEquals(thirdCommit, result.getMergedCommits()[0]);
		assertEquals(secondCommit, result.getMergedCommits()[1]);
		assertNull(result.getNewHead());
		assertEquals(RepositoryState.MERGING_RESOLVED, db.getRepositoryState());
	}

	@Test
	public void testSuccessfulContentMergeAndDirtyworkingTree()
			throws Exception {
		Git git = new Git(db);

		writeTrashFile("a", "1\na\n3\n");
		writeTrashFile("b", "1\nb\n3\n");
		writeTrashFile("d", "1\nd\n3\n");
		writeTrashFile("c/c/c", "1\nc\n3\n");
		git.add().addFilepattern("a").addFilepattern("b")
				.addFilepattern("c/c/c").addFilepattern("d").call();
		RevCommit initialCommit = git.commit().setMessage("initial").call();

		createBranch(initialCommit, "refs/heads/side");
		checkoutBranch("refs/heads/side");

		writeTrashFile("a", "1(side)\na\n3\n");
		writeTrashFile("b", "1\nb(side)\n3\n");
		git.add().addFilepattern("a").addFilepattern("b").call();
		RevCommit secondCommit = git.commit().setMessage("side").call();

		assertEquals("1\nb(side)\n3\n", read(new File(db.getWorkTree(), "b")));
		checkoutBranch("refs/heads/master");
		assertEquals("1\nb\n3\n", read(new File(db.getWorkTree(), "b")));

		writeTrashFile("a", "1\na\n3(main)\n");
		writeTrashFile("c/c/c", "1\nc(main)\n3\n");
		git.add().addFilepattern("a").addFilepattern("c/c/c").call();
		RevCommit thirdCommit = git.commit().setMessage("main").call();

		writeTrashFile("d", "--- dirty ---");
		MergeResult result = git.merge().include(secondCommit.getId())
				.setStrategy(MergeStrategy.RESOLVE).call();
		assertEquals(MergeStatus.MERGED, result.getMergeStatus());

		assertEquals("1(side)\na\n3(main)\n", read(new File(db.getWorkTree(),
				"a")));
		assertEquals("1\nb(side)\n3\n", read(new File(db.getWorkTree(), "b")));
		assertEquals("1\nc(main)\n3\n", read(new File(db.getWorkTree(),
				"c/c/c")));
		assertEquals("--- dirty ---", read(new File(db.getWorkTree(), "d")));

		assertEquals(null, result.getConflicts());

		assertEquals(2, result.getMergedCommits().length);
		assertEquals(thirdCommit, result.getMergedCommits()[0]);
		assertEquals(secondCommit, result.getMergedCommits()[1]);

		Iterator<RevCommit> it = git.log().call().iterator();
		RevCommit newHead = it.next();
		assertEquals(newHead, result.getNewHead());
		assertEquals(2, newHead.getParentCount());
		assertEquals(thirdCommit, newHead.getParent(0));
		assertEquals(secondCommit, newHead.getParent(1));
		assertEquals(
				"Merge commit '064d54d98a4cdb0fed1802a21c656bfda67fe879'",
				newHead.getFullMessage());

		assertEquals(RepositoryState.SAFE, db.getRepositoryState());
	}

	@Test
	public void testSingleDeletion() throws Exception {
		Git git = new Git(db);

		writeTrashFile("a", "1\na\n3\n");
		writeTrashFile("b", "1\nb\n3\n");
		writeTrashFile("d", "1\nd\n3\n");
		writeTrashFile("c/c/c", "1\nc\n3\n");
		git.add().addFilepattern("a").addFilepattern("b")
				.addFilepattern("c/c/c").addFilepattern("d").call();
		RevCommit initialCommit = git.commit().setMessage("initial").call();

		createBranch(initialCommit, "refs/heads/side");
		checkoutBranch("refs/heads/side");

		assertTrue(new File(db.getWorkTree(), "b").delete());
		git.add().addFilepattern("b").setUpdate(true).call();
		RevCommit secondCommit = git.commit().setMessage("side").call();

		assertFalse(new File(db.getWorkTree(), "b").exists());
		checkoutBranch("refs/heads/master");
		assertTrue(new File(db.getWorkTree(), "b").exists());

		writeTrashFile("a", "1\na\n3(main)\n");
		writeTrashFile("c/c/c", "1\nc(main)\n3\n");
		git.add().addFilepattern("a").addFilepattern("c/c/c").call();
		RevCommit thirdCommit = git.commit().setMessage("main").call();

		// We are merging a deletion into our branch
		MergeResult result = git.merge().include(secondCommit.getId())
				.setStrategy(MergeStrategy.RESOLVE).call();
		assertEquals(MergeStatus.MERGED, result.getMergeStatus());

		assertEquals("1\na\n3(main)\n", read(new File(db.getWorkTree(), "a")));
		assertFalse(new File(db.getWorkTree(), "b").exists());
		assertEquals("1\nc(main)\n3\n",
				read(new File(db.getWorkTree(), "c/c/c")));
		assertEquals("1\nd\n3\n", read(new File(db.getWorkTree(), "d")));

		// Do the opposite, be on a branch where we have deleted a file and
		// merge in a old commit where this file was not deleted
		checkoutBranch("refs/heads/side");
		assertFalse(new File(db.getWorkTree(), "b").exists());

		result = git.merge().include(thirdCommit.getId())
				.setStrategy(MergeStrategy.RESOLVE).call();
		assertEquals(MergeStatus.MERGED, result.getMergeStatus());

		assertEquals("1\na\n3(main)\n", read(new File(db.getWorkTree(), "a")));
		assertFalse(new File(db.getWorkTree(), "b").exists());
		assertEquals("1\nc(main)\n3\n",
				read(new File(db.getWorkTree(), "c/c/c")));
		assertEquals("1\nd\n3\n", read(new File(db.getWorkTree(), "d")));
	}

	@Test
	public void testMultipleDeletions() throws Exception {
		Git git = new Git(db);

		writeTrashFile("a", "1\na\n3\n");
		git.add().addFilepattern("a").call();
		RevCommit initialCommit = git.commit().setMessage("initial").call();

		createBranch(initialCommit, "refs/heads/side");
		checkoutBranch("refs/heads/side");

		assertTrue(new File(db.getWorkTree(), "a").delete());
		git.add().addFilepattern("a").setUpdate(true).call();
		RevCommit secondCommit = git.commit().setMessage("side").call();

		assertFalse(new File(db.getWorkTree(), "a").exists());
		checkoutBranch("refs/heads/master");
		assertTrue(new File(db.getWorkTree(), "a").exists());

		assertTrue(new File(db.getWorkTree(), "a").delete());
		git.add().addFilepattern("a").setUpdate(true).call();
		git.commit().setMessage("main").call();

		// We are merging a deletion into our branch
		MergeResult result = git.merge().include(secondCommit.getId())
				.setStrategy(MergeStrategy.RESOLVE).call();
		assertEquals(MergeStatus.MERGED, result.getMergeStatus());
	}

	@Test
	public void testDeletionAndConflict() throws Exception {
		Git git = new Git(db);

		writeTrashFile("a", "1\na\n3\n");
		writeTrashFile("b", "1\nb\n3\n");
		writeTrashFile("d", "1\nd\n3\n");
		writeTrashFile("c/c/c", "1\nc\n3\n");
		git.add().addFilepattern("a").addFilepattern("b")
				.addFilepattern("c/c/c").addFilepattern("d").call();
		RevCommit initialCommit = git.commit().setMessage("initial").call();

		createBranch(initialCommit, "refs/heads/side");
		checkoutBranch("refs/heads/side");

		assertTrue(new File(db.getWorkTree(), "b").delete());
		writeTrashFile("a", "1\na\n3(side)\n");
		git.add().addFilepattern("b").setUpdate(true).call();
		git.add().addFilepattern("a").setUpdate(true).call();
		RevCommit secondCommit = git.commit().setMessage("side").call();

		assertFalse(new File(db.getWorkTree(), "b").exists());
		checkoutBranch("refs/heads/master");
		assertTrue(new File(db.getWorkTree(), "b").exists());

		writeTrashFile("a", "1\na\n3(main)\n");
		writeTrashFile("c/c/c", "1\nc(main)\n3\n");
		git.add().addFilepattern("a").addFilepattern("c/c/c").call();
		git.commit().setMessage("main").call();

		// We are merging a deletion into our branch
		MergeResult result = git.merge().include(secondCommit.getId())
				.setStrategy(MergeStrategy.RESOLVE).call();
		assertEquals(MergeStatus.CONFLICTING, result.getMergeStatus());

		assertEquals(
				"1\na\n<<<<<<< HEAD\n3(main)\n=======\n3(side)\n>>>>>>> 54ffed45d62d252715fc20e41da92d44c48fb0ff\n",
				read(new File(db.getWorkTree(), "a")));
		assertFalse(new File(db.getWorkTree(), "b").exists());
		assertEquals("1\nc(main)\n3\n",
				read(new File(db.getWorkTree(), "c/c/c")));
		assertEquals("1\nd\n3\n", read(new File(db.getWorkTree(), "d")));
	}

	@Test
	public void testDeletionOnMasterConflict() throws Exception {
		Git git = new Git(db);

		writeTrashFile("a", "1\na\n3\n");
		writeTrashFile("b", "1\nb\n3\n");
		git.add().addFilepattern("a").addFilepattern("b").call();
		RevCommit initialCommit = git.commit().setMessage("initial").call();

		// create side branch and modify "a"
		createBranch(initialCommit, "refs/heads/side");
		checkoutBranch("refs/heads/side");
		writeTrashFile("a", "1\na(side)\n3\n");
		git.add().addFilepattern("a").call();
		RevCommit secondCommit = git.commit().setMessage("side").call();

		// delete a on master to generate conflict
		checkoutBranch("refs/heads/master");
		git.rm().addFilepattern("a").call();
		git.commit().setMessage("main").call();

		// merge side with master
		MergeResult result = git.merge().include(secondCommit.getId())
				.setStrategy(MergeStrategy.RESOLVE).call();
		assertEquals(MergeStatus.CONFLICTING, result.getMergeStatus());

		// result should be 'a' conflicting with workspace content from side
		assertTrue(new File(db.getWorkTree(), "a").exists());
		assertEquals("1\na(side)\n3\n", read(new File(db.getWorkTree(), "a")));
		assertEquals("1\nb\n3\n", read(new File(db.getWorkTree(), "b")));
	}

	@Test
	public void testDeletionOnSideConflict() throws Exception {
		Git git = new Git(db);

		writeTrashFile("a", "1\na\n3\n");
		writeTrashFile("b", "1\nb\n3\n");
		git.add().addFilepattern("a").addFilepattern("b").call();
		RevCommit initialCommit = git.commit().setMessage("initial").call();

		// create side branch and delete "a"
		createBranch(initialCommit, "refs/heads/side");
		checkoutBranch("refs/heads/side");
		git.rm().addFilepattern("a").call();
		RevCommit secondCommit = git.commit().setMessage("side").call();

		// update a on master to generate conflict
		checkoutBranch("refs/heads/master");
		writeTrashFile("a", "1\na(main)\n3\n");
		git.add().addFilepattern("a").call();
		git.commit().setMessage("main").call();

		// merge side with master
		MergeResult result = git.merge().include(secondCommit.getId())
				.setStrategy(MergeStrategy.RESOLVE).call();
		assertEquals(MergeStatus.CONFLICTING, result.getMergeStatus());

		assertTrue(new File(db.getWorkTree(), "a").exists());
		assertEquals("1\na(main)\n3\n", read(new File(db.getWorkTree(), "a")));
		assertEquals("1\nb\n3\n", read(new File(db.getWorkTree(), "b")));

		assertEquals(1, result.getConflicts().size());
		assertEquals(3, result.getConflicts().get("a")[0].length);
	}

	@Test
	public void testModifiedAndRenamed() throws Exception {
		// this test is essentially the same as testDeletionOnSideConflict,
		// however if once rename support is added this test should result in a
		// successful merge instead of a conflict
		Git git = new Git(db);

		writeTrashFile("x", "add x");
		git.add().addFilepattern("x").call();
		RevCommit initial = git.commit().setMessage("add x").call();

		createBranch(initial, "refs/heads/d1");
		createBranch(initial, "refs/heads/d2");

		// rename x to y on d1
		checkoutBranch("refs/heads/d1");
		new File(db.getWorkTree(), "x")
				.renameTo(new File(db.getWorkTree(), "y"));
		git.rm().addFilepattern("x").call();
		git.add().addFilepattern("y").call();
		RevCommit d1Commit = git.commit().setMessage("d1 rename x -> y").call();

		checkoutBranch("refs/heads/d2");
		writeTrashFile("x", "d2 change");
		git.add().addFilepattern("x").call();
		RevCommit d2Commit = git.commit().setMessage("d2 change in x").call();

		checkoutBranch("refs/heads/master");
		MergeResult d1Merge = git.merge().include(d1Commit).call();
		assertEquals(MergeResult.MergeStatus.FAST_FORWARD,
				d1Merge.getMergeStatus());

		MergeResult d2Merge = git.merge().include(d2Commit).call();
		assertEquals(MergeResult.MergeStatus.CONFLICTING,
				d2Merge.getMergeStatus());
		assertEquals(1, d2Merge.getConflicts().size());
		assertEquals(3, d2Merge.getConflicts().get("x")[0].length);
	}

	@Test
	public void testMergeFailingWithDirtyWorkingTree() throws Exception {
		Git git = new Git(db);

		writeTrashFile("a", "1\na\n3\n");
		writeTrashFile("b", "1\nb\n3\n");
		git.add().addFilepattern("a").addFilepattern("b").call();
		RevCommit initialCommit = git.commit().setMessage("initial").call();

		createBranch(initialCommit, "refs/heads/side");
		checkoutBranch("refs/heads/side");

		writeTrashFile("a", "1(side)\na\n3\n");
		writeTrashFile("b", "1\nb(side)\n3\n");
		git.add().addFilepattern("a").addFilepattern("b").call();
		RevCommit secondCommit = git.commit().setMessage("side").call();

		assertEquals("1\nb(side)\n3\n", read(new File(db.getWorkTree(), "b")));
		checkoutBranch("refs/heads/master");
		assertEquals("1\nb\n3\n", read(new File(db.getWorkTree(), "b")));

		writeTrashFile("a", "1\na\n3(main)\n");
		git.add().addFilepattern("a").call();
		git.commit().setMessage("main").call();

		writeTrashFile("a", "--- dirty ---");
		MergeResult result = git.merge().include(secondCommit.getId())
				.setStrategy(MergeStrategy.RESOLVE).call();

		assertEquals(MergeStatus.FAILED, result.getMergeStatus());

		assertEquals("--- dirty ---", read(new File(db.getWorkTree(), "a")));
		assertEquals("1\nb\n3\n", read(new File(db.getWorkTree(), "b")));

		assertEquals(null, result.getConflicts());

		assertEquals(RepositoryState.SAFE, db.getRepositoryState());
	}

	@Test
	public void testMergeConflictFileFolder() throws Exception {
		Git git = new Git(db);

		writeTrashFile("a", "1\na\n3\n");
		writeTrashFile("b", "1\nb\n3\n");
		git.add().addFilepattern("a").addFilepattern("b").call();
		RevCommit initialCommit = git.commit().setMessage("initial").call();

		createBranch(initialCommit, "refs/heads/side");
		checkoutBranch("refs/heads/side");

		writeTrashFile("c/c/c", "1\nc(side)\n3\n");
		writeTrashFile("d", "1\nd(side)\n3\n");
		git.add().addFilepattern("c/c/c").addFilepattern("d").call();
		RevCommit secondCommit = git.commit().setMessage("side").call();

		checkoutBranch("refs/heads/master");

		writeTrashFile("c", "1\nc(main)\n3\n");
		writeTrashFile("d/d/d", "1\nd(main)\n3\n");
		git.add().addFilepattern("c").addFilepattern("d/d/d").call();
		git.commit().setMessage("main").call();

		MergeResult result = git.merge().include(secondCommit.getId())
				.setStrategy(MergeStrategy.RESOLVE).call();

		assertEquals(MergeStatus.CONFLICTING, result.getMergeStatus());

		assertEquals("1\na\n3\n", read(new File(db.getWorkTree(), "a")));
		assertEquals("1\nb\n3\n", read(new File(db.getWorkTree(), "b")));
		assertEquals("1\nc(main)\n3\n", read(new File(db.getWorkTree(), "c")));
		assertEquals("1\nd(main)\n3\n", read(new File(db.getWorkTree(), "d/d/d")));

		assertEquals(null, result.getConflicts());

		assertEquals(RepositoryState.MERGING, db.getRepositoryState());
	}

	@Test
	public void testSuccessfulMergeFailsDueToDirtyIndex() throws Exception {
		Git git = new Git(db);

		File fileA = writeTrashFile("a", "a");
		RevCommit initialCommit = addAllAndCommit(git);

		// switch branch
		createBranch(initialCommit, "refs/heads/side");
		checkoutBranch("refs/heads/side");
		// modify file a
		write(fileA, "a(side)");
		writeTrashFile("b", "b");
		RevCommit sideCommit = addAllAndCommit(git);

		// switch branch
		checkoutBranch("refs/heads/master");
		writeTrashFile("c", "c");
		addAllAndCommit(git);

		// modify and add file a
		write(fileA, "a(modified)");
		git.add().addFilepattern("a").call();
		// do not commit

		// get current index state
		String indexState = indexState(CONTENT);

		// merge
		MergeResult result = git.merge().include(sideCommit.getId())
				.setStrategy(MergeStrategy.RESOLVE).call();

		checkMergeFailedResult(result, MergeFailureReason.DIRTY_INDEX,
				indexState, fileA);
	}

	@Test
	public void testConflictingMergeFailsDueToDirtyIndex() throws Exception {
		Git git = new Git(db);

		File fileA = writeTrashFile("a", "a");
		RevCommit initialCommit = addAllAndCommit(git);

		// switch branch
		createBranch(initialCommit, "refs/heads/side");
		checkoutBranch("refs/heads/side");
		// modify file a
		write(fileA, "a(side)");
		writeTrashFile("b", "b");
		RevCommit sideCommit = addAllAndCommit(git);

		// switch branch
		checkoutBranch("refs/heads/master");
		// modify file a - this will cause a conflict during merge
		write(fileA, "a(master)");
		writeTrashFile("c", "c");
		addAllAndCommit(git);

		// modify and add file a
		write(fileA, "a(modified)");
		git.add().addFilepattern("a").call();
		// do not commit

		// get current index state
		String indexState = indexState(CONTENT);

		// merge
		MergeResult result = git.merge().include(sideCommit.getId())
				.setStrategy(MergeStrategy.RESOLVE).call();

		checkMergeFailedResult(result, MergeFailureReason.DIRTY_INDEX,
				indexState, fileA);
	}

	@Test
	public void testSuccessfulMergeFailsDueToDirtyWorktree() throws Exception {
		Git git = new Git(db);

		File fileA = writeTrashFile("a", "a");
		RevCommit initialCommit = addAllAndCommit(git);

		// switch branch
		createBranch(initialCommit, "refs/heads/side");
		checkoutBranch("refs/heads/side");
		// modify file a
		write(fileA, "a(side)");
		writeTrashFile("b", "b");
		RevCommit sideCommit = addAllAndCommit(git);

		// switch branch
		checkoutBranch("refs/heads/master");
		writeTrashFile("c", "c");
		addAllAndCommit(git);

		// modify file a
		write(fileA, "a(modified)");
		// do not add and commit

		// get current index state
		String indexState = indexState(CONTENT);

		// merge
		MergeResult result = git.merge().include(sideCommit.getId())
				.setStrategy(MergeStrategy.RESOLVE).call();

		checkMergeFailedResult(result, MergeFailureReason.DIRTY_WORKTREE,
				indexState, fileA);
	}

	@Test
	public void testConflictingMergeFailsDueToDirtyWorktree() throws Exception {
		Git git = new Git(db);

		File fileA = writeTrashFile("a", "a");
		RevCommit initialCommit = addAllAndCommit(git);

		// switch branch
		createBranch(initialCommit, "refs/heads/side");
		checkoutBranch("refs/heads/side");
		// modify file a
		write(fileA, "a(side)");
		writeTrashFile("b", "b");
		RevCommit sideCommit = addAllAndCommit(git);

		// switch branch
		checkoutBranch("refs/heads/master");
		// modify file a - this will cause a conflict during merge
		write(fileA, "a(master)");
		writeTrashFile("c", "c");
		addAllAndCommit(git);

		// modify file a
		write(fileA, "a(modified)");
		// do not add and commit

		// get current index state
		String indexState = indexState(CONTENT);

		// merge
		MergeResult result = git.merge().include(sideCommit.getId())
				.setStrategy(MergeStrategy.RESOLVE).call();

		checkMergeFailedResult(result, MergeFailureReason.DIRTY_WORKTREE,
				indexState, fileA);
	}

	@Test
	public void testMergeRemovingFolders() throws Exception {
		File folder1 = new File(db.getWorkTree(), "folder1");
		File folder2 = new File(db.getWorkTree(), "folder2");
		FileUtils.mkdir(folder1);
		FileUtils.mkdir(folder2);
		File file = new File(folder1, "file1.txt");
		write(file, "folder1--file1.txt");
		file = new File(folder1, "file2.txt");
		write(file, "folder1--file2.txt");
		file = new File(folder2, "file1.txt");
		write(file, "folder--file1.txt");
		file = new File(folder2, "file2.txt");
		write(file, "folder2--file2.txt");

		Git git = new Git(db);
		git.add().addFilepattern(folder1.getName())
				.addFilepattern(folder2.getName()).call();
		RevCommit commit1 = git.commit().setMessage("adding folders").call();

		recursiveDelete(folder1);
		recursiveDelete(folder2);
		git.rm().addFilepattern("folder1/file1.txt")
				.addFilepattern("folder1/file2.txt")
				.addFilepattern("folder2/file1.txt")
				.addFilepattern("folder2/file2.txt").call();
		RevCommit commit2 = git.commit()
				.setMessage("removing folders on 'branch'").call();

		git.checkout().setName(commit1.name()).call();

		MergeResult result = git.merge().include(commit2.getId())
				.setStrategy(MergeStrategy.RESOLVE).call();
		assertEquals(MergeResult.MergeStatus.FAST_FORWARD,
				result.getMergeStatus());
		assertEquals(commit2, result.getNewHead());
		assertFalse(folder1.exists());
		assertFalse(folder2.exists());
	}

	@Test
	public void testMergeRemovingFoldersWithoutFastForward() throws Exception {
		File folder1 = new File(db.getWorkTree(), "folder1");
		File folder2 = new File(db.getWorkTree(), "folder2");
		FileUtils.mkdir(folder1);
		FileUtils.mkdir(folder2);
		File file = new File(folder1, "file1.txt");
		write(file, "folder1--file1.txt");
		file = new File(folder1, "file2.txt");
		write(file, "folder1--file2.txt");
		file = new File(folder2, "file1.txt");
		write(file, "folder--file1.txt");
		file = new File(folder2, "file2.txt");
		write(file, "folder2--file2.txt");

		Git git = new Git(db);
		git.add().addFilepattern(folder1.getName())
				.addFilepattern(folder2.getName()).call();
		RevCommit base = git.commit().setMessage("adding folders").call();

		recursiveDelete(folder1);
		recursiveDelete(folder2);
		git.rm().addFilepattern("folder1/file1.txt")
				.addFilepattern("folder1/file2.txt")
				.addFilepattern("folder2/file1.txt")
				.addFilepattern("folder2/file2.txt").call();
		RevCommit other = git.commit()
				.setMessage("removing folders on 'branch'").call();

		git.checkout().setName(base.name()).call();

		file = new File(folder2, "file3.txt");
		write(file, "folder2--file3.txt");

		git.add().addFilepattern(folder2.getName()).call();
		git.commit().setMessage("adding another file").call();

		MergeResult result = git.merge().include(other.getId())
				.setStrategy(MergeStrategy.RESOLVE).call();

		assertEquals(MergeResult.MergeStatus.MERGED,
				result.getMergeStatus());
		assertFalse(folder1.exists());
	}

	@Test
	public void testFileModeMerge() throws Exception {
		if (!FS.DETECTED.supportsExecute())
			return;
		// Only Java6
		Git git = new Git(db);

		writeTrashFile("mergeableMode", "a");
		setExecutable(git, "mergeableMode", false);
		writeTrashFile("conflictingModeWithBase", "a");
		setExecutable(git, "conflictingModeWithBase", false);
		RevCommit initialCommit = addAllAndCommit(git);

		// switch branch
		createBranch(initialCommit, "refs/heads/side");
		checkoutBranch("refs/heads/side");
		setExecutable(git, "mergeableMode", true);
		writeTrashFile("conflictingModeNoBase", "b");
		setExecutable(git, "conflictingModeNoBase", true);
		RevCommit sideCommit = addAllAndCommit(git);

		// switch branch
		createBranch(initialCommit, "refs/heads/side2");
		checkoutBranch("refs/heads/side2");
		setExecutable(git, "mergeableMode", false);
		assertFalse(new File(git.getRepository().getWorkTree(),
				"conflictingModeNoBase").exists());
		writeTrashFile("conflictingModeNoBase", "b");
		setExecutable(git, "conflictingModeNoBase", false);
		addAllAndCommit(git);

		// merge
		MergeResult result = git.merge().include(sideCommit.getId())
				.setStrategy(MergeStrategy.RESOLVE).call();
		assertEquals(MergeStatus.CONFLICTING, result.getMergeStatus());
		assertTrue(canExecute(git, "mergeableMode"));
		assertFalse(canExecute(git, "conflictingModeNoBase"));
	}

	@Test
	public void testFileModeMergeWithDirtyWorkTree() throws Exception {
		if (!FS.DETECTED.supportsExecute())
			return;
		// Only Java6 (or set x bit in index)

		Git git = new Git(db);

		writeTrashFile("mergeableButDirty", "a");
		setExecutable(git, "mergeableButDirty", false);
		RevCommit initialCommit = addAllAndCommit(git);

		// switch branch
		createBranch(initialCommit, "refs/heads/side");
		checkoutBranch("refs/heads/side");
		setExecutable(git, "mergeableButDirty", true);
		RevCommit sideCommit = addAllAndCommit(git);

		// switch branch
		createBranch(initialCommit, "refs/heads/side2");
		checkoutBranch("refs/heads/side2");
		setExecutable(git, "mergeableButDirty", false);
		addAllAndCommit(git);

		writeTrashFile("mergeableButDirty", "b");

		// merge
		MergeResult result = git.merge().include(sideCommit.getId())
				.setStrategy(MergeStrategy.RESOLVE).call();
		assertEquals(MergeStatus.FAILED, result.getMergeStatus());
		assertFalse(canExecute(git, "mergeableButDirty"));
	}

	@Test
	public void testSquashFastForward() throws Exception {
		Git git = new Git(db);

		writeTrashFile("file1", "file1");
		git.add().addFilepattern("file1").call();
		RevCommit first = git.commit().setMessage("initial commit").call();

		assertTrue(new File(db.getWorkTree(), "file1").exists());
		createBranch(first, "refs/heads/branch1");
		checkoutBranch("refs/heads/branch1");

		writeTrashFile("file2", "file2");
		git.add().addFilepattern("file2").call();
		RevCommit second = git.commit().setMessage("second commit").call();
		assertTrue(new File(db.getWorkTree(), "file2").exists());

		writeTrashFile("file3", "file3");
		git.add().addFilepattern("file3").call();
		RevCommit third = git.commit().setMessage("third commit").call();
		assertTrue(new File(db.getWorkTree(), "file3").exists());

		checkoutBranch("refs/heads/master");
		assertTrue(new File(db.getWorkTree(), "file1").exists());
		assertFalse(new File(db.getWorkTree(), "file2").exists());
		assertFalse(new File(db.getWorkTree(), "file3").exists());

		MergeResult result = git.merge().include(db.getRef("branch1"))
				.setSquash(true).call();

		assertTrue(new File(db.getWorkTree(), "file1").exists());
		assertTrue(new File(db.getWorkTree(), "file2").exists());
		assertTrue(new File(db.getWorkTree(), "file3").exists());
		assertEquals(MergeResult.MergeStatus.FAST_FORWARD_SQUASHED,
				result.getMergeStatus());
		assertEquals(first, result.getNewHead()); // HEAD didn't move
		assertEquals(first, db.resolve(Constants.HEAD + "^{commit}"));

		assertEquals(
				"Squashed commit of the following:\n\ncommit "
						+ third.getName()
						+ "\nAuthor: "
						+ third.getAuthorIdent().getName()
						+ " <"
						+ third.getAuthorIdent().getEmailAddress()
						+ ">\nDate:   "
						+ dateFormatter.formatDate(third
								.getAuthorIdent())
						+ "\n\n\tthird commit\n\ncommit "
						+ second.getName()
						+ "\nAuthor: "
						+ second.getAuthorIdent().getName()
						+ " <"
						+ second.getAuthorIdent().getEmailAddress()
						+ ">\nDate:   "
						+ dateFormatter.formatDate(second
								.getAuthorIdent()) + "\n\n\tsecond commit\n",
				db.readSquashCommitMsg());
		assertNull(db.readMergeCommitMsg());

		Status stat = git.status().call();
		assertEquals(StatusCommandTest.set("file2", "file3"), stat.getAdded());
	}

	@Test
	public void testSquashMerge() throws Exception {
		Git git = new Git(db);

		writeTrashFile("file1", "file1");
		git.add().addFilepattern("file1").call();
		RevCommit first = git.commit().setMessage("initial commit").call();

		assertTrue(new File(db.getWorkTree(), "file1").exists());
		createBranch(first, "refs/heads/branch1");

		writeTrashFile("file2", "file2");
		git.add().addFilepattern("file2").call();
		RevCommit second = git.commit().setMessage("second commit").call();
		assertTrue(new File(db.getWorkTree(), "file2").exists());

		checkoutBranch("refs/heads/branch1");

		writeTrashFile("file3", "file3");
		git.add().addFilepattern("file3").call();
		RevCommit third = git.commit().setMessage("third commit").call();
		assertTrue(new File(db.getWorkTree(), "file3").exists());

		checkoutBranch("refs/heads/master");
		assertTrue(new File(db.getWorkTree(), "file1").exists());
		assertTrue(new File(db.getWorkTree(), "file2").exists());
		assertFalse(new File(db.getWorkTree(), "file3").exists());

		MergeResult result = git.merge().include(db.getRef("branch1"))
				.setSquash(true).call();

		assertTrue(new File(db.getWorkTree(), "file1").exists());
		assertTrue(new File(db.getWorkTree(), "file2").exists());
		assertTrue(new File(db.getWorkTree(), "file3").exists());
		assertEquals(MergeResult.MergeStatus.MERGED_SQUASHED,
				result.getMergeStatus());
		assertEquals(second, result.getNewHead()); // HEAD didn't move
		assertEquals(second, db.resolve(Constants.HEAD + "^{commit}"));

		assertEquals(
				"Squashed commit of the following:\n\ncommit "
						+ third.getName()
						+ "\nAuthor: "
						+ third.getAuthorIdent().getName()
						+ " <"
						+ third.getAuthorIdent().getEmailAddress()
						+ ">\nDate:   "
						+ dateFormatter.formatDate(third
								.getAuthorIdent()) + "\n\n\tthird commit\n",
				db.readSquashCommitMsg());
		assertNull(db.readMergeCommitMsg());

		Status stat = git.status().call();
		assertEquals(StatusCommandTest.set("file3"), stat.getAdded());
	}

	@Test
	public void testSquashMergeConflict() throws Exception {
		Git git = new Git(db);

		writeTrashFile("file1", "file1");
		git.add().addFilepattern("file1").call();
		RevCommit first = git.commit().setMessage("initial commit").call();

		assertTrue(new File(db.getWorkTree(), "file1").exists());
		createBranch(first, "refs/heads/branch1");

		writeTrashFile("file2", "master");
		git.add().addFilepattern("file2").call();
		RevCommit second = git.commit().setMessage("second commit").call();
		assertTrue(new File(db.getWorkTree(), "file2").exists());

		checkoutBranch("refs/heads/branch1");

		writeTrashFile("file2", "branch");
		git.add().addFilepattern("file2").call();
		RevCommit third = git.commit().setMessage("third commit").call();
		assertTrue(new File(db.getWorkTree(), "file2").exists());

		checkoutBranch("refs/heads/master");
		assertTrue(new File(db.getWorkTree(), "file1").exists());
		assertTrue(new File(db.getWorkTree(), "file2").exists());

		MergeResult result = git.merge().include(db.getRef("branch1"))
				.setSquash(true).call();

		assertTrue(new File(db.getWorkTree(), "file1").exists());
		assertTrue(new File(db.getWorkTree(), "file2").exists());
		assertEquals(MergeResult.MergeStatus.CONFLICTING,
				result.getMergeStatus());
		assertNull(result.getNewHead());
		assertEquals(second, db.resolve(Constants.HEAD + "^{commit}"));

		assertEquals(
				"Squashed commit of the following:\n\ncommit "
						+ third.getName()
						+ "\nAuthor: "
						+ third.getAuthorIdent().getName()
						+ " <"
						+ third.getAuthorIdent().getEmailAddress()
						+ ">\nDate:   "
						+ dateFormatter.formatDate(third
								.getAuthorIdent()) + "\n\n\tthird commit\n",
				db.readSquashCommitMsg());
		assertEquals("\nConflicts:\n\tfile2\n", db.readMergeCommitMsg());

		Status stat = git.status().call();
		assertEquals(StatusCommandTest.set("file2"), stat.getConflicting());
	}

	@Test
	public void testFastForwardOnly() throws Exception {
		Git git = new Git(db);
		RevCommit initialCommit = git.commit().setMessage("initial commit")
				.call();
		createBranch(initialCommit, "refs/heads/branch1");
		git.commit().setMessage("second commit").call();
		checkoutBranch("refs/heads/branch1");

		MergeCommand merge = git.merge();
		merge.setFastForward(FastForwardMode.FF_ONLY);
		merge.include(db.getRef(Constants.MASTER));
		MergeResult result = merge.call();

		assertEquals(MergeStatus.FAST_FORWARD, result.getMergeStatus());
	}

	@Test
	public void testNoFastForward() throws Exception {
		Git git = new Git(db);
		RevCommit initialCommit = git.commit().setMessage("initial commit")
				.call();
		createBranch(initialCommit, "refs/heads/branch1");
		git.commit().setMessage("second commit").call();
		checkoutBranch("refs/heads/branch1");

		MergeCommand merge = git.merge();
		merge.setFastForward(FastForwardMode.NO_FF);
		merge.include(db.getRef(Constants.MASTER));
		MergeResult result = merge.call();

		assertEquals(MergeStatus.MERGED, result.getMergeStatus());
	}

	@Test
	public void testNoFastForwardNoCommit() throws Exception {
		// given
		Git git = new Git(db);
		RevCommit initialCommit = git.commit().setMessage("initial commit")
				.call();
		createBranch(initialCommit, "refs/heads/branch1");
		RevCommit secondCommit = git.commit().setMessage("second commit")
				.call();
		checkoutBranch("refs/heads/branch1");

		// when
		MergeCommand merge = git.merge();
		merge.setFastForward(FastForwardMode.NO_FF);
		merge.include(db.getRef(Constants.MASTER));
		merge.setCommit(false);
		MergeResult result = merge.call();

		// then
		assertEquals(MergeStatus.MERGED_NOT_COMMITTED, result.getMergeStatus());
		assertEquals(2, result.getMergedCommits().length);
		assertEquals(initialCommit, result.getMergedCommits()[0]);
		assertEquals(secondCommit, result.getMergedCommits()[1]);
		assertNull(result.getNewHead());
		assertEquals(RepositoryState.MERGING_RESOLVED, db.getRepositoryState());
	}

	@Test
	public void testFastForwardOnlyNotPossible() throws Exception {
		Git git = new Git(db);
		RevCommit initialCommit = git.commit().setMessage("initial commit")
				.call();
		createBranch(initialCommit, "refs/heads/branch1");
		git.commit().setMessage("second commit").call();
		checkoutBranch("refs/heads/branch1");
		writeTrashFile("file1", "branch1");
		git.add().addFilepattern("file").call();
		git.commit().setMessage("second commit on branch1").call();
		MergeCommand merge = git.merge();
		merge.setFastForward(FastForwardMode.FF_ONLY);
		merge.include(db.getRef(Constants.MASTER));
		MergeResult result = merge.call();

		assertEquals(MergeStatus.ABORTED, result.getMergeStatus());
	}
	private static void setExecutable(Git git, String path, boolean executable) {
		FS.DETECTED.setExecute(
				new File(git.getRepository().getWorkTree(), path), executable);
	}

	private static boolean canExecute(Git git, String path) {
		return FS.DETECTED.canExecute(new File(git.getRepository()
				.getWorkTree(), path));
	}

	private static RevCommit addAllAndCommit(final Git git) throws Exception {
		git.add().addFilepattern(".").call();
		return git.commit().setMessage("message").call();
	}

	private void checkMergeFailedResult(final MergeResult result,
			final MergeFailureReason reason,
			final String indexState, final File fileA) throws Exception {
		assertEquals(MergeStatus.FAILED, result.getMergeStatus());
		assertEquals(reason, result.getFailingPaths().get("a"));
		assertEquals("a(modified)", read(fileA));
		assertFalse(new File(db.getWorkTree(), "b").exists());
		assertEquals("c", read(new File(db.getWorkTree(), "c")));
		assertEquals(indexState, indexState(CONTENT));
		assertEquals(null, result.getConflicts());
		assertEquals(RepositoryState.SAFE, db.getRepositoryState());
	}
}
