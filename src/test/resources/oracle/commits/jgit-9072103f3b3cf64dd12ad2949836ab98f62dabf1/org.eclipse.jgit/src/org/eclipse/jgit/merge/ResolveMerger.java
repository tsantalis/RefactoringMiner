/*
 * Copyright (C) 2010, Christian Halstrick <christian.halstrick@sap.com>,
 * Copyright (C) 2010-2012, Matthias Sohn <matthias.sohn@sap.com>
 * Copyright (C) 2012, Research In Motion Limited
 * Copyright (C) 2017, Obeo (mathieu.cartaud@obeo.fr)
 * Copyright (C) 2018, 2023 Thomas Wolf <twolf@apache.org>
 * Copyright (C) 2023, Google Inc. and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0 which is available at
 * https://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package org.eclipse.jgit.merge;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.time.Instant.EPOCH;
import static org.eclipse.jgit.diff.DiffAlgorithm.SupportedAlgorithm.HISTOGRAM;
import static org.eclipse.jgit.lib.ConfigConstants.CONFIG_DIFF_SECTION;
import static org.eclipse.jgit.lib.ConfigConstants.CONFIG_KEY_ALGORITHM;
import static org.eclipse.jgit.lib.Constants.OBJ_BLOB;

import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

import org.eclipse.jgit.annotations.NonNull;
import org.eclipse.jgit.annotations.Nullable;
import org.eclipse.jgit.attributes.Attribute;
import org.eclipse.jgit.attributes.Attributes;
import org.eclipse.jgit.diff.DiffAlgorithm;
import org.eclipse.jgit.diff.DiffAlgorithm.SupportedAlgorithm;
import org.eclipse.jgit.diff.RawText;
import org.eclipse.jgit.diff.RawTextComparator;
import org.eclipse.jgit.diff.Sequence;
import org.eclipse.jgit.dircache.Checkout;
import org.eclipse.jgit.dircache.DirCache;
import org.eclipse.jgit.dircache.DirCacheBuildIterator;
import org.eclipse.jgit.dircache.DirCacheBuilder;
import org.eclipse.jgit.dircache.DirCacheCheckout;
import org.eclipse.jgit.dircache.DirCacheCheckout.CheckoutMetadata;
import org.eclipse.jgit.dircache.DirCacheCheckout.StreamSupplier;
import org.eclipse.jgit.dircache.DirCacheEntry;
import org.eclipse.jgit.errors.BinaryBlobException;
import org.eclipse.jgit.errors.IndexWriteException;
import org.eclipse.jgit.errors.NoWorkTreeException;
import org.eclipse.jgit.internal.JGitText;
import org.eclipse.jgit.lib.Config;
import org.eclipse.jgit.lib.ConfigConstants;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.CoreConfig.EolStreamType;
import org.eclipse.jgit.lib.FileMode;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectInserter;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.storage.pack.PackConfig;
import org.eclipse.jgit.submodule.SubmoduleConflict;
import org.eclipse.jgit.treewalk.AbstractTreeIterator;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.treewalk.NameConflictTreeWalk;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.TreeWalk.OperationType;
import org.eclipse.jgit.treewalk.WorkingTreeIterator;
import org.eclipse.jgit.treewalk.WorkingTreeOptions;
import org.eclipse.jgit.treewalk.filter.TreeFilter;
import org.eclipse.jgit.util.LfsFactory;
import org.eclipse.jgit.util.LfsFactory.LfsInputStream;
import org.eclipse.jgit.util.TemporaryBuffer;
import org.eclipse.jgit.util.io.EolStreamTypeUtil;

/**
 * A three-way merger performing a content-merge if necessary
 */
public class ResolveMerger extends ThreeWayMerger {

	/**
	 * Handles work tree updates on both the checkout and the index.
	 * <p>
	 * You should use a single instance for all of your file changes. In case of
	 * an error, make sure your instance is released, and initiate a new one if
	 * necessary.
	 *
	 * @since 6.3.1
	 */
	protected static class WorkTreeUpdater implements Closeable {

		/**
		 * The result of writing the index changes.
		 */
		public static class Result {

			private final List<String> modifiedFiles = new LinkedList<>();

			private final List<String> failedToDelete = new LinkedList<>();

			private ObjectId treeId = null;

			/**
			 * @return Modified tree ID if any, or null otherwise.
			 */
			public ObjectId getTreeId() {
				return treeId;
			}

			/**
			 * @return Files that couldn't be deleted.
			 */
			public List<String> getFailedToDelete() {
				return failedToDelete;
			}

			/**
			 * @return Files modified during this operation.
			 */
			public List<String> getModifiedFiles() {
				return modifiedFiles;
			}
		}

		Result result = new Result();

		/**
		 * The repository this handler operates on.
		 */
		@Nullable
		private final Repository repo;

		/**
		 * Set to true if this operation should work in-memory. The repo's
		 * dircache and workingtree are not touched by this method. Eventually
		 * needed files are created as temporary files and a new empty,
		 * in-memory dircache will be used instead the repo's one. Often used
		 * for bare repos where the repo doesn't even have a workingtree and
		 * dircache.
		 */
		private final boolean inCore;

		private final ObjectInserter inserter;

		private final ObjectReader reader;

		private DirCache dirCache;

		private boolean implicitDirCache = false;

		/**
		 * Builder to update the dir cache during this operation.
		 */
		private DirCacheBuilder builder;

		/**
		 * The {@link WorkingTreeOptions} are needed to determine line endings
		 * for affected files.
		 */
		private WorkingTreeOptions workingTreeOptions;

		/**
		 * The size limit (bytes) which controls a file to be stored in
		 * {@code Heap} or {@code LocalFile} during the operation.
		 */
		private int inCoreFileSizeLimit;

		/**
		 * If the operation has nothing to do for a file but check it out at the
		 * end of the operation, it can be added here.
		 */
		private final Map<String, DirCacheEntry> toBeCheckedOut = new HashMap<>();

		/**
		 * Files in this list will be deleted from the local copy at the end of
		 * the operation.
		 */
		private final TreeMap<String, File> toBeDeleted = new TreeMap<>();

		/**
		 * Keeps {@link CheckoutMetadata} for {@link #checkout()}.
		 */
		private Map<String, CheckoutMetadata> checkoutMetadataByPath;

		/**
		 * Keeps {@link CheckoutMetadata} for {@link #revertModifiedFiles()}.
		 */
		private Map<String, CheckoutMetadata> cleanupMetadataByPath;

		/**
		 * Whether the changes were successfully written.
		 */
		private boolean indexChangesWritten;

		/**
		 * {@link Checkout} to use for actually checking out files if
		 * {@link #inCore} is {@code false}.
		 */
		private Checkout checkout;

		/**
		 * @param repo
		 *            the {@link Repository}.
		 * @param dirCache
		 *            if set, use the provided dir cache. Otherwise, use the
		 *            default repository one
		 */
		private WorkTreeUpdater(Repository repo, DirCache dirCache) {
			this.repo = repo;
			this.dirCache = dirCache;

			this.inCore = false;
			this.inserter = repo.newObjectInserter();
			this.reader = inserter.newReader();
			Config config = repo.getConfig();
			this.workingTreeOptions = config.get(WorkingTreeOptions.KEY);
			this.inCoreFileSizeLimit = getInCoreFileSizeLimit(config);
			this.checkoutMetadataByPath = new HashMap<>();
			this.cleanupMetadataByPath = new HashMap<>();
			this.checkout = new Checkout(nonNullRepo(), workingTreeOptions);
		}

		/**
		 * Creates a new {@link WorkTreeUpdater} for the given repository.
		 *
		 * @param repo
		 *            the {@link Repository}.
		 * @param dirCache
		 *            if set, use the provided dir cache. Otherwise, use the
		 *            default repository one
		 * @return the {@link WorkTreeUpdater}.
		 */
		public static WorkTreeUpdater createWorkTreeUpdater(Repository repo,
				DirCache dirCache) {
			return new WorkTreeUpdater(repo, dirCache);
		}

		/**
		 * @param repo
		 *            the {@link Repository}.
		 * @param dirCache
		 *            if set, use the provided dir cache. Otherwise, creates a
		 *            new one
		 * @param oi
		 *            to use for writing the modified objects with.
		 */
		private WorkTreeUpdater(Repository repo, DirCache dirCache,
				ObjectInserter oi) {
			this.repo = repo;
			this.dirCache = dirCache;
			this.inserter = oi;

			this.inCore = true;
			this.reader = oi.newReader();
			if (repo != null) {
				this.inCoreFileSizeLimit = getInCoreFileSizeLimit(
						repo.getConfig());
			}
		}

		/**
		 * Creates a new {@link WorkTreeUpdater} that works in memory only.
		 *
		 * @param repo
		 *            the {@link Repository}.
		 * @param dirCache
		 *            if set, use the provided dir cache. Otherwise, creates a
		 *            new one
		 * @param oi
		 *            to use for writing the modified objects with.
		 * @return the {@link WorkTreeUpdater}
		 */
		public static WorkTreeUpdater createInCoreWorkTreeUpdater(
				Repository repo, DirCache dirCache, ObjectInserter oi) {
			return new WorkTreeUpdater(repo, dirCache, oi);
		}

		private static int getInCoreFileSizeLimit(Config config) {
			return config.getInt(ConfigConstants.CONFIG_MERGE_SECTION,
					ConfigConstants.CONFIG_KEY_IN_CORE_LIMIT, 10 << 20);
		}

		/**
		 * Gets the size limit for in-core files in this config.
		 *
		 * @return the size
		 */
		public int getInCoreFileSizeLimit() {
			return inCoreFileSizeLimit;
		}

		/**
		 * Gets dir cache for the repo. Locked if not inCore.
		 *
		 * @return the result dir cache
		 * @throws IOException
		 *             is case the dir cache cannot be read
		 */
		public DirCache getLockedDirCache() throws IOException {
			if (dirCache == null) {
				implicitDirCache = true;
				if (inCore) {
					dirCache = DirCache.newInCore();
				} else {
					dirCache = nonNullRepo().lockDirCache();
				}
			}
			if (builder == null) {
				builder = dirCache.builder();
			}
			return dirCache;
		}

		/**
		 * Creates a {@link DirCacheBuildIterator} for the builder of this
		 * {@link WorkTreeUpdater}.
		 *
		 * @return the {@link DirCacheBuildIterator}
		 */
		public DirCacheBuildIterator createDirCacheBuildIterator() {
			return new DirCacheBuildIterator(builder);
		}

		/**
		 * Writes the changes to the working tree (but not to the index).
		 *
		 * @param shouldCheckoutTheirs
		 *            before committing the changes
		 * @throws IOException
		 *             if any of the writes fail
		 */
		public void writeWorkTreeChanges(boolean shouldCheckoutTheirs)
				throws IOException {
			handleDeletedFiles();

			if (inCore) {
				builder.finish();
				return;
			}
			if (shouldCheckoutTheirs) {
				// No problem found. The only thing left to be done is to
				// check out all files from "theirs" which have been selected to
				// go into the new index.
				checkout();
			}

			// All content operations are successfully done. If we can now write
			// the new index we are on quite safe ground. Even if the checkout
			// of files coming from "theirs" fails the user can work around such
			// failures by checking out the index again.
			if (!builder.commit()) {
				revertModifiedFiles();
				throw new IndexWriteException();
			}
		}

		/**
		 * Writes the changes to the index.
		 *
		 * @return the {@link Result} of the operation.
		 * @throws IOException
		 *             if any of the writes fail
		 */
		public Result writeIndexChanges() throws IOException {
			result.treeId = getLockedDirCache().writeTree(inserter);
			indexChangesWritten = true;
			return result;
		}

		/**
		 * Adds a {@link DirCacheEntry} for direct checkout and remembers its
		 * {@link CheckoutMetadata}.
		 *
		 * @param path
		 *            of the entry
		 * @param entry
		 *            to add
		 * @param cleanupStreamType
		 *            to use for the cleanup metadata
		 * @param cleanupSmudgeCommand
		 *            to use for the cleanup metadata
		 * @param checkoutStreamType
		 *            to use for the checkout metadata
		 * @param checkoutSmudgeCommand
		 *            to use for the checkout metadata
		 */
		public void addToCheckout(String path, DirCacheEntry entry,
				EolStreamType cleanupStreamType, String cleanupSmudgeCommand,
				EolStreamType checkoutStreamType,
				String checkoutSmudgeCommand) {
			if (entry != null) {
				// In some cases, we just want to add the metadata.
				toBeCheckedOut.put(path, entry);
			}
			addCheckoutMetadata(cleanupMetadataByPath, path, cleanupStreamType,
					cleanupSmudgeCommand);
			addCheckoutMetadata(checkoutMetadataByPath, path,
					checkoutStreamType, checkoutSmudgeCommand);
		}

		/**
		 * Gets a map which maps the paths of files which have to be checked out
		 * because the operation created new fully-merged content for this file
		 * into the index.
		 * <p>
		 * This means: the operation wrote a new stage 0 entry for this path.
		 * </p>
		 *
		 * @return the map
		 */
		public Map<String, DirCacheEntry> getToBeCheckedOut() {
			return toBeCheckedOut;
		}

		/**
		 * Remembers the given file to be deleted.
		 * <p>
		 * Note the actual deletion is only done in
		 * {@link #writeWorkTreeChanges}.
		 *
		 * @param path
		 *            of the file to be deleted
		 * @param file
		 *            to be deleted
		 * @param streamType
		 *            to use for cleanup metadata
		 * @param smudgeCommand
		 *            to use for cleanup metadata
		 */
		public void deleteFile(String path, File file, EolStreamType streamType,
				String smudgeCommand) {
			toBeDeleted.put(path, file);
			if (file != null && file.isFile()) {
				addCheckoutMetadata(cleanupMetadataByPath, path, streamType,
						smudgeCommand);
			}
		}

		/**
		 * Remembers the {@link CheckoutMetadata} for the given path; it may be
		 * needed in {@link #checkout()} or in {@link #revertModifiedFiles()}.
		 *
		 * @param map
		 *            to add the metadata to
		 * @param path
		 *            of the current node
		 * @param streamType
		 *            to use for the metadata
		 * @param smudgeCommand
		 *            to use for the metadata
		 */
		private void addCheckoutMetadata(Map<String, CheckoutMetadata> map,
				String path, EolStreamType streamType, String smudgeCommand) {
			if (inCore || map == null) {
				return;
			}
			map.put(path, new CheckoutMetadata(streamType, smudgeCommand));
		}

		/**
		 * Detects if CRLF conversion has been configured.
		 * <p>
		 * </p>
		 * See {@link EolStreamTypeUtil#detectStreamType} for more info.
		 *
		 * @param attributes
		 *            of the file for which the type is to be detected
		 * @return the detected type
		 */
		public EolStreamType detectCheckoutStreamType(Attributes attributes) {
			if (inCore) {
				return null;
			}
			return EolStreamTypeUtil.detectStreamType(OperationType.CHECKOUT_OP,
					workingTreeOptions, attributes);
		}

		private void handleDeletedFiles() {
			// Iterate in reverse so that "folder/file" is deleted before
			// "folder". Otherwise, this could result in a failing path because
			// of a non-empty directory, for which delete() would fail.
			for (String path : toBeDeleted.descendingKeySet()) {
				File file = inCore ? null : toBeDeleted.get(path);
				if (file != null && !file.delete()) {
					if (!file.isDirectory()) {
						result.failedToDelete.add(path);
					}
				}
			}
		}

		/**
		 * Marks the given path as modified in the operation.
		 *
		 * @param path
		 *            to mark as modified
		 */
		public void markAsModified(String path) {
			result.modifiedFiles.add(path);
		}

		/**
		 * Gets the list of files which were modified in this operation.
		 *
		 * @return the list
		 */
		public List<String> getModifiedFiles() {
			return result.modifiedFiles;
		}

		private void checkout() throws NoWorkTreeException, IOException {
			for (Map.Entry<String, DirCacheEntry> entry : toBeCheckedOut
					.entrySet()) {
				DirCacheEntry dirCacheEntry = entry.getValue();
				String gitPath = entry.getKey();
				if (dirCacheEntry.getFileMode() == FileMode.GITLINK) {
					checkout.checkoutGitlink(dirCacheEntry, gitPath);
				} else {
					checkout.checkout(dirCacheEntry,
							checkoutMetadataByPath.get(gitPath), reader,
							gitPath);
					result.modifiedFiles.add(gitPath);
				}
			}
		}

		/**
		 * Reverts any uncommitted changes in the worktree. We know that for all
		 * modified files the old content was in the old index and the index
		 * contained only stage 0. In case of inCore operation just clear the
		 * history of modified files.
		 *
		 * @throws IOException
		 *             in case the cleaning up failed
		 */
		public void revertModifiedFiles() throws IOException {
			if (inCore) {
				result.modifiedFiles.clear();
				return;
			}
			if (indexChangesWritten) {
				return;
			}
			for (String path : result.modifiedFiles) {
				DirCacheEntry entry = dirCache.getEntry(path);
				if (entry != null) {
					checkout.checkout(entry, cleanupMetadataByPath.get(path),
							reader, path);
				}
			}
		}

		@Override
		public void close() throws IOException {
			if (implicitDirCache) {
				dirCache.unlock();
			}
		}

		/**
		 * Updates the file in the checkout with the given content.
		 *
		 * @param inputStream
		 *            the content to be updated
		 * @param streamType
		 *            for parsing the content
		 * @param smudgeCommand
		 *            for formatting the content
		 * @param path
		 *            of the file to be updated
		 * @param file
		 *            to be updated
		 * @throws IOException
		 *             if the file cannot be updated
		 */
		public void updateFileWithContent(StreamSupplier inputStream,
				EolStreamType streamType, String smudgeCommand, String path,
				File file) throws IOException {
			if (inCore) {
				return;
			}
			checkout.safeCreateParentDirectory(path, file.getParentFile(),
					false);
			CheckoutMetadata metadata = new CheckoutMetadata(streamType,
					smudgeCommand);

			try (OutputStream outputStream = new FileOutputStream(file)) {
				DirCacheCheckout.getContent(repo, path, metadata, inputStream,
						workingTreeOptions, outputStream);
			}
		}

		/**
		 * Creates a path with the given content, and adds it to the specified
		 * stage to the index builder.
		 *
		 * @param input
		 *            the content to be updated
		 * @param path
		 *            of the file to be updated
		 * @param fileMode
		 *            of the modified file
		 * @param entryStage
		 *            of the new entry
		 * @param lastModified
		 *            instant of the modified file
		 * @param len
		 *            of the content
		 * @param lfsAttribute
		 *            for checking for LFS enablement
		 * @return the entry which was added to the index
		 * @throws IOException
		 *             if inserting the content fails
		 */
		public DirCacheEntry insertToIndex(InputStream input, byte[] path,
				FileMode fileMode, int entryStage, Instant lastModified,
				int len, Attribute lfsAttribute) throws IOException {
			return addExistingToIndex(insertResult(input, lfsAttribute, len),
					path, fileMode, entryStage, lastModified, len);
		}

		/**
		 * Adds a path with the specified stage to the index builder.
		 *
		 * @param objectId
		 *            of the existing object to add
		 * @param path
		 *            of the modified file
		 * @param fileMode
		 *            of the modified file
		 * @param entryStage
		 *            of the new entry
		 * @param lastModified
		 *            instant of the modified file
		 * @param len
		 *            of the modified file content
		 * @return the entry which was added to the index
		 */
		public DirCacheEntry addExistingToIndex(ObjectId objectId, byte[] path,
				FileMode fileMode, int entryStage, Instant lastModified,
				int len) {
			DirCacheEntry dce = new DirCacheEntry(path, entryStage);
			dce.setFileMode(fileMode);
			if (lastModified != null) {
				dce.setLastModified(lastModified);
			}
			dce.setLength(inCore ? 0 : len);
			dce.setObjectId(objectId);
			builder.add(dce);
			return dce;
		}

		private ObjectId insertResult(InputStream input, Attribute lfsAttribute,
				long length) throws IOException {
			try (LfsInputStream is = LfsFactory.getInstance()
					.applyCleanFilter(repo, input, length, lfsAttribute)) {
				return inserter.insert(OBJ_BLOB, is.getLength(), is);
			}
		}

		/**
		 * Gets the non-null repository instance of this
		 * {@link WorkTreeUpdater}.
		 *
		 * @return non-null repository instance
		 * @throws NullPointerException
		 *             if the handler was constructed without a repository.
		 */
		@NonNull
		private Repository nonNullRepo() throws NullPointerException {
			return Objects.requireNonNull(repo,
					() -> JGitText.get().repositoryIsRequired);
		}
	}

	/**
	 * If the merge fails (means: not stopped because of unresolved conflicts)
	 * this enum is used to explain why it failed
	 */
	public enum MergeFailureReason {
		/** the merge failed because of a dirty index */
		DIRTY_INDEX,
		/** the merge failed because of a dirty workingtree */
		DIRTY_WORKTREE,
		/** the merge failed because of a file could not be deleted */
		COULD_NOT_DELETE
	}

	/**
	 * The tree walk which we'll iterate over to merge entries.
	 *
	 * @since 3.4
	 */
	protected NameConflictTreeWalk tw;

	/**
	 * string versions of a list of commit SHA1s
	 *
	 * @since 3.0
	 */
	protected String[] commitNames;

	/**
	 * Index of the base tree within the {@link #tw tree walk}.
	 *
	 * @since 3.4
	 */
	protected static final int T_BASE = 0;

	/**
	 * Index of our tree in withthe {@link #tw tree walk}.
	 *
	 * @since 3.4
	 */
	protected static final int T_OURS = 1;

	/**
	 * Index of their tree within the {@link #tw tree walk}.
	 *
	 * @since 3.4
	 */
	protected static final int T_THEIRS = 2;

	/**
	 * Index of the index tree within the {@link #tw tree walk}.
	 *
	 * @since 3.4
	 */
	protected static final int T_INDEX = 3;

	/**
	 * Index of the working directory tree within the {@link #tw tree walk}.
	 *
	 * @since 3.4
	 */
	protected static final int T_FILE = 4;

	/**
	 * Handler for repository I/O actions.
	 *
	 * @since 6.3
	 */
	protected WorkTreeUpdater workTreeUpdater;

	/**
	 * merge result as tree
	 *
	 * @since 3.0
	 */
	protected ObjectId resultTree;

	/**
	 * Files modified during this operation. Note this list is only updated after a successful write.
	 */
	protected List<String> modifiedFiles = new ArrayList<>();

	/**
	 * Paths that could not be merged by this merger because of an unsolvable
	 * conflict.
	 *
	 * @since 3.4
	 */
	protected List<String> unmergedPaths = new ArrayList<>();

	/**
	 * Low-level textual merge results. Will be passed on to the callers in case
	 * of conflicts.
	 *
	 * @since 3.4
	 */
	protected Map<String, MergeResult<? extends Sequence>> mergeResults = new HashMap<>();

	/**
	 * Paths for which the merge failed altogether.
	 *
	 * @since 3.4
	 */
	protected Map<String, MergeFailureReason> failingPaths = new HashMap<>();

	/**
	 * Updated as we merge entries of the tree walk. Tells us whether we should
	 * recurse into the entry if it is a subtree.
	 *
	 * @since 3.4
	 */
	protected boolean enterSubtree;

	/**
	 * Set to true if this merge should work in-memory. The repos dircache and
	 * workingtree are not touched by this method. Eventually needed files are
	 * created as temporary files and a new empty, in-memory dircache will be
	 * used instead the repo's one. Often used for bare repos where the repo
	 * doesn't even have a workingtree and dircache.
	 * @since 3.0
	 */
	protected boolean inCore;

	/**
	 * Directory cache
	 * @since 3.0
	 */
	protected DirCache dircache;

	/**
	 * The iterator to access the working tree. If set to <code>null</code> this
	 * merger will not touch the working tree.
	 * @since 3.0
	 */
	protected WorkingTreeIterator workingTreeIterator;

	/**
	 * our merge algorithm
	 * @since 3.0
	 */
	protected MergeAlgorithm mergeAlgorithm;

	/**
	 * The {@link ContentMergeStrategy} to use for "resolve" and "recursive"
	 * merges.
	 */
	@NonNull
	private ContentMergeStrategy contentStrategy = ContentMergeStrategy.CONFLICT;

	private static MergeAlgorithm getMergeAlgorithm(Config config) {
		SupportedAlgorithm diffAlg = config.getEnum(
				CONFIG_DIFF_SECTION, null, CONFIG_KEY_ALGORITHM,
				HISTOGRAM);
		return new MergeAlgorithm(DiffAlgorithm.getAlgorithm(diffAlg));
	}

	private static String[] defaultCommitNames() {
		return new String[]{"BASE", "OURS", "THEIRS"}; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}

	private static final Attributes NO_ATTRIBUTES = new Attributes();

	/**
	 * Constructor for ResolveMerger.
	 *
	 * @param local
	 *            the {@link org.eclipse.jgit.lib.Repository}.
	 * @param inCore
	 *            a boolean.
	 */
	protected ResolveMerger(Repository local, boolean inCore) {
		super(local);
		Config config = local.getConfig();
		mergeAlgorithm = getMergeAlgorithm(config);
		commitNames = defaultCommitNames();
		this.inCore = inCore;
	}

	/**
	 * Constructor for ResolveMerger.
	 *
	 * @param local
	 *            the {@link org.eclipse.jgit.lib.Repository}.
	 */
	protected ResolveMerger(Repository local) {
		this(local, false);
	}

	/**
	 * Constructor for ResolveMerger.
	 *
	 * @param inserter
	 *            an {@link org.eclipse.jgit.lib.ObjectInserter} object.
	 * @param config
	 *            the repository configuration
	 * @since 4.8
	 */
	protected ResolveMerger(ObjectInserter inserter, Config config) {
		super(inserter);
		mergeAlgorithm = getMergeAlgorithm(config);
		commitNames = defaultCommitNames();
		inCore = true;
	}

	/**
	 * Retrieves the content merge strategy for content conflicts.
	 *
	 * @return the {@link ContentMergeStrategy} in effect
	 * @since 5.12
	 */
	@NonNull
	public ContentMergeStrategy getContentMergeStrategy() {
		return contentStrategy;
	}

	/**
	 * Sets the content merge strategy for content conflicts.
	 *
	 * @param strategy
	 *            {@link ContentMergeStrategy} to use
	 * @since 5.12
	 */
	public void setContentMergeStrategy(ContentMergeStrategy strategy) {
		contentStrategy = strategy == null ? ContentMergeStrategy.CONFLICT
				: strategy;
	}

	/** {@inheritDoc} */
	@Override
	protected boolean mergeImpl() throws IOException {
		return mergeTrees(mergeBase(), sourceTrees[0], sourceTrees[1],
				false);
	}

	/**
	 * adds a new path with the specified stage to the index builder
	 *
	 * @param path
	 * @param p
	 * @param stage
	 * @param lastMod
	 * @param len
	 * @return the entry which was added to the index
	 */
	private DirCacheEntry add(byte[] path, CanonicalTreeParser p, int stage,
			Instant lastMod, long len) {
		if (p != null && !p.getEntryFileMode().equals(FileMode.TREE)) {
			return workTreeUpdater.addExistingToIndex(p.getEntryObjectId(), path,
					p.getEntryFileMode(), stage,
					lastMod, (int) len);
		}
		return null;
	}

	/**
	 * Adds the conflict stages for the current path of {@link #tw} to the index
	 * builder and returns the "theirs" stage; if present.
	 *
	 * @param base
	 *            of the conflict
	 * @param ours
	 *            of the conflict
	 * @param theirs
	 *            of the conflict
	 * @return the {@link DirCacheEntry} for the "theirs" stage, or {@code null}
	 */
	private DirCacheEntry addConflict(CanonicalTreeParser base,
			CanonicalTreeParser ours, CanonicalTreeParser theirs) {
		add(tw.getRawPath(), base, DirCacheEntry.STAGE_1, EPOCH, 0);
		add(tw.getRawPath(), ours, DirCacheEntry.STAGE_2, EPOCH, 0);
		return add(tw.getRawPath(), theirs, DirCacheEntry.STAGE_3, EPOCH, 0);
	}

	/**
	 * adds a entry to the index builder which is a copy of the specified
	 * DirCacheEntry
	 *
	 * @param e
	 *            the entry which should be copied
	 *
	 * @return the entry which was added to the index
	 */
	private DirCacheEntry keep(DirCacheEntry e) {
		return workTreeUpdater.addExistingToIndex(e.getObjectId(), e.getRawPath(), e.getFileMode(),
				e.getStage(), e.getLastModifiedInstant(), e.getLength());
	}

	/**
	 * Adds a {@link DirCacheEntry} for direct checkout and remembers its
	 * {@link CheckoutMetadata}.
	 *
	 * @param path
	 *            of the entry
	 * @param entry
	 *            to add
	 * @param attributes
	 *            the {@link Attributes} of the trees
	 * @throws IOException
	 *             if the {@link CheckoutMetadata} cannot be determined
	 * @since 6.1
	 */
	protected void addToCheckout(String path, DirCacheEntry entry,
			Attributes[] attributes)
			throws IOException {
		EolStreamType cleanupStreamType = workTreeUpdater.detectCheckoutStreamType(attributes[T_OURS]);
		String cleanupSmudgeCommand = tw.getSmudgeCommand(attributes[T_OURS]);
		EolStreamType checkoutStreamType = workTreeUpdater.detectCheckoutStreamType(attributes[T_THEIRS]);
		String checkoutSmudgeCommand = tw.getSmudgeCommand(attributes[T_THEIRS]);
		workTreeUpdater.addToCheckout(path, entry, cleanupStreamType, cleanupSmudgeCommand,
				checkoutStreamType, checkoutSmudgeCommand);
	}

	/**
	 * Remember a path for deletion, and remember its {@link CheckoutMetadata}
	 * in case it has to be restored in the cleanUp.
	 *
	 * @param path
	 *            of the entry
	 * @param isFile
	 *            whether it is a file
	 * @param attributes
	 *            to use for determining the {@link CheckoutMetadata}
	 * @throws IOException
	 *             if the {@link CheckoutMetadata} cannot be determined
	 * @since 5.1
	 */
	protected void addDeletion(String path, boolean isFile,
			Attributes attributes) throws IOException {
		if (db == null || nonNullRepo().isBare() || !isFile)
			return;

		File file = new File(nonNullRepo().getWorkTree(), path);
		EolStreamType streamType = workTreeUpdater.detectCheckoutStreamType(attributes);
		String smudgeCommand = tw.getSmudgeCommand(attributes);
		workTreeUpdater.deleteFile(path, file, streamType, smudgeCommand);
	}

	/**
	 * Processes one path and tries to merge taking git attributes in account.
	 * This method will do all trivial (not content) merges and will also detect
	 * if a merge will fail. The merge will fail when one of the following is
	 * true
	 * <ul>
	 * <li>the index entry does not match the entry in ours. When merging one
	 * branch into the current HEAD, ours will point to HEAD and theirs will
	 * point to the other branch. It is assumed that the index matches the HEAD
	 * because it will only not match HEAD if it was populated before the merge
	 * operation. But the merge commit should not accidentally contain
	 * modifications done before the merge. Check the <a href=
	 * "http://www.kernel.org/pub/software/scm/git/docs/git-read-tree.html#_3_way_merge"
	 * >git read-tree</a> documentation for further explanations.</li>
	 * <li>A conflict was detected and the working-tree file is dirty. When a
	 * conflict is detected the content-merge algorithm will try to write a
	 * merged version into the working-tree. If the file is dirty we would
	 * override unsaved data.</li>
	 * </ul>
	 *
	 * @param base
	 *            the common base for ours and theirs
	 * @param ours
	 *            the ours side of the merge. When merging a branch into the
	 *            HEAD ours will point to HEAD
	 * @param theirs
	 *            the theirs side of the merge. When merging a branch into the
	 *            current HEAD theirs will point to the branch which is merged
	 *            into HEAD.
	 * @param index
	 *            the index entry
	 * @param work
	 *            the file in the working tree
	 * @param ignoreConflicts
	 *            see
	 *            {@link org.eclipse.jgit.merge.ResolveMerger#mergeTrees(AbstractTreeIterator, RevTree, RevTree, boolean)}
	 * @param attributes
	 *            the {@link Attributes} for the three trees
	 * @return <code>false</code> if the merge will fail because the index entry
	 *         didn't match ours or the working-dir file was dirty and a
	 *         conflict occurred
	 * @throws java.io.IOException
	 * @since 6.1
	 */
	protected boolean processEntry(CanonicalTreeParser base,
			CanonicalTreeParser ours, CanonicalTreeParser theirs,
			DirCacheBuildIterator index, WorkingTreeIterator work,
			boolean ignoreConflicts, Attributes[] attributes)
			throws IOException {
		enterSubtree = true;
		final int modeO = tw.getRawMode(T_OURS);
		final int modeT = tw.getRawMode(T_THEIRS);
		final int modeB = tw.getRawMode(T_BASE);
		boolean gitLinkMerging = isGitLink(modeO) || isGitLink(modeT)
				|| isGitLink(modeB);
		if (modeO == 0 && modeT == 0 && modeB == 0) {
			// File is either untracked or new, staged but uncommitted
			return true;
		}

		if (isIndexDirty()) {
			return false;
		}

		DirCacheEntry ourDce = null;

		if (index == null || index.getDirCacheEntry() == null) {
			// create a fake DCE, but only if ours is valid. ours is kept only
			// in case it is valid, so a null ourDce is ok in all other cases.
			if (nonTree(modeO)) {
				ourDce = new DirCacheEntry(tw.getRawPath());
				ourDce.setObjectId(tw.getObjectId(T_OURS));
				ourDce.setFileMode(tw.getFileMode(T_OURS));
			}
		} else {
			ourDce = index.getDirCacheEntry();
		}

		if (nonTree(modeO) && nonTree(modeT) && tw.idEqual(T_OURS, T_THEIRS)) {
			// OURS and THEIRS have equal content. Check the file mode
			if (modeO == modeT) {
				// content and mode of OURS and THEIRS are equal: it doesn't
				// matter which one we choose. OURS is chosen. Since the index
				// is clean (the index matches already OURS) we can keep the existing one
				keep(ourDce);
				// no checkout needed!
				return true;
			}
			// same content but different mode on OURS and THEIRS.
			// Try to merge the mode and report an error if this is
			// not possible.
			int newMode = mergeFileModes(modeB, modeO, modeT);
			if (newMode != FileMode.MISSING.getBits()) {
				if (newMode == modeO) {
					// ours version is preferred
					keep(ourDce);
				} else {
					// the preferred version THEIRS has a different mode
					// than ours. Check it out!
					if (isWorktreeDirty(work, ourDce)) {
						return false;
					}
					// we know about length and lastMod only after we have
					// written the new content.
					// This will happen later. Set these values to 0 for know.
					DirCacheEntry e = add(tw.getRawPath(), theirs,
							DirCacheEntry.STAGE_0, EPOCH, 0);
					addToCheckout(tw.getPathString(), e, attributes);
				}
				return true;
			}
			if (!ignoreConflicts) {
				// FileModes are not mergeable. We found a conflict on modes.
				// For conflicting entries we don't know lastModified and
				// length.
				// This path can be skipped on ignoreConflicts, so the caller
				// could use virtual commit.
				addConflict(base, ours, theirs);
				unmergedPaths.add(tw.getPathString());
				mergeResults.put(tw.getPathString(),
						new MergeResult<>(Collections.emptyList()));
			}
			return true;
		}

		if (modeB == modeT && tw.idEqual(T_BASE, T_THEIRS)) {
			// THEIRS was not changed compared to BASE. All changes must be in
			// OURS. OURS is chosen. We can keep the existing entry.
			if (ourDce != null) {
				keep(ourDce);
			}
			// no checkout needed!
			return true;
		}

		if (modeB == modeO && tw.idEqual(T_BASE, T_OURS)) {
			// OURS was not changed compared to BASE. All changes must be in
			// THEIRS. THEIRS is chosen.

			// Check worktree before checking out THEIRS
			if (isWorktreeDirty(work, ourDce)) {
				return false;
			}
			if (nonTree(modeT)) {
				// we know about length and lastMod only after we have written
				// the new content.
				// This will happen later. Set these values to 0 for know.
				DirCacheEntry e = add(tw.getRawPath(), theirs,
						DirCacheEntry.STAGE_0, EPOCH, 0);
				if (e != null) {
					addToCheckout(tw.getPathString(), e, attributes);
				}
				return true;
			}
			// we want THEIRS ... but THEIRS contains a folder or the
			// deletion of the path. Delete what's in the working tree,
			// which we know to be clean.
			if (tw.getTreeCount() > T_FILE && tw.getRawMode(T_FILE) == 0) {
				// Not present in working tree, so nothing to delete
				return true;
			}
			if (modeT != 0 && modeT == modeB) {
				// Base, ours, and theirs all contain a folder: don't delete
				return true;
			}
			addDeletion(tw.getPathString(), nonTree(modeO), attributes[T_OURS]);
			return true;
		}

		if (tw.isSubtree()) {
			// file/folder conflicts: here I want to detect only file/folder
			// conflict between ours and theirs. file/folder conflicts between
			// base/index/workingTree and something else are not relevant or
			// detected later
			if (nonTree(modeO) != nonTree(modeT)) {
				if (ignoreConflicts) {
					// In case of merge failures, ignore this path instead of reporting unmerged, so
					// a caller can use virtual commit. This will not result in files with conflict
					// markers in the index/working tree. The actual diff on the path will be
					// computed directly on children.
					enterSubtree = false;
					return true;
				}
				if (nonTree(modeB)) {
					add(tw.getRawPath(), base, DirCacheEntry.STAGE_1, EPOCH, 0);
				}
				if (nonTree(modeO)) {
					add(tw.getRawPath(), ours, DirCacheEntry.STAGE_2, EPOCH, 0);
				}
				if (nonTree(modeT)) {
					add(tw.getRawPath(), theirs, DirCacheEntry.STAGE_3, EPOCH, 0);
				}
				unmergedPaths.add(tw.getPathString());
				enterSubtree = false;
				return true;
			}

			// ours and theirs are both folders or both files (and treewalk
			// tells us we are in a subtree because of index or working-dir).
			// If they are both folders no content-merge is required - we can
			// return here.
			if (!nonTree(modeO)) {
				return true;
			}

			// ours and theirs are both files, just fall out of the if block
			// and do the content merge
		}

		if (nonTree(modeO) && nonTree(modeT)) {
			// Check worktree before modifying files
			boolean worktreeDirty = isWorktreeDirty(work, ourDce);
			if (!attributes[T_OURS].canBeContentMerged() && worktreeDirty) {
				return false;
			}

			if (gitLinkMerging && ignoreConflicts) {
				// Always select 'ours' in case of GITLINK merge failures so
				// a caller can use virtual commit.
				add(tw.getRawPath(), ours, DirCacheEntry.STAGE_0, EPOCH, 0);
				return true;
			} else if (gitLinkMerging) {
				addConflict(base, ours, theirs);
				MergeResult<SubmoduleConflict> result = createGitLinksMergeResult(
						base, ours, theirs);
				result.setContainsConflicts(true);
				mergeResults.put(tw.getPathString(), result);
				unmergedPaths.add(tw.getPathString());
				return true;
			} else if (!attributes[T_OURS].canBeContentMerged()) {
				// File marked as binary
				switch (getContentMergeStrategy()) {
					case OURS:
						keep(ourDce);
						return true;
					case THEIRS:
						DirCacheEntry theirEntry = add(tw.getRawPath(), theirs,
								DirCacheEntry.STAGE_0, EPOCH, 0);
						addToCheckout(tw.getPathString(), theirEntry, attributes);
						return true;
					default:
						break;
				}
				addConflict(base, ours, theirs);

				// attribute merge issues are conflicts but not failures
				unmergedPaths.add(tw.getPathString());
				return true;
			}

			// Check worktree before modifying files
			if (worktreeDirty) {
				return false;
			}

			MergeResult<RawText> result = null;
			boolean hasSymlink = FileMode.SYMLINK.equals(modeO)
					|| FileMode.SYMLINK.equals(modeT);
			if (!hasSymlink) {
				try {
					result = contentMerge(base, ours, theirs, attributes,
							getContentMergeStrategy());
				} catch (BinaryBlobException e) {
					// result == null
				}
			}
			if (result == null) {
				switch (getContentMergeStrategy()) {
				case OURS:
					keep(ourDce);
					return true;
				case THEIRS:
					DirCacheEntry e = add(tw.getRawPath(), theirs,
							DirCacheEntry.STAGE_0, EPOCH, 0);
					if (e != null) {
						addToCheckout(tw.getPathString(), e, attributes);
					}
					return true;
				default:
					result = new MergeResult<>(Collections.emptyList());
					result.setContainsConflicts(true);
					break;
				}
			}
			if (ignoreConflicts) {
				result.setContainsConflicts(false);
			}
			String currentPath = tw.getPathString();
			if (hasSymlink) {
				if (ignoreConflicts) {
					if (((modeT & FileMode.TYPE_MASK) == FileMode.TYPE_FILE)) {
						DirCacheEntry e = add(tw.getRawPath(), theirs,
								DirCacheEntry.STAGE_0, EPOCH, 0);
						addToCheckout(currentPath, e, attributes);
					} else {
						keep(ourDce);
					}
				} else {
					// Record the conflict
					DirCacheEntry e = addConflict(base, ours, theirs);
					mergeResults.put(currentPath, result);
					// If theirs is a file, check it out. In link/file
					// conflicts, C git prefers the file.
					if (((modeT & FileMode.TYPE_MASK) == FileMode.TYPE_FILE)
							&& e != null) {
						addToCheckout(currentPath, e, attributes);
					}
				}
			} else {
				updateIndex(base, ours, theirs, result, attributes[T_OURS]);
			}
			if (result.containsConflicts() && !ignoreConflicts) {
				unmergedPaths.add(currentPath);
			}
			workTreeUpdater.markAsModified(currentPath);
			// Entry is null - only adds the metadata.
			addToCheckout(currentPath, null, attributes);
		} else if (modeO != modeT) {
			// OURS or THEIRS has been deleted
			if (((modeO != 0 && !tw.idEqual(T_BASE, T_OURS)) || (modeT != 0 && !tw
					.idEqual(T_BASE, T_THEIRS)))) {
				if (gitLinkMerging && ignoreConflicts) {
					add(tw.getRawPath(), ours, DirCacheEntry.STAGE_0, EPOCH, 0);
				} else if (gitLinkMerging) {
					addConflict(base, ours, theirs);
					MergeResult<SubmoduleConflict> result = createGitLinksMergeResult(
							base, ours, theirs);
					result.setContainsConflicts(true);
					mergeResults.put(tw.getPathString(), result);
					unmergedPaths.add(tw.getPathString());
				} else {
					boolean isSymLink = ((modeO | modeT)
							& FileMode.TYPE_MASK) == FileMode.TYPE_SYMLINK;
					// Content merge strategy does not apply to delete-modify
					// conflicts!
					MergeResult<RawText> result;
					if (isSymLink) {
						// No need to do a content merge
						result = new MergeResult<>(Collections.emptyList());
						result.setContainsConflicts(true);
					} else {
						try {
							result = contentMerge(base, ours, theirs,
									attributes, ContentMergeStrategy.CONFLICT);
						} catch (BinaryBlobException e) {
							result = new MergeResult<>(Collections.emptyList());
							result.setContainsConflicts(true);
						}
					}
					if (ignoreConflicts) {
						result.setContainsConflicts(false);
						if (isSymLink) {
							if (modeO != 0) {
								keep(ourDce);
							} else {
								// Check out theirs
								if (isWorktreeDirty(work, ourDce)) {
									return false;
								}
								DirCacheEntry e = add(tw.getRawPath(), theirs,
										DirCacheEntry.STAGE_0, EPOCH, 0);
								if (e != null) {
									addToCheckout(tw.getPathString(), e,
											attributes);
								}
							}
						} else {
							// In case a conflict is detected the working tree
							// file is again filled with new content (containing
							// conflict markers). But also stage 0 of the index
							// is filled with that content.
							updateIndex(base, ours, theirs, result,
									attributes[T_OURS]);
						}
					} else {
						DirCacheEntry e = addConflict(base, ours, theirs);

						// OURS was deleted checkout THEIRS
						if (modeO == 0) {
							// Check worktree before checking out THEIRS
							if (isWorktreeDirty(work, ourDce)) {
								return false;
							}
							if (nonTree(modeT) && e != null) {
								addToCheckout(tw.getPathString(), e,
										attributes);
							}
						}

						unmergedPaths.add(tw.getPathString());

						// generate a MergeResult for the deleted file
						mergeResults.put(tw.getPathString(), result);
					}
				}
			}
		}
		return true;
	}

	private static MergeResult<SubmoduleConflict> createGitLinksMergeResult(
			CanonicalTreeParser base, CanonicalTreeParser ours,
			CanonicalTreeParser theirs) {
		return new MergeResult<>(Arrays.asList(
				new SubmoduleConflict(
						base == null ? null : base.getEntryObjectId()),
				new SubmoduleConflict(
						ours == null ? null : ours.getEntryObjectId()),
				new SubmoduleConflict(
						theirs == null ? null : theirs.getEntryObjectId())));
	}

	/**
	 * Does the content merge. The three texts base, ours and theirs are
	 * specified with {@link CanonicalTreeParser}. If any of the parsers is
	 * specified as <code>null</code> then an empty text will be used instead.
	 *
	 * @param base
	 * @param ours
	 * @param theirs
	 * @param attributes
	 * @param strategy
	 *
	 * @return the result of the content merge
	 * @throws BinaryBlobException
	 *             if any of the blobs looks like a binary blob
	 * @throws IOException
	 */
	private MergeResult<RawText> contentMerge(CanonicalTreeParser base,
			CanonicalTreeParser ours, CanonicalTreeParser theirs,
			Attributes[] attributes, ContentMergeStrategy strategy)
			throws BinaryBlobException, IOException {
		// TW: The attributes here are used to determine the LFS smudge filter.
		// Is doing a content merge on LFS items really a good idea??
		RawText baseText = base == null ? RawText.EMPTY_TEXT
				: getRawText(base.getEntryObjectId(), attributes[T_BASE]);
		RawText ourText = ours == null ? RawText.EMPTY_TEXT
				: getRawText(ours.getEntryObjectId(), attributes[T_OURS]);
		RawText theirsText = theirs == null ? RawText.EMPTY_TEXT
				: getRawText(theirs.getEntryObjectId(), attributes[T_THEIRS]);
		mergeAlgorithm.setContentMergeStrategy(strategy);
		return mergeAlgorithm.merge(RawTextComparator.DEFAULT, baseText,
				ourText, theirsText);
	}

	private boolean isIndexDirty() {
		if (inCore) {
			return false;
		}

		final int modeI = tw.getRawMode(T_INDEX);
		final int modeO = tw.getRawMode(T_OURS);

		// Index entry has to match ours to be considered clean
		final boolean isDirty = nonTree(modeI)
				&& !(modeO == modeI && tw.idEqual(T_INDEX, T_OURS));
		if (isDirty) {
			failingPaths
					.put(tw.getPathString(), MergeFailureReason.DIRTY_INDEX);
		}
		return isDirty;
	}

	private boolean isWorktreeDirty(WorkingTreeIterator work,
			DirCacheEntry ourDce) throws IOException {
		if (work == null) {
			return false;
		}

		final int modeF = tw.getRawMode(T_FILE);
		final int modeO = tw.getRawMode(T_OURS);

		// Worktree entry has to match ours to be considered clean
		boolean isDirty;
		if (ourDce != null) {
			isDirty = work.isModified(ourDce, true, reader);
		} else {
			isDirty = work.isModeDifferent(modeO);
			if (!isDirty && nonTree(modeF)) {
				isDirty = !tw.idEqual(T_FILE, T_OURS);
			}
		}

		// Ignore existing empty directories
		if (isDirty && modeF == FileMode.TYPE_TREE
				&& modeO == FileMode.TYPE_MISSING) {
			isDirty = false;
		}
		if (isDirty) {
			failingPaths.put(tw.getPathString(),
					MergeFailureReason.DIRTY_WORKTREE);
		}
		return isDirty;
	}

	/**
	 * Updates the index after a content merge has happened. If no conflict has
	 * occurred this includes persisting the merged content to the object
	 * database. In case of conflicts this method takes care to write the
	 * correct stages to the index.
	 *
	 * @param base
	 * @param ours
	 * @param theirs
	 * @param result
	 * @param attributes
	 * @throws IOException
	 */
	private void updateIndex(CanonicalTreeParser base,
			CanonicalTreeParser ours, CanonicalTreeParser theirs,
			MergeResult<RawText> result, Attributes attributes)
			throws IOException {
		TemporaryBuffer rawMerged = null;
		try {
			rawMerged = doMerge(result);
			File mergedFile = inCore ? null
					: writeMergedFile(rawMerged, attributes);
			if (result.containsConflicts()) {
				// A conflict occurred, the file will contain conflict markers
				// the index will be populated with the three stages and the
				// workdir (if used) contains the halfway merged content.
				addConflict(base, ours, theirs);
				mergeResults.put(tw.getPathString(), result);
				return;
			}

			// No conflict occurred, the file will contain fully merged content.
			// The index will be populated with the new merged version.
			Instant lastModified = mergedFile == null ? null
					: nonNullRepo().getFS().lastModifiedInstant(mergedFile);
			// Set the mode for the new content. Fall back to REGULAR_FILE if
			// we can't merge modes of OURS and THEIRS.
			int newMode = mergeFileModes(tw.getRawMode(0), tw.getRawMode(1),
					tw.getRawMode(2));
			FileMode mode = newMode == FileMode.MISSING.getBits()
					? FileMode.REGULAR_FILE : FileMode.fromBits(newMode);
			workTreeUpdater.insertToIndex(rawMerged.openInputStream(),
					tw.getPathString().getBytes(UTF_8), mode,
					DirCacheEntry.STAGE_0, lastModified,
					(int) rawMerged.length(),
					attributes.get(Constants.ATTR_MERGE));
		} finally {
			if (rawMerged != null) {
				rawMerged.destroy();
			}
		}
	}

	/**
	 * Writes merged file content to the working tree.
	 *
	 * @param rawMerged
	 *            the raw merged content
	 * @param attributes
	 *            the files .gitattributes entries
	 * @return the working tree file to which the merged content was written.
	 * @throws IOException
	 */
	private File writeMergedFile(TemporaryBuffer rawMerged,
			Attributes attributes)
			throws IOException {
		File workTree = nonNullRepo().getWorkTree();
		String gitPath = tw.getPathString();
		File of = new File(workTree, gitPath);
		EolStreamType eol = workTreeUpdater.detectCheckoutStreamType(attributes);
		workTreeUpdater.updateFileWithContent(rawMerged::openInputStream,
				eol, tw.getSmudgeCommand(attributes), gitPath, of);
		return of;
	}

	private TemporaryBuffer doMerge(MergeResult<RawText> result)
			throws IOException {
		TemporaryBuffer.LocalFile buf = new TemporaryBuffer.LocalFile(
				db != null ? nonNullRepo().getDirectory() : null, workTreeUpdater.getInCoreFileSizeLimit());
		boolean success = false;
		try {
			new MergeFormatter().formatMerge(buf, result,
					Arrays.asList(commitNames), UTF_8);
			buf.close();
			success = true;
		} finally {
			if (!success) {
				buf.destroy();
			}
		}
		return buf;
	}

	/**
	 * Try to merge filemodes. If only ours or theirs have changed the mode
	 * (compared to base) we choose that one. If ours and theirs have equal
	 * modes return that one. If also that is not the case the modes are not
	 * mergeable. Return {@link FileMode#MISSING} int that case.
	 *
	 * @param modeB
	 *            filemode found in BASE
	 * @param modeO
	 *            filemode found in OURS
	 * @param modeT
	 *            filemode found in THEIRS
	 *
	 * @return the merged filemode or {@link FileMode#MISSING} in case of a
	 *         conflict
	 */
	private int mergeFileModes(int modeB, int modeO, int modeT) {
		if (modeO == modeT) {
			return modeO;
		}
		if (modeB == modeO) {
			// Base equal to Ours -> chooses Theirs if that is not missing
			return (modeT == FileMode.MISSING.getBits()) ? modeO : modeT;
		}
		if (modeB == modeT) {
			// Base equal to Theirs -> chooses Ours if that is not missing
			return (modeO == FileMode.MISSING.getBits()) ? modeT : modeO;
		}
		return FileMode.MISSING.getBits();
	}

	private RawText getRawText(ObjectId id,
			Attributes attributes)
			throws IOException, BinaryBlobException {
		if (id.equals(ObjectId.zeroId())) {
			return new RawText(new byte[]{});
		}

		ObjectLoader loader = LfsFactory.getInstance().applySmudgeFilter(
				getRepository(), reader.open(id, OBJ_BLOB),
				attributes.get(Constants.ATTR_MERGE));
		int threshold = PackConfig.DEFAULT_BIG_FILE_THRESHOLD;
		return RawText.load(loader, threshold);
	}

	private static boolean nonTree(int mode) {
		return mode != 0 && !FileMode.TREE.equals(mode);
	}

	private static boolean isGitLink(int mode) {
		return FileMode.GITLINK.equals(mode);
	}

	/** {@inheritDoc} */
	@Override
	public ObjectId getResultTreeId() {
		return (resultTree == null) ? null : resultTree.toObjectId();
	}

	/**
	 * Set the names of the commits as they would appear in conflict markers
	 *
	 * @param commitNames
	 *            the names of the commits as they would appear in conflict
	 *            markers
	 */
	public void setCommitNames(String[] commitNames) {
		this.commitNames = commitNames;
	}

	/**
	 * Get the names of the commits as they would appear in conflict markers.
	 *
	 * @return the names of the commits as they would appear in conflict
	 *         markers.
	 */
	public String[] getCommitNames() {
		return commitNames;
	}

	/**
	 * Get the paths with conflicts. This is a subset of the files listed by
	 * {@link #getModifiedFiles()}
	 *
	 * @return the paths with conflicts. This is a subset of the files listed by
	 *         {@link #getModifiedFiles()}
	 */
	public List<String> getUnmergedPaths() {
		return unmergedPaths;
	}

	/**
	 * Get the paths of files which have been modified by this merge.
	 *
	 * @return the paths of files which have been modified by this merge. A file
	 *         will be modified if a content-merge works on this path or if the
	 *         merge algorithm decides to take the theirs-version. This is a
	 *         superset of the files listed by {@link #getUnmergedPaths()}.
	 */
	public List<String> getModifiedFiles() {
		return workTreeUpdater != null ? workTreeUpdater.getModifiedFiles() : modifiedFiles;
	}

	/**
	 * Get a map which maps the paths of files which have to be checked out
	 * because the merge created new fully-merged content for this file into the
	 * index.
	 *
	 * @return a map which maps the paths of files which have to be checked out
	 *         because the merge created new fully-merged content for this file
	 *         into the index. This means: the merge wrote a new stage 0 entry
	 *         for this path.
	 */
	public Map<String, DirCacheEntry> getToBeCheckedOut() {
		return workTreeUpdater.getToBeCheckedOut();
	}

	/**
	 * Get the mergeResults
	 *
	 * @return the mergeResults
	 */
	public Map<String, MergeResult<? extends Sequence>> getMergeResults() {
		return mergeResults;
	}

	/**
	 * Get list of paths causing this merge to fail (not stopped because of a
	 * conflict).
	 *
	 * @return lists paths causing this merge to fail (not stopped because of a
	 *         conflict). <code>null</code> is returned if this merge didn't
	 *         fail.
	 */
	public Map<String, MergeFailureReason> getFailingPaths() {
		return failingPaths.isEmpty() ? null : failingPaths;
	}

	/**
	 * Returns whether this merge failed (i.e. not stopped because of a
	 * conflict)
	 *
	 * @return <code>true</code> if a failure occurred, <code>false</code>
	 *         otherwise
	 */
	public boolean failed() {
		return !failingPaths.isEmpty();
	}

	/**
	 * Sets the DirCache which shall be used by this merger. If the DirCache is
	 * not set explicitly and if this merger doesn't work in-core, this merger
	 * will implicitly get and lock a default DirCache. If the DirCache is
	 * explicitly set the caller is responsible to lock it in advance. Finally
	 * the merger will call {@link org.eclipse.jgit.dircache.DirCache#commit()}
	 * which requires that the DirCache is locked. If the {@link #mergeImpl()}
	 * returns without throwing an exception the lock will be released. In case
	 * of exceptions the caller is responsible to release the lock.
	 *
	 * @param dc
	 *            the DirCache to set
	 */
	public void setDirCache(DirCache dc) {
		this.dircache = dc;
	}

	/**
	 * Sets the WorkingTreeIterator to be used by this merger. If no
	 * WorkingTreeIterator is set this merger will ignore the working tree and
	 * fail if a content merge is necessary.
	 * <p>
	 * TODO: enhance WorkingTreeIterator to support write operations. Then this
	 * merger will be able to merge with a different working tree abstraction.
	 *
	 * @param workingTreeIterator
	 *            the workingTreeIt to set
	 */
	public void setWorkingTreeIterator(WorkingTreeIterator workingTreeIterator) {
		this.workingTreeIterator = workingTreeIterator;
	}


	/**
	 * The resolve conflict way of three way merging
	 *
	 * @param baseTree
	 *            a {@link org.eclipse.jgit.treewalk.AbstractTreeIterator}
	 *            object.
	 * @param headTree
	 *            a {@link org.eclipse.jgit.revwalk.RevTree} object.
	 * @param mergeTree
	 *            a {@link org.eclipse.jgit.revwalk.RevTree} object.
	 * @param ignoreConflicts
	 *            Controls what to do in case a content-merge is done and a
	 *            conflict is detected. The default setting for this should be
	 *            <code>false</code>. In this case the working tree file is
	 *            filled with new content (containing conflict markers) and the
	 *            index is filled with multiple stages containing BASE, OURS and
	 *            THEIRS content. Having such non-0 stages is the sign to git
	 *            tools that there are still conflicts for that path.
	 *            <p>
	 *            If <code>true</code> is specified the behavior is different.
	 *            In case a conflict is detected the working tree file is again
	 *            filled with new content (containing conflict markers). But
	 *            also stage 0 of the index is filled with that content. No
	 *            other stages are filled. Means: there is no conflict on that
	 *            path but the new content (including conflict markers) is
	 *            stored as successful merge result. This is needed in the
	 *            context of {@link org.eclipse.jgit.merge.RecursiveMerger}
	 *            where when determining merge bases we don't want to deal with
	 *            content-merge conflicts.
	 * @return whether the trees merged cleanly
	 * @throws java.io.IOException
	 * @since 3.5
	 */
	protected boolean mergeTrees(AbstractTreeIterator baseTree,
			RevTree headTree, RevTree mergeTree, boolean ignoreConflicts)
			throws IOException {
		try {
			workTreeUpdater = inCore ?
					WorkTreeUpdater.createInCoreWorkTreeUpdater(db, dircache, getObjectInserter()) :
					WorkTreeUpdater.createWorkTreeUpdater(db, dircache);
			dircache = workTreeUpdater.getLockedDirCache();
			tw = new NameConflictTreeWalk(db, reader);

			tw.addTree(baseTree);
			tw.setHead(tw.addTree(headTree));
			tw.addTree(mergeTree);
			DirCacheBuildIterator buildIt = workTreeUpdater.createDirCacheBuildIterator();
			int dciPos = tw.addTree(buildIt);
			if (workingTreeIterator != null) {
				tw.addTree(workingTreeIterator);
				workingTreeIterator.setDirCacheIterator(tw, dciPos);
			} else {
				tw.setFilter(TreeFilter.ANY_DIFF);
			}

			if (!mergeTreeWalk(tw, ignoreConflicts)) {
				return false;
			}

			workTreeUpdater.writeWorkTreeChanges(true);
			if (getUnmergedPaths().isEmpty() && !failed()) {
				WorkTreeUpdater.Result result = workTreeUpdater.writeIndexChanges();
				resultTree = result.getTreeId();
				modifiedFiles = result.getModifiedFiles();
				for (String f : result.getFailedToDelete()) {
					failingPaths.put(f, MergeFailureReason.COULD_NOT_DELETE);
				}
				return result.getFailedToDelete().isEmpty();
			}
			resultTree = null;
			return false;
		} finally {
			if(modifiedFiles.isEmpty()) {
				modifiedFiles = workTreeUpdater.getModifiedFiles();
			}
			workTreeUpdater.close();
			workTreeUpdater = null;
		}
	}

	/**
	 * Process the given TreeWalk's entries.
	 *
	 * @param treeWalk
	 *            The walk to iterate over.
	 * @param ignoreConflicts
	 *            see
	 *            {@link org.eclipse.jgit.merge.ResolveMerger#mergeTrees(AbstractTreeIterator, RevTree, RevTree, boolean)}
	 * @return Whether the trees merged cleanly.
	 * @throws java.io.IOException
	 * @since 3.5
	 */
	protected boolean mergeTreeWalk(TreeWalk treeWalk, boolean ignoreConflicts)
			throws IOException {
		boolean hasWorkingTreeIterator = tw.getTreeCount() > T_FILE;
		boolean hasAttributeNodeProvider = treeWalk
				.getAttributesNodeProvider() != null;
		while (treeWalk.next()) {
			Attributes[] attributes = {NO_ATTRIBUTES, NO_ATTRIBUTES,
					NO_ATTRIBUTES};
			if (hasAttributeNodeProvider) {
				attributes[T_BASE] = treeWalk.getAttributes(T_BASE);
				attributes[T_OURS] = treeWalk.getAttributes(T_OURS);
				attributes[T_THEIRS] = treeWalk.getAttributes(T_THEIRS);
			}
			if (!processEntry(
					treeWalk.getTree(T_BASE, CanonicalTreeParser.class),
					treeWalk.getTree(T_OURS, CanonicalTreeParser.class),
					treeWalk.getTree(T_THEIRS, CanonicalTreeParser.class),
					treeWalk.getTree(T_INDEX, DirCacheBuildIterator.class),
					hasWorkingTreeIterator ? treeWalk.getTree(T_FILE,
							WorkingTreeIterator.class) : null,
					ignoreConflicts, attributes)) {
				workTreeUpdater.revertModifiedFiles();
				return false;
			}
			if (treeWalk.isSubtree() && enterSubtree) {
				treeWalk.enterSubtree();
			}
		}
		return true;
	}
}
