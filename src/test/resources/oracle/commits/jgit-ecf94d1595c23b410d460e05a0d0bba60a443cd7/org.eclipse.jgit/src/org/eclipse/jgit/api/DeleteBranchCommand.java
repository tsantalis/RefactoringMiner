/*
 * Copyright (C) 2010, Mathias Kinzler <mathias.kinzler@sap.com>
 * Copyright (C) 2010, Chris Aniszczyk <caniszczyk@gmail.com> and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0 which is available at
 * https://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package org.eclipse.jgit.api;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.jgit.api.errors.CannotDeleteCurrentBranchException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.JGitInternalException;
import org.eclipse.jgit.api.errors.NotMergedException;
import org.eclipse.jgit.internal.JGitText;
import org.eclipse.jgit.lib.ConfigConstants;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.RefUpdate;
import org.eclipse.jgit.lib.RefUpdate.Result;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;

/**
 * Used to delete one or several branches.
 *
 * The result of {@link #call()} is a list with the (full) names of the deleted
 * branches.
 *
 * Note that we don't have a setter corresponding to the -r option; remote
 * tracking branches are simply deleted just like local branches.
 *
 * @see <a
 *      href="http://www.kernel.org/pub/software/scm/git/docs/git-branch.html"
 *      >Git documentation about Branch</a>
 */
public class DeleteBranchCommand extends GitCommand<List<String>> {
	private final Set<String> branchNames = new HashSet<>();

	private boolean force;

	/**
	 * Constructor for DeleteBranchCommand
	 *
	 * @param repo
	 *            the {@link org.eclipse.jgit.lib.Repository}
	 */
	protected DeleteBranchCommand(Repository repo) {
		super(repo);
	}

	@Override
	public List<String> call() throws GitAPIException,
			NotMergedException, CannotDeleteCurrentBranchException {
		checkCallable();
		List<String> result = new ArrayList<>();
		if (branchNames.isEmpty())
			return result;
		try {
			String currentBranch = repo.getFullBranch();
			if (!force) {
				// check if the branches to be deleted
				// are all merged into the current branch
				try (RevWalk walk = new RevWalk(repo)) {
					RevCommit tip = walk
							.parseCommit(repo.resolve(Constants.HEAD));
					for (String branchName : branchNames) {
						if (branchName == null)
							continue;
						Ref currentRef = repo.findRef(branchName);
						if (currentRef == null)
							continue;

						RevCommit base = walk
								.parseCommit(repo.resolve(branchName));
						if (!walk.isMergedInto(base, tip)) {
							throw new NotMergedException();
						}
					}
				}
			}
			setCallable(false);
			for (String branchName : branchNames) {
				if (branchName == null)
					continue;
				Ref currentRef = repo.findRef(branchName);
				if (currentRef == null)
					continue;
				String fullName = currentRef.getName();
				if (fullName.equals(currentBranch))
					throw new CannotDeleteCurrentBranchException(
							MessageFormat
									.format(
											JGitText.get().cannotDeleteCheckedOutBranch,
											branchName));
				RefUpdate update = repo.updateRef(fullName);
				update.setRefLogMessage("branch deleted", false); //$NON-NLS-1$
				update.setForceUpdate(true);
				Result deleteResult = update.delete();

				boolean ok = true;
				switch (deleteResult) {
				case IO_FAILURE:
				case LOCK_FAILURE:
				case REJECTED:
					ok = false;
					break;
				default:
					break;
				}

				if (ok) {
					result.add(fullName);
					if (fullName.startsWith(Constants.R_HEADS)) {
						String shortenedName = fullName
								.substring(Constants.R_HEADS.length());
						// remove upstream configuration if any
						final StoredConfig cfg = repo.getConfig();
						cfg.unsetSection(
								ConfigConstants.CONFIG_BRANCH_SECTION,
								shortenedName);
						cfg.save();
					}
				} else
					throw new JGitInternalException(MessageFormat.format(
							JGitText.get().deleteBranchUnexpectedResult,
							deleteResult.name()));
			}
			return result;
		} catch (IOException ioe) {
			throw new JGitInternalException(ioe.getMessage(), ioe);
		}
	}

	/**
	 * Set the names of the branches to delete
	 *
	 * @param branchnames
	 *            the names of the branches to delete; if not set, this will do
	 *            nothing; invalid branch names will simply be ignored
	 * @return this instance
	 */
	public DeleteBranchCommand setBranchNames(String... branchnames) {
		checkCallable();
		this.branchNames.clear();
		this.branchNames.addAll(Arrays.asList(branchnames));
		return this;
	}

	/**
	 * Set whether to forcefully delete branches
	 *
	 * @param force
	 *            <code>true</code> corresponds to the -D option,
	 *            <code>false</code> to the -d option (default) <br>
	 *            if <code>false</code> a check will be performed whether the
	 *            branch to be deleted is already merged into the current branch
	 *            and deletion will be refused in this case
	 * @return this instance
	 */
	public DeleteBranchCommand setForce(boolean force) {
		checkCallable();
		this.force = force;
		return this;
	}
}
