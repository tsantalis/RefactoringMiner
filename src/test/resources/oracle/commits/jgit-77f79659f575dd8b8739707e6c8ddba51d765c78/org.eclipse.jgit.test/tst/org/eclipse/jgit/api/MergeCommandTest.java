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

import org.eclipse.jgit.errors.CheckoutConflictException;
import org.eclipse.jgit.errors.CorruptObjectException;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.GitIndex;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.RefUpdate;
import org.eclipse.jgit.lib.RepositoryTestCase;
import org.eclipse.jgit.lib.WorkDirCheckout;
import org.eclipse.jgit.lib.GitIndex.Entry;
import org.eclipse.jgit.revwalk.RevCommit;

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

		addNewFileToIndex("file1");
		RevCommit first = git.commit().setMessage("initial commit").call();

		assertTrue(new File(db.getWorkTree(), "file1").exists());
		createBranch(first, "refs/heads/branch1");

		addNewFileToIndex("file2");
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

		addNewFileToIndex("file1");
		RevCommit first = git.commit().setMessage("initial commit").call();
		createBranch(first, "refs/heads/branch1");

		addNewFileToIndex("file2");
		RevCommit second = git.commit().setMessage("second commit").call();

		addNewFileToIndex("file3");
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
		}
	}

	private void createBranch(ObjectId objectId, String branchName) throws IOException {
		RefUpdate updateRef = db.updateRef(branchName);
		updateRef.setNewObjectId(objectId);
		updateRef.update();
	}

	private void checkoutBranch(String branchName) throws Exception  {
		File workDir = db.getWorkTree();
		if (workDir != null) {
			WorkDirCheckout workDirCheckout = new WorkDirCheckout(db,
					workDir, db.mapTree(Constants.HEAD),
					db.getIndex(), db.mapTree(branchName));
			workDirCheckout.setFailOnConflict(true);
			try {
				workDirCheckout.checkout();
			} catch (CheckoutConflictException e) {
				throw new JGitInternalException(
						"Couldn't check out because of conflicts", e);
			}
		}

		// update the HEAD
		RefUpdate refUpdate = db.updateRef(Constants.HEAD);
		refUpdate.link(branchName);
	}

	private void addNewFileToIndex(String filename) throws IOException,
			CorruptObjectException {
		File writeTrashFile = writeTrashFile(filename, filename);

		GitIndex index = db.getIndex();
		Entry entry = index.add(db.getWorkTree(), writeTrashFile);
		entry.update(writeTrashFile);
		index.write();
	}
}
