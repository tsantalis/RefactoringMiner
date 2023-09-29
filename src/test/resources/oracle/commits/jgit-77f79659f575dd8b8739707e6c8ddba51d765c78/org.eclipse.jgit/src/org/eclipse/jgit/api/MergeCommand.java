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

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.jgit.JGitText;
import org.eclipse.jgit.api.MergeResult.MergeStatus;
import org.eclipse.jgit.lib.AnyObjectId;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.GitIndex;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectIdRef;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.RefUpdate;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.WorkDirCheckout;
import org.eclipse.jgit.lib.Ref.Storage;
import org.eclipse.jgit.lib.RefUpdate.Result;
import org.eclipse.jgit.merge.MergeStrategy;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;

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
			InvalidMergeHeadsException {
		checkCallable();

		if (commits.size() != 1)
			throw new InvalidMergeHeadsException(
					commits.isEmpty() ? JGitText.get().noMergeHeadSpecified
							: MessageFormat.format(
									JGitText.get().mergeStrategyDoesNotSupportHeads,
									mergeStrategy.getName(), commits.size()));

		try {
			Ref head = repo.getRef(Constants.HEAD);
			if (head == null)
				throw new NoHeadException(
						JGitText.get().commitOnRepoWithoutHEADCurrentlyNotSupported);
			StringBuilder refLogMessage = new StringBuilder("merge ");

			// Check for FAST_FORWARD, ALREADY_UP_TO_DATE
			RevWalk revWalk = new RevWalk(repo);
			try {
				RevCommit headCommit = revWalk.lookupCommit(head.getObjectId());

				Ref ref = commits.get(0);

				refLogMessage.append(ref.getName());

				// handle annotated tags
				ObjectId objectId = ref.getPeeledObjectId();
				if (objectId == null)
					objectId = ref.getObjectId();

				RevCommit srcCommit = revWalk.lookupCommit(objectId);
				if (revWalk.isMergedInto(srcCommit, headCommit)) {
					setCallable(false);
					return new MergeResult(headCommit, srcCommit,
							new ObjectId[] { srcCommit, headCommit },
							MergeStatus.ALREADY_UP_TO_DATE, mergeStrategy);
				} else if (revWalk.isMergedInto(headCommit, srcCommit)) {
					// FAST_FORWARD detected: skip doing a real merge but only
					// update HEAD
					refLogMessage.append(": " + MergeStatus.FAST_FORWARD);
					checkoutNewHead(revWalk, headCommit, srcCommit);
					updateHead(refLogMessage, srcCommit, head.getObjectId());
					setCallable(false);
					return new MergeResult(srcCommit, headCommit,
							new ObjectId[] { srcCommit, headCommit },
							MergeStatus.FAST_FORWARD, mergeStrategy);
				} else {
					return new MergeResult(
							headCommit,
							null,
							new ObjectId[] { srcCommit, headCommit },
							MergeResult.MergeStatus.NOT_SUPPORTED,
							mergeStrategy,
							JGitText.get().onlyAlreadyUpToDateAndFastForwardMergesAreAvailable);
				}
			} finally {
				revWalk.release();
			}
		} catch (IOException e) {
			throw new JGitInternalException(
					MessageFormat.format(
							JGitText.get().exceptionCaughtDuringExecutionOfMergeCommand,
							e), e);
		}
	}

	private void checkoutNewHead(RevWalk revWalk, RevCommit headCommit,
			RevCommit newHeadCommit) throws IOException,
			CheckoutConflictException {
		GitIndex index = repo.getIndex();

		File workDir = repo.getWorkTree();
		if (workDir != null) {
			WorkDirCheckout workDirCheckout = new WorkDirCheckout(repo,
					workDir, repo.mapTree(headCommit.getTree()), index,
					repo.mapTree(newHeadCommit.getTree()));
			workDirCheckout.setFailOnConflict(true);
			try {
				workDirCheckout.checkout();
			} catch (org.eclipse.jgit.errors.CheckoutConflictException e) {
				throw new CheckoutConflictException(
						JGitText.get().couldNotCheckOutBecauseOfConflicts,
						workDirCheckout.getConflicts(), e);
			}
			index.write();
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
