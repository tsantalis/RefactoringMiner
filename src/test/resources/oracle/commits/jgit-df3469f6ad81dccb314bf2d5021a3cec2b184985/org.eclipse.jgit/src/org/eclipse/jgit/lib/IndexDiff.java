/*
 * Copyright (C) 2007, Dave Watson <dwatson@mimvista.com>
 * Copyright (C) 2007-2008, Robin Rosenberg <robin.rosenberg@dewire.com>
 * Copyright (C) 2010, Jens Baumgart <jens.baumgart@sap.com>
 * Copyright (C) 2013, Robin Stocker <robin@nibor.org>
 * Copyright (C) 2014, Axel Richard <axel.richard@obeo.fr>
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
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.jgit.dircache.DirCache;
import org.eclipse.jgit.dircache.DirCacheEntry;
import org.eclipse.jgit.dircache.DirCacheIterator;
import org.eclipse.jgit.errors.ConfigInvalidException;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.errors.StopWalkException;
import org.eclipse.jgit.internal.JGitText;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.submodule.SubmoduleWalk;
import org.eclipse.jgit.submodule.SubmoduleWalk.IgnoreSubmoduleMode;
import org.eclipse.jgit.treewalk.AbstractTreeIterator;
import org.eclipse.jgit.treewalk.EmptyTreeIterator;
import org.eclipse.jgit.treewalk.FileTreeIterator;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.TreeWalk.OperationType;
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

	/**
	 * Represents the state of the index for a certain path regarding the stages
	 * - which stages exist for a path and which not (base, ours, theirs).
	 * <p>
	 * This is used for figuring out what kind of conflict occurred.
	 *
	 * @see IndexDiff#getConflictingStageStates()
	 * @since 3.0
	 */
	public static enum StageState {
		/**
		 * Exists in base, but neither in ours nor in theirs.
		 */
		BOTH_DELETED(1),

		/**
		 * Only exists in ours.
		 */
		ADDED_BY_US(2),

		/**
		 * Exists in base and ours, but no in theirs.
		 */
		DELETED_BY_THEM(3),

		/**
		 * Only exists in theirs.
		 */
		ADDED_BY_THEM(4),

		/**
		 * Exists in base and theirs, but not in ours.
		 */
		DELETED_BY_US(5),

		/**
		 * Exists in ours and theirs, but not in base.
		 */
		BOTH_ADDED(6),

		/**
		 * Exists in all stages, content conflict.
		 */
		BOTH_MODIFIED(7);

		private final int stageMask;

		private StageState(int stageMask) {
			this.stageMask = stageMask;
		}

		int getStageMask() {
			return stageMask;
		}

		/**
		 * @return whether there is a "base" stage entry
		 */
		public boolean hasBase() {
			return (stageMask & 1) != 0;
		}

		/**
		 * @return whether there is an "ours" stage entry
		 */
		public boolean hasOurs() {
			return (stageMask & 2) != 0;
		}

		/**
		 * @return whether there is a "theirs" stage entry
		 */
		public boolean hasTheirs() {
			return (stageMask & 4) != 0;
		}

		static StageState fromMask(int stageMask) {
			// bits represent: theirs, ours, base
			switch (stageMask) {
			case 1: // 0b001
				return BOTH_DELETED;
			case 2: // 0b010
				return ADDED_BY_US;
			case 3: // 0b011
				return DELETED_BY_THEM;
			case 4: // 0b100
				return ADDED_BY_THEM;
			case 5: // 0b101
				return DELETED_BY_US;
			case 6: // 0b110
				return BOTH_ADDED;
			case 7: // 0b111
				return BOTH_MODIFIED;
			default:
				return null;
			}
		}
	}

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
					"Do not clone this kind of filter: " //$NON-NLS-1$
							+ getClass().getName());
		}
	}

	private final static int TREE = 0;

	private final static int INDEX = 1;

	private final static int WORKDIR = 2;

	private final Repository repository;

	private final AnyObjectId tree;

	private TreeFilter filter = null;

	private final WorkingTreeIterator initialWorkingTreeIterator;

	private Set<String> added = new HashSet<>();

	private Set<String> changed = new HashSet<>();

	private Set<String> removed = new HashSet<>();

	private Set<String> missing = new HashSet<>();

	private Set<String> modified = new HashSet<>();

	private Set<String> untracked = new HashSet<>();

	private Map<String, StageState> conflicts = new HashMap<>();

	private Set<String> ignored;

	private Set<String> assumeUnchanged;

	private DirCache dirCache;

	private IndexDiffFilter indexDiffFilter;

	private Map<String, IndexDiff> submoduleIndexDiffs = new HashMap<>();

	private IgnoreSubmoduleMode ignoreSubmoduleMode = null;

	private Map<FileMode, Set<String>> fileModes = new HashMap<>();

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
		this(repository, repository.resolve(revstr), workingTreeIterator);
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
		if (objectId != null) {
			try (RevWalk rw = new RevWalk(repository)) {
				tree = rw.parseTree(objectId);
			}
		} else {
			tree = null;
		}
		this.initialWorkingTreeIterator = workingTreeIterator;
	}

	/**
	 * @param mode
	 *            defines how modifications in submodules are treated
	 * @since 3.6
	 */
	public void setIgnoreSubmoduleMode(IgnoreSubmoduleMode mode) {
		this.ignoreSubmoduleMode = mode;
	}

	/**
	 * A factory to producing WorkingTreeIterators
	 * @since 3.6
	 */
	public interface WorkingTreeIteratorFactory {
		/**
		 * @param repo
		 * @return a WorkingTreeIterator for repo
		 */
		public WorkingTreeIterator getWorkingTreeIterator(Repository repo);
	}

	private WorkingTreeIteratorFactory wTreeIt = new WorkingTreeIteratorFactory() {
		@Override
		public WorkingTreeIterator getWorkingTreeIterator(Repository repo) {
			return new FileTreeIterator(repo);
		}
	};

	/**
	 * Allows higher layers to set the factory for WorkingTreeIterators.
	 *
	 * @param wTreeIt
	 * @since 3.6
	 */
	public void setWorkingTreeItFactory(WorkingTreeIteratorFactory wTreeIt) {
		this.wTreeIt = wTreeIt;
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
		return diff(null, 0, 0, ""); //$NON-NLS-1$
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

		try (TreeWalk treeWalk = new TreeWalk(repository)) {
			treeWalk.setOperationType(OperationType.CHECKIN_OP);
			treeWalk.setRecursive(true);
			// add the trees (tree, dirchache, workdir)
			if (tree != null)
				treeWalk.addTree(tree);
			else
				treeWalk.addTree(new EmptyTreeIterator());
			treeWalk.addTree(new DirCacheIterator(dirCache));
			treeWalk.addTree(initialWorkingTreeIterator);
			initialWorkingTreeIterator.setDirCacheIterator(treeWalk, 1);
			Collection<TreeFilter> filters = new ArrayList<>(4);

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
			indexDiffFilter = new IndexDiffFilter(INDEX, WORKDIR);
			filters.add(indexDiffFilter);
			treeWalk.setFilter(AndTreeFilter.create(filters));
			fileModes.clear();
			while (treeWalk.next()) {
				AbstractTreeIterator treeIterator = treeWalk.getTree(TREE,
						AbstractTreeIterator.class);
				DirCacheIterator dirCacheIterator = treeWalk.getTree(INDEX,
						DirCacheIterator.class);
				WorkingTreeIterator workingTreeIterator = treeWalk
						.getTree(WORKDIR, WorkingTreeIterator.class);

				if (dirCacheIterator != null) {
					final DirCacheEntry dirCacheEntry = dirCacheIterator
							.getDirCacheEntry();
					if (dirCacheEntry != null) {
						int stage = dirCacheEntry.getStage();
						if (stage > 0) {
							String path = treeWalk.getPathString();
							addConflict(path, stage);
							continue;
						}
					}
				}

				if (treeIterator != null) {
					if (dirCacheIterator != null) {
						if (!treeIterator.idEqual(dirCacheIterator)
								|| treeIterator
										.getEntryRawMode() != dirCacheIterator
												.getEntryRawMode()) {
							// in repo, in index, content diff => changed
							if (!isEntryGitLink(treeIterator)
									|| !isEntryGitLink(dirCacheIterator)
									|| ignoreSubmoduleMode != IgnoreSubmoduleMode.ALL)
								changed.add(treeWalk.getPathString());
						}
					} else {
						// in repo, not in index => removed
						if (!isEntryGitLink(treeIterator)
								|| ignoreSubmoduleMode != IgnoreSubmoduleMode.ALL)
							removed.add(treeWalk.getPathString());
						if (workingTreeIterator != null)
							untracked.add(treeWalk.getPathString());
					}
				} else {
					if (dirCacheIterator != null) {
						// not in repo, in index => added
						if (!isEntryGitLink(dirCacheIterator)
								|| ignoreSubmoduleMode != IgnoreSubmoduleMode.ALL)
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
						if (!isEntryGitLink(dirCacheIterator)
								|| ignoreSubmoduleMode != IgnoreSubmoduleMode.ALL)
							missing.add(treeWalk.getPathString());
					} else {
						if (workingTreeIterator.isModified(
								dirCacheIterator.getDirCacheEntry(), true,
								treeWalk.getObjectReader())) {
							// in index, in workdir, content differs => modified
							if (!isEntryGitLink(dirCacheIterator)
									|| !isEntryGitLink(workingTreeIterator)
									|| (ignoreSubmoduleMode != IgnoreSubmoduleMode.ALL
											&& ignoreSubmoduleMode != IgnoreSubmoduleMode.DIRTY))
								modified.add(treeWalk.getPathString());
						}
					}
				}

				String path = treeWalk.getPathString();
				if (path != null) {
					for (int i = 0; i < treeWalk.getTreeCount(); i++) {
						recordFileMode(path, treeWalk.getFileMode(i));
					}
				}
			}
		}

		if (ignoreSubmoduleMode != IgnoreSubmoduleMode.ALL) {
			IgnoreSubmoduleMode localIgnoreSubmoduleMode = ignoreSubmoduleMode;
			SubmoduleWalk smw = SubmoduleWalk.forIndex(repository);
			while (smw.next()) {
				try {
					if (localIgnoreSubmoduleMode == null)
						localIgnoreSubmoduleMode = smw.getModulesIgnore();
					if (IgnoreSubmoduleMode.ALL
							.equals(localIgnoreSubmoduleMode))
						continue;
				} catch (ConfigInvalidException e) {
					IOException e1 = new IOException(MessageFormat.format(
							JGitText.get().invalidIgnoreParamSubmodule,
							smw.getPath()));
					e1.initCause(e);
					throw e1;
				}
				Repository subRepo = smw.getRepository();
				if (subRepo != null) {
					String subRepoPath = smw.getPath();
					try {
						ObjectId subHead = subRepo.resolve("HEAD"); //$NON-NLS-1$
						if (subHead != null
								&& !subHead.equals(smw.getObjectId())) {
							modified.add(subRepoPath);
							recordFileMode(subRepoPath, FileMode.GITLINK);
						} else if (ignoreSubmoduleMode != IgnoreSubmoduleMode.DIRTY) {
							IndexDiff smid = submoduleIndexDiffs.get(smw
									.getPath());
							if (smid == null) {
								smid = new IndexDiff(subRepo,
										smw.getObjectId(),
										wTreeIt.getWorkingTreeIterator(subRepo));
								submoduleIndexDiffs.put(subRepoPath, smid);
							}
							if (smid.diff()) {
								if (ignoreSubmoduleMode == IgnoreSubmoduleMode.UNTRACKED
										&& smid.getAdded().isEmpty()
										&& smid.getChanged().isEmpty()
										&& smid.getConflicting().isEmpty()
										&& smid.getMissing().isEmpty()
										&& smid.getModified().isEmpty()
										&& smid.getRemoved().isEmpty()) {
									continue;
								}
								modified.add(subRepoPath);
								recordFileMode(subRepoPath, FileMode.GITLINK);
							}
						}
					} finally {
						subRepo.close();
					}
				}
			}

		}

		// consume the remaining work
		if (monitor != null)
			monitor.endTask();

		ignored = indexDiffFilter.getIgnoredPaths();
		if (added.isEmpty() && changed.isEmpty() && removed.isEmpty()
				&& missing.isEmpty() && modified.isEmpty()
				&& untracked.isEmpty())
			return false;
		else
			return true;
	}

	private void recordFileMode(String path, FileMode mode) {
		Set<String> values = fileModes.get(mode);
		if (path != null) {
			if (values == null) {
				values = new HashSet<>();
				fileModes.put(mode, values);
			}
			values.add(path);
		}
	}

	private boolean isEntryGitLink(AbstractTreeIterator ti) {
		return ((ti != null) && (ti.getEntryRawMode() == FileMode.GITLINK
				.getBits()));
	}

	private void addConflict(String path, int stage) {
		StageState existingStageStates = conflicts.get(path);
		byte stageMask = 0;
		if (existingStageStates != null)
			stageMask |= existingStageStates.getStageMask();
		// stage 1 (base) should be shifted 0 times
		int shifts = stage - 1;
		stageMask |= (1 << shifts);
		StageState stageState = StageState.fromMask(stageMask);
		conflicts.put(path, stageState);
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
	 * @return list of files modified on disk relative to the index
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
	 * @return list of files that are in conflict, corresponds to the keys of
	 *         {@link #getConflictingStageStates()}
	 */
	public Set<String> getConflicting() {
		return conflicts.keySet();
	}

	/**
	 * @return the map from each path of {@link #getConflicting()} to its
	 *         corresponding {@link StageState}
	 * @since 3.0
	 */
	public Map<String, StageState> getConflictingStageStates() {
		return conflicts;
	}

	/**
	 * The method returns the list of ignored files and folders. Only the root
	 * folder of an ignored folder hierarchy is reported. If a/b/c is listed in
	 * the .gitignore then you should not expect a/b/c/d/e/f to be reported
	 * here. Only a/b/c will be reported. Furthermore only ignored files /
	 * folders are returned that are NOT in the index.
	 *
	 * @return list of files / folders that are ignored
	 */
	public Set<String> getIgnoredNotInIndex() {
		return ignored;
	}

	/**
	 * @return list of files with the flag assume-unchanged
	 */
	public Set<String> getAssumeUnchanged() {
		if (assumeUnchanged == null) {
			HashSet<String> unchanged = new HashSet<>();
			for (int i = 0; i < dirCache.getEntryCount(); i++)
				if (dirCache.getEntry(i).isAssumeValid())
					unchanged.add(dirCache.getEntry(i).getPathString());
			assumeUnchanged = unchanged;
		}
		return assumeUnchanged;
	}

	/**
	 * @return list of folders containing only untracked files/folders
	 */
	public Set<String> getUntrackedFolders() {
		return ((indexDiffFilter == null) ? Collections.<String> emptySet()
				: new HashSet<>(indexDiffFilter.getUntrackedFolders()));
	}

	/**
	 * Get the file mode of the given path in the index
	 *
	 * @param path
	 * @return file mode
	 */
	public FileMode getIndexMode(final String path) {
		final DirCacheEntry entry = dirCache.getEntry(path);
		return entry != null ? entry.getFileMode() : FileMode.MISSING;
	}

	/**
	 * Get the list of paths that IndexDiff has detected to differ and have the
	 * given file mode
	 *
	 * @param mode
	 * @return the list of paths that IndexDiff has detected to differ and have
	 *         the given file mode
	 * @since 3.6
	 */
	public Set<String> getPathsWithIndexMode(final FileMode mode) {
		Set<String> paths = fileModes.get(mode);
		if (paths == null)
			paths = new HashSet<>();
		return paths;
	}
}
