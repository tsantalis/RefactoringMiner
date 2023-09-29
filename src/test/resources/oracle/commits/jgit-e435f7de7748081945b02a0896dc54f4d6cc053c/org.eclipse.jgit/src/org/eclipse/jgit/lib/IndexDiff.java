/*
 * Copyright (C) 2007, Dave Watson <dwatson@mimvista.com>
 * Copyright (C) 2007-2008, Robin Rosenberg <robin.rosenberg@dewire.com>
 * Copyright (C) 2010, Jens Baumgart <jens.baumgart@sap.com>
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.jgit.dircache.DirCache;
import org.eclipse.jgit.dircache.DirCacheEntry;
import org.eclipse.jgit.dircache.DirCacheIterator;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.errors.StopWalkException;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.AbstractTreeIterator;
import org.eclipse.jgit.treewalk.EmptyTreeIterator;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.WorkingTreeIterator;
import org.eclipse.jgit.treewalk.filter.AndTreeFilter;
import org.eclipse.jgit.treewalk.filter.IndexDiffFilter;
import org.eclipse.jgit.treewalk.filter.SkipWorkTreeFilter;
import org.eclipse.jgit.treewalk.filter.TreeFilter;

/**
 * Compares the index, a tree, and the working directory Ignored files are not
 * taken into account. The following information is retrieved:
 * <ul>
 * <li>added files</li>
 * <li>changed files</li>
 * <li>removed files</li>
 * <li>missing files</li>
 * <li>modified files</li>
 * <li>conflicting files</li>
 * <li>untracked files</li>
 * <li>files with assume-unchanged flag</li>
 * </ul>
 */
public class IndexDiff {

	private static final class ProgressReportingFilter extends TreeFilter {

		private final ProgressMonitor monitor;

		private int count = 0;

		private int stepSize;

		private final int total;

		private ProgressReportingFilter(ProgressMonitor monitor, int total) {
			this.monitor = monitor;
			this.total = total;
			stepSize = total / 100;
			if (stepSize == 0)
				stepSize = 1000;
		}

		@Override
		public boolean shouldBeRecursive() {
			return false;
		}

		@Override
		public boolean include(TreeWalk walker)
				throws MissingObjectException,
				IncorrectObjectTypeException, IOException {
			count++;
			if (count % stepSize == 0) {
				if (count <= total)
					monitor.update(stepSize);
				if (monitor.isCancelled())
					throw StopWalkException.INSTANCE;
			}
			return true;
		}

		@Override
		public TreeFilter clone() {
			throw new IllegalStateException(
					"Do not clone this kind of filter: "
							+ getClass().getName());
		}
	}

	private final static int TREE = 0;

	private final static int INDEX = 1;

	private final static int WORKDIR = 2;

	private final Repository repository;

	private final RevTree tree;

	private TreeFilter filter = null;

	private final WorkingTreeIterator initialWorkingTreeIterator;

	private Set<String> added = new HashSet<String>();

	private Set<String> changed = new HashSet<String>();

	private Set<String> removed = new HashSet<String>();

	private Set<String> missing = new HashSet<String>();

	private Set<String> modified = new HashSet<String>();

	private Set<String> untracked = new HashSet<String>();

	private Set<String> conflicts = new HashSet<String>();

	private Set<String> assumeUnchanged;

	private DirCache dirCache;

	/**
	 * Construct an IndexDiff
	 *
	 * @param repository
	 * @param revstr
	 *            symbolic name e.g. HEAD
	 *            An EmptyTreeIterator is used if <code>revstr</code> cannot be resolved.
	 * @param workingTreeIterator
	 *            iterator for working directory
	 * @throws IOException
	 */
	public IndexDiff(Repository repository, String revstr,
			WorkingTreeIterator workingTreeIterator) throws IOException {
		this.repository = repository;
		ObjectId objectId = repository.resolve(revstr);
		if (objectId != null)
			tree = new RevWalk(repository).parseTree(objectId);
		else
			tree = null;
		this.initialWorkingTreeIterator = workingTreeIterator;
	}

	/**
	 * Construct an Indexdiff
	 *
	 * @param repository
	 * @param objectId
	 *            tree id. If null, an EmptyTreeIterator is used.
	 * @param workingTreeIterator
	 *            iterator for working directory
	 * @throws IOException
	 */
	public IndexDiff(Repository repository, ObjectId objectId,
			WorkingTreeIterator workingTreeIterator) throws IOException {
		this.repository = repository;
		if (objectId != null)
			tree = new RevWalk(repository).parseTree(objectId);
		else
			tree = null;
		this.initialWorkingTreeIterator = workingTreeIterator;
	}

	/**
	 * Sets a filter. Can be used e.g. for restricting the tree walk to a set of
	 * files.
	 *
	 * @param filter
	 */
	public void setFilter(TreeFilter filter) {
		this.filter = filter;
	}

	/**
	 * Run the diff operation. Until this is called, all lists will be empty.
	 * Use {@link #diff(ProgressMonitor, int, int, String)} if a progress
	 * monitor is required.
	 *
	 * @return if anything is different between index, tree, and workdir
	 * @throws IOException
	 */
	public boolean diff() throws IOException {
		return diff(null, 0, 0, "");
	}

	/**
	 * Run the diff operation. Until this is called, all lists will be empty.
	 * <p>
	 * The operation may be aborted by the progress monitor. In that event it
	 * will report what was found before the cancel operation was detected.
	 * Callers should ignore the result if monitor.isCancelled() is true. If a
	 * progress monitor is not needed, callers should use {@link #diff()}
	 * instead. Progress reporting is crude and approximate and only intended
	 * for informing the user.
	 *
	 * @param monitor
	 *            for reporting progress, may be null
	 * @param estWorkTreeSize
	 *            number or estimated files in the working tree
	 * @param estIndexSize
	 *            number of estimated entries in the cache
	 * @param title
	 *
	 * @return if anything is different between index, tree, and workdir
	 * @throws IOException
	 */
	public boolean diff(final ProgressMonitor monitor, int estWorkTreeSize,
			int estIndexSize, final String title)
			throws IOException {
		dirCache = repository.readDirCache();

		TreeWalk treeWalk = new TreeWalk(repository);
		treeWalk.setRecursive(true);
		// add the trees (tree, dirchache, workdir)
		if (tree != null)
			treeWalk.addTree(tree);
		else
			treeWalk.addTree(new EmptyTreeIterator());
		treeWalk.addTree(new DirCacheIterator(dirCache));
		treeWalk.addTree(initialWorkingTreeIterator);
		Collection<TreeFilter> filters = new ArrayList<TreeFilter>(4);

		if (monitor != null) {
			// Get the maximum size of the work tree and index
			// and add some (quite arbitrary)
			if (estIndexSize == 0)
				estIndexSize = dirCache.getEntryCount();
			int total = Math.max(estIndexSize * 10 / 9,
					estWorkTreeSize * 10 / 9);
			monitor.beginTask(title, total);
			filters.add(new ProgressReportingFilter(monitor, total));
		}

		if (filter != null)
			filters.add(filter);
		filters.add(new SkipWorkTreeFilter(INDEX));
		filters.add(new IndexDiffFilter(INDEX, WORKDIR));
		treeWalk.setFilter(AndTreeFilter.create(filters));
		while (treeWalk.next()) {
			AbstractTreeIterator treeIterator = treeWalk.getTree(TREE,
					AbstractTreeIterator.class);
			DirCacheIterator dirCacheIterator = treeWalk.getTree(INDEX,
					DirCacheIterator.class);
			WorkingTreeIterator workingTreeIterator = treeWalk.getTree(WORKDIR,
					WorkingTreeIterator.class);

			if (treeIterator != null) {
				if (dirCacheIterator != null) {
					if (!treeIterator.idEqual(dirCacheIterator)
							|| treeIterator.getEntryRawMode()
							!= dirCacheIterator.getEntryRawMode()) {
						// in repo, in index, content diff => changed
						changed.add(treeWalk.getPathString());
					}
				} else {
					// in repo, not in index => removed
					removed.add(treeWalk.getPathString());
					if (workingTreeIterator != null)
						untracked.add(treeWalk.getPathString());
				}
			} else {
				if (dirCacheIterator != null) {
					// not in repo, in index => added
					added.add(treeWalk.getPathString());
				} else {
					// not in repo, not in index => untracked
					if (workingTreeIterator != null
							&& !workingTreeIterator.isEntryIgnored()) {
						untracked.add(treeWalk.getPathString());
					}
				}
			}

			if (dirCacheIterator != null) {
				if (workingTreeIterator == null) {
					// in index, not in workdir => missing
					missing.add(treeWalk.getPathString());
				} else {
					if (workingTreeIterator.isModified(
							dirCacheIterator.getDirCacheEntry(), true)) {
						// in index, in workdir, content differs => modified
						modified.add(treeWalk.getPathString());
					}
				}

				final DirCacheEntry dirCacheEntry = dirCacheIterator
						.getDirCacheEntry();
				if (dirCacheEntry != null && dirCacheEntry.getStage() > 0) {
					conflicts.add(treeWalk.getPathString());
				}
			}
		}

		// consume the remaining work
		if (monitor != null)
			monitor.endTask();

		if (added.isEmpty() && changed.isEmpty() && removed.isEmpty()
				&& missing.isEmpty() && modified.isEmpty()
				&& untracked.isEmpty())
			return false;
		else
			return true;
	}

	/**
	 * @return list of files added to the index, not in the tree
	 */
	public Set<String> getAdded() {
		return added;
	}

	/**
	 * @return list of files changed from tree to index
	 */
	public Set<String> getChanged() {
		return changed;
	}

	/**
	 * @return list of files removed from index, but in tree
	 */
	public Set<String> getRemoved() {
		return removed;
	}

	/**
	 * @return list of files in index, but not filesystem
	 */
	public Set<String> getMissing() {
		return missing;
	}

	/**
	 * @return list of files on modified on disk relative to the index
	 */
	public Set<String> getModified() {
		return modified;
	}

	/**
	 * @return list of files that are not ignored, and not in the index.
	 */
	public Set<String> getUntracked() {
		return untracked;
	}

	/**
	 * @return list of files that are in conflict
	 */
	public Set<String> getConflicting() {
		return conflicts;
	}

	/**
	 * @return list of files with the flag assume-unchanged
	 */
	public Set<String> getAssumeUnchanged() {
		if (assumeUnchanged == null) {
			HashSet<String> unchanged = new HashSet<String>();
			for (int i = 0; i < dirCache.getEntryCount(); i++)
				if (dirCache.getEntry(i).isAssumeValid())
					unchanged.add(dirCache.getEntry(i).getPathString());
			assumeUnchanged = unchanged;
		}
		return assumeUnchanged;
	}
}
