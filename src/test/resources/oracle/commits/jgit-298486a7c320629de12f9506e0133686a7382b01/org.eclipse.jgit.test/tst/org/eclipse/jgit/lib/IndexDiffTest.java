/*
 * Copyright (C) 2007, Dave Watson <dwatson@mimvista.com>
 * Copyright (C) 2008, Robin Rosenberg <robin.rosenberg@dewire.com>
 * Copyright (C) 2008, Shawn O. Pearce <spearce@spearce.org>
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
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.TreeSet;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.MergeResult;
import org.eclipse.jgit.api.MergeResult.MergeStatus;
import org.eclipse.jgit.dircache.DirCache;
import org.eclipse.jgit.dircache.DirCacheEditor;
import org.eclipse.jgit.dircache.DirCacheEntry;
import org.eclipse.jgit.merge.MergeStrategy;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.treewalk.FileTreeIterator;
import org.junit.Test;

public class IndexDiffTest extends RepositoryTestCase {
	@Test
	public void testAdded() throws IOException {
		GitIndex index = new GitIndex(db);
		writeTrashFile("file1", "file1");
		writeTrashFile("dir/subfile", "dir/subfile");
		Tree tree = new Tree(db);
		tree.setId(insertTree(tree));

		index.add(trash, new File(trash, "file1"));
		index.add(trash, new File(trash, "dir/subfile"));
		index.write();
		FileTreeIterator iterator = new FileTreeIterator(db);
		IndexDiff diff = new IndexDiff(db, tree.getId(), iterator);
		diff.diff();
		assertEquals(2, diff.getAdded().size());
		assertTrue(diff.getAdded().contains("file1"));
		assertTrue(diff.getAdded().contains("dir/subfile"));
		assertEquals(0, diff.getChanged().size());
		assertEquals(0, diff.getModified().size());
		assertEquals(0, diff.getRemoved().size());
	}

	@Test
	public void testRemoved() throws IOException {
		writeTrashFile("file2", "file2");
		writeTrashFile("dir/file3", "dir/file3");

		Tree tree = new Tree(db);
		tree.addFile("file2");
		tree.addFile("dir/file3");
		assertEquals(2, tree.memberCount());
		tree.findBlobMember("file2").setId(ObjectId.fromString("30d67d4672d5c05833b7192cc77a79eaafb5c7ad"));
		Tree tree2 = (Tree) tree.findTreeMember("dir");
		tree2.findBlobMember("file3").setId(ObjectId.fromString("873fb8d667d05436d728c52b1d7a09528e6eb59b"));
		tree2.setId(insertTree(tree2));
		tree.setId(insertTree(tree));

		FileTreeIterator iterator = new FileTreeIterator(db);
		IndexDiff diff = new IndexDiff(db, tree.getId(), iterator);
		diff.diff();
		assertEquals(2, diff.getRemoved().size());
		assertTrue(diff.getRemoved().contains("file2"));
		assertTrue(diff.getRemoved().contains("dir/file3"));
		assertEquals(0, diff.getChanged().size());
		assertEquals(0, diff.getModified().size());
		assertEquals(0, diff.getAdded().size());
	}

	@Test
	public void testModified() throws IOException {
		GitIndex index = new GitIndex(db);


		index.add(trash, writeTrashFile("file2", "file2"));
		index.add(trash, writeTrashFile("dir/file3", "dir/file3"));
		index.write();

		writeTrashFile("dir/file3", "changed");

		Tree tree = new Tree(db);
		tree.addFile("file2").setId(ObjectId.fromString("0123456789012345678901234567890123456789"));
		tree.addFile("dir/file3").setId(ObjectId.fromString("0123456789012345678901234567890123456789"));
		assertEquals(2, tree.memberCount());

		Tree tree2 = (Tree) tree.findTreeMember("dir");
		tree2.setId(insertTree(tree2));
		tree.setId(insertTree(tree));
		FileTreeIterator iterator = new FileTreeIterator(db);
		IndexDiff diff = new IndexDiff(db, tree.getId(), iterator);
		diff.diff();
		assertEquals(2, diff.getChanged().size());
		assertTrue(diff.getChanged().contains("file2"));
		assertTrue(diff.getChanged().contains("dir/file3"));
		assertEquals(1, diff.getModified().size());
		assertTrue(diff.getModified().contains("dir/file3"));
		assertEquals(0, diff.getAdded().size());
		assertEquals(0, diff.getRemoved().size());
		assertEquals(0, diff.getMissing().size());
	}

	@Test
	public void testConflicting() throws Exception {
		Git git = new Git(db);

		writeTrashFile("a", "1\na\n3\n");
		writeTrashFile("b", "1\nb\n3\n");
		git.add().addFilepattern("a").addFilepattern("b").call();
		RevCommit initialCommit = git.commit().setMessage("initial").call();

		// create side branch with two modifications
		createBranch(initialCommit, "refs/heads/side");
		checkoutBranch("refs/heads/side");
		writeTrashFile("a", "1\na(side)\n3\n");
		writeTrashFile("b", "1\nb\n3\n(side)");
		git.add().addFilepattern("a").addFilepattern("b").call();
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

		FileTreeIterator iterator = new FileTreeIterator(db);
		IndexDiff diff = new IndexDiff(db, Constants.HEAD, iterator);
		diff.diff();

		assertEquals("[b]",
				new TreeSet<String>(diff.getChanged()).toString());
		assertEquals("[]", diff.getAdded().toString());
		assertEquals("[]", diff.getRemoved().toString());
		assertEquals("[]", diff.getMissing().toString());
		assertEquals("[]", diff.getModified().toString());
		assertEquals("[a]", diff.getConflicting().toString());
	}

	@Test
	public void testConflictingDeletedAndModified() throws Exception {
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

		FileTreeIterator iterator = new FileTreeIterator(db);
		IndexDiff diff = new IndexDiff(db, Constants.HEAD, iterator);
		diff.diff();

		assertEquals("[]", new TreeSet<String>(diff.getChanged()).toString());
		assertEquals("[]", diff.getAdded().toString());
		assertEquals("[]", diff.getRemoved().toString());
		assertEquals("[]", diff.getMissing().toString());
		assertEquals("[]", diff.getModified().toString());
		assertEquals("[a]", diff.getConflicting().toString());
	}

	@Test
	public void testConflictingFromMultipleCreations() throws Exception {
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

		FileTreeIterator iterator = new FileTreeIterator(db);
		IndexDiff diff = new IndexDiff(db, Constants.HEAD, iterator);
		diff.diff();

		assertEquals("[]", new TreeSet<String>(diff.getChanged()).toString());
		assertEquals("[]", diff.getAdded().toString());
		assertEquals("[]", diff.getRemoved().toString());
		assertEquals("[]", diff.getMissing().toString());
		assertEquals("[]", diff.getModified().toString());
		assertEquals("[b]", diff.getConflicting().toString());
	}

	@Test
	public void testUnchangedSimple() throws IOException {
		GitIndex index = new GitIndex(db);

		index.add(trash, writeTrashFile("a.b", "a.b"));
		index.add(trash, writeTrashFile("a.c", "a.c"));
		index.add(trash, writeTrashFile("a=c", "a=c"));
		index.add(trash, writeTrashFile("a=d", "a=d"));
		index.write();

		Tree tree = new Tree(db);
		// got the hash id'd from the data using echo -n a.b|git hash-object -t blob --stdin
		tree.addFile("a.b").setId(ObjectId.fromString("f6f28df96c2b40c951164286e08be7c38ec74851"));
		tree.addFile("a.c").setId(ObjectId.fromString("6bc0e647512d2a0bef4f26111e484dc87df7f5ca"));
		tree.addFile("a=c").setId(ObjectId.fromString("06022365ddbd7fb126761319633bf73517770714"));
		tree.addFile("a=d").setId(ObjectId.fromString("fa6414df3da87840700e9eeb7fc261dd77ccd5c2"));

		tree.setId(insertTree(tree));

		FileTreeIterator iterator = new FileTreeIterator(db);
		IndexDiff diff = new IndexDiff(db, tree.getId(), iterator);
		diff.diff();
		assertEquals(0, diff.getChanged().size());
		assertEquals(0, diff.getAdded().size());
		assertEquals(0, diff.getRemoved().size());
		assertEquals(0, diff.getMissing().size());
		assertEquals(0, diff.getModified().size());
	}

	/**
	 * This test has both files and directories that involve
	 * the tricky ordering used by Git.
	 *
	 * @throws IOException
	 */
	@Test
	public void testUnchangedComplex() throws IOException {
		GitIndex index = new GitIndex(db);

		index.add(trash, writeTrashFile("a.b", "a.b"));
		index.add(trash, writeTrashFile("a.c", "a.c"));
		index.add(trash, writeTrashFile("a/b.b/b", "a/b.b/b"));
		index.add(trash, writeTrashFile("a/b", "a/b"));
		index.add(trash, writeTrashFile("a/c", "a/c"));
		index.add(trash, writeTrashFile("a=c", "a=c"));
		index.add(trash, writeTrashFile("a=d", "a=d"));
		index.write();

		Tree tree = new Tree(db);
		// got the hash id'd from the data using echo -n a.b|git hash-object -t blob --stdin
		tree.addFile("a.b").setId(ObjectId.fromString("f6f28df96c2b40c951164286e08be7c38ec74851"));
		tree.addFile("a.c").setId(ObjectId.fromString("6bc0e647512d2a0bef4f26111e484dc87df7f5ca"));
		tree.addFile("a/b.b/b").setId(ObjectId.fromString("8d840bd4e2f3a48ff417c8e927d94996849933fd"));
		tree.addFile("a/b").setId(ObjectId.fromString("db89c972fc57862eae378f45b74aca228037d415"));
		tree.addFile("a/c").setId(ObjectId.fromString("52ad142a008aeb39694bafff8e8f1be75ed7f007"));
		tree.addFile("a=c").setId(ObjectId.fromString("06022365ddbd7fb126761319633bf73517770714"));
		tree.addFile("a=d").setId(ObjectId.fromString("fa6414df3da87840700e9eeb7fc261dd77ccd5c2"));

		Tree tree3 = (Tree) tree.findTreeMember("a/b.b");
		tree3.setId(insertTree(tree3));
		Tree tree2 = (Tree) tree.findTreeMember("a");
		tree2.setId(insertTree(tree2));
		tree.setId(insertTree(tree));

		FileTreeIterator iterator = new FileTreeIterator(db);
		IndexDiff diff = new IndexDiff(db, tree.getId(), iterator);
		diff.diff();
		assertEquals(0, diff.getChanged().size());
		assertEquals(0, diff.getAdded().size());
		assertEquals(0, diff.getRemoved().size());
		assertEquals(0, diff.getMissing().size());
		assertEquals(0, diff.getModified().size());
	}

	private ObjectId insertTree(Tree tree) throws IOException {
		ObjectInserter oi = db.newObjectInserter();
		try {
			ObjectId id = oi.insert(Constants.OBJ_TREE, tree.format());
			oi.flush();
			return id;
		} finally {
			oi.release();
		}
	}

	/**
	 * A file is removed from the index but stays in the working directory. It
	 * is checked if IndexDiff detects this file as removed and untracked.
	 *
	 * @throws Exception
	 */
	@Test
	public void testRemovedUntracked() throws Exception{
		Git git = new Git(db);
		String path = "file";
		writeTrashFile(path, "content");
		git.add().addFilepattern(path).call();
		git.commit().setMessage("commit").call();
		removeFromIndex(path);
		FileTreeIterator iterator = new FileTreeIterator(db);
		IndexDiff diff = new IndexDiff(db, Constants.HEAD, iterator);
		diff.diff();
		assertTrue(diff.getRemoved().contains(path));
		assertTrue(diff.getUntracked().contains(path));
	}

	@Test
	public void testAssumeUnchanged() throws Exception {
		Git git = new Git(db);
		String path = "file";
		writeTrashFile(path, "content");
		git.add().addFilepattern(path).call();
		String path2 = "file2";
		writeTrashFile(path2, "content");
		git.add().addFilepattern(path2).call();
		git.commit().setMessage("commit").call();
		assumeUnchanged(path2);
		writeTrashFile(path, "more content");
		writeTrashFile(path2, "more content");

		FileTreeIterator iterator = new FileTreeIterator(db);
		IndexDiff diff = new IndexDiff(db, Constants.HEAD, iterator);
		diff.diff();
		assertEquals(1, diff.getAssumeUnchanged().size());
		assertEquals(1, diff.getModified().size());
		assertEquals(0, diff.getChanged().size());
		assertTrue(diff.getAssumeUnchanged().contains("file2"));
		assertTrue(diff.getModified().contains("file"));

		git.add().addFilepattern(".").call();

		iterator = new FileTreeIterator(db);
		diff = new IndexDiff(db, Constants.HEAD, iterator);
		diff.diff();
		assertEquals(1, diff.getAssumeUnchanged().size());
		assertEquals(0, diff.getModified().size());
		assertEquals(1, diff.getChanged().size());
		assertTrue(diff.getAssumeUnchanged().contains("file2"));
		assertTrue(diff.getChanged().contains("file"));
	}

	private void removeFromIndex(String path) throws IOException {
		final DirCache dirc = db.lockDirCache();
		final DirCacheEditor edit = dirc.editor();
		edit.add(new DirCacheEditor.DeletePath(path));
		if (!edit.commit())
			throw new IOException("could not commit");
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

}
