/*
 * Copyright (C) 2023, Thomas Wolf <twolf@apache.org> and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0 which is available at
 * https://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package org.eclipse.jgit.dircache;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.StandardCopyOption;
import java.text.MessageFormat;

import org.eclipse.jgit.annotations.NonNull;
import org.eclipse.jgit.dircache.DirCacheCheckout.CheckoutMetadata;
import org.eclipse.jgit.internal.JGitText;
import org.eclipse.jgit.lib.FileMode;
import org.eclipse.jgit.lib.FileModeCache;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.CoreConfig.EolStreamType;
import org.eclipse.jgit.lib.CoreConfig.SymLinks;
import org.eclipse.jgit.lib.FileModeCache.CacheItem;
import org.eclipse.jgit.treewalk.WorkingTreeOptions;
import org.eclipse.jgit.util.FS;
import org.eclipse.jgit.util.FileUtils;
import org.eclipse.jgit.util.RawParseUtils;

/**
 * An object that can be used to check out many files.
 *
 * @since 6.6.1
 */
public class Checkout {

	private final FileModeCache cache;

	private final WorkingTreeOptions options;

	private boolean recursiveDelete;

	/**
	 * Creates a new {@link Checkout} for checking out from the given
	 * repository.
	 *
	 * @param repo
	 *            the {@link Repository} to check out from
	 */
	public Checkout(@NonNull Repository repo) {
		this(repo, null);
	}

	/**
	 * Creates a new {@link Checkout} for checking out from the given
	 * repository.
	 *
	 * @param repo
	 *            the {@link Repository} to check out from
	 * @param options
	 *            the {@link WorkingTreeOptions} to use; if {@code null},
	 *            read from the {@code repo} config when this object is
	 *            created
	 */
	public Checkout(@NonNull Repository repo, WorkingTreeOptions options) {
		this.cache = new FileModeCache(repo);
		this.options = options != null ? options
				: repo.getConfig().get(WorkingTreeOptions.KEY);
	}

	/**
	 * Retrieves the {@link WorkingTreeOptions} of the repository that are
	 * used.
	 *
	 * @return the {@link WorkingTreeOptions}
	 */
	public WorkingTreeOptions getWorkingTreeOptions() {
		return options;
	}

	/**
	 * Defines whether directories that are in the way of the file to be checked
	 * out shall be deleted recursively.
	 *
	 * @param recursive
	 *            whether to delete such directories recursively
	 * @return {@code this}
	 */
	public Checkout setRecursiveDeletion(boolean recursive) {
		this.recursiveDelete = recursive;
		return this;
	}

	/**
	 * Ensure that the given parent directory exists, and cache the information
	 * that gitPath refers to a file.
	 *
	 * @param gitPath
	 *            of the file to be written
	 * @param parentDir
	 *            directory in which the file shall be placed, assumed to be the
	 *            parent of the {@code gitPath}
	 * @param makeSpace
	 *            whether to delete a possibly existing file at
	 *            {@code parentDir}
	 * @throws IOException
	 *             if the directory cannot be created, if necessary
	 */
	public void safeCreateParentDirectory(String gitPath, File parentDir,
			boolean makeSpace) throws IOException {
		cache.safeCreateParentDirectory(gitPath, parentDir, makeSpace);
	}

	/**
	 * Checks out the gitlink given by the {@link DirCacheEntry}.
	 *
	 * @param entry
	 *            {@link DirCacheEntry} to check out
	 * @param gitPath
	 *            the git path of the entry, if known already; otherwise
	 *            {@code null} and it's read from the entry itself
	 * @throws IOException
	 *             if the gitlink cannot be checked out
	 */
	public void checkoutGitlink(DirCacheEntry entry, String gitPath)
			throws IOException {
		FS fs = cache.getRepository().getFS();
		File workingTree = cache.getRepository().getWorkTree();
		String path = gitPath != null ? gitPath : entry.getPathString();
		File gitlinkDir = new File(workingTree, path);
		File parentDir = gitlinkDir.getParentFile();
		CacheItem cachedParent = cache.safeCreateDirectory(path, parentDir,
				false);
		FileUtils.mkdirs(gitlinkDir, true);
		cachedParent.insert(path.substring(path.lastIndexOf('/') + 1),
				FileMode.GITLINK);
		entry.setLastModified(fs.lastModifiedInstant(gitlinkDir));
	}

	/**
	 * Checks out the file given by the {@link DirCacheEntry}.
	 *
	 * @param entry
	 *            {@link DirCacheEntry} to check out
	 * @param metadata
	 *            {@link CheckoutMetadata} to use for CR/LF handling and
	 *            smudge filtering
	 * @param reader
	 *            {@link ObjectReader} to use
	 * @param gitPath
	 *            the git path of the entry, if known already; otherwise
	 *            {@code null} and it's read from the entry itself
	 * @throws IOException
	 *             if the file cannot be checked out
	 */
	public void checkout(DirCacheEntry entry, CheckoutMetadata metadata,
			ObjectReader reader, String gitPath) throws IOException {
		if (metadata == null) {
			metadata = CheckoutMetadata.EMPTY;
		}
		FS fs = cache.getRepository().getFS();
		ObjectLoader ol = reader.open(entry.getObjectId());
		String path = gitPath != null ? gitPath : entry.getPathString();
		File f = new File(cache.getRepository().getWorkTree(), path);
		File parentDir = f.getParentFile();
		CacheItem cachedParent = cache.safeCreateDirectory(path, parentDir,
				true);
		if (entry.getFileMode() == FileMode.SYMLINK
				&& options.getSymLinks() == SymLinks.TRUE) {
			byte[] bytes = ol.getBytes();
			String target = RawParseUtils.decode(bytes);
			if (recursiveDelete && Files.isDirectory(f.toPath(),
					LinkOption.NOFOLLOW_LINKS)) {
				FileUtils.delete(f, FileUtils.RECURSIVE);
			}
			fs.createSymLink(f, target);
			cachedParent.insert(f.getName(), FileMode.SYMLINK);
			entry.setLength(bytes.length);
			entry.setLastModified(fs.lastModifiedInstant(f));
			return;
		}

		String name = f.getName();
		if (name.length() > 200) {
			name = name.substring(0, 200);
		}
		File tmpFile = File.createTempFile("._" + name, null, parentDir); //$NON-NLS-1$

		DirCacheCheckout.getContent(cache.getRepository(), path, metadata, ol,
				options,
				new FileOutputStream(tmpFile));

		// The entry needs to correspond to the on-disk file size. If the
		// content was filtered (either by autocrlf handling or smudge
		// filters) ask the file system again for the length. Otherwise the
		// object loader knows the size
		if (metadata.eolStreamType == EolStreamType.DIRECT
				&& metadata.smudgeFilterCommand == null) {
			entry.setLength(ol.getSize());
		} else {
			entry.setLength(tmpFile.length());
		}

		if (options.isFileMode() && fs.supportsExecute()) {
			if (FileMode.EXECUTABLE_FILE.equals(entry.getRawMode())) {
				if (!fs.canExecute(tmpFile))
					fs.setExecute(tmpFile, true);
			} else {
				if (fs.canExecute(tmpFile))
					fs.setExecute(tmpFile, false);
			}
		}
		try {
			if (recursiveDelete && Files.isDirectory(f.toPath(),
					LinkOption.NOFOLLOW_LINKS)) {
				FileUtils.delete(f, FileUtils.RECURSIVE);
			}
			FileUtils.rename(tmpFile, f, StandardCopyOption.ATOMIC_MOVE);
			cachedParent.remove(f.getName());
		} catch (IOException e) {
			throw new IOException(
					MessageFormat.format(JGitText.get().renameFileFailed,
							tmpFile.getPath(), f.getPath()),
					e);
		} finally {
			if (tmpFile.exists()) {
				FileUtils.delete(tmpFile);
			}
		}
		entry.setLastModified(fs.lastModifiedInstant(f));
	}
}