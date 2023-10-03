/*
 * Copyright (C) 2011, Christian Halstrick <christian.halstrick@sap.com>
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

package org.eclipse.jgit.pgm;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.Map;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.MergeCommand;
import org.eclipse.jgit.api.MergeResult;
import org.eclipse.jgit.api.MergeCommand.FastForwardMode;
import org.eclipse.jgit.lib.AnyObjectId;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.merge.MergeStrategy;
import org.eclipse.jgit.merge.ResolveMerger.MergeFailureReason;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.Option;

@Command(common = true, usage = "usage_MergesTwoDevelopmentHistories")
class Merge extends TextBuiltin {

	@Option(name = "--strategy", aliases = { "-s" }, usage = "usage_mergeStrategy")
	private String strategyName;

	@Option(name = "--squash", usage = "usage_squash")
	private boolean squash;

	@Option(name = "--no-commit", usage = "usage_noCommit")
	private boolean noCommit = false;

	private MergeStrategy mergeStrategy = MergeStrategy.RESOLVE;

	@Argument(required = true)
	private String ref;

	@Option(name = "--ff")
	private FastForwardMode ff = FastForwardMode.FF;

	@Option(name = "--no-ff")
	void noff(@SuppressWarnings("unused") final boolean ignored) {
		ff = FastForwardMode.NO_FF;
	}

	@Option(name = "--ff-only")
	void ffonly(@SuppressWarnings("unused") final boolean ignored) {
		ff = FastForwardMode.FF_ONLY;
	}

	@Override
	protected void run() throws Exception {
		if (squash && ff == FastForwardMode.NO_FF)
			throw die(CLIText.get().cannotCombineSquashWithNoff);
		// determine the merge strategy
		if (strategyName != null) {
			mergeStrategy = MergeStrategy.get(strategyName);
			if (mergeStrategy == null)
				throw die(MessageFormat.format(
						CLIText.get().unknownMergeStrategy, strategyName));
		}

		// determine the other revision we want to merge with HEAD
		final Ref srcRef = db.getRef(ref);
		final ObjectId src = db.resolve(ref + "^{commit}"); //$NON-NLS-1$
		if (src == null)
			throw die(MessageFormat.format(
					CLIText.get().refDoesNotExistOrNoCommit, ref));

		Ref oldHead = db.getRef(Constants.HEAD);
		Git git = new Git(db);
		MergeCommand mergeCmd = git.merge().setStrategy(mergeStrategy)
				.setSquash(squash).setFastForward(ff).setCommit(!noCommit);
		if (srcRef != null)
			mergeCmd.include(srcRef);
		else
			mergeCmd.include(src);
		MergeResult result = mergeCmd.call();

		switch (result.getMergeStatus()) {
		case ALREADY_UP_TO_DATE:
			if (squash)
				outw.print(CLIText.get().nothingToSquash);
			outw.println(CLIText.get().alreadyUpToDate);
			break;
		case FAST_FORWARD:
			ObjectId oldHeadId = oldHead.getObjectId();
			outw.println(MessageFormat.format(CLIText.get().updating, oldHeadId
					.abbreviate(7).name(), result.getNewHead().abbreviate(7)
					.name()));
			outw.println(result.getMergeStatus().toString());
			break;
		case CONFLICTING:
			for (String collidingPath : result.getConflicts().keySet())
				outw.println(MessageFormat.format(CLIText.get().mergeConflict,
						collidingPath));
			outw.println(CLIText.get().mergeFailed);
			break;
		case FAILED:
			for (Map.Entry<String, MergeFailureReason> entry : result
					.getFailingPaths().entrySet())
				switch (entry.getValue()) {
				case DIRTY_WORKTREE:
				case DIRTY_INDEX:
					outw.println(CLIText.get().dontOverwriteLocalChanges);
					outw.println("        " + entry.getKey()); //$NON-NLS-1$
					break;
				case COULD_NOT_DELETE:
					outw.println(CLIText.get().cannotDeleteFile);
					outw.println("        " + entry.getKey()); //$NON-NLS-1$
					break;
				}
			break;
		case MERGED:
			String name;
			if (!isMergedInto(oldHead, src))
				name = mergeStrategy.getName();
			else
				name = "recursive"; //$NON-NLS-1$
			outw.println(MessageFormat.format(CLIText.get().mergeMadeBy, name));
			break;
		case MERGED_NOT_COMMITTED:
			outw.println(CLIText.get().mergeWentWellStoppedBeforeCommitting);
			break;
		case MERGED_SQUASHED:
		case FAST_FORWARD_SQUASHED:
		case MERGED_SQUASHED_NOT_COMMITTED:
			outw.println(CLIText.get().mergedSquashed);
			break;
		case ABORTED:
			throw die(CLIText.get().ffNotPossibleAborting);
		case NOT_SUPPORTED:
			outw.println(MessageFormat.format(
					CLIText.get().unsupportedOperation, result.toString()));
		}
	}

	private boolean isMergedInto(Ref oldHead, AnyObjectId src)
			throws IOException {
		RevWalk revWalk = new RevWalk(db);
		ObjectId oldHeadObjectId = oldHead.getPeeledObjectId();
		if (oldHeadObjectId == null)
			oldHeadObjectId = oldHead.getObjectId();
		RevCommit oldHeadCommit = revWalk.lookupCommit(oldHeadObjectId);
		RevCommit srcCommit = revWalk.lookupCommit(src);
		return revWalk.isMergedInto(oldHeadCommit, srcCommit);
	}
}
