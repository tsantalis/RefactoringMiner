/*
 * Copyright (C) 2010, Chris Aniszczyk <caniszczyk@gmail.com>
 * Copyright (C) 2011, 2023 Matthias Sohn <matthias.sohn@sap.com> and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0 which is available at
 * https://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package org.eclipse.jgit.api;

import static org.eclipse.jgit.treewalk.TreeWalk.OperationType.CHECKOUT_OP;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.eclipse.jgit.api.CheckoutResult.Status;
import org.eclipse.jgit.api.errors.CheckoutConflictException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.InvalidRefNameException;
import org.eclipse.jgit.api.errors.JGitInternalException;
import org.eclipse.jgit.api.errors.RefAlreadyExistsException;
import org.eclipse.jgit.api.errors.RefNotFoundException;
import org.eclipse.jgit.dircache.Checkout;
import org.eclipse.jgit.dircache.DirCache;
import org.eclipse.jgit.dircache.DirCacheCheckout;
import org.eclipse.jgit.dircache.DirCacheCheckout.CheckoutMetadata;
import org.eclipse.jgit.dircache.DirCacheEditor;
import org.eclipse.jgit.dircache.DirCacheEditor.PathEdit;
import org.eclipse.jgit.dircache.DirCacheEntry;
import org.eclipse.jgit.dircache.DirCacheIterator;
import org.eclipse.jgit.errors.AmbiguousObjectException;
import org.eclipse.jgit.errors.UnmergedPathException;
import org.eclipse.jgit.events.WorkingTreeModifiedEvent;
import org.eclipse.jgit.internal.JGitText;
import org.eclipse.jgit.lib.AnyObjectId;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.CoreConfig.EolStreamType;
import org.eclipse.jgit.lib.FileMode;
import org.eclipse.jgit.lib.NullProgressMonitor;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.ProgressMonitor;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.RefUpdate;
import org.eclipse.jgit.lib.RefUpdate.Result;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.PathFilterGroup;

/**
 * Checkout a branch to the working tree.
 * <p>
 * Examples (<code>git</code> is a {@link org.eclipse.jgit.api.Git} instance):
 * <p>
 * Check out an existing branch:
 *
 * <pre>
 * git.checkout().setName(&quot;feature&quot;).call();
 * </pre>
 * <p>
 * Check out paths from the index:
 *
 * <pre>
 * git.checkout().addPath(&quot;file1.txt&quot;).addPath(&quot;file2.txt&quot;).call();
 * </pre>
 * <p>
 * Check out a path from a commit:
 *
 * <pre>
 * git.checkout().setStartPoint(&quot;HEAD&circ;&quot;).addPath(&quot;file1.txt&quot;).call();
 * </pre>
 *
 * <p>
 * Create a new branch and check it out:
 *
 * <pre>
 * git.checkout().setCreateBranch(true).setName(&quot;newbranch&quot;).call();
 * </pre>
 * <p>
 * Create a new tracking branch for a remote branch and check it out:
 *
 * <pre>
 * git.checkout().setCreateBranch(true).setName(&quot;stable&quot;)
 * 		.setUpstreamMode(SetupUpstreamMode.SET_UPSTREAM)
 * 		.setStartPoint(&quot;origin/stable&quot;).call();
 * </pre>
 *
 * @see <a href=
 *      "http://www.kernel.org/pub/software/scm/git/docs/git-checkout.html" >Git
 *      documentation about Checkout</a>
 */
public class CheckoutCommand extends GitCommand<Ref> {

	/**
	 * Stage to check out, see {@link CheckoutCommand#setStage(Stage)}.
	 */
	public enum Stage {
		/**
		 * Base stage (#1)
		 */
		BASE(DirCacheEntry.STAGE_1),

		/**
		 * Ours stage (#2)
		 */
		OURS(DirCacheEntry.STAGE_2),

		/**
		 * Theirs stage (#3)
		 */
		THEIRS(DirCacheEntry.STAGE_3);

		private final int number;

		private Stage(int number) {
			this.number = number;
		}
	}

	private String name;

	private boolean forceRefUpdate = false;

	private boolean forced = false;

	private boolean createBranch = false;

	private boolean orphan = false;

	private CreateBranchCommand.SetupUpstreamMode upstreamMode;

	private String startPoint = null;

	private RevCommit startCommit;

	private Stage checkoutStage = null;

	private CheckoutResult status;

	private List<String> paths;

	private boolean checkoutAllPaths;

	private Set<String> actuallyModifiedPaths;

	private ProgressMonitor monitor = NullProgressMonitor.INSTANCE;

	/**
	 * Constructor for CheckoutCommand
	 *
	 * @param repo
	 *            the {@link org.eclipse.jgit.lib.Repository}
	 */
	protected CheckoutCommand(Repository repo) {
		super(repo);
		this.paths = new LinkedList<>();
	}

	/** {@inheritDoc} */
	@Override
	public Ref call() throws GitAPIException, RefAlreadyExistsException,
			RefNotFoundException, InvalidRefNameException,
			CheckoutConflictException {
		checkCallable();
		try {
			processOptions();
			if (checkoutAllPaths || !paths.isEmpty()) {
				checkoutPaths();
				status = new CheckoutResult(Status.OK, paths);
				setCallable(false);
				return null;
			}

			if (createBranch) {
				try (Git git = new Git(repo)) {
					CreateBranchCommand command = git.branchCreate();
					command.setName(name);
					if (startCommit != null)
						command.setStartPoint(startCommit);
					else
						command.setStartPoint(startPoint);
					if (upstreamMode != null)
						command.setUpstreamMode(upstreamMode);
					command.call();
				}
			}

			Ref headRef = repo.exactRef(Constants.HEAD);
			if (headRef == null) {
				// TODO Git CLI supports checkout from unborn branch, we should
				// also allow this
				throw new UnsupportedOperationException(
						JGitText.get().cannotCheckoutFromUnbornBranch);
			}
			String shortHeadRef = getShortBranchName(headRef);
			String refLogMessage = "checkout: moving from " + shortHeadRef; //$NON-NLS-1$
			ObjectId branch;
			if (orphan) {
				if (startPoint == null && startCommit == null) {
					Result r = repo.updateRef(Constants.HEAD).link(
							getBranchName());
					if (!EnumSet.of(Result.NEW, Result.FORCED).contains(r))
						throw new JGitInternalException(MessageFormat.format(
								JGitText.get().checkoutUnexpectedResult,
								r.name()));
					this.status = CheckoutResult.NOT_TRIED_RESULT;
					return repo.exactRef(Constants.HEAD);
				}
				branch = getStartPointObjectId();
			} else {
				branch = repo.resolve(name);
				if (branch == null)
					throw new RefNotFoundException(MessageFormat.format(
							JGitText.get().refNotResolved, name));
			}

			RevCommit headCommit = null;
			RevCommit newCommit = null;
			try (RevWalk revWalk = new RevWalk(repo)) {
				AnyObjectId headId = headRef.getObjectId();
				headCommit = headId == null ? null
						: revWalk.parseCommit(headId);
				newCommit = revWalk.parseCommit(branch);
			}
			RevTree headTree = headCommit == null ? null : headCommit.getTree();
			DirCacheCheckout dco;
			DirCache dc = repo.lockDirCache();
			try {
				dco = new DirCacheCheckout(repo, headTree, dc,
						newCommit.getTree());
				dco.setFailOnConflict(true);
				dco.setForce(forced);
				if (forced) {
					dco.setFailOnConflict(false);
				}
				dco.setProgressMonitor(monitor);
				try {
					dco.checkout();
				} catch (org.eclipse.jgit.errors.CheckoutConflictException e) {
					status = new CheckoutResult(Status.CONFLICTS,
							dco.getConflicts());
					throw new CheckoutConflictException(dco.getConflicts(), e);
				}
			} finally {
				dc.unlock();
			}
			Ref ref = repo.findRef(name);
			if (ref != null && !ref.getName().startsWith(Constants.R_HEADS))
				ref = null;
			String toName = Repository.shortenRefName(name);
			RefUpdate refUpdate = repo.updateRef(Constants.HEAD, ref == null);
			refUpdate.setForceUpdate(forceRefUpdate);
			refUpdate.setRefLogMessage(refLogMessage + " to " + toName, false); //$NON-NLS-1$
			Result updateResult;
			if (ref != null)
				updateResult = refUpdate.link(ref.getName());
			else if (orphan) {
				updateResult = refUpdate.link(getBranchName());
				ref = repo.exactRef(Constants.HEAD);
			} else {
				refUpdate.setNewObjectId(newCommit);
				updateResult = refUpdate.forceUpdate();
			}

			setCallable(false);

			boolean ok = false;
			switch (updateResult) {
			case NEW:
				ok = true;
				break;
			case NO_CHANGE:
			case FAST_FORWARD:
			case FORCED:
				ok = true;
				break;
			default:
				break;
			}

			if (!ok)
				throw new JGitInternalException(MessageFormat.format(JGitText
						.get().checkoutUnexpectedResult, updateResult.name()));


			if (!dco.getToBeDeleted().isEmpty()) {
				status = new CheckoutResult(Status.NONDELETED,
						dco.getToBeDeleted(),
						new ArrayList<>(dco.getUpdated().keySet()),
						dco.getRemoved());
			} else
				status = new CheckoutResult(new ArrayList<>(dco
						.getUpdated().keySet()), dco.getRemoved());

			return ref;
		} catch (IOException ioe) {
			throw new JGitInternalException(ioe.getMessage(), ioe);
		} finally {
			if (status == null)
				status = CheckoutResult.ERROR_RESULT;
		}
	}

	private String getShortBranchName(Ref headRef) {
		if (headRef.isSymbolic()) {
			return Repository.shortenRefName(headRef.getTarget().getName());
		}
		// Detached HEAD. Every non-symbolic ref in the ref database has an
		// object id, so this cannot be null.
		ObjectId id = headRef.getObjectId();
		if (id == null) {
			throw new NullPointerException();
		}
		return id.getName();
	}

	/**
	 * @param monitor
	 *            a progress monitor
	 * @return this instance
	 * @since 4.11
	 */
	public CheckoutCommand setProgressMonitor(ProgressMonitor monitor) {
		if (monitor == null) {
			monitor = NullProgressMonitor.INSTANCE;
		}
		this.monitor = monitor;
		return this;
	}

	/**
	 * Add a single slash-separated path to the list of paths to check out. To
	 * check out all paths, use {@link #setAllPaths(boolean)}.
	 * <p>
	 * If this option is set, neither the {@link #setCreateBranch(boolean)} nor
	 * {@link #setName(String)} option is considered. In other words, these
	 * options are exclusive.
	 *
	 * @param path
	 *            path to update in the working tree and index (with
	 *            <code>/</code> as separator)
	 * @return {@code this}
	 */
	public CheckoutCommand addPath(String path) {
		checkCallable();
		this.paths.add(path);
		return this;
	}

	/**
	 * Add multiple slash-separated paths to the list of paths to check out. To
	 * check out all paths, use {@link #setAllPaths(boolean)}.
	 * <p>
	 * If this option is set, neither the {@link #setCreateBranch(boolean)} nor
	 * {@link #setName(String)} option is considered. In other words, these
	 * options are exclusive.
	 *
	 * @param p
	 *            paths to update in the working tree and index (with
	 *            <code>/</code> as separator)
	 * @return {@code this}
	 * @since 4.6
	 */
	public CheckoutCommand addPaths(List<String> p) {
		checkCallable();
		this.paths.addAll(p);
		return this;
	}

	/**
	 * Set whether to checkout all paths.
	 * <p>
	 * This options should be used when you want to do a path checkout on the
	 * entire repository and so calling {@link #addPath(String)} is not possible
	 * since empty paths are not allowed.
	 * <p>
	 * If this option is set, neither the {@link #setCreateBranch(boolean)} nor
	 * {@link #setName(String)} option is considered. In other words, these
	 * options are exclusive.
	 *
	 * @param all
	 *            <code>true</code> to checkout all paths, <code>false</code>
	 *            otherwise
	 * @return {@code this}
	 * @since 2.0
	 */
	public CheckoutCommand setAllPaths(boolean all) {
		checkoutAllPaths = all;
		return this;
	}

	/**
	 * Checkout paths into index and working directory, firing a
	 * {@link org.eclipse.jgit.events.WorkingTreeModifiedEvent} if the working
	 * tree was modified.
	 *
	 * @return this instance
	 * @throws java.io.IOException
	 * @throws org.eclipse.jgit.api.errors.RefNotFoundException
	 */
	protected CheckoutCommand checkoutPaths() throws IOException,
			RefNotFoundException {
		actuallyModifiedPaths = new HashSet<>();
		Checkout checkout = new Checkout(repo).setRecursiveDeletion(true);
		DirCache dc = repo.lockDirCache();
		try (RevWalk revWalk = new RevWalk(repo);
				TreeWalk treeWalk = new TreeWalk(repo,
						revWalk.getObjectReader())) {
			treeWalk.setRecursive(true);
			if (!checkoutAllPaths)
				treeWalk.setFilter(PathFilterGroup.createFromStrings(paths));
			if (isCheckoutIndex())
				checkoutPathsFromIndex(treeWalk, dc, checkout);
			else {
				RevCommit commit = revWalk.parseCommit(getStartPointObjectId());
				checkoutPathsFromCommit(treeWalk, dc, commit, checkout);
			}
		} finally {
			try {
				dc.unlock();
			} finally {
				WorkingTreeModifiedEvent event = new WorkingTreeModifiedEvent(
						actuallyModifiedPaths, null);
				actuallyModifiedPaths = null;
				if (!event.isEmpty()) {
					repo.fireEvent(event);
				}
			}
		}
		return this;
	}

	private void checkoutPathsFromIndex(TreeWalk treeWalk, DirCache dc,
			Checkout checkout)
			throws IOException {
		DirCacheIterator dci = new DirCacheIterator(dc);
		treeWalk.addTree(dci);

		String previousPath = null;

		final ObjectReader r = treeWalk.getObjectReader();
		DirCacheEditor editor = dc.editor();
		while (treeWalk.next()) {
			String path = treeWalk.getPathString();
			// Only add one edit per path
			if (path.equals(previousPath))
				continue;

			final EolStreamType eolStreamType = treeWalk
					.getEolStreamType(CHECKOUT_OP);
			final String filterCommand = treeWalk
					.getFilterCommand(Constants.ATTR_FILTER_TYPE_SMUDGE);
			editor.add(new PathEdit(path) {
				@Override
				public void apply(DirCacheEntry ent) {
					int stage = ent.getStage();
					if (stage > DirCacheEntry.STAGE_0) {
						if (checkoutStage != null) {
							if (stage == checkoutStage.number) {
								checkoutPath(ent, r, checkout, path,
										new CheckoutMetadata(eolStreamType,
												filterCommand));
								actuallyModifiedPaths.add(path);
							}
						} else {
							UnmergedPathException e = new UnmergedPathException(
									ent);
							throw new JGitInternalException(e.getMessage(), e);
						}
					} else {
						checkoutPath(ent, r, checkout, path,
								new CheckoutMetadata(eolStreamType,
										filterCommand));
						actuallyModifiedPaths.add(path);
					}
				}
			});

			previousPath = path;
		}
		editor.commit();
	}

	private void checkoutPathsFromCommit(TreeWalk treeWalk, DirCache dc,
			RevCommit commit, Checkout checkout) throws IOException {
		treeWalk.addTree(commit.getTree());
		final ObjectReader r = treeWalk.getObjectReader();
		DirCacheEditor editor = dc.editor();
		while (treeWalk.next()) {
			final ObjectId blobId = treeWalk.getObjectId(0);
			final FileMode mode = treeWalk.getFileMode(0);
			final EolStreamType eolStreamType = treeWalk
					.getEolStreamType(CHECKOUT_OP);
			final String filterCommand = treeWalk
					.getFilterCommand(Constants.ATTR_FILTER_TYPE_SMUDGE);
			final String path = treeWalk.getPathString();
			editor.add(new PathEdit(path) {
				@Override
				public void apply(DirCacheEntry ent) {
					if (ent.getStage() != DirCacheEntry.STAGE_0) {
						// A checkout on a conflicting file stages the checked
						// out file and resolves the conflict.
						ent.setStage(DirCacheEntry.STAGE_0);
					}
					ent.setObjectId(blobId);
					ent.setFileMode(mode);
					checkoutPath(ent, r, checkout, path,
							new CheckoutMetadata(eolStreamType, filterCommand));
					actuallyModifiedPaths.add(path);
				}
			});
		}
		editor.commit();
	}

	private void checkoutPath(DirCacheEntry entry, ObjectReader reader,
			Checkout checkout, String path, CheckoutMetadata checkoutMetadata) {
		try {
			checkout.checkout(entry, checkoutMetadata, reader, path);
		} catch (IOException e) {
			throw new JGitInternalException(MessageFormat.format(
					JGitText.get().checkoutConflictWithFile,
					entry.getPathString()), e);
		}
	}

	private boolean isCheckoutIndex() {
		return startCommit == null && startPoint == null;
	}

	private ObjectId getStartPointObjectId() throws AmbiguousObjectException,
			RefNotFoundException, IOException {
		if (startCommit != null)
			return startCommit.getId();

		String startPointOrHead = (startPoint != null) ? startPoint
				: Constants.HEAD;
		ObjectId result = repo.resolve(startPointOrHead);
		if (result == null)
			throw new RefNotFoundException(MessageFormat.format(
					JGitText.get().refNotResolved, startPointOrHead));
		return result;
	}

	private void processOptions() throws InvalidRefNameException,
			RefAlreadyExistsException, IOException {
		if (((!checkoutAllPaths && paths.isEmpty()) || orphan)
				&& (name == null || !Repository
						.isValidRefName(Constants.R_HEADS + name)))
			throw new InvalidRefNameException(MessageFormat.format(JGitText
					.get().branchNameInvalid, name == null ? "<null>" : name)); //$NON-NLS-1$

		if (orphan) {
			Ref refToCheck = repo.exactRef(getBranchName());
			if (refToCheck != null)
				throw new RefAlreadyExistsException(MessageFormat.format(
						JGitText.get().refAlreadyExists, name));
		}
	}

	private String getBranchName() {
		if (name.startsWith(Constants.R_REFS))
			return name;

		return Constants.R_HEADS + name;
	}

	/**
	 * Specify the name of the branch or commit to check out, or the new branch
	 * name.
	 * <p>
	 * When only checking out paths and not switching branches, use
	 * {@link #setStartPoint(String)} or {@link #setStartPoint(RevCommit)} to
	 * specify from which branch or commit to check out files.
	 * <p>
	 * When {@link #setCreateBranch(boolean)} is set to <code>true</code>, use
	 * this method to set the name of the new branch to create and
	 * {@link #setStartPoint(String)} or {@link #setStartPoint(RevCommit)} to
	 * specify the start point of the branch.
	 *
	 * @param name
	 *            the name of the branch or commit
	 * @return this instance
	 */
	public CheckoutCommand setName(String name) {
		checkCallable();
		this.name = name;
		return this;
	}

	/**
	 * Specify whether to create a new branch.
	 * <p>
	 * If <code>true</code> is used, the name of the new branch must be set
	 * using {@link #setName(String)}. The commit at which to start the new
	 * branch can be set using {@link #setStartPoint(String)} or
	 * {@link #setStartPoint(RevCommit)}; if not specified, HEAD is used. Also
	 * see {@link #setUpstreamMode} for setting up branch tracking.
	 *
	 * @param createBranch
	 *            if <code>true</code> a branch will be created as part of the
	 *            checkout and set to the specified start point
	 * @return this instance
	 */
	public CheckoutCommand setCreateBranch(boolean createBranch) {
		checkCallable();
		this.createBranch = createBranch;
		return this;
	}

	/**
	 * Specify whether to create a new orphan branch.
	 * <p>
	 * If <code>true</code> is used, the name of the new orphan branch must be
	 * set using {@link #setName(String)}. The commit at which to start the new
	 * orphan branch can be set using {@link #setStartPoint(String)} or
	 * {@link #setStartPoint(RevCommit)}; if not specified, HEAD is used.
	 *
	 * @param orphan
	 *            if <code>true</code> a orphan branch will be created as part
	 *            of the checkout to the specified start point
	 * @return this instance
	 * @since 3.3
	 */
	public CheckoutCommand setOrphan(boolean orphan) {
		checkCallable();
		this.orphan = orphan;
		return this;
	}

	/**
	 * Specify to force the ref update in case of a branch switch.
	 *
	 * @param force
	 *            if <code>true</code> and the branch with the given name
	 *            already exists, the start-point of an existing branch will be
	 *            set to a new start-point; if false, the existing branch will
	 *            not be changed
	 * @return this instance
	 * @deprecated this method was badly named comparing its semantics to native
	 *             git's checkout --force option, use
	 *             {@link #setForceRefUpdate(boolean)} instead
	 */
	@Deprecated
	public CheckoutCommand setForce(boolean force) {
		return setForceRefUpdate(force);
	}

	/**
	 * Specify to force the ref update in case of a branch switch.
	 *
	 * In releases prior to 5.2 this method was called setForce() but this name
	 * was misunderstood to implement native git's --force option, which is not
	 * true.
	 *
	 * @param forceRefUpdate
	 *            if <code>true</code> and the branch with the given name
	 *            already exists, the start-point of an existing branch will be
	 *            set to a new start-point; if false, the existing branch will
	 *            not be changed
	 * @return this instance
	 * @since 5.3
	 */
	public CheckoutCommand setForceRefUpdate(boolean forceRefUpdate) {
		checkCallable();
		this.forceRefUpdate = forceRefUpdate;
		return this;
	}

	/**
	 * Allow a checkout even if the workingtree or index differs from HEAD. This
	 * matches native git's '--force' option.
	 *
	 * JGit releases before 5.2 had a method <code>setForce()</code> offering
	 * semantics different from this new <code>setForced()</code>. This old
	 * semantic can now be found in {@link #setForceRefUpdate(boolean)}
	 *
	 * @param forced
	 *            if set to <code>true</code> then allow the checkout even if
	 *            workingtree or index doesn't match HEAD. Overwrite workingtree
	 *            files and index content with the new content in this case.
	 * @return this instance
	 * @since 5.3
	 */
	public CheckoutCommand setForced(boolean forced) {
		checkCallable();
		this.forced = forced;
		return this;
	}

	/**
	 * Set the name of the commit that should be checked out.
	 * <p>
	 * When checking out files and this is not specified or <code>null</code>,
	 * the index is used.
	 * <p>
	 * When creating a new branch, this will be used as the start point. If not
	 * specified or <code>null</code>, the current HEAD is used.
	 *
	 * @param startPoint
	 *            commit name to check out
	 * @return this instance
	 */
	public CheckoutCommand setStartPoint(String startPoint) {
		checkCallable();
		this.startPoint = startPoint;
		this.startCommit = null;
		checkOptions();
		return this;
	}

	/**
	 * Set the commit that should be checked out.
	 * <p>
	 * When creating a new branch, this will be used as the start point. If not
	 * specified or <code>null</code>, the current HEAD is used.
	 * <p>
	 * When checking out files and this is not specified or <code>null</code>,
	 * the index is used.
	 *
	 * @param startCommit
	 *            commit to check out
	 * @return this instance
	 */
	public CheckoutCommand setStartPoint(RevCommit startCommit) {
		checkCallable();
		this.startCommit = startCommit;
		this.startPoint = null;
		checkOptions();
		return this;
	}

	/**
	 * When creating a branch with {@link #setCreateBranch(boolean)}, this can
	 * be used to configure branch tracking.
	 *
	 * @param mode
	 *            corresponds to the --track/--no-track options; may be
	 *            <code>null</code>
	 * @return this instance
	 */
	public CheckoutCommand setUpstreamMode(
			CreateBranchCommand.SetupUpstreamMode mode) {
		checkCallable();
		this.upstreamMode = mode;
		return this;
	}

	/**
	 * When checking out the index, check out the specified stage (ours or
	 * theirs) for unmerged paths.
	 * <p>
	 * This can not be used when checking out a branch, only when checking out
	 * the index.
	 *
	 * @param stage
	 *            the stage to check out
	 * @return this
	 */
	public CheckoutCommand setStage(Stage stage) {
		checkCallable();
		this.checkoutStage = stage;
		checkOptions();
		return this;
	}

	/**
	 * Get the result, never <code>null</code>
	 *
	 * @return the result, never <code>null</code>
	 */
	public CheckoutResult getResult() {
		if (status == null)
			return CheckoutResult.NOT_TRIED_RESULT;
		return status;
	}

	private void checkOptions() {
		if (checkoutStage != null && !isCheckoutIndex())
			throw new IllegalStateException(
					JGitText.get().cannotCheckoutOursSwitchBranch);
	}
}
