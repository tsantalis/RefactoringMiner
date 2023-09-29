/*
 * Copyright (C) 2007, Dave Watson <dwatson@mimvista.com>
 * Copyright (C) 2008, Robin Rosenberg <robin.rosenberg@dewire.com>
 * Copyright (C) 2008, Roger C. Soares <rogersoares@intelinet.com.br>
 * Copyright (C) 2006, Shawn O. Pearce <spearce@spearce.org>
 * Copyright (C) 2010, Chrisian Halstrick <christian.halstrick@sap.com> and
 * other copyright owners as documented in the project's IP log.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v1.0 which accompanies this
 * distribution, is reproduced below, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * - Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * - Neither the name of the Eclipse Foundation, Inc. nor the names of its
 * contributors may be used to endorse or promote products derived from this
 * software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package org.eclipse.jgit.dircache;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jgit.JGitText;
import org.eclipse.jgit.errors.CheckoutConflictException;
import org.eclipse.jgit.errors.CorruptObjectException;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.IndexWriteException;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.FileMode;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.treewalk.AbstractTreeIterator;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.treewalk.EmptyTreeIterator;
import org.eclipse.jgit.treewalk.FileTreeIterator;
import org.eclipse.jgit.treewalk.NameConflictTreeWalk;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.WorkingTreeIterator;
import org.eclipse.jgit.treewalk.WorkingTreeOptions;
import org.eclipse.jgit.treewalk.filter.PathFilter;
import org.eclipse.jgit.util.FS;

/**
 * This class handles checking out one or two trees merging with the index. This
 * class does similar things as {@code WorkDirCheckout} but uses
 * {@link DirCache} instead of {@code GitIndex}
 * <p>
 * The initial implementation of this class was refactored from
 * WorkDirCheckout}.
 */
public class DirCacheCheckout {
	private Repository repo;

	private HashMap<String, ObjectId> updated = new HashMap<String, ObjectId>();

	private ArrayList<String> conflicts = new ArrayList<String>();

	private ArrayList<String> removed = new ArrayList<String>();

	private ObjectId mergeCommitTree;

	private DirCache dc;

	private DirCacheBuilder builder;

	private NameConflictTreeWalk walk;

	private ObjectId headCommitTree;

	private WorkingTreeIterator workingTree;

	private boolean failOnConflict = true;

	private ArrayList<String> toBeDeleted = new ArrayList<String>();

	/**
	 * @return a list of updated pathes and objectIds
	 */
	public Map<String, ObjectId> getUpdated() {
		return updated;
	}

	/**
	 * @return a list of conflicts created by this checkout
	 */
	public List<String> getConflicts() {
		return conflicts;
	}

	/**
	 * @return a list of paths (relative to the start of the working tree) of
	 *         files which couldn't be deleted during last call to
	 *         {@link #checkout()} . {@link #checkout()} detected that these
	 *         files should be deleted but the deletion in the filesystem failed
	 *         (e.g. because a file was locked). To have a consistent state of
	 *         the working tree these files have to be deleted by the callers of
	 *         {@link DirCacheCheckout}.
	 */
	public List<String> getToBeDeleted() {
		return conflicts;
	}

	/**
	 * @return a list of all files removed by this checkout
	 */
	public List<String> getRemoved() {
		return removed;
	}

	/**
	 * Constructs a DirCacheCeckout for fast-forwarding from one tree to
	 * another, merging it with the index
	 *
	 * @param repo
	 *            the repository in which we do the checkout
	 * @param headCommitTree
	 *            the id of the tree of the head commit
	 * @param dc
	 *            the (already locked) Dircache for this repo
	 * @param mergeCommitTree
	 *            the id of the tree we want to fast-forward to
	 * @param workingTree
	 *            an iterator over the repositories Working Tree
	 * @throws IOException
	 */
	public DirCacheCheckout(Repository repo, ObjectId headCommitTree, DirCache dc,
			ObjectId mergeCommitTree, WorkingTreeIterator workingTree)
			throws IOException {
		this.repo = repo;
		this.dc = dc;
		this.headCommitTree = headCommitTree;
		this.mergeCommitTree = mergeCommitTree;
		this.workingTree = workingTree;
	}

	/**
	 * Constructs a DirCacheCeckout for checking out one tree, merging with the
	 * index. As iterator over the working tree this constructor creates a
	 * standard {@link FileTreeIterator}
	 *
	 * @param repo
	 *            the repository in which we do the checkout
	 * @param headCommitTree
	 *            the id of the tree of the head commit
	 * @param dc
	 *            the (already locked) Dircache for this repo
	 * @param mergeCommitTree
	 *            the id of the tree of the
	 * @throws IOException
	 */
	public DirCacheCheckout(Repository repo, ObjectId headCommitTree, DirCache dc,
			ObjectId mergeCommitTree) throws IOException {
		this(repo, headCommitTree, dc, mergeCommitTree, new FileTreeIterator(
				repo.getWorkTree(), repo.getFS(),
				WorkingTreeOptions.createDefaultInstance()));
	}

	/**
	 * Scan head, index and merge tree. Used during normal checkout or merge
	 * operations.
	 *
	 * @throws CorruptObjectException
	 * @throws IOException
	 */
	public void preScanTwoTrees() throws CorruptObjectException, IOException {
		removed.clear();
		updated.clear();
		conflicts.clear();
		walk = new NameConflictTreeWalk(repo);
		builder = dc.builder();

		walk.reset();
		addTree(walk, headCommitTree);
		addTree(walk, mergeCommitTree);
		walk.addTree(new DirCacheBuildIterator(builder));
		walk.addTree(workingTree);

		while (walk.next()) {
			processEntry(walk.getTree(0, CanonicalTreeParser.class),
					walk.getTree(1, CanonicalTreeParser.class),
					walk.getTree(2, DirCacheBuildIterator.class),
					walk.getTree(3, WorkingTreeIterator.class));
			if (walk.isSubtree())
				walk.enterSubtree();
		}
	}

	private void addTree(TreeWalk tw, ObjectId id) throws MissingObjectException, IncorrectObjectTypeException, IOException {
		if (id == null)
			tw.addTree(new EmptyTreeIterator());
		else
			tw.addTree(id);
	}

	/**
	 * Scan index and merge tree (no HEAD). Used e.g. for initial checkout when
	 * there is no head yet.
	 *
	 * @throws MissingObjectException
	 * @throws IncorrectObjectTypeException
	 * @throws CorruptObjectException
	 * @throws IOException
	 */
	public void prescanOneTree()
			throws MissingObjectException, IncorrectObjectTypeException,
			CorruptObjectException, IOException {
		removed.clear();
		updated.clear();
		conflicts.clear();

		builder = dc.builder();

		walk = new NameConflictTreeWalk(repo);
		walk.reset();
		walk.addTree(mergeCommitTree);
		walk.addTree(new DirCacheBuildIterator(builder));
		walk.addTree(workingTree);

		while (walk.next()) {
			processEntry(walk.getTree(0, CanonicalTreeParser.class),
					walk.getTree(1, DirCacheBuildIterator.class),
					walk.getTree(2, WorkingTreeIterator.class));
			if (walk.isSubtree())
				walk.enterSubtree();
		}
		conflicts.removeAll(removed);
	}

	/**
	 * Processing an entry in the context of {@link #prescanOneTree()} when only
	 * one tree is given
	 *
	 * @param m the tree to merge
	 * @param i the index
	 * @param f the working tree
	 */
	void processEntry(CanonicalTreeParser m, DirCacheBuildIterator i,
			WorkingTreeIterator f) {
		if (m != null) {
			update(m.getEntryPathString(), m.getEntryObjectId(),
					m.getEntryFileMode());
		} else {
			if (f != null) {
				if (walk.isDirectoryFileConflict()) {
					conflicts.add(walk.getPathString());
				} else {
					// ... and the working dir contained a file or folder ->
					// add it to the removed set and remove it from conflicts set
					remove(i.getEntryPathString());
					conflicts.remove(i.getEntryPathString());
				}
			} else
				keep(i.getDirCacheEntry());
		}
	}

	/**
	 * Execute this checkout
	 *
	 * @return <code>false</code> if this method could not delete all the files
	 *         which should be deleted (e.g. because of of the files was
	 *         locked). In this case {@link #getToBeDeleted()} lists the files
	 *         which should be tried to be deleted outside of this method.
	 *         Although <code>false</code> is returned the checkout was
	 *         successful and the working tree was updated for all other files.
	 *         <code>true</code> is returned when no such problem occurred
	 *
	 * @throws IOException
	 */
	public boolean checkout() throws IOException {
		toBeDeleted.clear();
		if (headCommitTree != null)
			preScanTwoTrees();
		else
			prescanOneTree();

		if (!conflicts.isEmpty()) {
			if (failOnConflict) {
				dc.unlock();
				throw new CheckoutConflictException(conflicts.toArray(new String[conflicts.size()]));
			} else
				cleanUpConflicts();
		}

		// update our index
		builder.finish();

		File file=null;
		String last = "";
		for (String r : removed) {
			file = new File(repo.getWorkTree(), r);
			if (!file.delete())
				toBeDeleted.add(r);
			else {
				if (!isSamePrefix(r, last))
					removeEmptyParents(file);
				last = r;
			}
		}
		if (file != null)
			removeEmptyParents(file);

		for (String path : updated.keySet()) {
			// ... create/overwrite this file ...
			file = new File(repo.getWorkTree(), path);
			file.getParentFile().mkdirs();
			file.createNewFile();
			DirCacheEntry entry = dc.getEntry(path);
			checkoutEntry(repo, file, entry, config_filemode());
		}


		// commit the index builder - a new index is persisted
		if (!builder.commit()) {
			dc.unlock();
			throw new IndexWriteException();
		}

		return toBeDeleted == null;
	}

	private static boolean isSamePrefix(String a, String b) {
		int as = a.lastIndexOf('/');
		int bs = b.lastIndexOf('/');
		return a.substring(0, as + 1).equals(b.substring(0, bs + 1));
	}

	 private void removeEmptyParents(File f) {
		File parentFile = f.getParentFile();

		while (!parentFile.equals(repo.getWorkTree())) {
			if (!parentFile.delete())
				break;
			parentFile = parentFile.getParentFile();
		}
	}

	/**
	 * Here the main work is done. This method is called for each existing path
	 * in head, index and merge. This method decides what to do with the
	 * corresponding index entry: keep it, update it, remove it or mark a
	 * conflict.
	 *
	 * @param h
	 *            the entry for the head
	 * @param m
	 *            the entry for the merge
	 * @param i
	 *            the entry for the index
	 * @param f
	 *            the file in the working tree
	 * @throws IOException
	 */

	void processEntry(AbstractTreeIterator h, AbstractTreeIterator m,
			DirCacheBuildIterator i, WorkingTreeIterator f) throws IOException {
		DirCacheEntry dce;

		String name = walk.getPathString();

		if (i == null && m == null && h == null) {
			// File/Directory conflict case #20
			if (walk.isDirectoryFileConflict())
				// TODO: check whether it is always correct to report a conflict here
				conflict(name, null, h, m);

			// file only exists in working tree -> ignore it
			return;
		}

		ObjectId iId = (i == null ? null : i.getEntryObjectId());
		ObjectId mId = (m == null ? null : m.getEntryObjectId());
		ObjectId hId = (h == null ? null : h.getEntryObjectId());

		/**
		 * <pre>
		 *  File/Directory conflicts:
		 *  the following table from ReadTreeTest tells what to do in case of directory/file
		 *  conflicts. I give comments here
		 *
		 *      H        I       M     Clean     H==M     H==I    I==M         Result
		 *      ------------------------------------------------------------------
		 * 1    D        D       F       Y         N       Y       N           Update
		 * 2    D        D       F       N         N       Y       N           Conflict
		 * 3    D        F       D                 Y       N       N           Update
		 * 4    D        F       D                 N       N       N           Update
		 * 5    D        F       F       Y         N       N       Y           Keep
		 * 6    D        F       F       N         N       N       Y           Keep
		 * 7    F        D       F       Y         Y       N       N           Update
		 * 8    F        D       F       N         Y       N       N           Conflict
		 * 9    F        D       F       Y         N       N       N           Update
		 * 10   F        D       D                 N       N       Y           Keep
		 * 11   F        D       D                 N       N       N           Conflict
		 * 12   F        F       D       Y         N       Y       N           Update
		 * 13   F        F       D       N         N       Y       N           Conflict
		 * 14   F        F       D                 N       N       N           Conflict
		 * 15   0        F       D                 N       N       N           Conflict
		 * 16   0        D       F       Y         N       N       N           Update
		 * 17   0        D       F                 N       N       N           Conflict
		 * 18   F        0       D                                             Update
		 * 19   D        0       F                                             Update
		 * 20   0        0       F       N (worktree=dir)                      Conflict
		 * </pre>
		 */

		// The information whether head,index,merge iterators are currently
		// pointing to file/folder/non-existing is encoded into this variable.
		//
		// To decode write down ffMask in hexadecimal form. The last digit
		// represents the state for the merge iterator, the second last the
		// state for the index iterator and the third last represents the state
		// for the head iterator. The hexadecimal constant "F" stands for
		// "file",
		// an "D" stands for "directory" (tree), and a "0" stands for
		// non-existing
		//
		// Examples:
		// ffMask == 0xFFD -> Head=File, Index=File, Merge=Tree
		// ffMask == 0xDD0 -> Head=Tree, Index=Tree, Merge=Non-Existing

		int ffMask = 0;
		if (h != null)
			ffMask = FileMode.TREE.equals(h.getEntryFileMode()) ? 0xD00 : 0xF00;
		if (i != null)
			ffMask |= FileMode.TREE.equals(i.getEntryFileMode()) ? 0x0D0
					: 0x0F0;
		if (m != null)
			ffMask |= FileMode.TREE.equals(m.getEntryFileMode()) ? 0x00D
					: 0x00F;

		// Check whether we have a possible file/folder conflict. Therefore we
		// need a least one file and one folder.
		if (((ffMask & 0x222) != 0x000)
				&& (((ffMask & 0x00F) == 0x00D) || ((ffMask & 0x0F0) == 0x0D0) || ((ffMask & 0xF00) == 0xD00))) {

			// There are 3*3*3=27 possible combinations of file/folder
			// conflicts. Some of them are not-relevant because
			// they represent no conflict, e.g. 0xFFF, 0xDDD, ... The following
			// switch processes all relevant cases.
			switch (ffMask) {
			case 0xDDF: // 1 2
				if (isModified(name)) {
					conflict(name, i.getDirCacheEntry(), h, m); // 1
				} else {
					update(name, m.getEntryObjectId(), m.getEntryFileMode()); // 2
				}

				break;
			case 0xDFD: // 3 4
				// CAUTION: I put it into removed instead of updated, because
				// that's what our tests expect
				// updated.put(name, mId);
				remove(name);
				break;
			case 0xF0D: // 18
				remove(name);
				break;
			case 0xDFF: // 5 6
			case 0xFDD: // 10 11
				// TODO: make use of tree extension as soon as available in jgit
				// we would like to do something like
				// if (!iId.equals(mId))
				//   conflict(name, i.getDirCacheEntry(), h, m);
				// But since we don't know the id of a tree in the index we do
				// nothing here and wait that conflicts between index and merge
				// are found later
				break;
			case 0xD0F: // 19
				update(name, mId, m.getEntryFileMode());
				break;
			case 0xDF0: // conflict without a rule
			case 0x0FD: // 15
				conflict(name, (i != null) ? i.getDirCacheEntry() : null, h, m);
				break;
			case 0xFDF: // 7 8 9
				dce = i.getDirCacheEntry();
				if (hId.equals(mId)) {
					if (isModified(name))
						conflict(name, i.getDirCacheEntry(), h, m); // 8
					else
						update(name, mId, m.getEntryFileMode()); // 7
				} else if (!isModified(name))
					update(name, mId, m.getEntryFileMode());  // 9
				else
					// To be confirmed - this case is not in the table.
					conflict(name, i.getDirCacheEntry(), h, m);
				break;
			case 0xFD0: // keep without a rule
				keep(i.getDirCacheEntry());
				break;
			case 0xFFD: // 12 13 14
				if (hId.equals(iId)) {
					dce = i.getDirCacheEntry();
					if (f == null
							|| f.isModified(dce, true, config_filemode(),
									repo.getFS()))
						conflict(name, i.getDirCacheEntry(), h, m);
					else
						remove(name);
				} else
					conflict(name, i.getDirCacheEntry(), h, m);
				break;
			case 0x0DF: // 16 17
				if (!isModified(name))
					update(name, mId, m.getEntryFileMode());
				else
					conflict(name, i.getDirCacheEntry(), h, m);
				break;
			default:
				keep(i.getDirCacheEntry());
			}
			return;
		}

		// if we have no file at all then there is nothing to do
		if ((ffMask & 0x222) == 0)
			return;

		if ((ffMask == 0x00F) && f != null && FileMode.TREE.equals(f.getEntryFileMode())) {
			// File/Directory conflict case #20
			conflict(name, null, h, m);
		}

		if (i == null) {
			/**
			 * <pre>
			 * 		    I (index)                H        M        Result
			 * 	        -------------------------------------------------------
			 * 	        0 nothing             nothing  nothing  (does not happen)
			 * 	        1 nothing             nothing  exists   use M
			 * 	        2 nothing             exists   nothing  remove path from index
			 * 	        3 nothing             exists   exists   use M
			 * </pre>
			 */

			if (h == null)
				update(name, mId, m.getEntryFileMode()); // 1
			else if (m == null)
				remove(name); // 2
			else
				update(name, mId, m.getEntryFileMode()); // 3
		} else {
			dce = i.getDirCacheEntry();
			if (h == null) {
				/**
				 * <pre>
				 * 	          clean I==H  I==M       H        M        Result
				 * 	         -----------------------------------------------------
				 * 	        4 yes   N/A   N/A     nothing  nothing  keep index
				 * 	        5 no    N/A   N/A     nothing  nothing  keep index
				 *
				 * 	        6 yes   N/A   yes     nothing  exists   keep index
				 * 	        7 no    N/A   yes     nothing  exists   keep index
				 * 	        8 yes   N/A   no      nothing  exists   fail
				 * 	        9 no    N/A   no      nothing  exists   fail
				 * </pre>
				 */

				if (m == null || mId.equals(iId)) {
					if (m==null && walk.isDirectoryFileConflict()) {
						if (dce != null
								&& (f == null || f.isModified(dce, true,
										config_filemode(), repo.getFS())))
							conflict(name, i.getDirCacheEntry(), h, m);
						else
							remove(name);
					} else
						keep(i.getDirCacheEntry());
				} else
					conflict(name, i.getDirCacheEntry(), h, m);
			} else if (m == null) {

				/**
				 * <pre>
				 * 	           clean I==H  I==M       H        M        Result
				 * 	         -----------------------------------------------------
				 * 	        10 yes   yes   N/A     exists   nothing  remove path from index
				 * 	        11 no    yes   N/A     exists   nothing  fail
				 * 	        12 yes   no    N/A     exists   nothing  fail
				 * 	        13 no    no    N/A     exists   nothing  fail
				 * </pre>
				 */

				if (hId.equals(iId)) {
					if (f == null
							|| f.isModified(dce, true, config_filemode(),
									repo.getFS()))
						conflict(name, i.getDirCacheEntry(), h, m);
					else
						remove(name);
				} else
					conflict(name, i.getDirCacheEntry(), h, m);
			} else {
				if (!hId.equals(mId) && !hId.equals(iId) && !mId.equals(iId))
					conflict(name, i.getDirCacheEntry(), h, m);
				else if (hId.equals(iId) && !mId.equals(iId)) {
					if (dce != null
							&& (f == null || f.isModified(dce, true,
									config_filemode(), repo.getFS())))
						conflict(name, i.getDirCacheEntry(), h, m);
					else
						update(name, mId, m.getEntryFileMode());
				} else {
					keep(i.getDirCacheEntry());
				}
			}
		}
	}

	/**
	 * A conflict is detected - add the three different stages to the index
	 * @param path the path of the conflicting entry
	 * @param e the previous index entry
	 * @param h the first tree you want to merge (the HEAD)
	 * @param m the second tree you want to merge
	 */
	private void conflict(String path, DirCacheEntry e, AbstractTreeIterator h, AbstractTreeIterator m) {
		conflicts.add(path);

		DirCacheEntry entry;
		if (e != null) {
			entry = new DirCacheEntry(e.getPathString(), DirCacheEntry.STAGE_1);
			entry.copyMetaData(e);
			builder.add(entry);
		}

		if (h != null && !FileMode.TREE.equals(h.getEntryFileMode())) {
			entry = new DirCacheEntry(h.getEntryPathString(), DirCacheEntry.STAGE_2);
			entry.setFileMode(h.getEntryFileMode());
			entry.setObjectId(h.getEntryObjectId());
			builder.add(entry);
		}

		if (m != null && !FileMode.TREE.equals(m.getEntryFileMode())) {
			entry = new DirCacheEntry(m.getEntryPathString(), DirCacheEntry.STAGE_3);
			entry.setFileMode(m.getEntryFileMode());
			entry.setObjectId(m.getEntryObjectId());
			builder.add(entry);
		}
	}

	private void keep(DirCacheEntry e) {
		if (e != null && !FileMode.TREE.equals(e.getFileMode()))
			builder.add(e);
	}

	private void remove(String path) {
		removed.add(path);
	}

	private void update(String path, ObjectId mId, FileMode mode) {
		if (!FileMode.TREE.equals(mode)) {
			updated.put(path, mId);
			DirCacheEntry entry = new DirCacheEntry(path, DirCacheEntry.STAGE_0);
			entry.setObjectId(mId);
			entry.setFileMode(mode);
			builder.add(entry);
		}
	}

	private Boolean filemode;

	private boolean config_filemode() {
		// TODO: temporary till we can actually set parameters. We need to be
		// able to change this for testing.
		if (filemode == null) {
			StoredConfig config = repo.getConfig();
			filemode = Boolean.valueOf(config.getBoolean("core", null,
					"filemode", true));
		}
		return filemode.booleanValue();
	}

	/**
	 * If <code>true</code>, will scan first to see if it's possible to check
	 * out, otherwise throw {@link CheckoutConflictException}. If
	 * <code>false</code>, it will silently deal with the problem.
	 *
	 * @param failOnConflict
	 */
	public void setFailOnConflict(boolean failOnConflict) {
		this.failOnConflict = failOnConflict;
	}

	/**
	 * This method implements how to handle conflicts when
	 * {@link #failOnConflict} is false
	 *
	 * @throws CheckoutConflictException
	 */
	private void cleanUpConflicts() throws CheckoutConflictException {
		// TODO: couldn't we delete unsaved worktree content here?
		for (String c : conflicts) {
			File conflict = new File(repo.getWorkTree(), c);
			if (!conflict.delete())
				throw new CheckoutConflictException(MessageFormat.format(
						JGitText.get().cannotDeleteFile, c));
			removeEmptyParents(conflict);
		}
		for (String r : removed) {
			File file = new File(repo.getWorkTree(), r);
			file.delete();
			removeEmptyParents(file);
		}
	}

	private boolean isModified(String path) throws CorruptObjectException, IOException {
		NameConflictTreeWalk tw = new NameConflictTreeWalk(repo);
		tw.reset();
		tw.addTree(new DirCacheIterator(dc));
		tw.addTree(new FileTreeIterator(repo.getWorkTree(), repo.getFS(),
				WorkingTreeOptions.createDefaultInstance()));
		tw.setRecursive(true);
		tw.setFilter(PathFilter.create(path));
		DirCacheIterator dcIt;
		WorkingTreeIterator wtIt;
		while(tw.next()) {
			dcIt = tw.getTree(0, DirCacheIterator.class);
			wtIt = tw.getTree(1, WorkingTreeIterator.class);
			if (dcIt == null || wtIt == null)
				return true;
			if (wtIt.isModified(dcIt.getDirCacheEntry(), true, config_filemode(), repo.getFS())) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Updates the file in the working tree with content and mode from an entry
	 * in the index. The new content is first written to a new temporary file in
	 * the same directory as the real file. Then that new file is renamed to the
	 * final filename.
	 *
	 * TODO: this method works directly on File IO, we may need another
	 * abstraction (like WorkingTreeIterator). This way we could tell e.g.
	 * Eclipse that Files in the workspace got changed
	 * @param repo
	 * @param f
	 *            the file to be modified. The parent directory for this file
	 *            has to exist already
	 * @param entry
	 *            the entry containing new mode and content
	 * @param config_filemode
	 *            whether the mode bits should be handled at all.
	 * @throws IOException
	 */
	public static void checkoutEntry(final Repository repo, File f, DirCacheEntry entry,
			boolean config_filemode) throws IOException {
		ObjectLoader ol = repo.open(entry.getObjectId());
		if (ol == null)
			throw new MissingObjectException(entry.getObjectId(),
					Constants.TYPE_BLOB);

		byte[] bytes = ol.getCachedBytes();

		File parentDir = f.getParentFile();
		File tmpFile = File.createTempFile("._" + f.getName(), null, parentDir);
		FileChannel channel = new FileOutputStream(tmpFile).getChannel();
		ByteBuffer buffer = ByteBuffer.wrap(bytes);
		try {
			int j = channel.write(buffer);
			if (j != bytes.length)
				throw new IOException(MessageFormat.format(
						JGitText.get().couldNotWriteFile, tmpFile));
		} finally {
			channel.close();
		}
		FS fs = repo.getFS();
		if (config_filemode && fs.supportsExecute()) {
			if (FileMode.EXECUTABLE_FILE.equals(entry.getRawMode())) {
				if (!fs.canExecute(tmpFile))
					fs.setExecute(tmpFile, true);
			} else {
				if (fs.canExecute(tmpFile))
					fs.setExecute(tmpFile, false);
			}
		}
		if (!tmpFile.renameTo(f)) {
			// tried to rename which failed. Let' delete the target file and try
			// again
			f.delete();
			if (!tmpFile.renameTo(f)) {
				throw new IOException(MessageFormat.format(
						JGitText.get().couldNotWriteFile, tmpFile.getPath(),
						f.getPath()));
			}
		}
		entry.setLastModified(f.lastModified());
		entry.setLength((int) ol.getSize());
	}
}
