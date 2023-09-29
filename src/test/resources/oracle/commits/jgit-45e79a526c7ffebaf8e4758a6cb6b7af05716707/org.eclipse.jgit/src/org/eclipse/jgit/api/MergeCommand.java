/*
 * Copyright (C) 2010, Christian Halstrick <christian.halstrick@sap.com>
 * Copyright (C) 2010, Stefan Lay <stefan.lay@sap.com>
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
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.eclipse.jgit.JGitText;
import org.eclipse.jgit.api.MergeResult.MergeStatus;
import org.eclipse.jgit.dircache.DirCacheCheckout;
import org.eclipse.jgit.lib.AnyObjectId;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectIdRef;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.RefUpdate;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.Ref.Storage;
import org.eclipse.jgit.lib.RefUpdate.Result;
import org.eclipse.jgit.merge.MergeStrategy;
import org.eclipse.jgit.merge.ResolveMerger;
import org.eclipse.jgit.merge.ResolveMerger.MergeFailureReason;
import org.eclipse.jgit.merge.ThreeWayMerger;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.FileTreeIterator;

/**
 * A class used to execute a {@code Merge} command. It has setters for all
 * supported options and arguments of this command and a {@link #call()} method
 * to finally execute the command. Each instance of this class should only be
 * used for one invocation of the command (means: one call to {@link #call()})
 * <p>
 * This is currently a very basic implementation which takes only one commits to
 * merge with as option. Furthermore it does supports only fast forward.
 *
 * @see <a href="http://www.kernel.org/pub/software/scm/git/docs/git-merge.html"
 *      >Git documentation about Merge</a>
 */
public class MergeCommand extends GitCommand<MergeResult> {

	private MergeStrategy mergeStrategy = MergeStrategy.SIMPLE_TWO_WAY_IN_CORE;

	private List<Ref> commits = new LinkedList<Ref>();

	/**
	 * @param repo
	 */
	protected MergeCommand(Repository repo) {
		super(repo);
	}

	/**
	 * Executes the {@code Merge} command with all the options and parameters
	 * collected by the setter methods (e.g. {@link #include(Ref)}) of this
	 * class. Each instance of this class should only be used for one invocation
	 * of the command. Don't call this method twice on an instance.
	 *
	 * @return the result of the merge
	 */
	public MergeResult call() throws NoHeadException,
			ConcurrentRefUpdateException, CheckoutConflictException,
			InvalidMergeHeadsException, WrongRepositoryStateException, NoMessageException {
		checkCallable();

		if (commits.size() != 1)
			throw new InvalidMergeHeadsException(
					commits.isEmpty() ? JGitText.get().noMergeHeadSpecified
							: MessageFormat.format(
									JGitText.get().mergeStrategyDoesNotSupportHeads,
									mergeStrategy.getName(),
									Integer.valueOf(commits.size())));

		RevWalk revWalk = null;
		try {
			Ref head = repo.getRef(Constants.HEAD);
			if (head == null)
				throw new NoHeadException(
						JGitText.get().commitOnRepoWithoutHEADCurrentlyNotSupported);
			StringBuilder refLogMessage = new StringBuilder("merge ");

			// Check for FAST_FORWARD, ALREADY_UP_TO_DATE
			revWalk = new RevWalk(repo);
			RevCommit headCommit = revWalk.lookupCommit(head.getObjectId());

			// we know for know there is only one commit
			Ref ref = commits.get(0);

			refLogMessage.append(ref.getName());

			// handle annotated tags
			ObjectId objectId = ref.getPeeledObjectId();
			if (objectId == null)
				objectId = ref.getObjectId();

			RevCommit srcCommit = revWalk.lookupCommit(objectId);
			if (revWalk.isMergedInto(srcCommit, headCommit)) {
				setCallable(false);
				return new MergeResult(headCommit, srcCommit, new ObjectId[] {
						headCommit, srcCommit },
						MergeStatus.ALREADY_UP_TO_DATE, mergeStrategy, null, null);
			} else if (revWalk.isMergedInto(headCommit, srcCommit)) {
				// FAST_FORWARD detected: skip doing a real merge but only
				// update HEAD
				refLogMessage.append(": " + MergeStatus.FAST_FORWARD);
				DirCacheCheckout dco = new DirCacheCheckout(repo,
						headCommit.getTree(), repo.lockDirCache(),
						srcCommit.getTree());
				dco.setFailOnConflict(true);
				dco.checkout();

				updateHead(refLogMessage, srcCommit, head.getObjectId());
				setCallable(false);
				return new MergeResult(srcCommit, srcCommit, new ObjectId[] {
						headCommit, srcCommit }, MergeStatus.FAST_FORWARD,
						mergeStrategy, null, null);
			} else {
				repo.writeMergeCommitMsg("merging " + ref.getName() + " into "
						+ head.getName());
				repo.writeMergeHeads(Arrays.asList(ref.getObjectId()));
				ThreeWayMerger merger = (ThreeWayMerger) mergeStrategy
						.newMerger(repo);
				boolean noProblems;
				Map<String, org.eclipse.jgit.merge.MergeResult> lowLevelResults = null;
				Map<String, MergeFailureReason> failingPaths = null;
				if (merger instanceof ResolveMerger) {
					ResolveMerger resolveMerger = (ResolveMerger) merger;
					resolveMerger.setCommitNames(new String[] {
							"BASE", "HEAD", ref.getName() });
					resolveMerger.setWorkingTreeIt(new FileTreeIterator(repo));
					noProblems = merger.merge(headCommit, srcCommit);
					lowLevelResults = resolveMerger
							.getMergeResults();
					failingPaths = resolveMerger.getFailingPathes();
				} else
					noProblems = merger.merge(headCommit, srcCommit);

				if (noProblems) {
					DirCacheCheckout dco = new DirCacheCheckout(repo,
							headCommit.getTree(), repo.lockDirCache(),
							merger.getResultTreeId());
					dco.setFailOnConflict(true);
					dco.checkout();
					RevCommit newHead = new Git(getRepository()).commit().call();
					return new MergeResult(newHead.getId(),
							null, new ObjectId[] {
									headCommit.getId(), srcCommit.getId() },
							MergeStatus.MERGED, mergeStrategy, null, null);
				} else {
					if (failingPaths != null) {
						repo.writeMergeCommitMsg(null);
						repo.writeMergeHeads(null);
						return new MergeResult(null,
								merger.getBaseCommit(0, 1),
								new ObjectId[] {
										headCommit.getId(), srcCommit.getId() },
								MergeStatus.FAILED, mergeStrategy,
								lowLevelResults, null);
					} else
						return new MergeResult(null,
								merger.getBaseCommit(0, 1),
								new ObjectId[] { headCommit.getId(),
										srcCommit.getId() },
								MergeStatus.CONFLICTING, mergeStrategy,
								lowLevelResults, null);
				}
			}
		} catch (IOException e) {
			throw new JGitInternalException(
					MessageFormat.format(
							JGitText.get().exceptionCaughtDuringExecutionOfMergeCommand,
							e), e);
		} finally {
			if (revWalk != null)
				revWalk.release();
		}
	}

	private void updateHead(StringBuilder refLogMessage, ObjectId newHeadId,
			ObjectId oldHeadID) throws IOException,
			ConcurrentRefUpdateException {
		RefUpdate refUpdate = repo.updateRef(Constants.HEAD);
		refUpdate.setNewObjectId(newHeadId);
		refUpdate.setRefLogMessage(refLogMessage.toString(), false);
		refUpdate.setExpectedOldObjectId(oldHeadID);
		Result rc = refUpdate.update();
		switch (rc) {
		case NEW:
		case FAST_FORWARD:
			return;
		case REJECTED:
		case LOCK_FAILURE:
			throw new ConcurrentRefUpdateException(
					JGitText.get().couldNotLockHEAD, refUpdate.getRef(), rc);
		default:
			throw new JGitInternalException(MessageFormat.format(
					JGitText.get().updatingRefFailed, Constants.HEAD,
					newHeadId.toString(), rc));
		}
	}

	/**
	 *
	 * @param mergeStrategy
	 *            the {@link MergeStrategy} to be used
	 * @return {@code this}
	 */
	public MergeCommand setStrategy(MergeStrategy mergeStrategy) {
		checkCallable();
		this.mergeStrategy = mergeStrategy;
		return this;
	}

	/**
	 * @param commit
	 *            a reference to a commit which is merged with the current head
	 * @return {@code this}
	 */
	public MergeCommand include(Ref commit) {
		checkCallable();
		commits.add(commit);
		return this;
	}

	/**
	 * @param commit
	 *            the Id of a commit which is merged with the current head
	 * @return {@code this}
	 */
	public MergeCommand include(AnyObjectId commit) {
		return include(commit.getName(), commit);
	}

	/**
	 * @param name
	 *            a name given to the commit
	 * @param commit
	 *            the Id of a commit which is merged with the current head
	 * @return {@code this}
	 */
	public MergeCommand include(String name, AnyObjectId commit) {
		return include(new ObjectIdRef.Unpeeled(Storage.LOOSE, name,
				commit.copy()));
	}
}
