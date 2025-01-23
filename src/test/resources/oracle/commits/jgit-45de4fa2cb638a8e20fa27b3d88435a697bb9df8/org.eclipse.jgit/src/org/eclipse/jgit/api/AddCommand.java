/*
 * Copyright (C) 2010, Christian Halstrick <christian.halstrick@sap.com>
 * Copyright (C) 2010, Stefan Lay <stefan.lay@sap.com> and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0 which is available at
 * https://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package org.eclipse.jgit.api;

import static org.eclipse.jgit.lib.Constants.OBJ_BLOB;
import static org.eclipse.jgit.lib.FileMode.GITLINK;
import static org.eclipse.jgit.lib.FileMode.TYPE_GITLINK;
import static org.eclipse.jgit.lib.FileMode.TYPE_TREE;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.Collection;
import java.util.LinkedList;

import org.eclipse.jgit.api.errors.FilterFailedException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.JGitInternalException;
import org.eclipse.jgit.api.errors.NoFilepatternException;
import org.eclipse.jgit.dircache.DirCache;
import org.eclipse.jgit.dircache.DirCacheBuildIterator;
import org.eclipse.jgit.dircache.DirCacheBuilder;
import org.eclipse.jgit.dircache.DirCacheEntry;
import org.eclipse.jgit.dircache.DirCacheIterator;
import org.eclipse.jgit.internal.JGitText;
import org.eclipse.jgit.lib.FileMode;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectInserter;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.treewalk.FileTreeIterator;
import org.eclipse.jgit.treewalk.NameConflictTreeWalk;
import org.eclipse.jgit.treewalk.TreeWalk.OperationType;
import org.eclipse.jgit.treewalk.WorkingTreeIterator;
import org.eclipse.jgit.treewalk.filter.PathFilterGroup;

/**
 * A class used to execute a {@code Add} command. It has setters for all
 * supported options and arguments of this command and a {@link #call()} method
 * to finally execute the command. Each instance of this class should only be
 * used for one invocation of the command (means: one call to {@link #call()})
 *
 * @see <a href="http://www.kernel.org/pub/software/scm/git/docs/git-add.html"
 *      >Git documentation about Add</a>
 */
public class AddCommand extends GitCommand<DirCache> {

	private Collection<String> filepatterns;

	private WorkingTreeIterator workingTreeIterator;

	private boolean update = false;

	/**
	 * Constructor for AddCommand
	 *
	 * @param repo
	 *            the {@link org.eclipse.jgit.lib.Repository}
	 */
	public AddCommand(Repository repo) {
		super(repo);
		filepatterns = new LinkedList<>();
	}

	/**
	 * Add a path to a file/directory whose content should be added.
	 * <p>
	 * A directory name (e.g. <code>dir</code> to add <code>dir/file1</code> and
	 * <code>dir/file2</code>) can also be given to add all files in the
	 * directory, recursively. Fileglobs (e.g. *.c) are not yet supported.
	 *
	 * @param filepattern
	 *            repository-relative path of file/directory to add (with
	 *            <code>/</code> as separator)
	 * @return {@code this}
	 */
	public AddCommand addFilepattern(String filepattern) {
		checkCallable();
		filepatterns.add(filepattern);
		return this;
	}

	/**
	 * Allow clients to provide their own implementation of a FileTreeIterator
	 *
	 * @param f
	 *            a {@link org.eclipse.jgit.treewalk.WorkingTreeIterator}
	 *            object.
	 * @return {@code this}
	 */
	public AddCommand setWorkingTreeIterator(WorkingTreeIterator f) {
		workingTreeIterator = f;
		return this;
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * Executes the {@code Add} command. Each instance of this class should only
	 * be used for one invocation of the command. Don't call this method twice
	 * on an instance.
	 */
	@Override
	public DirCache call() throws GitAPIException, NoFilepatternException {

		if (filepatterns.isEmpty())
			throw new NoFilepatternException(JGitText.get().atLeastOnePatternIsRequired);
		checkCallable();
		DirCache dc = null;
		boolean addAll = filepatterns.contains("."); //$NON-NLS-1$

		try (ObjectInserter inserter = repo.newObjectInserter();
				NameConflictTreeWalk tw = new NameConflictTreeWalk(repo)) {
			tw.setOperationType(OperationType.CHECKIN_OP);
			dc = repo.lockDirCache();

			DirCacheBuilder builder = dc.builder();
			tw.addTree(new DirCacheBuildIterator(builder));
			if (workingTreeIterator == null)
				workingTreeIterator = new FileTreeIterator(repo);
			workingTreeIterator.setDirCacheIterator(tw, 0);
			tw.addTree(workingTreeIterator);
			if (!addAll)
				tw.setFilter(PathFilterGroup.createFromStrings(filepatterns));

			byte[] lastAdded = null;

			while (tw.next()) {
				DirCacheIterator c = tw.getTree(0, DirCacheIterator.class);
				WorkingTreeIterator f = tw.getTree(1, WorkingTreeIterator.class);
				if (c == null && f != null && f.isEntryIgnored()) {
					// file is not in index but is ignored, do nothing
					continue;
				} else if (c == null && update) {
					// Only update of existing entries was requested.
					continue;
				}

				DirCacheEntry entry = c != null ? c.getDirCacheEntry() : null;
				if (entry != null && entry.getStage() > 0
						&& lastAdded != null
						&& lastAdded.length == tw.getPathLength()
						&& tw.isPathPrefix(lastAdded, lastAdded.length) == 0) {
					// In case of an existing merge conflict the
					// DirCacheBuildIterator iterates over all stages of
					// this path, we however want to add only one
					// new DirCacheEntry per path.
					continue;
				}

				if (tw.isSubtree() && !tw.isDirectoryFileConflict()) {
					tw.enterSubtree();
					continue;
				}

				if (f == null) { // working tree file does not exist
					if (entry != null
							&& (!update || GITLINK == entry.getFileMode())) {
						builder.add(entry);
					}
					continue;
				}

				if (entry != null && entry.isAssumeValid()) {
					// Index entry is marked assume valid. Even though
					// the user specified the file to be added JGit does
					// not consider the file for addition.
					builder.add(entry);
					continue;
				}

				if ((f.getEntryRawMode() == TYPE_TREE
						&& f.getIndexFileMode(c) != FileMode.GITLINK) ||
						(f.getEntryRawMode() == TYPE_GITLINK
								&& f.getIndexFileMode(c) == FileMode.TREE)) {
					// Index entry exists and is symlink, gitlink or file,
					// otherwise the tree would have been entered above.
					// Replace the index entry by diving into tree of files.
					tw.enterSubtree();
					continue;
				}

				byte[] path = tw.getRawPath();
				if (entry == null || entry.getStage() > 0) {
					entry = new DirCacheEntry(path);
				}
				FileMode mode = f.getIndexFileMode(c);
				entry.setFileMode(mode);

				if (GITLINK != mode) {
					entry.setLength(f.getEntryLength());
					entry.setLastModified(f.getEntryLastModifiedInstant());
					long len = f.getEntryContentLength();
					// We read and filter the content multiple times.
					// f.getEntryContentLength() reads and filters the input and
					// inserter.insert(...) does it again. That's because an
					// ObjectInserter needs to know the length before it starts
					// inserting. TODO: Fix this by using Buffers.
					try (InputStream in = f.openEntryStream()) {
						ObjectId id = inserter.insert(OBJ_BLOB, len, in);
						entry.setObjectId(id);
					}
				} else {
					entry.setLength(0);
					entry.setLastModified(Instant.ofEpochSecond(0));
					entry.setObjectId(f.getEntryObjectId());
				}
				builder.add(entry);
				lastAdded = path;
			}
			inserter.flush();
			builder.commit();
			setCallable(false);
		} catch (IOException e) {
			Throwable cause = e.getCause();
			if (cause != null && cause instanceof FilterFailedException)
				throw (FilterFailedException) cause;
			throw new JGitInternalException(
					JGitText.get().exceptionCaughtDuringExecutionOfAddCommand, e);
		} finally {
			if (dc != null)
				dc.unlock();
		}

		return dc;
	}

	/**
	 * Set whether to only match against already tracked files
	 *
	 * @param update
	 *            If set to true, the command only matches {@code filepattern}
	 *            against already tracked files in the index rather than the
	 *            working tree. That means that it will never stage new files,
	 *            but that it will stage modified new contents of tracked files
	 *            and that it will remove files from the index if the
	 *            corresponding files in the working tree have been removed. In
	 *            contrast to the git command line a {@code filepattern} must
	 *            exist also if update is set to true as there is no concept of
	 *            a working directory here.
	 * @return {@code this}
	 */
	public AddCommand setUpdate(boolean update) {
		this.update = update;
		return this;
	}

	/**
	 * Whether to only match against already tracked files
	 *
	 * @return whether to only match against already tracked files
	 */
	public boolean isUpdate() {
		return update;
	}
}
