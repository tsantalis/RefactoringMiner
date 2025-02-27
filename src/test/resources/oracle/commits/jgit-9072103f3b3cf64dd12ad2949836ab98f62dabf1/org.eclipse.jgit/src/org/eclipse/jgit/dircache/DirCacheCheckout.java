/*
 * Copyright (C) 2007, Dave Watson <dwatson@mimvista.com>
 * Copyright (C) 2008, Robin Rosenberg <robin.rosenberg@dewire.com>
 * Copyright (C) 2008, Roger C. Soares <rogersoares@intelinet.com.br>
 * Copyright (C) 2006, Shawn O. Pearce <spearce@spearce.org>
 * Copyright (C) 2010, Chrisian Halstrick <christian.halstrick@sap.com>
 * Copyright (C) 2019, 2020, Andre Bossert <andre.bossert@siemens.com>
 * Copyright (C) 2017, 2023, Thomas Wolf <twolf@apache.org> and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0 which is available at
 * https://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package org.eclipse.jgit.dircache;

import static org.eclipse.jgit.treewalk.TreeWalk.OperationType.CHECKOUT_OP;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.MessageFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.jgit.api.errors.CanceledException;
import org.eclipse.jgit.api.errors.FilterFailedException;
import org.eclipse.jgit.attributes.FilterCommand;
import org.eclipse.jgit.attributes.FilterCommandRegistry;
import org.eclipse.jgit.errors.CheckoutConflictException;
import org.eclipse.jgit.errors.CorruptObjectException;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.IndexWriteException;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.events.WorkingTreeModifiedEvent;
import org.eclipse.jgit.internal.JGitText;
import org.eclipse.jgit.lib.ConfigConstants;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.CoreConfig.AutoCRLF;
import org.eclipse.jgit.lib.CoreConfig.EolStreamType;
import org.eclipse.jgit.lib.FileMode;
import org.eclipse.jgit.lib.NullProgressMonitor;
import org.eclipse.jgit.lib.ObjectChecker;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.ProgressMonitor;
import org.eclipse.jgit.lib.Repository;
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
import org.eclipse.jgit.util.FS.ExecutionResult;
import org.eclipse.jgit.util.IntList;
import org.eclipse.jgit.util.SystemReader;
import org.eclipse.jgit.util.io.EolStreamTypeUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class handles checking out one or two trees merging with the index.
 */
public class DirCacheCheckout {
	private static final Logger LOG = LoggerFactory
			.getLogger(DirCacheCheckout.class);

	private static final int MAX_EXCEPTION_TEXT_SIZE = 10 * 1024;

	/**
	 * Metadata used in checkout process
	 *
	 * @since 4.3
	 */
	public static class CheckoutMetadata {
		/** git attributes */
		public final EolStreamType eolStreamType;

		/** filter command to apply */
		public final String smudgeFilterCommand;

		/**
		 * @param eolStreamType
		 * @param smudgeFilterCommand
		 */
		public CheckoutMetadata(EolStreamType eolStreamType,
				String smudgeFilterCommand) {
			this.eolStreamType = eolStreamType;
			this.smudgeFilterCommand = smudgeFilterCommand;
		}

		static CheckoutMetadata EMPTY = new CheckoutMetadata(
				EolStreamType.DIRECT, null);
	}

	private Repository repo;

	private Map<String, CheckoutMetadata> updated = new LinkedHashMap<>();

	private ArrayList<String> conflicts = new ArrayList<>();

	private ArrayList<String> removed = new ArrayList<>();

	private ArrayList<String> kept = new ArrayList<>();

	private ObjectId mergeCommitTree;

	private DirCache dc;

	private DirCacheBuilder builder;

	private NameConflictTreeWalk walk;

	private ObjectId headCommitTree;

	private WorkingTreeIterator workingTree;

	private boolean failOnConflict = true;

	private boolean force = false;

	private ArrayList<String> toBeDeleted = new ArrayList<>();

	private boolean initialCheckout;

	private boolean performingCheckout;

	private Checkout checkout;

	private ProgressMonitor monitor = NullProgressMonitor.INSTANCE;

	/**
	 * Get list of updated paths and smudgeFilterCommands
	 *
	 * @return a list of updated paths and smudgeFilterCommands
	 */
	public Map<String, CheckoutMetadata> getUpdated() {
		return updated;
	}

	/**
	 * Get a list of conflicts created by this checkout
	 *
	 * @return a list of conflicts created by this checkout
	 */
	public List<String> getConflicts() {
		return conflicts;
	}

	/**
	 * Get list of paths of files which couldn't be deleted during last call to
	 * {@link #checkout()}
	 *
	 * @return a list of paths (relative to the start of the working tree) of
	 *         files which couldn't be deleted during last call to
	 *         {@link #checkout()} . {@link #checkout()} detected that these
	 *         files should be deleted but the deletion in the filesystem failed
	 *         (e.g. because a file was locked). To have a consistent state of
	 *         the working tree these files have to be deleted by the callers of
	 *         {@link org.eclipse.jgit.dircache.DirCacheCheckout}.
	 */
	public List<String> getToBeDeleted() {
		return toBeDeleted;
	}

	/**
	 * Get list of all files removed by this checkout
	 *
	 * @return a list of all files removed by this checkout
	 */
	public List<String> getRemoved() {
		return removed;
	}

	/**
	 * Constructs a DirCacheCeckout for merging and checking out two trees (HEAD
	 * and mergeCommitTree) and the index.
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
	 * @throws java.io.IOException
	 */
	public DirCacheCheckout(Repository repo, ObjectId headCommitTree, DirCache dc,
			ObjectId mergeCommitTree, WorkingTreeIterator workingTree)
			throws IOException {
		this.repo = repo;
		this.dc = dc;
		this.headCommitTree = headCommitTree;
		this.mergeCommitTree = mergeCommitTree;
		this.workingTree = workingTree;
		this.initialCheckout = !repo.isBare() && !repo.getIndexFile().exists();
	}

	/**
	 * Constructs a DirCacheCeckout for merging and checking out two trees (HEAD
	 * and mergeCommitTree) and the index. As iterator over the working tree
	 * this constructor creates a standard
	 * {@link org.eclipse.jgit.treewalk.FileTreeIterator}
	 *
	 * @param repo
	 *            the repository in which we do the checkout
	 * @param headCommitTree
	 *            the id of the tree of the head commit
	 * @param dc
	 *            the (already locked) Dircache for this repo
	 * @param mergeCommitTree
	 *            the id of the tree we want to fast-forward to
	 * @throws java.io.IOException
	 */
	public DirCacheCheckout(Repository repo, ObjectId headCommitTree,
			DirCache dc, ObjectId mergeCommitTree) throws IOException {
		this(repo, headCommitTree, dc, mergeCommitTree, new FileTreeIterator(repo));
	}

	/**
	 * Constructs a DirCacheCeckout for checking out one tree, merging with the
	 * index.
	 *
	 * @param repo
	 *            the repository in which we do the checkout
	 * @param dc
	 *            the (already locked) Dircache for this repo
	 * @param mergeCommitTree
	 *            the id of the tree we want to fast-forward to
	 * @param workingTree
	 *            an iterator over the repositories Working Tree
	 * @throws java.io.IOException
	 */
	public DirCacheCheckout(Repository repo, DirCache dc,
			ObjectId mergeCommitTree, WorkingTreeIterator workingTree)
			throws IOException {
		this(repo, null, dc, mergeCommitTree, workingTree);
	}

	/**
	 * Constructs a DirCacheCeckout for checking out one tree, merging with the
	 * index. As iterator over the working tree this constructor creates a
	 * standard {@link org.eclipse.jgit.treewalk.FileTreeIterator}
	 *
	 * @param repo
	 *            the repository in which we do the checkout
	 * @param dc
	 *            the (already locked) Dircache for this repo
	 * @param mergeCommitTree
	 *            the id of the tree of the
	 * @throws java.io.IOException
	 */
	public DirCacheCheckout(Repository repo, DirCache dc,
			ObjectId mergeCommitTree) throws IOException {
		this(repo, null, dc, mergeCommitTree, new FileTreeIterator(repo));
	}

	/**
	 * Set a progress monitor which can be passed to built-in filter commands,
	 * providing progress information for long running tasks.
	 *
	 * @param monitor
	 *            the {@link ProgressMonitor}
	 * @since 4.11
	 */
	public void setProgressMonitor(ProgressMonitor monitor) {
		this.monitor = monitor != null ? monitor : NullProgressMonitor.INSTANCE;
	}

	/**
	 * Scan head, index and merge tree. Used during normal checkout or merge
	 * operations.
	 *
	 * @throws org.eclipse.jgit.errors.CorruptObjectException
	 * @throws java.io.IOException
	 */
	public void preScanTwoTrees() throws CorruptObjectException, IOException {
		removed.clear();
		updated.clear();
		conflicts.clear();
		walk = new NameConflictTreeWalk(repo);
		builder = dc.builder();

		walk.setHead(addTree(walk, headCommitTree));
		addTree(walk, mergeCommitTree);
		int dciPos = walk.addTree(new DirCacheBuildIterator(builder));
		walk.addTree(workingTree);
		workingTree.setDirCacheIterator(walk, dciPos);

		while (walk.next()) {
			processEntry(walk.getTree(0, CanonicalTreeParser.class),
					walk.getTree(1, CanonicalTreeParser.class),
					walk.getTree(2, DirCacheBuildIterator.class),
					walk.getTree(3, WorkingTreeIterator.class));
			if (walk.isSubtree())
				walk.enterSubtree();
		}
	}

	/**
	 * Scan index and merge tree (no HEAD). Used e.g. for initial checkout when
	 * there is no head yet.
	 *
	 * @throws org.eclipse.jgit.errors.MissingObjectException
	 * @throws org.eclipse.jgit.errors.IncorrectObjectTypeException
	 * @throws org.eclipse.jgit.errors.CorruptObjectException
	 * @throws java.io.IOException
	 */
	public void prescanOneTree()
			throws MissingObjectException, IncorrectObjectTypeException,
			CorruptObjectException, IOException {
		removed.clear();
		updated.clear();
		conflicts.clear();

		builder = dc.builder();

		walk = new NameConflictTreeWalk(repo);
		walk.setHead(addTree(walk, mergeCommitTree));
		int dciPos = walk.addTree(new DirCacheBuildIterator(builder));
		walk.addTree(workingTree);
		workingTree.setDirCacheIterator(walk, dciPos);

		while (walk.next()) {
			processEntry(walk.getTree(0, CanonicalTreeParser.class),
					walk.getTree(1, DirCacheBuildIterator.class),
					walk.getTree(2, WorkingTreeIterator.class));
			if (walk.isSubtree())
				walk.enterSubtree();
		}
		conflicts.removeAll(removed);
	}

	private int addTree(TreeWalk tw, ObjectId id) throws MissingObjectException,
			IncorrectObjectTypeException, IOException {
		if (id == null) {
			return tw.addTree(new EmptyTreeIterator());
		}
		return tw.addTree(id);
	}

	/**
	 * Processing an entry in the context of {@link #prescanOneTree()} when only
	 * one tree is given
	 *
	 * @param m
	 *            the tree to merge
	 * @param i
	 *            the index
	 * @param f
	 *            the working tree
	 * @throws IOException
	 */
	void processEntry(CanonicalTreeParser m, DirCacheBuildIterator i,
			WorkingTreeIterator f) throws IOException {
		if (m != null) {
			checkValidPath(m);
			// There is an entry in the merge commit. Means: we want to update
			// what's currently in the index and working-tree to that one
			if (i == null) {
				// The index entry is missing
				if (f != null && !FileMode.TREE.equals(f.getEntryFileMode())
						&& !f.isEntryIgnored()) {
					if (failOnConflict) {
						// don't overwrite an untracked and not ignored file
						conflicts.add(walk.getPathString());
					} else {
						// failOnConflict is false. Putting something to conflicts
						// would mean we delete it. Instead we want the mergeCommit
						// content to be checked out.
						update(m);
					}
				} else
					update(m);
			} else if (f == null || !m.idEqual(i)) {
				// The working tree file is missing or the merge content differs
				// from index content
				update(m);
			} else if (i.getDirCacheEntry() != null) {
				// The index contains a file (and not a folder)
				if (f.isModified(i.getDirCacheEntry(), true,
						this.walk.getObjectReader())
						|| i.getDirCacheEntry().getStage() != 0)
					// The working tree file is dirty or the index contains a
					// conflict
					update(m);
				else {
					// update the timestamp of the index with the one from the
					// file if not set, as we are sure to be in sync here.
					DirCacheEntry entry = i.getDirCacheEntry();
					Instant mtime = entry.getLastModifiedInstant();
					if (mtime == null || mtime.equals(Instant.EPOCH)) {
						entry.setLastModified(f.getEntryLastModifiedInstant());
					}
					keep(i.getEntryPathString(), entry, f);
				}
			} else
				// The index contains a folder
				keep(i.getEntryPathString(), i.getDirCacheEntry(), f);
		} else {
			// There is no entry in the merge commit. Means: we want to delete
			// what's currently in the index and working tree
			if (f != null) {
				// There is a file/folder for that path in the working tree
				if (walk.isDirectoryFileConflict()) {
					// We put it in conflicts. Even if failOnConflict is false
					// this would cause the path to be deleted. Thats exactly what
					// we want in this situation
					conflicts.add(walk.getPathString());
				} else {
					// No file/folder conflict exists. All entries are files or
					// all entries are folders
					if (i != null) {
						// ... and the working tree contained a file or folder
						// -> add it to the removed set and remove it from
						// conflicts set
						remove(i.getEntryPathString());
						conflicts.remove(i.getEntryPathString());
					} else {
						// untracked file, neither contained in tree to merge
						// nor in index
					}
				}
			} else {
				// There is no file/folder for that path in the working tree,
				// nor in the merge head.
				// The only entry we have is the index entry. Like the case
				// where there is a file with the same name, remove it,
			}
		}
	}

	/**
	 * Execute this checkout. A
	 * {@link org.eclipse.jgit.events.WorkingTreeModifiedEvent} is fired if the
	 * working tree was modified; even if the checkout fails.
	 *
	 * @return <code>false</code> if this method could not delete all the files
	 *         which should be deleted (e.g. because one of the files was
	 *         locked). In this case {@link #getToBeDeleted()} lists the files
	 *         which should be tried to be deleted outside of this method.
	 *         Although <code>false</code> is returned the checkout was
	 *         successful and the working tree was updated for all other files.
	 *         <code>true</code> is returned when no such problem occurred
	 * @throws java.io.IOException
	 */
	public boolean checkout() throws IOException {
		try {
			return doCheckout();
		} catch (CanceledException ce) {
			// should actually be propagated, but this would change a LOT of
			// APIs
			throw new IOException(ce);
		} finally {
			try {
				dc.unlock();
			} finally {
				if (performingCheckout) {
					Set<String> touched = new HashSet<>(conflicts);
					touched.addAll(getUpdated().keySet());
					touched.addAll(kept);
					WorkingTreeModifiedEvent event = new WorkingTreeModifiedEvent(
							touched, getRemoved());
					if (!event.isEmpty()) {
						repo.fireEvent(event);
					}
				}
			}
		}
	}

	private boolean doCheckout() throws CorruptObjectException, IOException,
			MissingObjectException, IncorrectObjectTypeException,
			CheckoutConflictException, IndexWriteException, CanceledException {
		toBeDeleted.clear();
		try (ObjectReader objectReader = repo.getObjectDatabase().newReader()) {
			checkout = new Checkout(repo, null);
			if (headCommitTree != null)
				preScanTwoTrees();
			else
				prescanOneTree();

			if (!conflicts.isEmpty()) {
				if (failOnConflict) {
					throw new CheckoutConflictException(conflicts.toArray(new String[0]));
				}
				cleanUpConflicts();
			}

			// update our index
			builder.finish();

			// init progress reporting
			int numTotal = removed.size() + updated.size() + conflicts.size();
			monitor.beginTask(JGitText.get().checkingOutFiles, numTotal);

			performingCheckout = true;
			File file = null;
			String last = null;
			// when deleting files process them in the opposite order as they have
			// been reported. This ensures the files are deleted before we delete
			// their parent folders
			IntList nonDeleted = new IntList();
			for (int i = removed.size() - 1; i >= 0; i--) {
				String r = removed.get(i);
				file = new File(repo.getWorkTree(), r);
				if (!file.delete() && repo.getFS().exists(file)) {
					// The list of stuff to delete comes from the index
					// which will only contain a directory if it is
					// a submodule, in which case we shall not attempt
					// to delete it. A submodule is not empty, so it
					// is safe to check this after a failed delete.
					if (!repo.getFS().isDirectory(file)) {
						nonDeleted.add(i);
						toBeDeleted.add(r);
					}
				} else {
					if (last != null && !isSamePrefix(r, last))
						removeEmptyParents(new File(repo.getWorkTree(), last));
					last = r;
				}
				monitor.update(1);
				if (monitor.isCancelled()) {
					throw new CanceledException(MessageFormat.format(
							JGitText.get().operationCanceled,
							JGitText.get().checkingOutFiles));
				}
			}
			if (file != null) {
				removeEmptyParents(file);
			}
			removed = filterOut(removed, nonDeleted);
			nonDeleted = null;
			Iterator<Map.Entry<String, CheckoutMetadata>> toUpdate = updated
					.entrySet().iterator();
			Map.Entry<String, CheckoutMetadata> e = null;
			try {
				while (toUpdate.hasNext()) {
					e = toUpdate.next();
					String path = e.getKey();
					CheckoutMetadata meta = e.getValue();
					DirCacheEntry entry = dc.getEntry(path);
					if (FileMode.GITLINK.equals(entry.getRawMode())) {
						checkout.checkoutGitlink(entry, path);
					} else {
						checkout.checkout(entry, meta, objectReader, path);
					}
					e = null;

					monitor.update(1);
					if (monitor.isCancelled()) {
						throw new CanceledException(MessageFormat.format(
								JGitText.get().operationCanceled,
								JGitText.get().checkingOutFiles));
					}
				}
			} catch (Exception ex) {
				// We didn't actually modify the current entry nor any that
				// might follow.
				if (e != null) {
					toUpdate.remove();
				}
				while (toUpdate.hasNext()) {
					e = toUpdate.next();
					toUpdate.remove();
				}
				throw ex;
			}
			for (String conflict : conflicts) {
				// the conflicts are likely to have multiple entries in the
				// dircache, we only want to check out the one for the "theirs"
				// tree
				int entryIdx = dc.findEntry(conflict);
				if (entryIdx >= 0) {
					while (entryIdx < dc.getEntryCount()) {
						DirCacheEntry entry = dc.getEntry(entryIdx);
						if (!entry.getPathString().equals(conflict)) {
							break;
						}
						if (entry.getStage() == DirCacheEntry.STAGE_3) {
							checkout.checkout(entry, null, objectReader,
									conflict);
							break;
						}
						++entryIdx;
					}
				}

				monitor.update(1);
				if (monitor.isCancelled()) {
					throw new CanceledException(MessageFormat.format(
							JGitText.get().operationCanceled,
							JGitText.get().checkingOutFiles));
				}
			}
			monitor.endTask();

			// commit the index builder - a new index is persisted
			if (!builder.commit())
				throw new IndexWriteException();
		}
		return toBeDeleted.isEmpty();
	}

	private static ArrayList<String> filterOut(ArrayList<String> strings,
			IntList indicesToRemove) {
		int n = indicesToRemove.size();
		if (n == strings.size()) {
			return new ArrayList<>(0);
		}
		switch (n) {
		case 0:
			return strings;
		case 1:
			strings.remove(indicesToRemove.get(0));
			return strings;
		default:
			int length = strings.size();
			ArrayList<String> result = new ArrayList<>(length - n);
			// Process indicesToRemove from the back; we know that it
			// contains indices in descending order.
			int j = n - 1;
			int idx = indicesToRemove.get(j);
			for (int i = 0; i < length; i++) {
				if (i == idx) {
					idx = (--j >= 0) ? indicesToRemove.get(j) : -1;
				} else {
					result.add(strings.get(i));
				}
			}
			return result;
		}
	}

	private static boolean isSamePrefix(String a, String b) {
		int as = a.lastIndexOf('/');
		int bs = b.lastIndexOf('/');
		return a.substring(0, as + 1).equals(b.substring(0, bs + 1));
	}

	 private void removeEmptyParents(File f) {
		File parentFile = f.getParentFile();

		while (parentFile != null && !parentFile.equals(repo.getWorkTree())) {
			if (!parentFile.delete())
				break;
			parentFile = parentFile.getParentFile();
		}
	}

	/**
	 * Compares whether two pairs of ObjectId and FileMode are equal.
	 *
	 * @param id1
	 * @param mode1
	 * @param id2
	 * @param mode2
	 * @return <code>true</code> if FileModes and ObjectIds are equal.
	 *         <code>false</code> otherwise
	 */
	private boolean equalIdAndMode(ObjectId id1, FileMode mode1, ObjectId id2,
			FileMode mode2) {
		if (!mode1.equals(mode2))
			return false;
		return id1 != null ? id1.equals(id2) : id2 == null;
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

	void processEntry(CanonicalTreeParser h, CanonicalTreeParser m,
			DirCacheBuildIterator i, WorkingTreeIterator f) throws IOException {
		DirCacheEntry dce = i != null ? i.getDirCacheEntry() : null;

		String name = walk.getPathString();

		if (m != null)
			checkValidPath(m);

		if (i == null && m == null && h == null) {
			// File/Directory conflict case #20
			if (walk.isDirectoryFileConflict())
				// TODO: check whether it is always correct to report a conflict here
				conflict(name, null, null, null);

			// file only exists in working tree -> ignore it
			return;
		}

		ObjectId iId = (i == null ? null : i.getEntryObjectId());
		ObjectId mId = (m == null ? null : m.getEntryObjectId());
		ObjectId hId = (h == null ? null : h.getEntryObjectId());
		FileMode iMode = (i == null ? null : i.getEntryFileMode());
		FileMode mMode = (m == null ? null : m.getEntryFileMode());
		FileMode hMode = (h == null ? null : h.getEntryFileMode());

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
		 * 3    D        F       D                 Y       N       N           Keep
		 * 4    D        F       D                 N       N       N           Conflict
		 * 5    D        F       F       Y         N       N       Y           Keep
		 * 5b   D        F       F       Y         N       N       N           Conflict
		 * 6    D        F       F       N         N       N       Y           Keep
		 * 6b   D        F       F       N         N       N       N           Conflict
		 * 7    F        D       F       Y         Y       N       N           Update
		 * 8    F        D       F       N         Y       N       N           Conflict
		 * 9    F        D       F                 N       N       N           Conflict
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
		// "file", a "D" stands for "directory" (tree), and a "0" stands for
		// non-existing. Symbolic links and git links are treated as File here.
		//
		// Examples:
		// ffMask == 0xFFD -> Head=File, Index=File, Merge=Tree
		// ffMask == 0xDD0 -> Head=Tree, Index=Tree, Merge=Non-Existing

		int ffMask = 0;
		if (h != null)
			ffMask = FileMode.TREE.equals(hMode) ? 0xD00 : 0xF00;
		if (i != null)
			ffMask |= FileMode.TREE.equals(iMode) ? 0x0D0 : 0x0F0;
		if (m != null)
			ffMask |= FileMode.TREE.equals(mMode) ? 0x00D : 0x00F;

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
				if (f != null && isModifiedSubtree_IndexWorkingtree(name)) {
					conflict(name, dce, h, m); // 1
				} else {
					update(1, name, mId, mMode); // 2
				}

				break;
			case 0xDFD: // 3 4
				keep(name, dce, f);
				break;
			case 0xF0D: // 18
				remove(name);
				break;
			case 0xDFF: // 5 5b 6 6b
				if (equalIdAndMode(iId, iMode, mId, mMode))
					keep(name, dce, f); // 5 6
				else
					conflict(name, dce, h, m); // 5b 6b
				break;
			case 0xFDD: // 10 11
				// TODO: make use of tree extension as soon as available in jgit
				// we would like to do something like
				// if (!equalIdAndMode(iId, iMode, mId, mMode)
				//   conflict(name, i.getDirCacheEntry(), h, m);
				// But since we don't know the id of a tree in the index we do
				// nothing here and wait that conflicts between index and merge
				// are found later
				break;
			case 0xD0F: // 19
				update(1, name, mId, mMode);
				break;
			case 0xDF0: // conflict without a rule
			case 0x0FD: // 15
				conflict(name, dce, h, m);
				break;
			case 0xFDF: // 7 8 9
				if (equalIdAndMode(hId, hMode, mId, mMode)) {
					if (isModifiedSubtree_IndexWorkingtree(name))
						conflict(name, dce, h, m); // 8
					else
						update(1, name, mId, mMode); // 7
				} else
					conflict(name, dce, h, m); // 9
				break;
			case 0xFD0: // keep without a rule
				keep(name, dce, f);
				break;
			case 0xFFD: // 12 13 14
				if (equalIdAndMode(hId, hMode, iId, iMode))
					if (f != null
							&& f.isModified(dce, true,
									this.walk.getObjectReader()))
						conflict(name, dce, h, m); // 13
					else
						remove(name); // 12
				else
					conflict(name, dce, h, m); // 14
				break;
			case 0x0DF: // 16 17
				if (!isModifiedSubtree_IndexWorkingtree(name))
					update(1, name, mId, mMode);
				else
					conflict(name, dce, h, m);
				break;
			default:
				keep(name, dce, f);
			}
			return;
		}

		if ((ffMask & 0x222) == 0) {
			// HEAD, MERGE and index don't contain a file (e.g. all contain a
			// folder)
			if (f == null || FileMode.TREE.equals(f.getEntryFileMode())) {
				// the workingtree entry doesn't exist or also contains a folder
				// -> no problem
				return;
			}
			// the workingtree entry exists and is not a folder
			if (!idEqual(h, m)) {
				// Because HEAD and MERGE differ we will try to update the
				// workingtree with a folder -> return a conflict
				conflict(name, null, null, null);
			}
			return;
		}

		if ((ffMask == 0x00F) && f != null && FileMode.TREE.equals(f.getEntryFileMode())) {
			// File/Directory conflict case #20
			conflict(name, null, h, m);
			return;
		}

		if (i == null) {
			// Nothing in Index
			// At least one of Head, Index, Merge is not empty
			// make sure not to overwrite untracked files
			if (f != null && !f.isEntryIgnored()) {
				// A submodule is not a file. We should ignore it
				if (!FileMode.GITLINK.equals(mMode)) {
					// a dirty worktree: the index is empty but we have a
					// workingtree-file
					if (mId == null
							|| !equalIdAndMode(mId, mMode,
									f.getEntryObjectId(), f.getEntryFileMode())) {
						conflict(name, null, h, m);
						return;
					}
				}
			}

			/**
			 * <pre>
			 * 	          I (index)     H        M     H==M  Result
			 * 	        -------------------------------------------
			 * 	        0 nothing    nothing  nothing        (does not happen)
			 * 	        1 nothing    nothing  exists         use M
			 * 	        2 nothing    exists   nothing        remove path from index
			 * 	        3 nothing    exists   exists   yes   keep index if not in initial checkout
			 *                                               , otherwise use M
			 * 	          nothing    exists   exists   no    fail
			 * </pre>
			 */

			if (h == null)
				// Nothing in Head
				// Nothing in Index
				// At least one of Head, Index, Merge is not empty
				// -> only Merge contains something for this path. Use it!
				// Potentially update the file
				update(1, name, mId, mMode); // 1
			else if (m == null)
				// Nothing in Merge
				// Something in Head
				// Nothing in Index
				// -> only Head contains something for this path and it should
				// be deleted. Potentially removes the file!
				remove(name); // 2
			else { // 3
				// Something in Merge
				// Something in Head
				// Nothing in Index
				// -> Head and Merge contain something (maybe not the same) and
				// in the index there is nothing (e.g. 'git rm ...' was
				// called before). Ignore the cached deletion and use what we
				// find in Merge. Potentially updates the file.
				if (equalIdAndMode(hId, hMode, mId, mMode)) {
					if (initialCheckout || force) {
						update(1, name, mId, mMode);
					} else {
						keep(name, dce, f);
					}
				} else {
					conflict(name, dce, h, m);
				}
			}
		} else {
			// Something in Index
			if (h == null) {
				// Nothing in Head
				// Something in Index
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

				if (m == null
						|| !isModified_IndexTree(name, iId, iMode, mId, mMode,
								mergeCommitTree)) {
					// Merge contains nothing or the same as Index
					// Nothing in Head
					// Something in Index
					if (m==null && walk.isDirectoryFileConflict()) {
						// Nothing in Merge and current path is part of
						// File/Folder conflict
						// Nothing in Head
						// Something in Index
						if (dce != null
								&& (f == null || f.isModified(dce, true,
										this.walk.getObjectReader())))
							// No file or file is dirty
							// Nothing in Merge and current path is part of
							// File/Folder conflict
							// Nothing in Head
							// Something in Index
							// -> File folder conflict and Merge wants this
							// path to be removed. Since the file is dirty
							// report a conflict
							conflict(name, dce, h, m);
						else
							// A file is present and file is not dirty
							// Nothing in Merge and current path is part of
							// File/Folder conflict
							// Nothing in Head
							// Something in Index
							// -> File folder conflict and Merge wants this path
							// to be removed. Since the file is not dirty remove
							// file and index entry
							remove(name);
					} else
						// Something in Merge or current path is not part of
						// File/Folder conflict
						// Merge contains nothing or the same as Index
						// Nothing in Head
						// Something in Index
						// -> Merge contains nothing new. Keep the index.
						keep(name, dce, f);
				} else
					// Merge contains something and it is not the same as Index
					// Nothing in Head
					// Something in Index
					// -> Index contains something new (different from Head)
					// and Merge is different from Index. Report a conflict
					conflict(name, dce, h, m);
			} else if (m == null) {
				// Nothing in Merge
				// Something in Head
				// Something in Index

				/**
				 * <pre>
				 * 	           clean I==H  I==M       H        M        Result
				 * 	         -----------------------------------------------------
				 * 	        10 yes   yes   N/A     exists   nothing  remove path from index
				 * 	        11 no    yes   N/A     exists   nothing  keep file
				 * 	        12 yes   no    N/A     exists   nothing  fail
				 * 	        13 no    no    N/A     exists   nothing  fail
				 * </pre>
				 */

				if (iMode == FileMode.GITLINK) {
					// A submodule in Index
					// Nothing in Merge
					// Something in Head
					// Submodules that disappear from the checkout must
					// be removed from the index, but not deleted from disk.
					remove(name);
				} else {
					// Something different from a submodule in Index
					// Nothing in Merge
					// Something in Head
					if (!isModified_IndexTree(name, iId, iMode, hId, hMode,
							headCommitTree)) {
						// Index contains the same as Head
						// Something different from a submodule in Index
						// Nothing in Merge
						// Something in Head
						if (f != null
								&& f.isModified(dce, true,
										this.walk.getObjectReader())) {
							// file is dirty
							// Index contains the same as Head
							// Something different from a submodule in Index
							// Nothing in Merge
							// Something in Head

							if (!FileMode.TREE.equals(f.getEntryFileMode())
									&& FileMode.TREE.equals(iMode)) {
								// The workingtree contains a file and the index semantically contains a folder.
								// Git considers the workingtree file as untracked. Just keep the untracked file.
								return;
							}
							// -> file is dirty and tracked but is should be
							// removed. That's a conflict
							conflict(name, dce, h, m);
						} else {
							// file doesn't exist or is clean
							// Index contains the same as Head
							// Something different from a submodule in Index
							// Nothing in Merge
							// Something in Head
							// -> Remove from index and delete the file
							remove(name);
						}
					} else {
						// Index contains something different from Head
						// Something different from a submodule in Index
						// Nothing in Merge
						// Something in Head
						// -> Something new is in index (and maybe even on the
						// filesystem). But Merge wants the path to be removed.
						// Report a conflict
						conflict(name, dce, h, m);
					}
				}
			} else {
				// Something in Merge
				// Something in Head
				// Something in Index
				if (!equalIdAndMode(hId, hMode, mId, mMode)
						&& isModified_IndexTree(name, iId, iMode, hId, hMode,
								headCommitTree)
						&& isModified_IndexTree(name, iId, iMode, mId, mMode,
								mergeCommitTree))
					// All three contents in Head, Merge, Index differ from each
					// other
					// -> All contents differ. Report a conflict.
					conflict(name, dce, h, m);
				else
					// At least two of the contents of Head, Index, Merge
					// are the same
					// Something in Merge
					// Something in Head
					// Something in Index

				if (!isModified_IndexTree(name, iId, iMode, hId, hMode,
						headCommitTree)
						&& isModified_IndexTree(name, iId, iMode, mId, mMode,
								mergeCommitTree)) {
						// Head contains the same as Index. Merge differs
						// Something in Merge

					// For submodules just update the index with the new SHA-1
					if (dce != null
							&& FileMode.GITLINK.equals(dce.getFileMode())) {
						// Index and Head contain the same submodule. Merge
						// differs
						// Something in Merge
						// -> Nothing new in index. Move to merge.
						// Potentially updates the file

						// TODO check that we don't overwrite some unsaved
						// file content
						update(1, name, mId, mMode);
					} else if (dce != null
							&& (f != null && f.isModified(dce, true,
									this.walk.getObjectReader()))) {
						// File exists and is dirty
						// Head and Index don't contain a submodule
						// Head contains the same as Index. Merge differs
						// Something in Merge
						// -> Merge wants the index and file to be updated
						// but the file is dirty. Report a conflict
						conflict(name, dce, h, m);
					} else {
						// File doesn't exist or is clean
						// Head and Index don't contain a submodule
						// Head contains the same as Index. Merge differs
						// Something in Merge
						// -> Standard case when switching between branches:
						// Nothing new in index but something different in
						// Merge. Update index and file
						update(1, name, mId, mMode);
					}
				} else {
					// Head differs from index or merge is same as index
					// At least two of the contents of Head, Index, Merge
					// are the same
					// Something in Merge
					// Something in Head
					// Something in Index

					// Can be formulated as: Either all three states are
					// equal or Merge is equal to Head or Index and differs
					// to the other one.
					// -> In all three cases we don't touch index and file.

					keep(name, dce, f);
				}
			}
		}
	}

	private static boolean idEqual(AbstractTreeIterator a,
			AbstractTreeIterator b) {
		if (a == b) {
			return true;
		}
		if (a == null || b == null) {
			return false;
		}
		return a.getEntryObjectId().equals(b.getEntryObjectId());
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
			entry.copyMetaData(e, true);
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

	private void keep(String path, DirCacheEntry e, WorkingTreeIterator f)
			throws IOException {
		if (e == null) {
			return;
		}
		if (!FileMode.TREE.equals(e.getFileMode())) {
			builder.add(e);
		}
		if (force) {
			if (f == null || f.isModified(e, true, walk.getObjectReader())) {
				kept.add(path);
				checkout.checkout(e,
						new CheckoutMetadata(walk.getEolStreamType(CHECKOUT_OP),
								walk.getFilterCommand(
										Constants.ATTR_FILTER_TYPE_SMUDGE)),
						walk.getObjectReader(), path);
			}
		}
	}

	private void remove(String path) {
		removed.add(path);
	}

	private void update(CanonicalTreeParser tree) throws IOException {
		update(0, tree.getEntryPathString(), tree.getEntryObjectId(),
				tree.getEntryFileMode());
	}

	private void update(int index, String path, ObjectId mId,
			FileMode mode) throws IOException {
		if (!FileMode.TREE.equals(mode)) {
			updated.put(path, new CheckoutMetadata(
					walk.getCheckoutEolStreamType(index),
					walk.getSmudgeCommand(index)));

			DirCacheEntry entry = new DirCacheEntry(path, DirCacheEntry.STAGE_0);
			entry.setObjectId(mId);
			entry.setFileMode(mode);
			builder.add(entry);
		}
	}

	/**
	 * If <code>true</code>, will scan first to see if it's possible to check
	 * out, otherwise throw
	 * {@link org.eclipse.jgit.errors.CheckoutConflictException}. If
	 * <code>false</code>, it will silently deal with the problem.
	 *
	 * @param failOnConflict
	 *            a boolean.
	 */
	public void setFailOnConflict(boolean failOnConflict) {
		this.failOnConflict = failOnConflict;
	}

	/**
	 * If <code>true</code>, dirty worktree files may be overridden. If
	 * <code>false</code> dirty worktree files will not be overridden in order
	 * not to delete unsaved content. This corresponds to native git's 'git
	 * checkout -f' option. By default this option is set to false.
	 *
	 * @param force
	 *            a boolean.
	 * @since 5.3
	 */
	public void setForce(boolean force) {
		this.force = force;
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
	}

	/**
	 * Checks whether the subtree starting at a given path differs between Index and
	 * workingtree.
	 *
	 * @param path
	 * @return true if the subtrees differ
	 * @throws CorruptObjectException
	 * @throws IOException
	 */
	private boolean isModifiedSubtree_IndexWorkingtree(String path)
			throws CorruptObjectException, IOException {
		try (NameConflictTreeWalk tw = new NameConflictTreeWalk(repo)) {
			int dciPos = tw.addTree(new DirCacheIterator(dc));
			FileTreeIterator fti = new FileTreeIterator(repo);
			tw.addTree(fti);
			fti.setDirCacheIterator(tw, dciPos);
			tw.setRecursive(true);
			tw.setFilter(PathFilter.create(path));
			DirCacheIterator dcIt;
			WorkingTreeIterator wtIt;
			while (tw.next()) {
				dcIt = tw.getTree(0, DirCacheIterator.class);
				wtIt = tw.getTree(1, WorkingTreeIterator.class);
				if (dcIt == null || wtIt == null)
					return true;
				if (wtIt.isModified(dcIt.getDirCacheEntry(), true,
						this.walk.getObjectReader())) {
					return true;
				}
			}
			return false;
		}
	}

	private boolean isModified_IndexTree(String path, ObjectId iId,
			FileMode iMode, ObjectId tId, FileMode tMode, ObjectId rootTree)
			throws CorruptObjectException, IOException {
		if (iMode != tMode) {
			return true;
		}
		if (FileMode.TREE.equals(iMode)
				&& (iId == null || ObjectId.zeroId().equals(iId))) {
			return isModifiedSubtree_IndexTree(path, rootTree);
		}
		return !equalIdAndMode(iId, iMode, tId, tMode);
	}

	/**
	 * Checks whether the subtree starting at a given path differs between Index and
	 * some tree.
	 *
	 * @param path
	 * @param tree
	 *            the tree to compare
	 * @return true if the subtrees differ
	 * @throws CorruptObjectException
	 * @throws IOException
	 */
	private boolean isModifiedSubtree_IndexTree(String path, ObjectId tree)
			throws CorruptObjectException, IOException {
		try (NameConflictTreeWalk tw = new NameConflictTreeWalk(repo)) {
			tw.addTree(new DirCacheIterator(dc));
			tw.addTree(tree);
			tw.setRecursive(true);
			tw.setFilter(PathFilter.create(path));
			while (tw.next()) {
				AbstractTreeIterator dcIt = tw.getTree(0,
						DirCacheIterator.class);
				AbstractTreeIterator treeIt = tw.getTree(1,
						AbstractTreeIterator.class);
				if (dcIt == null || treeIt == null)
					return true;
				if (dcIt.getEntryRawMode() != treeIt.getEntryRawMode())
					return true;
				if (!dcIt.getEntryObjectId().equals(treeIt.getEntryObjectId()))
					return true;
			}
			return false;
		}
	}

	/**
	 * Updates the file in the working tree with content and mode from an entry
	 * in the index. The new content is first written to a new temporary file in
	 * the same directory as the real file. Then that new file is renamed to the
	 * final filename.
	 *
	 * <p>
	 * <b>Note:</b> if the entry path on local file system exists as a non-empty
	 * directory, and the target entry type is a link or file, the checkout will
	 * fail with {@link java.io.IOException} since existing non-empty directory
	 * cannot be renamed to file or link without deleting it recursively.
	 * </p>
	 *
	 * @param repo
	 *            repository managing the destination work tree.
	 * @param entry
	 *            the entry containing new mode and content
	 * @param or
	 *            object reader to use for checkout
	 * @throws java.io.IOException
	 * @since 3.6
	 * @deprecated since 5.1, use
	 *             {@link #checkoutEntry(Repository, DirCacheEntry, ObjectReader, boolean, CheckoutMetadata, WorkingTreeOptions)}
	 *             instead
	 */
	@Deprecated
	public static void checkoutEntry(Repository repo, DirCacheEntry entry,
			ObjectReader or) throws IOException {
		checkoutEntry(repo, entry, or, false, null, null);
	}


	/**
	 * Updates the file in the working tree with content and mode from an entry
	 * in the index. The new content is first written to a new temporary file in
	 * the same directory as the real file. Then that new file is renamed to the
	 * final filename.
	 *
	 * <p>
	 * <b>Note:</b> if the entry path on local file system exists as a file, it
	 * will be deleted and if it exists as a directory, it will be deleted
	 * recursively, independently if has any content.
	 * </p>
	 *
	 * @param repo
	 *            repository managing the destination work tree.
	 * @param entry
	 *            the entry containing new mode and content
	 * @param or
	 *            object reader to use for checkout
	 * @param deleteRecursive
	 *            true to recursively delete final path if it exists on the file
	 *            system
	 * @param checkoutMetadata
	 *            containing
	 *            <ul>
	 *            <li>smudgeFilterCommand to be run for smudging the entry to be
	 *            checked out</li>
	 *            <li>eolStreamType used for stream conversion</li>
	 *            </ul>
	 * @throws java.io.IOException
	 * @since 4.2
	 * @deprecated since 6.3, use
	 *             {@link #checkoutEntry(Repository, DirCacheEntry, ObjectReader, boolean, CheckoutMetadata, WorkingTreeOptions)}
	 *             instead
	 */
	@Deprecated
	public static void checkoutEntry(Repository repo, DirCacheEntry entry,
			ObjectReader or, boolean deleteRecursive,
			CheckoutMetadata checkoutMetadata) throws IOException {
		checkoutEntry(repo, entry, or, deleteRecursive, checkoutMetadata, null);
	}

	/**
	 * Updates the file in the working tree with content and mode from an entry
	 * in the index. The new content is first written to a new temporary file in
	 * the same directory as the real file. Then that new file is renamed to the
	 * final filename.
	 *
	 * <p>
	 * <b>Note:</b> if the entry path on local file system exists as a file, it
	 * will be deleted and if it exists as a directory, it will be deleted
	 * recursively, independently if has any content.
	 * </p>
	 *
	 * @param repo
	 *            repository managing the destination work tree.
	 * @param entry
	 *            the entry containing new mode and content
	 * @param or
	 *            object reader to use for checkout
	 * @param deleteRecursive
	 *            true to recursively delete final path if it exists on the file
	 *            system
	 * @param checkoutMetadata
	 *            containing
	 *            <ul>
	 *            <li>smudgeFilterCommand to be run for smudging the entry to be
	 *            checked out</li>
	 *            <li>eolStreamType used for stream conversion</li>
	 *            </ul>
	 * @param options
	 *            {@link WorkingTreeOptions} that are effective; if {@code null}
	 *            they are loaded from the repository config
	 * @throws java.io.IOException
	 * @since 6.3
	 * @deprecated since 6.6.1; use {@link Checkout} instead
	 */
	@Deprecated
	public static void checkoutEntry(Repository repo, DirCacheEntry entry,
			ObjectReader or, boolean deleteRecursive,
			CheckoutMetadata checkoutMetadata, WorkingTreeOptions options)
			throws IOException {
		Checkout checkout = new Checkout(repo, options)
				.setRecursiveDeletion(deleteRecursive);
		checkout.checkout(entry, checkoutMetadata, or, null);
	}

	/**
	 * Return filtered content for a specific object (blob). EOL handling and
	 * smudge-filter handling are applied in the same way as it would be done
	 * during a checkout.
	 *
	 * @param repo
	 *            the repository
	 * @param path
	 *            the path used to determine the correct filters for the object
	 * @param checkoutMetadata
	 *            containing
	 *            <ul>
	 *            <li>smudgeFilterCommand to be run for smudging the object</li>
	 *            <li>eolStreamType used for stream conversion (can be
	 *            null)</li>
	 *            </ul>
	 * @param ol
	 *            the object loader to read raw content of the object
	 * @param opt
	 *            the working tree options where only 'core.autocrlf' is used
	 *            for EOL handling if 'checkoutMetadata.eolStreamType' is not
	 *            valid
	 * @param os
	 *            the output stream the filtered content is written to. The
	 *            caller is responsible to close the stream.
	 * @throws IOException
	 *
	 * @since 5.7
	 */
	public static void getContent(Repository repo, String path,
			CheckoutMetadata checkoutMetadata, ObjectLoader ol,
			WorkingTreeOptions opt, OutputStream os)
			throws IOException {
		getContent(repo, path, checkoutMetadata, ol::openStream, opt, os);
	}


	/**
	 * Something that can supply an {@link InputStream}.
	 *
	 * @since 6.3
	 */
	public interface StreamSupplier {

		/**
		 * Loads the input stream.
		 *
		 * @return the loaded stream
		 * @throws IOException
		 *             if any reading error occurs
		 */
		InputStream load() throws IOException;
	}

	/**
	 * Return filtered content for blob contents. EOL handling and smudge-filter
	 * handling are applied in the same way as it would be done during a
	 * checkout.
	 *
	 * @param repo
	 *            the repository
	 * @param path
	 *            the path used to determine the correct filters for the object
	 * @param checkoutMetadata
	 *            containing
	 *            <ul>
	 *            <li>smudgeFilterCommand to be run for smudging the object</li>
	 *            <li>eolStreamType used for stream conversion (can be
	 *            null)</li>
	 *            </ul>
	 * @param inputStream
	 *            A supplier for the raw content of the object. Each call should
	 *            yield a fresh stream of the same object.
	 * @param opt
	 *            the working tree options where only 'core.autocrlf' is used
	 *            for EOL handling if 'checkoutMetadata.eolStreamType' is not
	 *            valid
	 * @param os
	 *            the output stream the filtered content is written to. The
	 *            caller is responsible to close the stream.
	 * @throws IOException
	 * @since 6.3
	 */
	public static void getContent(Repository repo, String path,
			CheckoutMetadata checkoutMetadata, StreamSupplier inputStream,
			WorkingTreeOptions opt, OutputStream os)
			throws IOException {
		EolStreamType nonNullEolStreamType;
		if (checkoutMetadata.eolStreamType != null) {
			nonNullEolStreamType = checkoutMetadata.eolStreamType;
		} else if (opt.getAutoCRLF() == AutoCRLF.TRUE) {
			nonNullEolStreamType = EolStreamType.AUTO_CRLF;
		} else {
			nonNullEolStreamType = EolStreamType.DIRECT;
		}
		try (OutputStream channel = EolStreamTypeUtil.wrapOutputStream(
				os, nonNullEolStreamType)) {
			if (checkoutMetadata.smudgeFilterCommand != null) {
				if (FilterCommandRegistry
						.isRegistered(checkoutMetadata.smudgeFilterCommand)) {
					runBuiltinFilterCommand(repo, checkoutMetadata, inputStream,
							channel);
				} else {
					runExternalFilterCommand(repo, path, checkoutMetadata, inputStream,
							channel);
				}
			} else {
				try (InputStream in = inputStream.load()) {
					in.transferTo(channel);
				}
			}
		}
	}

	// Run an external filter command
	private static void runExternalFilterCommand(Repository repo, String path,
			CheckoutMetadata checkoutMetadata, StreamSupplier inputStream,
			OutputStream channel) throws IOException {
		FS fs = repo.getFS();
		ProcessBuilder filterProcessBuilder = fs.runInShell(
				checkoutMetadata.smudgeFilterCommand, new String[0]);
		filterProcessBuilder.directory(repo.getWorkTree());
		filterProcessBuilder.environment().put(Constants.GIT_DIR_KEY,
				repo.getDirectory().getAbsolutePath());
		ExecutionResult result;
		int rc;
		try {
			// TODO: wire correctly with AUTOCRLF
			try (InputStream in = inputStream.load()) {
				result = fs.execute(filterProcessBuilder, in);
			}
			rc = result.getRc();
			if (rc == 0) {
				result.getStdout().writeTo(channel,
						NullProgressMonitor.INSTANCE);
			}
		} catch (IOException | InterruptedException e) {
			throw new IOException(new FilterFailedException(e,
					checkoutMetadata.smudgeFilterCommand,
					path));
		}
		if (rc != 0) {
			throw new IOException(new FilterFailedException(rc,
					checkoutMetadata.smudgeFilterCommand, path,
					result.getStdout().toByteArray(MAX_EXCEPTION_TEXT_SIZE),
					result.getStderr().toString(MAX_EXCEPTION_TEXT_SIZE)));
		}
	}

	// Run a builtin filter command
	private static void runBuiltinFilterCommand(Repository repo,
			CheckoutMetadata checkoutMetadata, StreamSupplier inputStream,
			OutputStream channel) throws MissingObjectException, IOException {
		boolean isMandatory = repo.getConfig().getBoolean(
				ConfigConstants.CONFIG_FILTER_SECTION,
				ConfigConstants.CONFIG_SECTION_LFS,
				ConfigConstants.CONFIG_KEY_REQUIRED, false);
		FilterCommand command = null;
		try (InputStream in = inputStream.load()) {
			try {
				command = FilterCommandRegistry.createFilterCommand(
						checkoutMetadata.smudgeFilterCommand, repo, in,
						channel);
			} catch (IOException e) {
				LOG.error(JGitText.get().failedToDetermineFilterDefinition, e);
				if (!isMandatory) {
					// In case an IOException occurred during creating of the
					// command then proceed as if there would not have been a
					// builtin filter (only if the filter is not mandatory).
					try (InputStream again = inputStream.load()) {
						again.transferTo(channel);
					}
				} else {
					throw e;
				}
			}
			if (command != null) {
				while (command.run() != -1) {
					// loop as long as command.run() tells there is work to do
				}
			}
		}
	}

	@SuppressWarnings("deprecation")
	private static void checkValidPath(CanonicalTreeParser t)
			throws InvalidPathException {
		ObjectChecker chk = new ObjectChecker()
			.setSafeForWindows(SystemReader.getInstance().isWindows())
			.setSafeForMacOS(SystemReader.getInstance().isMacOS());
		for (CanonicalTreeParser i = t; i != null; i = i.getParent())
			checkValidPathSegment(chk, i);
	}

	private static void checkValidPathSegment(ObjectChecker chk,
			CanonicalTreeParser t) throws InvalidPathException {
		try {
			int ptr = t.getNameOffset();
			int end = ptr + t.getNameLength();
			chk.checkPathSegment(t.getEntryPathBuffer(), ptr, end);
		} catch (CorruptObjectException err) {
			String path = t.getEntryPathString();
			InvalidPathException i = new InvalidPathException(path);
			i.initCause(err);
			throw i;
		}
	}
}
