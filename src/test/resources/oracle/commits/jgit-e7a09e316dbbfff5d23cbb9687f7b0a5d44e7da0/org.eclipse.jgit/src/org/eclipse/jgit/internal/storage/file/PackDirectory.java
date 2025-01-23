/*
 * Copyright (C) 2009, Google Inc. and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0 which is available at
 * https://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package org.eclipse.jgit.internal.storage.file;

import static org.eclipse.jgit.internal.storage.pack.PackExt.BITMAP_INDEX;
import static org.eclipse.jgit.internal.storage.pack.PackExt.INDEX;
import static org.eclipse.jgit.internal.storage.pack.PackExt.PACK;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.jgit.annotations.Nullable;
import org.eclipse.jgit.errors.CorruptObjectException;
import org.eclipse.jgit.errors.PackInvalidException;
import org.eclipse.jgit.errors.PackMismatchException;
import org.eclipse.jgit.errors.SearchForReuseTimeout;
import org.eclipse.jgit.internal.JGitText;
import org.eclipse.jgit.internal.storage.pack.ObjectToPack;
import org.eclipse.jgit.internal.storage.pack.PackExt;
import org.eclipse.jgit.internal.storage.pack.PackWriter;
import org.eclipse.jgit.lib.AbbreviatedObjectId;
import org.eclipse.jgit.lib.AnyObjectId;
import org.eclipse.jgit.lib.Config;
import org.eclipse.jgit.lib.ConfigConstants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.util.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Traditional file system packed objects directory handler.
 * <p>
 * This is the {@link org.eclipse.jgit.internal.storage.file.Pack}s object
 * representation for a Git object database, where objects are stored in
 * compressed containers known as
 * {@link org.eclipse.jgit.internal.storage.file.Pack}s.
 */
class PackDirectory {
	private final static Logger LOG = LoggerFactory
			.getLogger(PackDirectory.class);

	private static final int MAX_PACKLIST_RESCAN_ATTEMPTS = 5;

	private static final PackList NO_PACKS = new PackList(FileSnapshot.DIRTY,
			new Pack[0]);

	private final Config config;

	private final File directory;

	private final AtomicReference<PackList> packList;

	private final boolean trustFolderStat;

	/**
	 * Initialize a reference to an on-disk 'pack' directory.
	 *
	 * @param config
	 *            configuration this directory consults for write settings.
	 * @param directory
	 *            the location of the {@code pack} directory.
	 */
	PackDirectory(Config config, File directory) {
		this.config = config;
		this.directory = directory;
		packList = new AtomicReference<>(NO_PACKS);

		// Whether to trust the pack folder's modification time. If set to false
		// we will always scan the .git/objects/pack folder to check for new
		// pack files. If set to true (default) we use the folder's size,
		// modification time, and key (inode) and assume that no new pack files
		// can be in this folder if these attributes have not changed.
		trustFolderStat = config.getBoolean(ConfigConstants.CONFIG_CORE_SECTION,
				ConfigConstants.CONFIG_KEY_TRUSTFOLDERSTAT, true);
	}

	/**
	 * Getter for the field {@code directory}.
	 *
	 * @return the location of the {@code pack} directory.
	 */
	File getDirectory() {
		return directory;
	}

	void create() throws IOException {
		FileUtils.mkdir(directory);
	}

	void close() {
		PackList packs = packList.get();
		if (packs != NO_PACKS && packList.compareAndSet(packs, NO_PACKS)) {
			for (Pack p : packs.packs) {
				p.close();
			}
		}
	}

	Collection<Pack> getPacks() {
		PackList list = packList.get();
		if (list == NO_PACKS) {
			list = scanPacks(list);
		}
		Pack[] packs = list.packs;
		return Collections.unmodifiableCollection(Arrays.asList(packs));
	}

	@Override
	public String toString() {
		return "PackDirectory[" + getDirectory() + "]"; //$NON-NLS-1$ //$NON-NLS-2$
	}

	/**
	 * Does the requested object exist in this PackDirectory?
	 *
	 * @param objectId
	 *            identity of the object to test for existence of.
	 * @return {@code true} if the specified object is stored in this PackDirectory.
	 */
	boolean has(AnyObjectId objectId) {
		return getPack(objectId) != null;
	}

	/**
	 * Get the {@link org.eclipse.jgit.internal.storage.file.Pack} for the
	 * specified object if it is stored in this PackDirectory.
	 *
	 * @param objectId
	 *            identity of the object to find the Pack for.
	 * @return {@link org.eclipse.jgit.internal.storage.file.Pack} which
	 *         contains the specified object or {@code null} if it is not stored
	 *         in this PackDirectory.
	 */
	@Nullable
	Pack getPack(AnyObjectId objectId) {
		PackList pList;
		do {
			pList = packList.get();
			for (Pack p : pList.packs) {
				try {
					if (p.hasObject(objectId)) {
						return p;
					}
				} catch (IOException e) {
					// The hasObject call should have only touched the index, so
					// any failure here indicates the index is unreadable by
					// this process, and the pack is likewise not readable.
					LOG.warn(MessageFormat.format(
							JGitText.get().unableToReadPackfile,
							p.getPackFile().getAbsolutePath()), e);
					remove(p);
				}
			}
		} while (searchPacksAgain(pList));
		return null;
	}

	/**
	 * Find objects matching the prefix abbreviation.
	 *
	 * @param matches
	 *            set to add any located ObjectIds to. This is an output
	 *            parameter.
	 * @param id
	 *            prefix to search for.
	 * @param matchLimit
	 *            maximum number of results to return. At most this many
	 *            ObjectIds should be added to matches before returning.
	 * @return {@code true} if the matches were exhausted before reaching
	 *         {@code maxLimit}.
	 */
	boolean resolve(Set<ObjectId> matches, AbbreviatedObjectId id,
			int matchLimit) {
		// Go through the packs once. If we didn't find any resolutions
		// scan for new packs and check once more.
		int oldSize = matches.size();
		PackList pList;
		do {
			pList = packList.get();
			for (Pack p : pList.packs) {
				try {
					p.resolve(matches, id, matchLimit);
					p.resetTransientErrorCount();
				} catch (IOException e) {
					handlePackError(e, p);
				}
				if (matches.size() > matchLimit) {
					return false;
				}
			}
		} while (matches.size() == oldSize && searchPacksAgain(pList));
		return true;
	}

	ObjectLoader open(WindowCursor curs, AnyObjectId objectId)
			throws PackMismatchException {
		PackList pList;
		do {
			int retries = 0;
			SEARCH: for (;;) {
				pList = packList.get();
				for (Pack p : pList.packs) {
					try {
						ObjectLoader ldr = p.get(curs, objectId);
						p.resetTransientErrorCount();
						if (ldr != null)
							return ldr;
					} catch (PackMismatchException e) {
						// Pack was modified; refresh the entire pack list.
						if (searchPacksAgain(pList)) {
							retries = checkRescanPackThreshold(retries, e);
							continue SEARCH;
						}
					} catch (IOException e) {
						handlePackError(e, p);
					}
				}
				break SEARCH;
			}
		} while (searchPacksAgain(pList));
		return null;
	}

	long getSize(WindowCursor curs, AnyObjectId id)
			throws PackMismatchException {
		PackList pList;
		do {
			int retries = 0;
			SEARCH: for (;;) {
				pList = packList.get();
				for (Pack p : pList.packs) {
					try {
						long len = p.getObjectSize(curs, id);
						p.resetTransientErrorCount();
						if (0 <= len) {
							return len;
						}
					} catch (PackMismatchException e) {
						// Pack was modified; refresh the entire pack list.
						if (searchPacksAgain(pList)) {
							retries = checkRescanPackThreshold(retries, e);
							continue SEARCH;
						}
					} catch (IOException e) {
						handlePackError(e, p);
					}
				}
				break SEARCH;
			}
		} while (searchPacksAgain(pList));
		return -1;
	}

	void selectRepresentation(PackWriter packer, ObjectToPack otp,
			WindowCursor curs) throws PackMismatchException {
		PackList pList = packList.get();
		int retries = 0;
		SEARCH: for (;;) {
			for (Pack p : pList.packs) {
				try {
					LocalObjectRepresentation rep = p.representation(curs, otp);
					p.resetTransientErrorCount();
					if (rep != null) {
						packer.select(otp, rep);
						packer.checkSearchForReuseTimeout();
					}
				} catch (SearchForReuseTimeout e) {
					break SEARCH;
				} catch (PackMismatchException e) {
					// Pack was modified; refresh the entire pack list.
					//
					retries = checkRescanPackThreshold(retries, e);
					pList = scanPacks(pList);
					continue SEARCH;
				} catch (IOException e) {
					handlePackError(e, p);
				}
			}
			break SEARCH;
		}
	}

	private int checkRescanPackThreshold(int retries, PackMismatchException e)
			throws PackMismatchException {
		if (retries++ > MAX_PACKLIST_RESCAN_ATTEMPTS) {
			e.setPermanent(true);
			throw e;
		}
		return retries;
	}

	private void handlePackError(IOException e, Pack p) {
		String warnTmpl = null;
		int transientErrorCount = 0;
		String errTmpl = JGitText.get().exceptionWhileReadingPack;
		if ((e instanceof CorruptObjectException)
				|| (e instanceof PackInvalidException)) {
			warnTmpl = JGitText.get().corruptPack;
			LOG.warn(MessageFormat.format(warnTmpl,
					p.getPackFile().getAbsolutePath()), e);
			// Assume the pack is corrupted, and remove it from the list.
			remove(p);
		} else if (e instanceof FileNotFoundException) {
			if (p.getPackFile().exists()) {
				errTmpl = JGitText.get().packInaccessible;
				transientErrorCount = p.incrementTransientErrorCount();
			} else {
				warnTmpl = JGitText.get().packWasDeleted;
				remove(p);
			}
		} else if (FileUtils.isStaleFileHandleInCausalChain(e)) {
			warnTmpl = JGitText.get().packHandleIsStale;
			remove(p);
		} else {
			transientErrorCount = p.incrementTransientErrorCount();
		}
		if (warnTmpl != null) {
			LOG.warn(MessageFormat.format(warnTmpl,
					p.getPackFile().getAbsolutePath()), e);
		} else {
			if (doLogExponentialBackoff(transientErrorCount)) {
				// Don't remove the pack from the list, as the error may be
				// transient.
				LOG.error(MessageFormat.format(errTmpl,
						p.getPackFile().getAbsolutePath(),
						Integer.valueOf(transientErrorCount)), e);
			}
		}
	}

	/**
	 * @param n
	 *            count of consecutive failures
	 * @return {@code true} if i is a power of 2
	 */
	private boolean doLogExponentialBackoff(int n) {
		return (n & (n - 1)) == 0;
	}

	boolean searchPacksAgain(PackList old) {
		return ((!trustFolderStat) || old.snapshot.isModified(directory))
				&& old != scanPacks(old);
	}

	void insert(Pack pack) {
		PackList o, n;
		do {
			o = packList.get();

			// If the pack in question is already present in the list
			// (picked up by a concurrent thread that did a scan?) we
			// do not want to insert it a second time.
			//
			final Pack[] oldList = o.packs;
			final String name = pack.getPackFile().getName();
			for (Pack p : oldList) {
				if (name.equals(p.getPackFile().getName())) {
					return;
				}
			}

			final Pack[] newList = new Pack[1 + oldList.length];
			newList[0] = pack;
			System.arraycopy(oldList, 0, newList, 1, oldList.length);
			n = new PackList(o.snapshot, newList);
		} while (!packList.compareAndSet(o, n));
	}

	private void remove(Pack deadPack) {
		PackList o, n;
		do {
			o = packList.get();

			final Pack[] oldList = o.packs;
			final int j = indexOf(oldList, deadPack);
			if (j < 0) {
				break;
			}

			final Pack[] newList = new Pack[oldList.length - 1];
			System.arraycopy(oldList, 0, newList, 0, j);
			System.arraycopy(oldList, j + 1, newList, j, newList.length - j);
			n = new PackList(o.snapshot, newList);
		} while (!packList.compareAndSet(o, n));
		deadPack.close();
	}

	private static int indexOf(Pack[] list, Pack pack) {
		for (int i = 0; i < list.length; i++) {
			if (list[i] == pack) {
				return i;
			}
		}
		return -1;
	}

	private PackList scanPacks(PackList original) {
		synchronized (packList) {
			PackList o, n;
			do {
				o = packList.get();
				if (o != original) {
					// Another thread did the scan for us, while we
					// were blocked on the monitor above.
					//
					return o;
				}
				n = scanPacksImpl(o);
				if (n == o) {
					return n;
				}
			} while (!packList.compareAndSet(o, n));
			return n;
		}
	}

	private PackList scanPacksImpl(PackList old) {
		final Map<String, Pack> forReuse = reuseMap(old);
		final FileSnapshot snapshot = FileSnapshot.save(directory);
		Map<String, Map<PackExt, PackFile>> packFilesByExtById = getPackFilesByExtById();
		List<Pack> list = new ArrayList<>(packFilesByExtById.size());
		boolean foundNew = false;
		for (Map<PackExt, PackFile> packFilesByExt : packFilesByExtById
				.values()) {
			PackFile packFile = packFilesByExt.get(PACK);
			if (packFile == null || !packFilesByExt.containsKey(INDEX)) {
				// Sometimes C Git's HTTP fetch transport leaves a
				// .idx file behind and does not download the .pack.
				// We have to skip over such useless indexes.
				// Also skip if we don't have any index for this id
				continue;
			}

			Pack oldPack = forReuse.get(packFile.getName());
			if (oldPack != null
					&& !oldPack.getFileSnapshot().isModified(packFile)) {
				forReuse.remove(packFile.getName());
				list.add(oldPack);
				continue;
			}

			list.add(new Pack(config, packFile, packFilesByExt.get(BITMAP_INDEX)));
			foundNew = true;
		}

		// If we did not discover any new files, the modification time was not
		// changed, and we did not remove any files, then the set of files is
		// the same as the set we were given. Instead of building a new object
		// return the same collection.
		//
		if (!foundNew && forReuse.isEmpty() && snapshot.equals(old.snapshot)) {
			old.snapshot.setClean(snapshot);
			return old;
		}

		for (Pack p : forReuse.values()) {
			p.close();
		}

		if (list.isEmpty()) {
			return new PackList(snapshot, NO_PACKS.packs);
		}

		final Pack[] r = list.toArray(new Pack[0]);
		Arrays.sort(r, Pack.SORT);
		return new PackList(snapshot, r);
	}

	private static Map<String, Pack> reuseMap(PackList old) {
		final Map<String, Pack> forReuse = new HashMap<>();
		for (Pack p : old.packs) {
			if (p.invalid()) {
				// The pack instance is corrupted, and cannot be safely used
				// again. Do not include it in our reuse map.
				//
				p.close();
				continue;
			}

			final Pack prior = forReuse.put(p.getPackFile().getName(), p);
			if (prior != null) {
				// This should never occur. It should be impossible for us
				// to have two pack files with the same name, as all of them
				// came out of the same directory. If it does, we promised to
				// close any PackFiles we did not reuse, so close the second,
				// readers are likely to be actively using the first.
				//
				forReuse.put(prior.getPackFile().getName(), prior);
				p.close();
			}
		}
		return forReuse;
	}

	/**
	 * Scans the pack directory for
	 * {@link org.eclipse.jgit.internal.storage.file.PackFile}s and returns them
	 * organized by their extensions and their pack ids
	 *
	 * Skips files in the directory that we cannot create a
	 * {@link org.eclipse.jgit.internal.storage.file.PackFile} for.
	 *
	 * @return a map of {@link org.eclipse.jgit.internal.storage.file.PackFile}s
	 *         and {@link org.eclipse.jgit.internal.storage.pack.PackExt}s keyed
	 *         by pack ids
	 */
	private Map<String, Map<PackExt, PackFile>> getPackFilesByExtById() {
		final String[] nameList = directory.list();
		if (nameList == null) {
			return Collections.emptyMap();
		}
		Map<String, Map<PackExt, PackFile>> packFilesByExtById = new HashMap<>(
				nameList.length / 2); // assume roughly 2 files per id
		for (String name : nameList) {
			try {
				PackFile pack = new PackFile(directory, name);
				if (pack.getPackExt() != null) {
					Map<PackExt, PackFile> packByExt = packFilesByExtById
							.get(pack.getId());
					if (packByExt == null) {
						packByExt = new EnumMap<>(PackExt.class);
						packFilesByExtById.put(pack.getId(), packByExt);
					}
					packByExt.put(pack.getPackExt(), pack);
				}
			} catch (IllegalArgumentException e) {
				continue;
			}
		}
		return packFilesByExtById;
	}

	static final class PackList {
		/** State just before reading the pack directory. */
		final FileSnapshot snapshot;

		/** All known packs, sorted by {@link Pack#SORT}. */
		final Pack[] packs;

		PackList(FileSnapshot monitor, Pack[] packs) {
			this.snapshot = monitor;
			this.packs = packs;
		}
	}
}
