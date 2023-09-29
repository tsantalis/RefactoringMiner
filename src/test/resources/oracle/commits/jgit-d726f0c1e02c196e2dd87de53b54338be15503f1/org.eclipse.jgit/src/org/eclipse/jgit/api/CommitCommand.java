/*
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

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.jgit.api.errors.AbortedByHookException;
import org.eclipse.jgit.api.errors.ConcurrentRefUpdateException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.JGitInternalException;
import org.eclipse.jgit.api.errors.NoFilepatternException;
import org.eclipse.jgit.api.errors.NoHeadException;
import org.eclipse.jgit.api.errors.NoMessageException;
import org.eclipse.jgit.api.errors.UnmergedPathsException;
import org.eclipse.jgit.api.errors.WrongRepositoryStateException;
import org.eclipse.jgit.dircache.DirCache;
import org.eclipse.jgit.dircache.DirCacheBuildIterator;
import org.eclipse.jgit.dircache.DirCacheBuilder;
import org.eclipse.jgit.dircache.DirCacheEntry;
import org.eclipse.jgit.dircache.DirCacheIterator;
import org.eclipse.jgit.errors.UnmergedPathException;
import org.eclipse.jgit.hooks.Hooks;
import org.eclipse.jgit.internal.JGitText;
import org.eclipse.jgit.lib.CommitBuilder;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.FileMode;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectInserter;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.RefUpdate;
import org.eclipse.jgit.lib.RefUpdate.Result;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.RepositoryState;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevObject;
import org.eclipse.jgit.revwalk.RevTag;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.treewalk.FileTreeIterator;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.util.ChangeIdUtil;

/**
 * A class used to execute a {@code Commit} command. It has setters for all
 * supported options and arguments of this command and a {@link #call()} method
 * to finally execute the command.
 *
 * @see <a
 *      href="http://www.kernel.org/pub/software/scm/git/docs/git-commit.html"
 *      >Git documentation about Commit</a>
 */
public class CommitCommand extends GitCommand<RevCommit> {
	private PersonIdent author;

	private PersonIdent committer;

	private String message;

	private boolean all;

	private List<String> only = new ArrayList<String>();

	private boolean[] onlyProcessed;

	private boolean amend;

	private boolean insertChangeId;

	/**
	 * parents this commit should have. The current HEAD will be in this list
	 * and also all commits mentioned in .git/MERGE_HEAD
	 */
	private List<ObjectId> parents = new LinkedList<ObjectId>();

	private String reflogComment;

	/**
	 * Setting this option bypasses the pre-commit and commit-msg hooks.
	 */
	private boolean noVerify;

	private PrintStream hookOutRedirect;

	/**
	 * @param repo
	 */
	protected CommitCommand(Repository repo) {
		super(repo);
	}

	/**
	 * Executes the {@code commit} command with all the options and parameters
	 * collected by the setter methods of this class. Each instance of this
	 * class should only be used for one invocation of the command (means: one
	 * call to {@link #call()})
	 *
	 * @return a {@link RevCommit} object representing the successful commit.
	 * @throws NoHeadException
	 *             when called on a git repo without a HEAD reference
	 * @throws NoMessageException
	 *             when called without specifying a commit message
	 * @throws UnmergedPathsException
	 *             when the current index contained unmerged paths (conflicts)
	 * @throws ConcurrentRefUpdateException
	 *             when HEAD or branch ref is updated concurrently by someone
	 *             else
	 * @throws WrongRepositoryStateException
	 *             when repository is not in the right state for committing
	 * @throws AbortedByHookException
	 *             if there are either pre-commit or commit-msg hooks present in
	 *             the repository and one of them rejects the commit.
	 */
	public RevCommit call() throws GitAPIException, NoHeadException,
			NoMessageException, UnmergedPathsException,
			ConcurrentRefUpdateException, WrongRepositoryStateException,
			AbortedByHookException {
		checkCallable();
		Collections.sort(only);

		try (RevWalk rw = new RevWalk(repo)) {
			RepositoryState state = repo.getRepositoryState();
			if (!state.canCommit())
				throw new WrongRepositoryStateException(MessageFormat.format(
						JGitText.get().cannotCommitOnARepoWithState,
						state.name()));

			if (!noVerify) {
				Hooks.preCommit(repo, hookOutRedirect).call();
			}

			processOptions(state, rw);

			if (all && !repo.isBare() && repo.getWorkTree() != null) {
				try (Git git = new Git(repo)) {
					git.add()
							.addFilepattern(".") //$NON-NLS-1$
							.setUpdate(true).call();
				} catch (NoFilepatternException e) {
					// should really not happen
					throw new JGitInternalException(e.getMessage(), e);
				}
			}

			Ref head = repo.getRef(Constants.HEAD);
			if (head == null)
				throw new NoHeadException(
						JGitText.get().commitOnRepoWithoutHEADCurrentlyNotSupported);

			// determine the current HEAD and the commit it is referring to
			ObjectId headId = repo.resolve(Constants.HEAD + "^{commit}"); //$NON-NLS-1$
			if (headId == null && amend)
				throw new WrongRepositoryStateException(
						JGitText.get().commitAmendOnInitialNotPossible);

			if (headId != null)
				if (amend) {
					RevCommit previousCommit = rw.parseCommit(headId);
					for (RevCommit p : previousCommit.getParents())
						parents.add(p.getId());
					if (author == null)
						author = previousCommit.getAuthorIdent();
				} else {
					parents.add(0, headId);
				}

			if (!noVerify) {
				message = Hooks.commitMsg(repo, hookOutRedirect)
						.setCommitMessage(message).call();
			}

			// lock the index
			DirCache index = repo.lockDirCache();
			try (ObjectInserter odi = repo.newObjectInserter()) {
				if (!only.isEmpty())
					index = createTemporaryIndex(headId, index, rw);

				// Write the index as tree to the object database. This may
				// fail for example when the index contains unmerged paths
				// (unresolved conflicts)
				ObjectId indexTreeId = index.writeTree(odi);

				if (insertChangeId)
					insertChangeId(indexTreeId);

				// Create a Commit object, populate it and write it
				CommitBuilder commit = new CommitBuilder();
				commit.setCommitter(committer);
				commit.setAuthor(author);
				commit.setMessage(message);

				commit.setParentIds(parents);
				commit.setTreeId(indexTreeId);
				ObjectId commitId = odi.insert(commit);
				odi.flush();

				RevCommit revCommit = rw.parseCommit(commitId);
				RefUpdate ru = repo.updateRef(Constants.HEAD);
				ru.setNewObjectId(commitId);
				if (reflogComment != null) {
					ru.setRefLogMessage(reflogComment, false);
				} else {
					String prefix = amend ? "commit (amend): " //$NON-NLS-1$
							: parents.size() == 0 ? "commit (initial): " //$NON-NLS-1$
									: "commit: "; //$NON-NLS-1$
					ru.setRefLogMessage(prefix + revCommit.getShortMessage(),
							false);
				}
				if (headId != null)
					ru.setExpectedOldObjectId(headId);
				else
					ru.setExpectedOldObjectId(ObjectId.zeroId());
				Result rc = ru.forceUpdate();
				switch (rc) {
				case NEW:
				case FORCED:
				case FAST_FORWARD: {
					setCallable(false);
					if (state == RepositoryState.MERGING_RESOLVED
							|| isMergeDuringRebase(state)) {
						// Commit was successful. Now delete the files
						// used for merge commits
						repo.writeMergeCommitMsg(null);
						repo.writeMergeHeads(null);
					} else if (state == RepositoryState.CHERRY_PICKING_RESOLVED) {
						repo.writeMergeCommitMsg(null);
						repo.writeCherryPickHead(null);
					} else if (state == RepositoryState.REVERTING_RESOLVED) {
						repo.writeMergeCommitMsg(null);
						repo.writeRevertHead(null);
					}
					return revCommit;
				}
				case REJECTED:
				case LOCK_FAILURE:
					throw new ConcurrentRefUpdateException(
							JGitText.get().couldNotLockHEAD, ru.getRef(), rc);
				default:
					throw new JGitInternalException(MessageFormat.format(
							JGitText.get().updatingRefFailed, Constants.HEAD,
							commitId.toString(), rc));
				}
			} finally {
				index.unlock();
			}
		} catch (UnmergedPathException e) {
			throw new UnmergedPathsException(e);
		} catch (IOException e) {
			throw new JGitInternalException(
					JGitText.get().exceptionCaughtDuringExecutionOfCommitCommand, e);
		}
	}

	private void insertChangeId(ObjectId treeId) throws IOException {
		ObjectId firstParentId = null;
		if (!parents.isEmpty())
			firstParentId = parents.get(0);
		ObjectId changeId = ChangeIdUtil.computeChangeId(treeId, firstParentId,
				author, committer, message);
		message = ChangeIdUtil.insertId(message, changeId);
		if (changeId != null)
			message = message.replaceAll("\nChange-Id: I" //$NON-NLS-1$
					+ ObjectId.zeroId().getName() + "\n", "\nChange-Id: I" //$NON-NLS-1$ //$NON-NLS-2$
					+ changeId.getName() + "\n"); //$NON-NLS-1$
	}

	private DirCache createTemporaryIndex(ObjectId headId, DirCache index,
			RevWalk rw)
			throws IOException {
		ObjectInserter inserter = null;

		// get DirCacheBuilder for existing index
		DirCacheBuilder existingBuilder = index.builder();

		// get DirCacheBuilder for newly created in-core index to build a
		// temporary index for this commit
		DirCache inCoreIndex = DirCache.newInCore();
		DirCacheBuilder tempBuilder = inCoreIndex.builder();

		onlyProcessed = new boolean[only.size()];
		boolean emptyCommit = true;

		try (TreeWalk treeWalk = new TreeWalk(repo)) {
			int dcIdx = treeWalk
					.addTree(new DirCacheBuildIterator(existingBuilder));
			int fIdx = treeWalk.addTree(new FileTreeIterator(repo));
			int hIdx = -1;
			if (headId != null)
				hIdx = treeWalk.addTree(rw.parseTree(headId));
			treeWalk.setRecursive(true);

			String lastAddedFile = null;
			while (treeWalk.next()) {
				String path = treeWalk.getPathString();
				// check if current entry's path matches a specified path
				int pos = lookupOnly(path);

				CanonicalTreeParser hTree = null;
				if (hIdx != -1)
					hTree = treeWalk.getTree(hIdx, CanonicalTreeParser.class);

				DirCacheIterator dcTree = treeWalk.getTree(dcIdx,
						DirCacheIterator.class);

				if (pos >= 0) {
					// include entry in commit

					FileTreeIterator fTree = treeWalk.getTree(fIdx,
							FileTreeIterator.class);

					// check if entry refers to a tracked file
					boolean tracked = dcTree != null || hTree != null;
					if (!tracked)
						break;

					// for an unmerged path, DirCacheBuildIterator will yield 3
					// entries, we only want to add one
					if (path.equals(lastAddedFile))
						continue;

					lastAddedFile = path;

					if (fTree != null) {
						// create a new DirCacheEntry with data retrieved from
						// disk
						final DirCacheEntry dcEntry = new DirCacheEntry(path);
						long entryLength = fTree.getEntryLength();
						dcEntry.setLength(entryLength);
						dcEntry.setLastModified(fTree.getEntryLastModified());
						dcEntry.setFileMode(fTree.getIndexFileMode(dcTree));

						boolean objectExists = (dcTree != null
								&& fTree.idEqual(dcTree))
								|| (hTree != null && fTree.idEqual(hTree));
						if (objectExists) {
							dcEntry.setObjectId(fTree.getEntryObjectId());
						} else {
							if (FileMode.GITLINK.equals(dcEntry.getFileMode()))
								dcEntry.setObjectId(fTree.getEntryObjectId());
							else {
								// insert object
								if (inserter == null)
									inserter = repo.newObjectInserter();
								long contentLength = fTree
										.getEntryContentLength();
								InputStream inputStream = fTree
										.openEntryStream();
								try {
									dcEntry.setObjectId(inserter.insert(
											Constants.OBJ_BLOB, contentLength,
											inputStream));
								} finally {
									inputStream.close();
								}
							}
						}

						// add to existing index
						existingBuilder.add(dcEntry);
						// add to temporary in-core index
						tempBuilder.add(dcEntry);

						if (emptyCommit
								&& (hTree == null || !hTree.idEqual(fTree)
										|| hTree.getEntryRawMode() != fTree
												.getEntryRawMode()))
							// this is a change
							emptyCommit = false;
					} else {
						// if no file exists on disk, neither add it to
						// index nor to temporary in-core index

						if (emptyCommit && hTree != null)
							// this is a change
							emptyCommit = false;
					}

					// keep track of processed path
					onlyProcessed[pos] = true;
				} else {
					// add entries from HEAD for all other paths
					if (hTree != null) {
						// create a new DirCacheEntry with data retrieved from
						// HEAD
						final DirCacheEntry dcEntry = new DirCacheEntry(path);
						dcEntry.setObjectId(hTree.getEntryObjectId());
						dcEntry.setFileMode(hTree.getEntryFileMode());

						// add to temporary in-core index
						tempBuilder.add(dcEntry);
					}

					// preserve existing entry in index
					if (dcTree != null)
						existingBuilder.add(dcTree.getDirCacheEntry());
				}
			}
		}

		// there must be no unprocessed paths left at this point; otherwise an
		// untracked or unknown path has been specified
		for (int i = 0; i < onlyProcessed.length; i++)
			if (!onlyProcessed[i])
				throw new JGitInternalException(MessageFormat.format(
						JGitText.get().entryNotFoundByPath, only.get(i)));

		// there must be at least one change
		if (emptyCommit)
			throw new JGitInternalException(JGitText.get().emptyCommit);

		// update index
		existingBuilder.commit();
		// finish temporary in-core index used for this commit
		tempBuilder.finish();
		return inCoreIndex;
	}

	/**
	 * Look an entry's path up in the list of paths specified by the --only/ -o
	 * option
	 *
	 * In case the complete (file) path (e.g. "d1/d2/f1") cannot be found in
	 * <code>only</code>, lookup is also tried with (parent) directory paths
	 * (e.g. "d1/d2" and "d1").
	 *
	 * @param pathString
	 *            entry's path
	 * @return the item's index in <code>only</code>; -1 if no item matches
	 */
	private int lookupOnly(String pathString) {
		String p = pathString;
		while (true) {
			int position = Collections.binarySearch(only, p);
			if (position >= 0)
				return position;
			int l = p.lastIndexOf("/"); //$NON-NLS-1$
			if (l < 1)
				break;
			p = p.substring(0, l);
		}
		return -1;
	}

	/**
	 * Sets default values for not explicitly specified options. Then validates
	 * that all required data has been provided.
	 *
	 * @param state
	 *            the state of the repository we are working on
	 * @param rw
	 *            the RevWalk to use
	 *
	 * @throws NoMessageException
	 *             if the commit message has not been specified
	 */
	private void processOptions(RepositoryState state, RevWalk rw)
			throws NoMessageException {
		if (committer == null)
			committer = new PersonIdent(repo);
		if (author == null && !amend)
			author = committer;

		// when doing a merge commit parse MERGE_HEAD and MERGE_MSG files
		if (state == RepositoryState.MERGING_RESOLVED
				|| isMergeDuringRebase(state)) {
			try {
				parents = repo.readMergeHeads();
				if (parents != null)
					for (int i = 0; i < parents.size(); i++) {
						RevObject ro = rw.parseAny(parents.get(i));
						if (ro instanceof RevTag)
							parents.set(i, rw.peel(ro));
					}
			} catch (IOException e) {
				throw new JGitInternalException(MessageFormat.format(
						JGitText.get().exceptionOccurredDuringReadingOfGIT_DIR,
						Constants.MERGE_HEAD, e), e);
			}
			if (message == null) {
				try {
					message = repo.readMergeCommitMsg();
				} catch (IOException e) {
					throw new JGitInternalException(MessageFormat.format(
							JGitText.get().exceptionOccurredDuringReadingOfGIT_DIR,
							Constants.MERGE_MSG, e), e);
				}
			}
		} else if (state == RepositoryState.SAFE && message == null) {
			try {
				message = repo.readSquashCommitMsg();
				if (message != null)
					repo.writeSquashCommitMsg(null /* delete */);
			} catch (IOException e) {
				throw new JGitInternalException(MessageFormat.format(
						JGitText.get().exceptionOccurredDuringReadingOfGIT_DIR,
						Constants.MERGE_MSG, e), e);
			}

		}
		if (message == null)
			// as long as we don't support -C option we have to have
			// an explicit message
			throw new NoMessageException(JGitText.get().commitMessageNotSpecified);
	}

	private boolean isMergeDuringRebase(RepositoryState state) {
		if (state != RepositoryState.REBASING_INTERACTIVE
				&& state != RepositoryState.REBASING_MERGE)
			return false;
		try {
			return repo.readMergeHeads() != null;
		} catch (IOException e) {
			throw new JGitInternalException(MessageFormat.format(
					JGitText.get().exceptionOccurredDuringReadingOfGIT_DIR,
					Constants.MERGE_HEAD, e), e);
		}
	}

	/**
	 * @param message
	 *            the commit message used for the {@code commit}
	 * @return {@code this}
	 */
	public CommitCommand setMessage(String message) {
		checkCallable();
		this.message = message;
		return this;
	}

	/**
	 * @return the commit message used for the <code>commit</code>
	 */
	public String getMessage() {
		return message;
	}

	/**
	 * Sets the committer for this {@code commit}. If no committer is explicitly
	 * specified because this method is never called or called with {@code null}
	 * value then the committer will be deduced from config info in repository,
	 * with current time.
	 *
	 * @param committer
	 *            the committer used for the {@code commit}
	 * @return {@code this}
	 */
	public CommitCommand setCommitter(PersonIdent committer) {
		checkCallable();
		this.committer = committer;
		return this;
	}

	/**
	 * Sets the committer for this {@code commit}. If no committer is explicitly
	 * specified because this method is never called then the committer will be
	 * deduced from config info in repository, with current time.
	 *
	 * @param name
	 *            the name of the committer used for the {@code commit}
	 * @param email
	 *            the email of the committer used for the {@code commit}
	 * @return {@code this}
	 */
	public CommitCommand setCommitter(String name, String email) {
		checkCallable();
		return setCommitter(new PersonIdent(name, email));
	}

	/**
	 * @return the committer used for the {@code commit}. If no committer was
	 *         specified {@code null} is returned and the default
	 *         {@link PersonIdent} of this repo is used during execution of the
	 *         command
	 */
	public PersonIdent getCommitter() {
		return committer;
	}

	/**
	 * Sets the author for this {@code commit}. If no author is explicitly
	 * specified because this method is never called or called with {@code null}
	 * value then the author will be set to the committer or to the original
	 * author when amending.
	 *
	 * @param author
	 *            the author used for the {@code commit}
	 * @return {@code this}
	 */
	public CommitCommand setAuthor(PersonIdent author) {
		checkCallable();
		this.author = author;
		return this;
	}

	/**
	 * Sets the author for this {@code commit}. If no author is explicitly
	 * specified because this method is never called then the author will be set
	 * to the committer or to the original author when amending.
	 *
	 * @param name
	 *            the name of the author used for the {@code commit}
	 * @param email
	 *            the email of the author used for the {@code commit}
	 * @return {@code this}
	 */
	public CommitCommand setAuthor(String name, String email) {
		checkCallable();
		return setAuthor(new PersonIdent(name, email));
	}

	/**
	 * @return the author used for the {@code commit}. If no author was
	 *         specified {@code null} is returned and the default
	 *         {@link PersonIdent} of this repo is used during execution of the
	 *         command
	 */
	public PersonIdent getAuthor() {
		return author;
	}

	/**
	 * If set to true the Commit command automatically stages files that have
	 * been modified and deleted, but new files not known by the repository are
	 * not affected. This corresponds to the parameter -a on the command line.
	 *
	 * @param all
	 * @return {@code this}
	 * @throws JGitInternalException
	 *             in case of an illegal combination of arguments/ options
	 */
	public CommitCommand setAll(boolean all) {
		checkCallable();
		if (!only.isEmpty())
			throw new JGitInternalException(MessageFormat.format(
					JGitText.get().illegalCombinationOfArguments, "--all", //$NON-NLS-1$
					"--only")); //$NON-NLS-1$
		this.all = all;
		return this;
	}

	/**
	 * Used to amend the tip of the current branch. If set to true, the previous
	 * commit will be amended. This is equivalent to --amend on the command
	 * line.
	 *
	 * @param amend
	 * @return {@code this}
	 */
	public CommitCommand setAmend(boolean amend) {
		checkCallable();
		this.amend = amend;
		return this;
	}

	/**
	 * Commit dedicated path only.
	 * <p>
	 * This method can be called several times to add multiple paths. Full file
	 * paths are supported as well as directory paths; in the latter case this
	 * commits all files/directories below the specified path.
	 *
	 * @param only
	 *            path to commit (with <code>/</code> as separator)
	 * @return {@code this}
	 */
	public CommitCommand setOnly(String only) {
		checkCallable();
		if (all)
			throw new JGitInternalException(MessageFormat.format(
					JGitText.get().illegalCombinationOfArguments, "--only", //$NON-NLS-1$
					"--all")); //$NON-NLS-1$
		String o = only.endsWith("/") ? only.substring(0, only.length() - 1) //$NON-NLS-1$
				: only;
		// ignore duplicates
		if (!this.only.contains(o))
			this.only.add(o);
		return this;
	}

	/**
	 * If set to true a change id will be inserted into the commit message
	 *
	 * An existing change id is not replaced. An initial change id (I000...)
	 * will be replaced by the change id.
	 *
	 * @param insertChangeId
	 *
	 * @return {@code this}
	 */
	public CommitCommand setInsertChangeId(boolean insertChangeId) {
		checkCallable();
		this.insertChangeId = insertChangeId;
		return this;
	}

	/**
	 * Override the message written to the reflog
	 *
	 * @param reflogComment
	 * @return {@code this}
	 */
	public CommitCommand setReflogComment(String reflogComment) {
		this.reflogComment = reflogComment;
		return this;
	}

	/**
	 * Sets the {@link #noVerify} option on this commit command.
	 * <p>
	 * Both the pre-commit and commit-msg hooks can block a commit by their
	 * return value; setting this option to <code>true</code> will bypass these
	 * two hooks.
	 * </p>
	 *
	 * @param noVerify
	 *            Whether this commit should be verified by the pre-commit and
	 *            commit-msg hooks.
	 * @return {@code this}
	 * @since 3.7
	 */
	public CommitCommand setNoVerify(boolean noVerify) {
		this.noVerify = noVerify;
		return this;
	}

	/**
	 * Set the output stream for hook scripts executed by this command. If not
	 * set it defaults to {@code System.out}.
	 *
	 * @param hookStdOut
	 *            the output stream for hook scripts executed by this command
	 * @return {@code this}
	 * @since 3.7
	 */
	public CommitCommand setHookOutputStream(PrintStream hookStdOut) {
		this.hookOutRedirect = hookStdOut;
		return this;
	}
}
