/*
 * Copyright (C) 2010, Stefan Lay <stefan.lay@sap.com>
 * Copyright (C) 2010, Christian Halstrick <christian.halstrick@sap.com>
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

import java.io.File;
import java.io.IOException;
import java.util.Iterator;

import org.eclipse.jgit.api.MergeResult.MergeStatus;
import org.eclipse.jgit.dircache.DirCacheCheckout;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.RefUpdate;
import org.eclipse.jgit.lib.RepositoryState;
import org.eclipse.jgit.lib.RepositoryTestCase;
import org.eclipse.jgit.merge.MergeStrategy;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;

public class MergeCommandTest extends RepositoryTestCase {
	public void testMergeInItself() throws Exception {
		Git git = new Git(db);
		git.commit().setMessage("initial commit").call();

		MergeResult result = git.merge().include(db.getRef(Constants.HEAD)).call();
		assertEquals(MergeResult.MergeStatus.ALREADY_UP_TO_DATE, result.getMergeStatus());
	}

	public void testAlreadyUpToDate() throws Exception {
		Git git = new Git(db);
		RevCommit first = git.commit().setMessage("initial commit").call();
		createBranch(first, "refs/heads/branch1");

		RevCommit second = git.commit().setMessage("second commit").call();
		MergeResult result = git.merge().include(db.getRef("refs/heads/branch1")).call();
		assertEquals(MergeResult.MergeStatus.ALREADY_UP_TO_DATE, result.getMergeStatus());
		assertEquals(second, result.getNewHead());

	}

	public void testFastForward() throws Exception {
		Git git = new Git(db);
		RevCommit first = git.commit().setMessage("initial commit").call();
		createBranch(first, "refs/heads/branch1");

		RevCommit second = git.commit().setMessage("second commit").call();

		checkoutBranch("refs/heads/branch1");

		MergeResult result = git.merge().include(db.getRef(Constants.MASTER)).call();

		assertEquals(MergeResult.MergeStatus.FAST_FORWARD, result.getMergeStatus());
		assertEquals(second, result.getNewHead());
	}

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
	}

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

		assertTrue(2 == result.getMergedCommits().length);
		assertEquals(thirdCommit, result.getMergedCommits()[0]);
		assertEquals(secondCommit, result.getMergedCommits()[1]);

		Iterator<RevCommit> it = git.log().call().iterator();
		RevCommit newHead = it.next();
		assertEquals(newHead, result.getNewHead());
		assertEquals(2, newHead.getParentCount());
		assertEquals(thirdCommit, newHead.getParent(0));
		assertEquals(secondCommit, newHead.getParent(1));
		assertEquals(
				"merging 3fa334456d236a92db020289fe0bf481d91777b4 into HEAD",
				newHead.getFullMessage());
		// @TODO fix me
		assertEquals(RepositoryState.SAFE, db.getRepositoryState());
		// test index state
	}

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

		assertEquals(null, result.getConflicts());

		assertTrue(2 == result.getMergedCommits().length);
		assertEquals(thirdCommit, result.getMergedCommits()[0]);
		assertEquals(secondCommit, result.getMergedCommits()[1]);

		Iterator<RevCommit> it = git.log().call().iterator();
		RevCommit newHead = it.next();
		assertEquals(newHead, result.getNewHead());
		assertEquals(2, newHead.getParentCount());
		assertEquals(thirdCommit, newHead.getParent(0));
		assertEquals(secondCommit, newHead.getParent(1));
		assertEquals(
				"merging 064d54d98a4cdb0fed1802a21c656bfda67fe879 into HEAD",
				newHead.getFullMessage());

		assertEquals(RepositoryState.SAFE, db.getRepositoryState());
	}

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

	private void createBranch(ObjectId objectId, String branchName) throws IOException {
		RefUpdate updateRef = db.updateRef(branchName);
		updateRef.setNewObjectId(objectId);
		updateRef.update();
	}

	private void checkoutBranch(String branchName) throws IllegalStateException, IOException {
		RevWalk walk = new RevWalk(db);
		RevCommit head = walk.parseCommit(db.resolve(Constants.HEAD));
		RevCommit branch = walk.parseCommit(db.resolve(branchName));
		DirCacheCheckout dco = new DirCacheCheckout(db,
				head.getTree().getId(), db.lockDirCache(),
				branch.getTree().getId());
		dco.setFailOnConflict(true);
		dco.checkout();
		walk.release();
		// update the HEAD
		RefUpdate refUpdate = db.updateRef(Constants.HEAD);
		refUpdate.link(branchName);
	}
}
