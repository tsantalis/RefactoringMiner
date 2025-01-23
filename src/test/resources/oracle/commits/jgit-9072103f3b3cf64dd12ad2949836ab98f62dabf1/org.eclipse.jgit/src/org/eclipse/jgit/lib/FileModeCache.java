/*
 * Copyright (C) 2023, Thomas Wolf <twolf@apache.org> and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0 which is available at
 * https://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package org.eclipse.jgit.lib;

import java.io.File;
import java.io.IOException;
import java.nio.file.InvalidPathException;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.jgit.annotations.NonNull;
import org.eclipse.jgit.util.FS;
import org.eclipse.jgit.util.FileUtils;

/**
 * A hierarchical cache of {@link FileMode}s per git path.
 *
 * @since 6.6.1
 */
public class FileModeCache {

	@NonNull
	private final CacheItem root = new CacheItem(FileMode.TREE);

	@NonNull
	private final Repository repo;

	/**
	 * Creates a new {@link FileModeCache} for a {@link Repository}.
	 *
	 * @param repo
	 *            {@link Repository} this cache is for
	 */
	public FileModeCache(@NonNull Repository repo) {
		this.repo = repo;
	}

	/**
	 * Retrieves the {@link Repository}.
	 *
	 * @return the {@link Repository} this {@link FileModeCache} was created for
	 */
	@NonNull
	public Repository getRepository() {
		return repo;
	}

	/**
	 * Obtains the {@link CacheItem} for the working tree root.
	 *
	 * @return the {@link CacheItem}
	 */
	@NonNull
	public CacheItem getRoot() {
		return root;
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
		CacheItem cachedParent = safeCreateDirectory(gitPath, parentDir,
				makeSpace);
		cachedParent.remove(gitPath.substring(gitPath.lastIndexOf('/') + 1));
	}

	/**
	 * Ensures the given directory {@code dir} with the given git path exists.
	 *
	 * @param gitPath
	 *            of a file to be written
	 * @param dir
	 *            directory in which the file shall be placed, assumed to be the
	 *            parent of the {@code gitPath}
	 * @param makeSpace
	 *            whether to remove a file that already at that name
	 * @return A {@link CacheItem} describing the directory, which is guaranteed
	 *         to exist
	 * @throws IOException
	 *             if the directory cannot be made to exist at the given
	 *             location
	 */
	public CacheItem safeCreateDirectory(String gitPath, File dir,
			boolean makeSpace) throws IOException {
		FS fs = repo.getFS();
		int i = gitPath.lastIndexOf('/');
		String parentPath = null;
		if (i >= 0) {
			if ((makeSpace && dir.isFile()) || fs.isSymLink(dir)) {
				FileUtils.delete(dir);
			}
			parentPath = gitPath.substring(0, i);
			deleteSymlinkParent(fs, parentPath, repo.getWorkTree());
		}
		FileUtils.mkdirs(dir, true);
		CacheItem cachedParent = getRoot();
		if (parentPath != null) {
			cachedParent = add(parentPath, FileMode.TREE);
		}
		return cachedParent;
	}

	private void deleteSymlinkParent(FS fs, String gitPath, File workingTree)
			throws IOException {
		if (!fs.supportsSymlinks()) {
			return;
		}
		String[] parts = gitPath.split("/"); //$NON-NLS-1$
		int n = parts.length;
		CacheItem cached = getRoot();
		File p = workingTree;
		for (int i = 0; i < n; i++) {
			p = new File(p, parts[i]);
			CacheItem cachedChild = cached != null ? cached.child(parts[i])
					: null;
			boolean delete = false;
			if (cachedChild != null) {
				if (FileMode.SYMLINK.equals(cachedChild.getMode())) {
					delete = true;
				}
			} else {
				try {
					Path nioPath = FileUtils.toPath(p);
					BasicFileAttributes attributes = nioPath.getFileSystem()
							.provider()
							.getFileAttributeView(nioPath,
									BasicFileAttributeView.class,
									LinkOption.NOFOLLOW_LINKS)
							.readAttributes();
					if (attributes.isSymbolicLink()) {
						delete = p.isDirectory();
					} else if (attributes.isRegularFile()) {
						break;
					}
				} catch (InvalidPathException | IOException e) {
					// If we can't get the attributes the path does not exist,
					// or if it does a subsequent mkdirs() will also throw an
					// exception.
					break;
				}
			}
			if (delete) {
				// Deletes the symlink
				FileUtils.delete(p, FileUtils.SKIP_MISSING);
				if (cached != null) {
					cached.remove(parts[i]);
				}
				break;
			}
			cached = cachedChild;
		}
	}

	/**
	 * Records the given {@link FileMode} for the given git path in the cache.
	 * If an entry already exists for the given path, the previously cached file
	 * mode is overwritten.
	 *
	 * @param gitPath
	 *            to cache the {@link FileMode} for
	 * @param finalMode
	 *            {@link FileMode} to cache
	 * @return the {@link CacheItem} for the path
	 */
	@NonNull
	private CacheItem add(String gitPath, FileMode finalMode) {
		if (gitPath.isEmpty()) {
			throw new IllegalArgumentException();
		}
		String[] parts = gitPath.split("/"); //$NON-NLS-1$
		int n = parts.length;
		int i = 0;
		CacheItem curr = getRoot();
		while (i < n) {
			CacheItem next = curr.child(parts[i]);
			if (next == null) {
				break;
			}
			curr = next;
			i++;
		}
		if (i == n) {
			curr.setMode(finalMode);
		} else {
			while (i < n) {
				curr = curr.insert(parts[i],
						i + 1 == n ? finalMode : FileMode.TREE);
				i++;
			}
		}
		return curr;
	}

	/**
	 * An item from a {@link FileModeCache}, recording information about a git
	 * path (known from context).
	 */
	public static class CacheItem {

		@NonNull
		private FileMode mode;

		private Map<String, CacheItem> children;

		/**
		 * Creates a new {@link CacheItem}.
		 *
		 * @param mode
		 *            {@link FileMode} to cache
		 */
		public CacheItem(@NonNull FileMode mode) {
			this.mode = mode;
		}

		/**
		 * Retrieves the cached {@link FileMode}.
		 *
		 * @return the {@link FileMode}
		 */
		@NonNull
		public FileMode getMode() {
			return mode;
		}

		/**
		 * Retrieves an immediate child of this {@link CacheItem} by name.
		 *
		 * @param childName
		 *            name of the child to get
		 * @return the {@link CacheItem}, or {@code null} if no such child is
		 *         known
		 */
		public CacheItem child(String childName) {
			if (children == null) {
				return null;
			}
			return children.get(childName);
		}

		/**
		 * Inserts a new cached {@link FileMode} as an immediate child of this
		 * {@link CacheItem}. If there is already a child with the same name, it
		 * is overwritten.
		 *
		 * @param childName
		 *            name of the child to create
		 * @param childMode
		 *            {@link FileMode} to cache
		 * @return the new {@link CacheItem} created for the child
		 */
		public CacheItem insert(String childName, @NonNull FileMode childMode) {
			if (!FileMode.TREE.equals(mode)) {
				throw new IllegalArgumentException();
			}
			if (children == null) {
				children = new HashMap<>();
			}
			CacheItem newItem = new CacheItem(childMode);
			children.put(childName, newItem);
			return newItem;
		}

		/**
		 * Removes the immediate child with the given name.
		 *
		 * @param childName
		 *            name of the child to remove
		 * @return the previously cached {@link CacheItem}, if any
		 */
		public CacheItem remove(String childName) {
			if (children == null) {
				return null;
			}
			return children.remove(childName);
		}

		void setMode(@NonNull FileMode mode) {
			this.mode = mode;
			if (!FileMode.TREE.equals(mode)) {
				children = null;
			}
		}
	}

}
