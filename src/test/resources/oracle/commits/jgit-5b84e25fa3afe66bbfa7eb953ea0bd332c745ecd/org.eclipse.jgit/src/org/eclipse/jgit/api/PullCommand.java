/*
 * Copyright (C) 2010, Christian Halstrick <christian.halstrick@sap.com>
 * Copyright (C) 2010, Mathias Kinzler <mathias.kinzler@sap.com>
 * Copyright (C) 2016, Laurent Delaigue <laurent.delaigue@obeo.fr>
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

import org.eclipse.jgit.api.RebaseCommand.Operation;
import org.eclipse.jgit.api.errors.CanceledException;
import org.eclipse.jgit.api.errors.DetachedHeadException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.InvalidConfigurationException;
import org.eclipse.jgit.api.errors.InvalidRemoteException;
import org.eclipse.jgit.api.errors.JGitInternalException;
import org.eclipse.jgit.api.errors.NoHeadException;
import org.eclipse.jgit.api.errors.RefNotAdvertisedException;
import org.eclipse.jgit.api.errors.RefNotFoundException;
import org.eclipse.jgit.api.errors.WrongRepositoryStateException;
import org.eclipse.jgit.internal.JGitText;
import org.eclipse.jgit.lib.AnyObjectId;
import org.eclipse.jgit.lib.BranchConfig.BranchRebaseMode;
import org.eclipse.jgit.lib.Config;
import org.eclipse.jgit.lib.ConfigConstants;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.NullProgressMonitor;
import org.eclipse.jgit.lib.ProgressMonitor;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.RepositoryState;
import org.eclipse.jgit.lib.SubmoduleConfig.FetchRecurseSubmodulesMode;
import org.eclipse.jgit.merge.MergeStrategy;
import org.eclipse.jgit.transport.FetchResult;
import org.eclipse.jgit.transport.TagOpt;

/**
 * The Pull command
 *
 * @see <a href="http://www.kernel.org/pub/software/scm/git/docs/git-pull.html"
 *      >Git documentation about Pull</a>
 */
public class PullCommand extends TransportCommand<PullCommand, PullResult> {

	private final static String DOT = "."; //$NON-NLS-1$

	private ProgressMonitor monitor = NullProgressMonitor.INSTANCE;

	private BranchRebaseMode pullRebaseMode = null;

	private String remote;

	private String remoteBranchName;

	private MergeStrategy strategy = MergeStrategy.RECURSIVE;

	private TagOpt tagOption;

	private FetchRecurseSubmodulesMode submoduleRecurseMode = null;

	/**
	 * @param repo
	 */
	protected PullCommand(Repository repo) {
		super(repo);
	}

	/**
	 * @param monitor
	 *            a progress monitor
	 * @return this instance
	 */
	public PullCommand setProgressMonitor(ProgressMonitor monitor) {
		if (monitor == null) {
			monitor = NullProgressMonitor.INSTANCE;
		}
		this.monitor = monitor;
		return this;
	}

	/**
	 * Set if rebase should be used after fetching. If set to true, rebase is
	 * used instead of merge. This is equivalent to --rebase on the command
	 * line.
	 * <p>
	 * If set to false, merge is used after fetching, overriding the
	 * configuration file. This is equivalent to --no-rebase on the command
	 * line.
	 * <p>
	 * This setting overrides the settings in the configuration file. By
	 * default, the setting in the repository configuration file is used.
	 * <p>
	 * A branch can be configured to use rebase by default. See
	 * branch.[name].rebase and branch.autosetuprebase.
	 *
	 * @param useRebase
	 * @return {@code this}
	 */
	public PullCommand setRebase(boolean useRebase) {
		checkCallable();
		pullRebaseMode = useRebase ? BranchRebaseMode.REBASE
				: BranchRebaseMode.NONE;
		return this;
	}

	/**
	 * Sets the {@link BranchRebaseMode} to use after fetching.
	 *
	 * <dl>
	 * <dt>BranchRebaseMode.REBASE</dt>
	 * <dd>Equivalent to {@code --rebase} on the command line: use rebase
	 * instead of merge after fetching.</dd>
	 * <dt>BranchRebaseMode.PRESERVE</dt>
	 * <dd>Equivalent to {@code --preserve-merges} on the command line: rebase
	 * preserving local merge commits.</dd>
	 * <dt>BranchRebaseMode.INTERACTIVE</dt>
	 * <dd>Equivalent to {@code --interactive} on the command line: use
	 * interactive rebase.</dd>
	 * <dt>BranchRebaseMode.NONE</dt>
	 * <dd>Equivalent to {@code --no-rebase}: merge instead of rebasing.
	 * <dt>{@code null}</dt>
	 * <dd>Use the setting defined in the git configuration, either {@code
	 * branch.[name].rebase} or, if not set, {@code pull.rebase}</dd>
	 * </dl>
	 *
	 * This setting overrides the settings in the configuration file. By
	 * default, the setting in the repository configuration file is used.
	 * <p>
	 * A branch can be configured to use rebase by default. See
	 * {@code branch.[name].rebase}, {@code branch.autosetuprebase}, and
	 * {@code pull.rebase}.
	 *
	 * @param rebaseMode
	 *            the {@link BranchRebaseMode} to use
	 * @return {@code this}
	 * @since 4.5
	 */
	public PullCommand setRebase(BranchRebaseMode rebaseMode) {
		checkCallable();
		pullRebaseMode = rebaseMode;
		return this;
	}

	/**
	 * Executes the {@code Pull} command with all the options and parameters
	 * collected by the setter methods (e.g.
	 * {@link #setProgressMonitor(ProgressMonitor)}) of this class. Each
	 * instance of this class should only be used for one invocation of the
	 * command. Don't call this method twice on an instance.
	 *
	 * @return the result of the pull
	 * @throws WrongRepositoryStateException
	 * @throws InvalidConfigurationException
	 * @throws DetachedHeadException
	 * @throws InvalidRemoteException
	 * @throws CanceledException
	 * @throws RefNotFoundException
	 * @throws RefNotAdvertisedException
	 * @throws NoHeadException
	 * @throws org.eclipse.jgit.api.errors.TransportException
	 * @throws GitAPIException
	 */
	@Override
	public PullResult call() throws GitAPIException,
			WrongRepositoryStateException, InvalidConfigurationException,
			InvalidRemoteException, CanceledException,
			RefNotFoundException, RefNotAdvertisedException, NoHeadException,
			org.eclipse.jgit.api.errors.TransportException {
		checkCallable();

		monitor.beginTask(JGitText.get().pullTaskName, 2);
		Config repoConfig = repo.getConfig();

		String branchName = null;
		try {
			String fullBranch = repo.getFullBranch();
			if (fullBranch != null
					&& fullBranch.startsWith(Constants.R_HEADS)) {
				branchName = fullBranch.substring(Constants.R_HEADS.length());
			}
		} catch (IOException e) {
			throw new JGitInternalException(
					JGitText.get().exceptionCaughtDuringExecutionOfPullCommand,
					e);
		}
		if (remoteBranchName == null && branchName != null) {
			// get the name of the branch in the remote repository
			// stored in configuration key branch.<branch name>.merge
			remoteBranchName = repoConfig.getString(
					ConfigConstants.CONFIG_BRANCH_SECTION, branchName,
					ConfigConstants.CONFIG_KEY_MERGE);
		}
		if (remoteBranchName == null) {
			remoteBranchName = branchName;
		}
		if (remoteBranchName == null) {
			throw new NoHeadException(
					JGitText.get().cannotCheckoutFromUnbornBranch);
		}

		if (!repo.getRepositoryState().equals(RepositoryState.SAFE))
			throw new WrongRepositoryStateException(MessageFormat.format(
					JGitText.get().cannotPullOnARepoWithState, repo
							.getRepositoryState().name()));

		if (remote == null && branchName != null) {
			// get the configured remote for the currently checked out branch
			// stored in configuration key branch.<branch name>.remote
			remote = repoConfig.getString(
					ConfigConstants.CONFIG_BRANCH_SECTION, branchName,
					ConfigConstants.CONFIG_KEY_REMOTE);
		}
		if (remote == null) {
			// fall back to default remote
			remote = Constants.DEFAULT_REMOTE_NAME;
		}

		// determines whether rebase should be used after fetching
		if (pullRebaseMode == null && branchName != null) {
			pullRebaseMode = getRebaseMode(branchName, repoConfig);
		}


		final boolean isRemote = !remote.equals("."); //$NON-NLS-1$
		String remoteUri;
		FetchResult fetchRes;
		if (isRemote) {
			remoteUri = repoConfig.getString(
					ConfigConstants.CONFIG_REMOTE_SECTION, remote,
					ConfigConstants.CONFIG_KEY_URL);
			if (remoteUri == null) {
				String missingKey = ConfigConstants.CONFIG_REMOTE_SECTION + DOT
						+ remote + DOT + ConfigConstants.CONFIG_KEY_URL;
				throw new InvalidConfigurationException(MessageFormat.format(
						JGitText.get().missingConfigurationForKey, missingKey));
			}

			if (monitor.isCancelled())
				throw new CanceledException(MessageFormat.format(
						JGitText.get().operationCanceled,
						JGitText.get().pullTaskName));

			FetchCommand fetch = new FetchCommand(repo).setRemote(remote)
					.setProgressMonitor(monitor).setTagOpt(tagOption)
					.setRecurseSubmodules(submoduleRecurseMode);
			configure(fetch);

			fetchRes = fetch.call();
		} else {
			// we can skip the fetch altogether
			remoteUri = JGitText.get().localRepository;
			fetchRes = null;
		}

		monitor.update(1);

		if (monitor.isCancelled())
			throw new CanceledException(MessageFormat.format(
					JGitText.get().operationCanceled,
					JGitText.get().pullTaskName));

		// we check the updates to see which of the updated branches
		// corresponds
		// to the remote branch name
		AnyObjectId commitToMerge;
		if (isRemote) {
			Ref r = null;
			if (fetchRes != null) {
				r = fetchRes.getAdvertisedRef(remoteBranchName);
				if (r == null)
					r = fetchRes.getAdvertisedRef(Constants.R_HEADS
							+ remoteBranchName);
			}
			if (r == null) {
				throw new RefNotAdvertisedException(MessageFormat.format(
						JGitText.get().couldNotGetAdvertisedRef, remote,
						remoteBranchName));
			} else {
				commitToMerge = r.getObjectId();
			}
		} else {
			try {
				commitToMerge = repo.resolve(remoteBranchName);
				if (commitToMerge == null)
					throw new RefNotFoundException(MessageFormat.format(
							JGitText.get().refNotResolved, remoteBranchName));
			} catch (IOException e) {
				throw new JGitInternalException(
						JGitText.get().exceptionCaughtDuringExecutionOfPullCommand,
						e);
			}
		}

		String upstreamName = MessageFormat.format(
				JGitText.get().upstreamBranchName,
				Repository.shortenRefName(remoteBranchName), remoteUri);

		PullResult result;
		if (pullRebaseMode != BranchRebaseMode.NONE) {
			RebaseCommand rebase = new RebaseCommand(repo);
			RebaseResult rebaseRes = rebase.setUpstream(commitToMerge)
					.setUpstreamName(upstreamName).setProgressMonitor(monitor)
					.setOperation(Operation.BEGIN).setStrategy(strategy)
					.setPreserveMerges(
							pullRebaseMode == BranchRebaseMode.PRESERVE)
					.call();
			result = new PullResult(fetchRes, remote, rebaseRes);
		} else {
			MergeCommand merge = new MergeCommand(repo);
			merge.include(upstreamName, commitToMerge);
			merge.setStrategy(strategy);
			merge.setProgressMonitor(monitor);
			MergeResult mergeRes = merge.call();
			monitor.update(1);
			result = new PullResult(fetchRes, remote, mergeRes);
		}
		monitor.endTask();
		return result;
	}

	/**
	 * The remote (uri or name) to be used for the pull operation. If no remote
	 * is set, the branch's configuration will be used. If the branch
	 * configuration is missing the default value of
	 * <code>Constants.DEFAULT_REMOTE_NAME</code> will be used.
	 *
	 * @see Constants#DEFAULT_REMOTE_NAME
	 * @param remote
	 * @return {@code this}
	 * @since 3.3
	 */
	public PullCommand setRemote(String remote) {
		checkCallable();
		this.remote = remote;
		return this;
	}

	/**
	 * The remote branch name to be used for the pull operation. If no
	 * remoteBranchName is set, the branch's configuration will be used. If the
	 * branch configuration is missing the remote branch with the same name as
	 * the current branch is used.
	 *
	 * @param remoteBranchName
	 * @return {@code this}
	 * @since 3.3
	 */
	public PullCommand setRemoteBranchName(String remoteBranchName) {
		checkCallable();
		this.remoteBranchName = remoteBranchName;
		return this;
	}

	/**
	 * @return the remote used for the pull operation if it was set explicitly
	 * @since 3.3
	 */
	public String getRemote() {
		return remote;
	}

	/**
	 * @return the remote branch name used for the pull operation if it was set
	 *         explicitly
	 * @since 3.3
	 */
	public String getRemoteBranchName() {
		return remoteBranchName;
	}

	/**
	 * @param strategy
	 *            The merge strategy to use during this pull operation.
	 * @return {@code this}
	 * @since 3.4
	 */
	public PullCommand setStrategy(MergeStrategy strategy) {
		this.strategy = strategy;
		return this;
	}

	/**
	 * Sets the specification of annotated tag behavior during fetch
	 *
	 * @param tagOpt
	 * @return {@code this}
	 * @since 4.7
	 */
	public PullCommand setTagOpt(TagOpt tagOpt) {
		checkCallable();
		this.tagOption = tagOpt;
		return this;
	}

	/**
	 * Set the mode to be used for recursing into submodules.
	 *
	 * @param recurse
	 * @return {@code this}
	 * @since 4.7
	 */
	public PullCommand setRecurseSubmodules(
			FetchRecurseSubmodulesMode recurse) {
		this.submoduleRecurseMode = recurse;
		return this;
	}

	/**
	 * Reads the rebase mode to use for a pull command from the repository
	 * configuration. This is the value defined for the configurations
	 * {@code branch.[branchName].rebase}, or,if not set, {@code pull.rebase}.
	 * If neither is set, yields {@link BranchRebaseMode#NONE}.
	 *
	 * @param branchName
	 *            name of the local branch
	 * @param config
	 *            the {@link Config} to read the value from
	 * @return the {@link BranchRebaseMode}
	 * @since 4.5
	 */
	public static BranchRebaseMode getRebaseMode(String branchName,
			Config config) {
		BranchRebaseMode mode = config.getEnum(BranchRebaseMode.values(),
				ConfigConstants.CONFIG_BRANCH_SECTION,
				branchName, ConfigConstants.CONFIG_KEY_REBASE, null);
		if (mode == null) {
			mode = config.getEnum(BranchRebaseMode.values(),
					ConfigConstants.CONFIG_PULL_SECTION, null,
					ConfigConstants.CONFIG_KEY_REBASE, BranchRebaseMode.NONE);
		}
		return mode;
	}
}
