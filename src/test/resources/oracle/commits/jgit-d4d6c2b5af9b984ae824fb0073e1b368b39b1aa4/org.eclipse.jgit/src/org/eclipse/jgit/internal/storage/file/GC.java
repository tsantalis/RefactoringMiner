/*
 * Copyright (C) 2012, Christian Halstrick <christian.halstrick@sap.com>
 * Copyright (C) 2011, Shawn O. Pearce <spearce@spearce.org> and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0 which is available at
 * https://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package org.eclipse.jgit.internal.storage.file;

import static org.eclipse.jgit.internal.storage.pack.PackExt.BITMAP_INDEX;
import static org.eclipse.jgit.internal.storage.pack.PackExt.COMMIT_GRAPH;
import static org.eclipse.jgit.internal.storage.pack.PackExt.INDEX;
import static org.eclipse.jgit.internal.storage.pack.PackExt.KEEP;
import static org.eclipse.jgit.internal.storage.pack.PackExt.PACK;
import static org.eclipse.jgit.internal.storage.pack.PackExt.REVERSE_INDEX;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.io.StringWriter;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.text.MessageFormat;
import java.text.ParseException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.jgit.annotations.NonNull;
import org.eclipse.jgit.dircache.DirCacheIterator;
import org.eclipse.jgit.errors.CancelledException;
import org.eclipse.jgit.errors.CorruptObjectException;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.errors.NoWorkTreeException;
import org.eclipse.jgit.internal.JGitText;
import org.eclipse.jgit.internal.storage.commitgraph.CommitGraphWriter;
import org.eclipse.jgit.internal.storage.commitgraph.GraphCommits;
import org.eclipse.jgit.internal.storage.pack.PackExt;
import org.eclipse.jgit.internal.storage.pack.PackWriter;
import org.eclipse.jgit.lib.ConfigConstants;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.CoreConfig;
import org.eclipse.jgit.lib.FileMode;
import org.eclipse.jgit.lib.NullProgressMonitor;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectIdSet;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.ProgressMonitor;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Ref.Storage;
import org.eclipse.jgit.lib.RefDatabase;
import org.eclipse.jgit.lib.ReflogEntry;
import org.eclipse.jgit.lib.ReflogReader;
import org.eclipse.jgit.lib.internal.WorkQueue;
import org.eclipse.jgit.revwalk.ObjectWalk;
import org.eclipse.jgit.revwalk.RevObject;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.pack.PackConfig;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.TreeFilter;
import org.eclipse.jgit.util.FS;
import org.eclipse.jgit.util.FS.LockToken;
import org.eclipse.jgit.util.FileUtils;
import org.eclipse.jgit.util.GitDateParser;
import org.eclipse.jgit.util.StringUtils;
import org.eclipse.jgit.util.SystemReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A garbage collector for git
 * {@link org.eclipse.jgit.internal.storage.file.FileRepository}. Instances of
 * this class are not thread-safe. Don't use the same instance from multiple
 * threads.
 *
 * This class started as a copy of DfsGarbageCollector from Shawn O. Pearce
 * adapted to FileRepositories.
 */
public class GC {
	private static final Logger LOG = LoggerFactory
			.getLogger(GC.class);

	private static final String PRUNE_EXPIRE_DEFAULT = "2.weeks.ago"; //$NON-NLS-1$

	private static final String PRUNE_PACK_EXPIRE_DEFAULT = "1.hour.ago"; //$NON-NLS-1$

	private static final Pattern PATTERN_LOOSE_OBJECT = Pattern
			.compile("[0-9a-fA-F]{38}"); //$NON-NLS-1$

	private static final Set<PackExt> PARENT_EXTS = Set.of(PACK, KEEP);

	private static final Set<PackExt> CHILD_EXTS = Set.of(BITMAP_INDEX, INDEX,
			REVERSE_INDEX);

	private static final int DEFAULT_AUTOPACKLIMIT = 50;

	private static final int DEFAULT_AUTOLIMIT = 6700;

	private static final boolean DEFAULT_WRITE_BLOOM_FILTER = false;

	private static final boolean DEFAULT_WRITE_COMMIT_GRAPH = false;

	private static volatile ExecutorService executor;

	/**
	 * Set the executor for running auto-gc in the background. If no executor is
	 * set JGit's own WorkQueue will be used.
	 *
	 * @param e
	 *            the executor to be used for running auto-gc
	 */
	public static void setExecutor(ExecutorService e) {
		executor = e;
	}

	private final FileRepository repo;

	private ProgressMonitor pm;

	private long expireAgeMillis = -1;

	private Date expire;

	private long packExpireAgeMillis = -1;

	private Date packExpire;

	private PackConfig pconfig;

	/**
	 * the refs which existed during the last call to {@link #repack()}. This is
	 * needed during {@link #prune(Set)} where we can optimize by looking at the
	 * difference between the current refs and the refs which existed during
	 * last {@link #repack()}.
	 */
	private Collection<Ref> lastPackedRefs;

	/**
	 * Holds the starting time of the last repack() execution. This is needed in
	 * prune() to inspect only those reflog entries which have been added since
	 * last repack().
	 */
	private long lastRepackTime;

	/**
	 * Whether gc should do automatic housekeeping
	 */
	private boolean automatic;

	/**
	 * Whether to run gc in a background thread
	 */
	private boolean background;

	/**
	 * Creates a new garbage collector with default values. An expirationTime of
	 * two weeks and <code>null</code> as progress monitor will be used.
	 *
	 * @param repo
	 *            the repo to work on
	 */
	public GC(FileRepository repo) {
		this.repo = repo;
		this.pconfig = new PackConfig(repo);
		this.pm = NullProgressMonitor.INSTANCE;
	}

	/**
	 * Runs a garbage collector on a
	 * {@link org.eclipse.jgit.internal.storage.file.FileRepository}. It will
	 * <ul>
	 * <li>pack loose references into packed-refs</li>
	 * <li>repack all reachable objects into new pack files and delete the old
	 * pack files</li>
	 * <li>prune all loose objects which are now reachable by packs</li>
	 * </ul>
	 *
	 * If {@link #setAuto(boolean)} was set to {@code true} {@code gc} will
	 * first check whether any housekeeping is required; if not, it exits
	 * without performing any work.
	 *
	 * If {@link #setBackground(boolean)} was set to {@code true}
	 * {@code collectGarbage} will start the gc in the background, and then
	 * return immediately. In this case, errors will not be reported except in
	 * gc.log.
	 *
	 * @return the collection of
	 *         {@link org.eclipse.jgit.internal.storage.file.Pack}'s which are
	 *         newly created
	 * @throws java.io.IOException
	 *             if an IO error occurred
	 * @throws java.text.ParseException
	 *             If the configuration parameter "gc.pruneexpire" couldn't be
	 *             parsed
	 */
	public CompletableFuture<Collection<Pack>> gc()
			throws IOException, ParseException {
		if (!background) {
			return CompletableFuture.completedFuture(doGc());
		}
		final GcLog gcLog = new GcLog(repo);
		if (!gcLog.lock()) {
			// there is already a background gc running
			return CompletableFuture.completedFuture(Collections.emptyList());
		}

		Supplier<Collection<Pack>> gcTask = () -> {
			try {
				Collection<Pack> newPacks = doGc();
				if (automatic && tooManyLooseObjects()) {
					String message = JGitText.get().gcTooManyUnpruned;
					gcLog.write(message);
					gcLog.commit();
				}
				return newPacks;
			} catch (IOException | ParseException e) {
				try {
					gcLog.write(e.getMessage());
					StringWriter sw = new StringWriter();
					e.printStackTrace(new PrintWriter(sw));
					gcLog.write(sw.toString());
					gcLog.commit();
				} catch (IOException e2) {
					e2.addSuppressed(e);
					LOG.error(e2.getMessage(), e2);
				}
			} finally {
				gcLog.unlock();
			}
			return Collections.emptyList();
		};
		return CompletableFuture.supplyAsync(gcTask, executor());
	}

	private ExecutorService executor() {
		return (executor != null) ? executor : WorkQueue.getExecutor();
	}

	private Collection<Pack> doGc() throws IOException, ParseException {
		if (automatic && !needGc()) {
			return Collections.emptyList();
		}
		try (PidLock lock = new PidLock()) {
			if (!lock.lock()) {
				return Collections.emptyList();
			}
			pm.start(6 /* tasks */);
			packRefs();
			// TODO: implement reflog_expire(pm, repo);
			Collection<Pack> newPacks = repack();
			prune(Collections.emptySet());
			// TODO: implement rerere_gc(pm);
			if (shouldWriteCommitGraphWhenGc()) {
				writeCommitGraph(refsToObjectIds(getAllRefs()));
			}
			return newPacks;
		}
	}

	/**
	 * Loosen objects in a pack file which are not also in the newly-created
	 * pack files.
	 *
	 * @param inserter
	 *            used to insert objects
	 * @param reader
	 *            used to read objects
	 * @param pack
	 *            the pack file to loosen objects for
	 * @param existing
	 *            existing objects
	 * @throws IOException
	 *             if an IO error occurred
	 */
	private void loosen(ObjectDirectoryInserter inserter, ObjectReader reader, Pack pack, HashSet<ObjectId> existing)
			throws IOException {
		for (PackIndex.MutableEntry entry : pack) {
			ObjectId oid = entry.toObjectId();
			if (existing.contains(oid)) {
				continue;
			}
			existing.add(oid);
			ObjectLoader loader = reader.open(oid);
			inserter.insert(loader.getType(),
					loader.getSize(),
					loader.openStream(),
					true /* create this object even though it's a duplicate */);
		}
	}

	/**
	 * Delete old pack files. What is 'old' is defined by specifying a set of
	 * old pack files and a set of new pack files. Each pack file contained in
	 * old pack files but not contained in new pack files will be deleted. If
	 * preserveOldPacks is set, keep a copy of the pack file in the preserve
	 * directory. If an expirationDate is set then pack files which are younger
	 * than the expirationDate will not be deleted nor preserved.
	 * <p>
	 * If we're not immediately expiring loose objects, loosen any objects in
	 * the old pack files which aren't in the new pack files.
	 *
	 * @param oldPacks
	 *            old pack files
	 * @param newPacks
	 *            new pack files
	 * @throws ParseException
	 *             if an error occurred during parsing
	 * @throws IOException
	 *             if an IO error occurred
	 */
	private void deleteOldPacks(Collection<Pack> oldPacks,
			Collection<Pack> newPacks) throws ParseException, IOException {
		HashSet<ObjectId> ids = new HashSet<>();
		for (Pack pack : newPacks) {
			for (PackIndex.MutableEntry entry : pack) {
				ids.add(entry.toObjectId());
			}
		}
		ObjectReader reader = repo.newObjectReader();
		ObjectDirectory dir = repo.getObjectDatabase();
		ObjectDirectoryInserter inserter = dir.newInserter();
		boolean shouldLoosen = !"now".equals(getPruneExpireStr()) && //$NON-NLS-1$
			getExpireDate() < Long.MAX_VALUE;

		prunePreserved();
		long packExpireDate = getPackExpireDate();
		List<PackFile> packFilesToPrune = new ArrayList<>();
		oldPackLoop: for (Pack oldPack : oldPacks) {
			checkCancelled();
			String oldName = oldPack.getPackName();
			// check whether an old pack file is also among the list of new
			// pack files. Then we must not delete it.
			for (Pack newPack : newPacks)
				if (oldName.equals(newPack.getPackName()))
					continue oldPackLoop;

			if (!oldPack.shouldBeKept()
					&& repo.getFS()
							.lastModifiedInstant(oldPack.getPackFile())
							.toEpochMilli() < packExpireDate) {
				if (shouldLoosen) {
					loosen(inserter, reader, oldPack, ids);
				}
				oldPack.close();
				packFilesToPrune.add(oldPack.getPackFile());
			}
		}
		packFilesToPrune.forEach(this::prunePack);

		// close the complete object database. That's my only chance to force
		// rescanning and to detect that certain pack files are now deleted.
		repo.getObjectDatabase().close();
	}

	/**
	 * Deletes old pack file, unless 'preserve-oldpacks' is set, in which case
	 * it moves the pack file to the preserved directory
	 *
	 * @param packFile
	 *            the packfile to delete
	 * @param deleteOptions
	 *            delete option flags
	 * @throws IOException
	 *             if an IO error occurred
	 */
	private void removeOldPack(PackFile packFile, int deleteOptions)
			throws IOException {
		if (pconfig.isPreserveOldPacks()) {
			File oldPackDir = repo.getObjectDatabase().getPreservedDirectory();
			FileUtils.mkdir(oldPackDir, true);

			PackFile oldPackFile = packFile
					.createPreservedForDirectory(oldPackDir);
			FileUtils.rename(packFile, oldPackFile);
		} else {
			FileUtils.delete(packFile, deleteOptions);
		}
	}

	/**
	 * Delete the preserved directory including all pack files within
	 */
	private void prunePreserved() {
		if (pconfig.isPrunePreserved()) {
			try {
				FileUtils.delete(repo.getObjectDatabase().getPreservedDirectory(),
						FileUtils.RECURSIVE | FileUtils.RETRY | FileUtils.SKIP_MISSING);
			} catch (IOException e) {
				// Deletion of the preserved pack files failed. Silently return.
			}
		}
	}

	/**
	 * Delete files associated with a single pack file. First try to delete the
	 * ".pack" file because on some platforms the ".pack" file may be locked and
	 * can't be deleted. In such a case it is better to detect this early and
	 * give up on deleting files for this packfile. Otherwise we may delete the
	 * ".index" file and when failing to delete the ".pack" file we are left
	 * with a ".pack" file without a ".index" file.
	 *
	 * @param packFile
	 *            the pack file to prune files for
	 */
	private void prunePack(PackFile packFile) {
		try {
			// Delete the .pack file first and if this fails give up on deleting
			// the other files
			int deleteOptions = FileUtils.RETRY | FileUtils.SKIP_MISSING;
			removeOldPack(packFile.create(PackExt.PACK), deleteOptions);

			// The .pack file has been deleted. Delete as many as the other
			// files as you can.
			deleteOptions |= FileUtils.IGNORE_ERRORS;
			for (PackExt ext : PackExt.values()) {
				if (!PackExt.PACK.equals(ext)) {
					removeOldPack(packFile.create(ext), deleteOptions);
				}
			}
		} catch (IOException e) {
			// Deletion of the .pack file failed. Silently return.
		}
	}

	/**
	 * Like "git prune-packed" this method tries to prune all loose objects
	 * which can be found in packs. If certain objects can't be pruned (e.g.
	 * because the filesystem delete operation fails) this is silently ignored.
	 *
	 * @throws java.io.IOException
	 *             if an IO error occurred
	 */
	public void prunePacked() throws IOException {
		ObjectDirectory objdb = repo.getObjectDatabase();
		Collection<Pack> packs = objdb.getPacks();
		File objects = repo.getObjectsDirectory();
		String[] fanout = objects.list();

		if (fanout != null && fanout.length > 0) {
			pm.beginTask(JGitText.get().pruneLoosePackedObjects, fanout.length);
			try {
				for (String d : fanout) {
					checkCancelled();
					pm.update(1);
					if (d.length() != 2)
						continue;
					String[] entries = new File(objects, d).list();
					if (entries == null)
						continue;
					for (String e : entries) {
						checkCancelled();
						if (e.length() != Constants.OBJECT_ID_STRING_LENGTH - 2)
							continue;
						ObjectId id;
						try {
							id = ObjectId.fromString(d + e);
						} catch (IllegalArgumentException notAnObject) {
							// ignoring the file that does not represent loose
							// object
							continue;
						}
						boolean found = false;
						for (Pack p : packs) {
							checkCancelled();
							if (p.hasObject(id)) {
								found = true;
								break;
							}
						}
						if (found)
							FileUtils.delete(objdb.fileFor(id), FileUtils.RETRY
									| FileUtils.SKIP_MISSING
									| FileUtils.IGNORE_ERRORS);
					}
				}
			} finally {
				pm.endTask();
			}
		}
	}

	/**
	 * Like "git prune" this method tries to prune all loose objects which are
	 * unreferenced. If certain objects can't be pruned (e.g. because the
	 * filesystem delete operation fails) this is silently ignored.
	 *
	 * @param objectsToKeep
	 *            a set of objects which should explicitly not be pruned
	 * @throws java.io.IOException
	 *             if an IO error occurred
	 * @throws java.text.ParseException
	 *             If the configuration parameter "gc.pruneexpire" couldn't be
	 *             parsed
	 */
	public void prune(Set<ObjectId> objectsToKeep) throws IOException,
			ParseException {
		long expireDate = getExpireDate();

		// Collect all loose objects which are old enough, not referenced from
		// the index and not in objectsToKeep
		Map<ObjectId, File> deletionCandidates = new HashMap<>();
		Set<ObjectId> indexObjects = null;
		File objects = repo.getObjectsDirectory();
		String[] fanout = objects.list();
		if (fanout == null || fanout.length == 0) {
			return;
		}
		pm.beginTask(JGitText.get().pruneLooseUnreferencedObjects,
				fanout.length);
		try {
			for (String d : fanout) {
				checkCancelled();
				pm.update(1);
				if (d.length() != 2)
					continue;
				File dir = new File(objects, d);
				File[] entries = dir.listFiles();
				if (entries == null || entries.length == 0) {
					FileUtils.delete(dir, FileUtils.IGNORE_ERRORS);
					continue;
				}
				for (File f : entries) {
					checkCancelled();
					String fName = f.getName();
					if (fName.length() != Constants.OBJECT_ID_STRING_LENGTH - 2)
						continue;
					if (repo.getFS().lastModifiedInstant(f)
							.toEpochMilli() >= expireDate) {
						continue;
					}
					try {
						ObjectId id = ObjectId.fromString(d + fName);
						if (objectsToKeep.contains(id))
							continue;
						if (indexObjects == null)
							indexObjects = listNonHEADIndexObjects();
						if (indexObjects.contains(id))
							continue;
						deletionCandidates.put(id, f);
					} catch (IllegalArgumentException notAnObject) {
						// ignoring the file that does not represent loose
						// object
					}
				}
			}
		} finally {
			pm.endTask();
		}

		if (deletionCandidates.isEmpty()) {
			return;
		}

		checkCancelled();

		// From the set of current refs remove all those which have been handled
		// during last repack(). Only those refs will survive which have been
		// added or modified since the last repack. Only these can save existing
		// loose refs from being pruned.
		Collection<Ref> newRefs;
		if (lastPackedRefs == null || lastPackedRefs.isEmpty())
			newRefs = getAllRefs();
		else {
			Map<String, Ref> last = new HashMap<>();
			for (Ref r : lastPackedRefs) {
				last.put(r.getName(), r);
			}
			newRefs = new ArrayList<>();
			for (Ref r : getAllRefs()) {
				Ref old = last.get(r.getName());
				if (!equals(r, old)) {
					newRefs.add(r);
				}
			}
		}

		if (!newRefs.isEmpty()) {
			// There are new/modified refs! Check which loose objects are now
			// referenced by these modified refs (or their reflogentries).
			// Remove these loose objects
			// from the deletionCandidates. When the last candidate is removed
			// leave this method.
			ObjectWalk w = new ObjectWalk(repo);
			try {
				for (Ref cr : newRefs) {
					checkCancelled();
					w.markStart(w.parseAny(cr.getObjectId()));
				}
				if (lastPackedRefs != null)
					for (Ref lpr : lastPackedRefs) {
						w.markUninteresting(w.parseAny(lpr.getObjectId()));
					}
				removeReferenced(deletionCandidates, w);
			} finally {
				w.dispose();
			}
		}

		if (deletionCandidates.isEmpty())
			return;

		// Since we have not left the method yet there are still
		// deletionCandidates. Last chance for these objects not to be pruned is
		// that they are referenced by reflog entries. Even refs which currently
		// point to the same object as during last repack() may have
		// additional reflog entries not handled during last repack()
		ObjectWalk w = new ObjectWalk(repo);
		try {
			for (Ref ar : getAllRefs())
				for (ObjectId id : listRefLogObjects(ar, lastRepackTime)) {
					checkCancelled();
					w.markStart(w.parseAny(id));
				}
			if (lastPackedRefs != null)
				for (Ref lpr : lastPackedRefs) {
					checkCancelled();
					w.markUninteresting(w.parseAny(lpr.getObjectId()));
				}
			removeReferenced(deletionCandidates, w);
		} finally {
			w.dispose();
		}

		if (deletionCandidates.isEmpty())
			return;

		checkCancelled();

		// delete all candidates which have survived: these are unreferenced
		// loose objects. Make a last check, though, to avoid deleting objects
		// that could have been referenced while the candidates list was being
		// built (by an incoming push, for example).
		Set<File> touchedFanout = new HashSet<>();
		for (File f : deletionCandidates.values()) {
			if (f.lastModified() < expireDate) {
				f.delete();
				touchedFanout.add(f.getParentFile());
			}
		}

		for (File f : touchedFanout) {
			FileUtils.delete(f,
					FileUtils.EMPTY_DIRECTORIES_ONLY | FileUtils.IGNORE_ERRORS);
		}

		repo.getObjectDatabase().close();
	}

	private long getExpireDate() throws ParseException {
		long expireDate = Long.MAX_VALUE;

		if (expire == null && expireAgeMillis == -1) {
			String pruneExpireStr = getPruneExpireStr();
			if (pruneExpireStr == null)
				pruneExpireStr = PRUNE_EXPIRE_DEFAULT;
			expire = GitDateParser.parse(pruneExpireStr, null, SystemReader
					.getInstance().getLocale());
			expireAgeMillis = -1;
		}
		if (expire != null)
			expireDate = expire.getTime();
		if (expireAgeMillis != -1)
			expireDate = System.currentTimeMillis() - expireAgeMillis;
		return expireDate;
	}

	private String getPruneExpireStr() {
		return repo.getConfig().getString(
                        ConfigConstants.CONFIG_GC_SECTION, null,
                        ConfigConstants.CONFIG_KEY_PRUNEEXPIRE);
	}

	private long getPackExpireDate() throws ParseException {
		long packExpireDate = Long.MAX_VALUE;

		if (packExpire == null && packExpireAgeMillis == -1) {
			String prunePackExpireStr = repo.getConfig().getString(
					ConfigConstants.CONFIG_GC_SECTION, null,
					ConfigConstants.CONFIG_KEY_PRUNEPACKEXPIRE);
			if (prunePackExpireStr == null)
				prunePackExpireStr = PRUNE_PACK_EXPIRE_DEFAULT;
			packExpire = GitDateParser.parse(prunePackExpireStr, null,
					SystemReader.getInstance().getLocale());
			packExpireAgeMillis = -1;
		}
		if (packExpire != null)
			packExpireDate = packExpire.getTime();
		if (packExpireAgeMillis != -1)
			packExpireDate = System.currentTimeMillis() - packExpireAgeMillis;
		return packExpireDate;
	}

	/**
	 * Remove all entries from a map which key is the id of an object referenced
	 * by the given ObjectWalk
	 *
	 * @param id2File
	 *            mapping objectIds to files
	 * @param w
	 *            used to walk objects
	 * @throws MissingObjectException
	 *             if an object is missing
	 * @throws IncorrectObjectTypeException
	 *             if an object has an unexpected tyoe
	 * @throws IOException
	 *             if an IO error occurred
	 */
	private void removeReferenced(Map<ObjectId, File> id2File,
			ObjectWalk w) throws MissingObjectException,
			IncorrectObjectTypeException, IOException {
		RevObject ro = w.next();
		while (ro != null) {
			checkCancelled();
			if (id2File.remove(ro.getId()) != null && id2File.isEmpty()) {
				return;
			}
			ro = w.next();
		}
		ro = w.nextObject();
		while (ro != null) {
			checkCancelled();
			if (id2File.remove(ro.getId()) != null && id2File.isEmpty()) {
				return;
			}
			ro = w.nextObject();
		}
	}

	private static boolean equals(Ref r1, Ref r2) {
		if (r1 == null || r2 == null) {
			return false;
		}
		if (r1.isSymbolic()) {
			return r2.isSymbolic() && r1.getTarget().getName()
					.equals(r2.getTarget().getName());
		}
		return !r2.isSymbolic()
				&& Objects.equals(r1.getObjectId(), r2.getObjectId());
	}

	/**
	 * Pack ref storage. For a RefDirectory database, this packs all
	 * non-symbolic, loose refs into packed-refs. For Reftable, all of the data
	 * is compacted into a single table.
	 *
	 * @throws java.io.IOException
	 *             if an IO error occurred
	 */
	public void packRefs() throws IOException {
		RefDatabase refDb = repo.getRefDatabase();
		if (refDb instanceof FileReftableDatabase) {
			// TODO: abstract this more cleanly.
			pm.beginTask(JGitText.get().packRefs, 1);
			try {
				((FileReftableDatabase) refDb).compactFully();
			} finally {
				pm.endTask();
			}
			return;
		}

		Collection<Ref> refs = refDb.getRefsByPrefix(Constants.R_REFS);
		List<String> refsToBePacked = new ArrayList<>(refs.size());
		pm.beginTask(JGitText.get().packRefs, refs.size());
		try {
			for (Ref ref : refs) {
				checkCancelled();
				if (!ref.isSymbolic() && ref.getStorage().isLoose())
					refsToBePacked.add(ref.getName());
				pm.update(1);
			}
			((RefDirectory) repo.getRefDatabase()).pack(refsToBePacked);
		} finally {
			pm.endTask();
		}
	}

	/**
	 * Packs all objects which reachable from any of the heads into one pack
	 * file. Additionally all objects which are not reachable from any head but
	 * which are reachable from any of the other refs (e.g. tags), special refs
	 * (e.g. FETCH_HEAD) or index are packed into a separate pack file. Objects
	 * included in pack files which have a .keep file associated are never
	 * repacked. All old pack files which existed before are deleted.
	 *
	 * @return a collection of the newly created pack files
	 * @throws java.io.IOException
	 *             when during reading of refs, index, packfiles, objects,
	 *             reflog-entries or during writing to the packfiles
	 *             {@link java.io.IOException} occurs
	 */
	public Collection<Pack> repack() throws IOException {
		Collection<Pack> toBeDeleted = repo.getObjectDatabase().getPacks();

		long time = System.currentTimeMillis();
		Collection<Ref> refsBefore = getAllRefs();

		Set<ObjectId> allHeadsAndTags = new HashSet<>();
		Set<ObjectId> allHeads = new HashSet<>();
		Set<ObjectId> allTags = new HashSet<>();
		Set<ObjectId> nonHeads = new HashSet<>();
		Set<ObjectId> tagTargets = new HashSet<>();
		Set<ObjectId> indexObjects = listNonHEADIndexObjects();

		Set<ObjectId> refsToExcludeFromBitmap = repo.getRefDatabase()
				.getRefsByPrefix(pconfig.getBitmapExcludedRefsPrefixes())
				.stream().map(Ref::getObjectId).collect(Collectors.toSet());

		for (Ref ref : refsBefore) {
			checkCancelled();
			nonHeads.addAll(listRefLogObjects(ref, 0));
			if (ref.isSymbolic() || ref.getObjectId() == null) {
				continue;
			}
			if (isHead(ref)) {
				allHeads.add(ref.getObjectId());
			} else if (isTag(ref)) {
				allTags.add(ref.getObjectId());
			} else {
				nonHeads.add(ref.getObjectId());
			}
			if (ref.getPeeledObjectId() != null) {
				tagTargets.add(ref.getPeeledObjectId());
			}
		}

		List<ObjectIdSet> excluded = new LinkedList<>();
		for (Pack p : repo.getObjectDatabase().getPacks()) {
			checkCancelled();
			if (p.shouldBeKept())
				excluded.add(p.getIndex());
		}

		// Don't exclude tags that are also branch tips
		allTags.removeAll(allHeads);
		allHeadsAndTags.addAll(allHeads);
		allHeadsAndTags.addAll(allTags);

		// Hoist all branch tips and tags earlier in the pack file
		tagTargets.addAll(allHeadsAndTags);
		nonHeads.addAll(indexObjects);

		// Combine the GC_REST objects into the GC pack if requested
		if (pconfig.getSinglePack()) {
			allHeadsAndTags.addAll(nonHeads);
			nonHeads.clear();
		}

		List<Pack> ret = new ArrayList<>(2);
		Pack heads = null;
		if (!allHeadsAndTags.isEmpty()) {
			heads = writePack(allHeadsAndTags, PackWriter.NONE, allTags,
					refsToExcludeFromBitmap, tagTargets, excluded, true);
			if (heads != null) {
				ret.add(heads);
				excluded.add(0, heads.getIndex());
			}
		}
		if (!nonHeads.isEmpty()) {
			Pack rest = writePack(nonHeads, allHeadsAndTags, PackWriter.NONE,
					PackWriter.NONE, tagTargets, excluded, false);
			if (rest != null)
				ret.add(rest);
		}
		try {
			deleteOldPacks(toBeDeleted, ret);
		} catch (ParseException e) {
			// TODO: the exception has to be wrapped into an IOException because
			// throwing the ParseException directly would break the API, instead
			// we should throw a ConfigInvalidException
			throw new IOException(e);
		}
		prunePacked();
		if (repo.getRefDatabase() instanceof RefDirectory) {
			// TODO: abstract this more cleanly.
			deleteEmptyRefsFolders();
		}
		deleteOrphans();
		deleteTempPacksIdx();

		lastPackedRefs = refsBefore;
		lastRepackTime = time;
		return ret;
	}

	private Set<ObjectId> refsToObjectIds(Collection<Ref> refs)
			throws IOException {
		Set<ObjectId> objectIds = new HashSet<>();
		for (Ref ref : refs) {
			checkCancelled();
			if (ref.getPeeledObjectId() != null) {
				objectIds.add(ref.getPeeledObjectId());
				continue;
			}

			if (ref.getObjectId() != null) {
				objectIds.add(ref.getObjectId());
			}
		}
		return objectIds;
	}

	/**
	 * Generate a new commit-graph file when 'core.commitGraph' is true.
	 *
	 * @param wants
	 *            the list of wanted objects, writer walks commits starting at
	 *            these. Must not be {@code null}.
	 * @throws IOException
	 *             if an IO error occurred
	 */
	void writeCommitGraph(@NonNull Set<? extends ObjectId> wants)
			throws IOException {
		if (!repo.getConfig().get(CoreConfig.KEY).enableCommitGraph()) {
			return;
		}
		if (repo.getObjectDatabase().getShallowCommits().size() > 0) {
			return;
		}
		checkCancelled();
		if (wants.isEmpty()) {
			return;
		}
		File tmpFile = null;
		try (RevWalk walk = new RevWalk(repo)) {
			CommitGraphWriter writer = new CommitGraphWriter(
					GraphCommits.fromWalk(pm, wants, walk),
					shouldWriteBloomFilter());
			tmpFile = File.createTempFile("commit_", //$NON-NLS-1$
					COMMIT_GRAPH.getTmpExtension(),
					repo.getObjectDatabase().getInfoDirectory());
			// write the commit-graph file
			try (FileOutputStream fos = new FileOutputStream(tmpFile);
					FileChannel channel = fos.getChannel();
					OutputStream channelStream = Channels
							.newOutputStream(channel)) {
				writer.write(pm, channelStream);
				channel.force(true);
			}

			// rename the temporary file to real file
			File realFile = new File(repo.getObjectsDirectory(),
					Constants.INFO_COMMIT_GRAPH);
			FileUtils.rename(tmpFile, realFile, StandardCopyOption.ATOMIC_MOVE);
		} finally {
			if (tmpFile != null && tmpFile.exists()) {
				tmpFile.delete();
			}
		}
		deleteTempCommitGraph();
	}

	private void deleteTempCommitGraph() {
		Path objectsDir = repo.getObjectDatabase().getInfoDirectory().toPath();
		Instant threshold = Instant.now().minus(1, ChronoUnit.DAYS);
		if (!Files.exists(objectsDir)) {
			return;
		}
		try (DirectoryStream<Path> stream = Files.newDirectoryStream(objectsDir,
				"commit_*_tmp")) { //$NON-NLS-1$
			stream.forEach(t -> {
				try {
					Instant lastModified = Files.getLastModifiedTime(t)
							.toInstant();
					if (lastModified.isBefore(threshold)) {
						Files.deleteIfExists(t);
					}
				} catch (IOException e) {
					LOG.error(e.getMessage(), e);
				}
			});
		} catch (IOException e) {
			LOG.error(e.getMessage(), e);
		}
	}

	/**
	 * If {@code true}, will rewrite the commit-graph file when gc is run.
	 *
	 * @return true if commit-graph should be written. Default is {@code false}.
	 */
	boolean shouldWriteCommitGraphWhenGc() {
		return repo.getConfig().getBoolean(ConfigConstants.CONFIG_GC_SECTION,
				ConfigConstants.CONFIG_KEY_WRITE_COMMIT_GRAPH,
				DEFAULT_WRITE_COMMIT_GRAPH);
	}

	/**
	 * If {@code true}, generates bloom filter in the commit-graph file.
	 *
	 * @return true if bloom filter should be written. Default is {@code false}.
	 */
	boolean shouldWriteBloomFilter() {
		return repo.getConfig().getBoolean(ConfigConstants.CONFIG_GC_SECTION,
				ConfigConstants.CONFIG_KEY_WRITE_CHANGED_PATHS,
				DEFAULT_WRITE_BLOOM_FILTER);
	}

	private static boolean isHead(Ref ref) {
		return ref.getName().startsWith(Constants.R_HEADS);
	}

	private static boolean isTag(Ref ref) {
		return ref.getName().startsWith(Constants.R_TAGS);
	}

	private void deleteEmptyRefsFolders() throws IOException {
		Path refs = repo.getDirectory().toPath().resolve(Constants.R_REFS);
		// Avoid deleting a folder that was created after the threshold so that concurrent
		// operations trying to create a reference are not impacted
		Instant threshold = Instant.now().minus(30, ChronoUnit.SECONDS);
		try (Stream<Path> entries = Files.list(refs)
				.filter(Files::isDirectory)) {
			Iterator<Path> iterator = entries.iterator();
			while (iterator.hasNext()) {
				try (Stream<Path> s = Files.list(iterator.next())) {
					s.filter(path -> canBeSafelyDeleted(path, threshold)).forEach(this::deleteDir);
				}
			}
		}
	}

	private boolean canBeSafelyDeleted(Path path, Instant threshold) {
		try {
			return Files.getLastModifiedTime(path).toInstant().isBefore(threshold);
		}
		catch (IOException e) {
			LOG.warn(MessageFormat.format(
					JGitText.get().cannotAccessLastModifiedForSafeDeletion,
					path), e);
			return false;
		}
	}

	private void deleteDir(Path dir) {
		try (Stream<Path> dirs = Files.walk(dir)) {
			dirs.filter(this::isDirectory).sorted(Comparator.reverseOrder())
					.forEach(this::delete);
		} catch (IOException e) {
			LOG.error(e.getMessage(), e);
		}
	}

	private boolean isDirectory(Path p) {
		return p.toFile().isDirectory();
	}

	private void delete(Path d) {
		try {
			Files.delete(d);
		} catch (DirectoryNotEmptyException e) {
			// Don't log
		} catch (IOException e) {
			LOG.error(MessageFormat.format(JGitText.get().cannotDeleteFile, d),
					e);
		}
	}

	private static Optional<PackFile> toPackFileWithValidExt(
			Path packFilePath) {
		try {
			PackFile packFile = new PackFile(packFilePath.toFile());
			if (packFile.getPackExt() == null) {
				return Optional.empty();
			}
			return Optional.of(packFile);
		} catch (IllegalArgumentException e) {
			return Optional.empty();
		}
	}

	/**
	 * Deletes orphans
	 * <p>
	 * A file is considered an orphan if it is some type of index file, but
	 * there is not a corresponding pack or keep file present in the directory.
	 * </p>
	 */
	private void deleteOrphans() {
		Path packDir = repo.getObjectDatabase().getPackDirectory().toPath();
		List<PackFile> childFiles;
		Set<String> seenParentIds = new HashSet<>();
		try (Stream<Path> files = Files.list(packDir)) {
			childFiles = files.map(GC::toPackFileWithValidExt)
					.filter(Optional::isPresent).map(Optional::get)
					.filter(packFile -> {
						PackExt ext = packFile.getPackExt();
						if (PARENT_EXTS.contains(ext)) {
							seenParentIds.add(packFile.getId());
							return false;
						}
						return CHILD_EXTS.contains(ext);
					}).collect(Collectors.toList());
		} catch (IOException e) {
			LOG.error(e.getMessage(), e);
			return;
		}

		for (PackFile child : childFiles) {
			if (!seenParentIds.contains(child.getId())) {
				try {
					FileUtils.delete(child,
							FileUtils.RETRY | FileUtils.SKIP_MISSING);
					LOG.warn(JGitText.get().deletedOrphanInPackDir, child);
				} catch (IOException e) {
					LOG.error(e.getMessage(), e);
				}
			}
		}
	}

	private void deleteTempPacksIdx() {
		Path packDir = repo.getObjectDatabase().getPackDirectory().toPath();
		Instant threshold = Instant.now().minus(1, ChronoUnit.DAYS);
		if (!Files.exists(packDir)) {
			return;
		}
		try (DirectoryStream<Path> stream =
				Files.newDirectoryStream(packDir, "gc_*_tmp")) { //$NON-NLS-1$
			stream.forEach(t -> {
				try {
					Instant lastModified = Files.getLastModifiedTime(t)
							.toInstant();
					if (lastModified.isBefore(threshold)) {
						Files.deleteIfExists(t);
					}
				} catch (IOException e) {
					LOG.error(e.getMessage(), e);
				}
			});
		} catch (IOException e) {
			LOG.error(e.getMessage(), e);
		}
	}

	/**
	 * @param ref
	 *            the ref which log should be inspected
	 * @param minTime
	 *            only reflog entries not older then this time are processed
	 * @return the {@link ObjectId}s contained in the reflog
	 * @throws IOException
	 *             if an IO error occurred
	 */
	private Set<ObjectId> listRefLogObjects(Ref ref, long minTime) throws IOException {
		ReflogReader reflogReader = repo.getReflogReader(ref);
		List<ReflogEntry> rlEntries = reflogReader
				.getReverseEntries();
		if (rlEntries == null || rlEntries.isEmpty())
			return Collections.emptySet();
		Set<ObjectId> ret = new HashSet<>();
		for (ReflogEntry e : rlEntries) {
			if (e.getWho().getWhen().getTime() < minTime)
				break;
			ObjectId newId = e.getNewId();
			if (newId != null && !ObjectId.zeroId().equals(newId))
				ret.add(newId);
			ObjectId oldId = e.getOldId();
			if (oldId != null && !ObjectId.zeroId().equals(oldId))
				ret.add(oldId);
		}
		return ret;
	}

	/**
	 * Returns a collection of all refs and additional refs.
	 *
	 * Additional refs which don't start with "refs/" are not returned because
	 * they should not save objects from being garbage collected. Examples for
	 * such references are ORIG_HEAD, MERGE_HEAD, FETCH_HEAD and
	 * CHERRY_PICK_HEAD.
	 *
	 * @return a collection of refs pointing to live objects.
	 * @throws IOException
	 *             if an IO error occurred
	 */
	private Collection<Ref> getAllRefs() throws IOException {
		RefDatabase refdb = repo.getRefDatabase();
		Collection<Ref> refs = refdb.getRefs();
		List<Ref> addl = refdb.getAdditionalRefs();
		if (!addl.isEmpty()) {
			List<Ref> all = new ArrayList<>(refs.size() + addl.size());
			all.addAll(refs);
			// add additional refs which start with refs/
			for (Ref r : addl) {
				checkCancelled();
				if (r.getName().startsWith(Constants.R_REFS)) {
					all.add(r);
				}
			}
			return all;
		}
		return refs;
	}

	/**
	 * Return a list of those objects in the index which differ from whats in
	 * HEAD
	 *
	 * @return a set of ObjectIds of changed objects in the index
	 * @throws IOException
	 *             if an IO error occurred
	 * @throws CorruptObjectException
	 *             if an object is corrupt
	 * @throws NoWorkTreeException
	 *             if the repository has no working directory
	 */
	private Set<ObjectId> listNonHEADIndexObjects()
			throws CorruptObjectException, IOException {
		if (repo.isBare()) {
			return Collections.emptySet();
		}
		try (TreeWalk treeWalk = new TreeWalk(repo)) {
			treeWalk.addTree(new DirCacheIterator(repo.readDirCache()));
			ObjectId headID = repo.resolve(Constants.HEAD);
			if (headID != null) {
				try (RevWalk revWalk = new RevWalk(repo)) {
					treeWalk.addTree(revWalk.parseTree(headID));
				}
			}

			treeWalk.setFilter(TreeFilter.ANY_DIFF);
			treeWalk.setRecursive(true);
			Set<ObjectId> ret = new HashSet<>();

			while (treeWalk.next()) {
				checkCancelled();
				ObjectId objectId = treeWalk.getObjectId(0);
				switch (treeWalk.getRawMode(0) & FileMode.TYPE_MASK) {
				case FileMode.TYPE_MISSING:
				case FileMode.TYPE_GITLINK:
					continue;
				case FileMode.TYPE_TREE:
				case FileMode.TYPE_FILE:
				case FileMode.TYPE_SYMLINK:
					ret.add(objectId);
					continue;
				default:
					throw new IOException(MessageFormat.format(
							JGitText.get().corruptObjectInvalidMode3,
							String.format("%o", //$NON-NLS-1$
									Integer.valueOf(treeWalk.getRawMode(0))),
							(objectId == null) ? "null" : objectId.name(), //$NON-NLS-1$
							treeWalk.getPathString(), //
							repo.getIndexFile()));
				}
			}
			return ret;
		}
	}

	private Pack writePack(@NonNull Set<? extends ObjectId> want,
			@NonNull Set<? extends ObjectId> have, @NonNull Set<ObjectId> tags,
			@NonNull Set<ObjectId> excludedRefsTips,
			Set<ObjectId> tagTargets, List<ObjectIdSet> excludeObjects, boolean createBitmap)
			throws IOException {
		checkCancelled();
		File tmpPack = null;
		Map<PackExt, File> tmpExts = new TreeMap<>((o1, o2) -> {
			// INDEX entries must be returned last, so the pack
			// scanner does pick up the new pack until all the
			// PackExt entries have been written.
			if (o1 == o2) {
				return 0;
			}
			if (o1 == PackExt.INDEX) {
				return 1;
			}
			if (o2 == PackExt.INDEX) {
				return -1;
			}
			return Integer.signum(o1.hashCode() - o2.hashCode());
		});
		try (PackWriter pw = new PackWriter(
				pconfig,
				repo.newObjectReader())) {
			// prepare the PackWriter
			pw.setDeltaBaseAsOffset(true);
			pw.setReuseDeltaCommits(false);
			if (tagTargets != null) {
				pw.setTagTargets(tagTargets);
			}
			if (excludeObjects != null)
				for (ObjectIdSet idx : excludeObjects)
					pw.excludeObjects(idx);
			pw.setCreateBitmaps(createBitmap);
			pw.preparePack(pm, want, have, PackWriter.NONE,
					union(tags, excludedRefsTips));
			if (pw.getObjectCount() == 0)
				return null;
			checkCancelled();

			// create temporary files
			ObjectId id = pw.computeName();
			File packdir = repo.getObjectDatabase().getPackDirectory();
			packdir.mkdirs();
			tmpPack = File.createTempFile("gc_", //$NON-NLS-1$
					PACK.getTmpExtension(), packdir);
			String tmpBase = tmpPack.getName()
					.substring(0, tmpPack.getName().lastIndexOf('.'));
			File tmpIdx = new File(packdir, tmpBase + INDEX.getTmpExtension());
			tmpExts.put(INDEX, tmpIdx);

			if (!tmpIdx.createNewFile())
				throw new IOException(MessageFormat.format(
						JGitText.get().cannotCreateIndexfile, tmpIdx.getPath()));

			// write the packfile
			try (FileOutputStream fos = new FileOutputStream(tmpPack);
					FileChannel channel = fos.getChannel();
					OutputStream channelStream = Channels
							.newOutputStream(channel)) {
				pw.writePack(pm, pm, channelStream);
				channel.force(true);
			}

			// write the packindex
			try (FileOutputStream fos = new FileOutputStream(tmpIdx);
					FileChannel idxChannel = fos.getChannel();
					OutputStream idxStream = Channels
							.newOutputStream(idxChannel)) {
				pw.writeIndex(idxStream);
				idxChannel.force(true);
			}

			if (pw.isReverseIndexEnabled()) {
				File tmpReverseIndexFile = new File(packdir,
						tmpBase + REVERSE_INDEX.getTmpExtension());
				tmpExts.put(REVERSE_INDEX, tmpReverseIndexFile);
				if (!tmpReverseIndexFile.createNewFile()) {
					throw new IOException(MessageFormat.format(
							JGitText.get().cannotCreateIndexfile,
							tmpReverseIndexFile.getPath()));
				}
				try (FileOutputStream fos = new FileOutputStream(
						tmpReverseIndexFile);
						FileChannel channel = fos.getChannel();
						OutputStream stream = Channels
								.newOutputStream(channel)) {
					pw.writeReverseIndex(stream);
					channel.force(true);
				}
			}

			if (pw.prepareBitmapIndex(pm)) {
				File tmpBitmapIdx = new File(packdir,
						tmpBase + BITMAP_INDEX.getTmpExtension());
				tmpExts.put(BITMAP_INDEX, tmpBitmapIdx);

				if (!tmpBitmapIdx.createNewFile())
					throw new IOException(MessageFormat.format(
							JGitText.get().cannotCreateIndexfile,
							tmpBitmapIdx.getPath()));

				try (FileOutputStream fos = new FileOutputStream(tmpBitmapIdx);
						FileChannel idxChannel = fos.getChannel();
						OutputStream idxStream = Channels
								.newOutputStream(idxChannel)) {
					pw.writeBitmapIndex(idxStream);
					idxChannel.force(true);
				}
			}

			// rename the temporary files to real files
			File packDir = repo.getObjectDatabase().getPackDirectory();
			PackFile realPack = new PackFile(packDir, id, PackExt.PACK);

			repo.getObjectDatabase().closeAllPackHandles(realPack);
			tmpPack.setReadOnly();

			FileUtils.rename(tmpPack, realPack, StandardCopyOption.ATOMIC_MOVE);
			for (Map.Entry<PackExt, File> tmpEntry : tmpExts.entrySet()) {
				File tmpExt = tmpEntry.getValue();
				tmpExt.setReadOnly();

				PackFile realExt = new PackFile(packDir, id, tmpEntry.getKey());
				try {
					FileUtils.rename(tmpExt, realExt,
							StandardCopyOption.ATOMIC_MOVE);
				} catch (IOException e) {
					File newExt = new File(realExt.getParentFile(),
							realExt.getName() + ".new"); //$NON-NLS-1$
					try {
						FileUtils.rename(tmpExt, newExt,
								StandardCopyOption.ATOMIC_MOVE);
					} catch (IOException e2) {
						newExt = tmpExt;
						e = e2;
					}
					throw new IOException(MessageFormat.format(
							JGitText.get().panicCantRenameIndexFile, newExt,
							realExt), e);
				}
			}
			boolean interrupted = false;
			try {
				FileSnapshot snapshot = FileSnapshot.save(realPack);
				if (pconfig.doWaitPreventRacyPack(snapshot.size())) {
					snapshot.waitUntilNotRacy();
				}
			} catch (InterruptedException e) {
				interrupted = true;
			}
			try {
				return repo.getObjectDatabase().openPack(realPack);
			} finally {
				if (interrupted) {
					// Re-set interrupted flag
					Thread.currentThread().interrupt();
				}
			}
		} finally {
			if (tmpPack != null && tmpPack.exists())
				tmpPack.delete();
			for (File tmpExt : tmpExts.values()) {
				if (tmpExt.exists())
					tmpExt.delete();
			}
		}
	}

	private Set<? extends ObjectId> union(Set<ObjectId> tags,
			Set<ObjectId> excludedRefsHeadsTips) {
		HashSet<ObjectId> unionSet = new HashSet<>(
				tags.size() + excludedRefsHeadsTips.size());
		unionSet.addAll(tags);
		unionSet.addAll(excludedRefsHeadsTips);
		return unionSet;
	}

	private void checkCancelled() throws CancelledException {
		if (pm.isCancelled() || Thread.currentThread().isInterrupted()) {
			throw new CancelledException(JGitText.get().operationCanceled);
		}
	}

	/**
	 * A class holding statistical data for a FileRepository regarding how many
	 * objects are stored as loose or packed objects
	 */
	public static class RepoStatistics {
		/**
		 * The number of objects stored in pack files. If the same object is
		 * stored in multiple pack files then it is counted as often as it
		 * occurs in pack files.
		 */
		public long numberOfPackedObjects;

		/**
		 * The number of pack files
		 */
		public long numberOfPackFiles;

		/**
		 * The number of objects stored as loose objects.
		 */
		public long numberOfLooseObjects;

		/**
		 * The sum of the sizes of all files used to persist loose objects.
		 */
		public long sizeOfLooseObjects;

		/**
		 * The sum of the sizes of all pack files.
		 */
		public long sizeOfPackedObjects;

		/**
		 * The number of loose refs.
		 */
		public long numberOfLooseRefs;

		/**
		 * The number of refs stored in pack files.
		 */
		public long numberOfPackedRefs;

		/**
		 * The number of bitmaps in the bitmap indices.
		 */
		public long numberOfBitmaps;

		@Override
		public String toString() {
			final StringBuilder b = new StringBuilder();
			b.append("numberOfPackedObjects=").append(numberOfPackedObjects); //$NON-NLS-1$
			b.append(", numberOfPackFiles=").append(numberOfPackFiles); //$NON-NLS-1$
			b.append(", numberOfLooseObjects=").append(numberOfLooseObjects); //$NON-NLS-1$
			b.append(", numberOfLooseRefs=").append(numberOfLooseRefs); //$NON-NLS-1$
			b.append(", numberOfPackedRefs=").append(numberOfPackedRefs); //$NON-NLS-1$
			b.append(", sizeOfLooseObjects=").append(sizeOfLooseObjects); //$NON-NLS-1$
			b.append(", sizeOfPackedObjects=").append(sizeOfPackedObjects); //$NON-NLS-1$
			b.append(", numberOfBitmaps=").append(numberOfBitmaps); //$NON-NLS-1$
			return b.toString();
		}
	}

	/**
	 * Returns information about objects and pack files for a FileRepository.
	 *
	 * @return information about objects and pack files for a FileRepository
	 * @throws java.io.IOException
	 *             if an IO error occurred
	 */
	public RepoStatistics getStatistics() throws IOException {
		RepoStatistics ret = new RepoStatistics();
		Collection<Pack> packs = repo.getObjectDatabase().getPacks();
		for (Pack p : packs) {
			ret.numberOfPackedObjects += p.getIndex().getObjectCount();
			ret.numberOfPackFiles++;
			ret.sizeOfPackedObjects += p.getPackFile().length();
			if (p.getBitmapIndex() != null)
				ret.numberOfBitmaps += p.getBitmapIndex().getBitmapCount();
		}
		File objDir = repo.getObjectsDirectory();
		String[] fanout = objDir.list();
		if (fanout != null && fanout.length > 0) {
			for (String d : fanout) {
				if (d.length() != 2)
					continue;
				File[] entries = new File(objDir, d).listFiles();
				if (entries == null)
					continue;
				for (File f : entries) {
					if (f.getName().length() != Constants.OBJECT_ID_STRING_LENGTH - 2)
						continue;
					ret.numberOfLooseObjects++;
					ret.sizeOfLooseObjects += f.length();
				}
			}
		}

		RefDatabase refDb = repo.getRefDatabase();
		for (Ref r : refDb.getRefs()) {
			Storage storage = r.getStorage();
			if (storage == Storage.LOOSE || storage == Storage.LOOSE_PACKED)
				ret.numberOfLooseRefs++;
			if (storage == Storage.PACKED || storage == Storage.LOOSE_PACKED)
				ret.numberOfPackedRefs++;
		}

		return ret;
	}

	/**
	 * Set the progress monitor used for garbage collection methods.
	 *
	 * @param pm a {@link org.eclipse.jgit.lib.ProgressMonitor} object.
	 * @return this
	 */
	public GC setProgressMonitor(ProgressMonitor pm) {
		this.pm = (pm == null) ? NullProgressMonitor.INSTANCE : pm;
		return this;
	}

	/**
	 * During gc() or prune() each unreferenced, loose object which has been
	 * created or modified in the last <code>expireAgeMillis</code> milliseconds
	 * will not be pruned. Only older objects may be pruned. If set to 0 then
	 * every object is a candidate for pruning.
	 *
	 * @param expireAgeMillis
	 *            minimal age of objects to be pruned in milliseconds.
	 */
	public void setExpireAgeMillis(long expireAgeMillis) {
		this.expireAgeMillis = expireAgeMillis;
		expire = null;
	}

	/**
	 * During gc() or prune() packfiles which are created or modified in the
	 * last <code>packExpireAgeMillis</code> milliseconds will not be deleted.
	 * Only older packfiles may be deleted. If set to 0 then every packfile is a
	 * candidate for deletion.
	 *
	 * @param packExpireAgeMillis
	 *            minimal age of packfiles to be deleted in milliseconds.
	 */
	public void setPackExpireAgeMillis(long packExpireAgeMillis) {
		this.packExpireAgeMillis = packExpireAgeMillis;
		expire = null;
	}

	/**
	 * Set the PackConfig used when (re-)writing packfiles. This allows to
	 * influence how packs are written and to implement something similar to
	 * "git gc --aggressive"
	 *
	 * @param pconfig
	 *            the {@link org.eclipse.jgit.storage.pack.PackConfig} used when
	 *            writing packs
	 */
	public void setPackConfig(@NonNull PackConfig pconfig) {
		this.pconfig = pconfig;
	}

	/**
	 * During gc() or prune() each unreferenced, loose object which has been
	 * created or modified after or at <code>expire</code> will not be pruned.
	 * Only older objects may be pruned. If set to null then every object is a
	 * candidate for pruning.
	 *
	 * @param expire
	 *            instant in time which defines object expiration
	 *            objects with modification time before this instant are expired
	 *            objects with modification time newer or equal to this instant
	 *            are not expired
	 */
	public void setExpire(Date expire) {
		this.expire = expire;
		expireAgeMillis = -1;
	}

	/**
	 * During gc() or prune() packfiles which are created or modified after or
	 * at <code>packExpire</code> will not be deleted. Only older packfiles may
	 * be deleted. If set to null then every packfile is a candidate for
	 * deletion.
	 *
	 * @param packExpire
	 *            instant in time which defines packfile expiration
	 */
	public void setPackExpire(Date packExpire) {
		this.packExpire = packExpire;
		packExpireAgeMillis = -1;
	}

	/**
	 * Set the {@code gc --auto} option.
	 *
	 * With this option, gc checks whether any housekeeping is required; if not,
	 * it exits without performing any work. Some JGit commands run
	 * {@code gc --auto} after performing operations that could create many
	 * loose objects.
	 * <p>
	 * Housekeeping is required if there are too many loose objects or too many
	 * packs in the repository. If the number of loose objects exceeds the value
	 * of the gc.auto option JGit GC consolidates all existing packs into a
	 * single pack (equivalent to {@code -A} option), whereas git-core would
	 * combine all loose objects into a single pack using {@code repack -d -l}.
	 * Setting the value of {@code gc.auto} to 0 disables automatic packing of
	 * loose objects.
	 * <p>
	 * If the number of packs exceeds the value of {@code gc.autoPackLimit},
	 * then existing packs (except those marked with a .keep file) are
	 * consolidated into a single pack by using the {@code -A} option of repack.
	 * Setting {@code gc.autoPackLimit} to 0 disables automatic consolidation of
	 * packs.
	 * <p>
	 * Like git the following jgit commands run auto gc:
	 * <ul>
	 * <li>fetch</li>
	 * <li>merge</li>
	 * <li>rebase</li>
	 * <li>receive-pack</li>
	 * </ul>
	 * The auto gc for receive-pack can be suppressed by setting the config
	 * option {@code receive.autogc = false}
	 *
	 * @param auto
	 *            defines whether gc should do automatic housekeeping
	 */
	public void setAuto(boolean auto) {
		this.automatic = auto;
	}

	/**
	 * @param background
	 *            whether to run the gc in a background thread.
	 */
	void setBackground(boolean background) {
		this.background = background;
	}

	private boolean needGc() {
		if (tooManyPacks()) {
			addRepackAllOption();
		} else {
			return tooManyLooseObjects();
		}
		// TODO run pre-auto-gc hook, if it fails return false
		return true;
	}

	private void addRepackAllOption() {
		// TODO: if JGit GC is enhanced to support repack's option -l this
		// method needs to be implemented
	}

	/**
	 * @return {@code true} if number of packs &gt; gc.autopacklimit (default
	 *         50)
	 */
	boolean tooManyPacks() {
		int autopacklimit = repo.getConfig().getInt(
				ConfigConstants.CONFIG_GC_SECTION,
				ConfigConstants.CONFIG_KEY_AUTOPACKLIMIT,
				DEFAULT_AUTOPACKLIMIT);
		if (autopacklimit <= 0) {
			return false;
		}
		// JGit always creates two packfiles, one for the objects reachable from
		// branches, and another one for the rest
		return repo.getObjectDatabase().getPacks().size() > (autopacklimit + 1);
	}

	/**
	 * Quickly estimate number of loose objects, SHA1 is distributed evenly so
	 * counting objects in one directory (bucket 17) is sufficient
	 *
	 * @return {@code true} if number of loose objects &gt; gc.auto (default
	 *         6700)
	 */
	boolean tooManyLooseObjects() {
		int auto = getLooseObjectLimit();
		if (auto <= 0) {
			return false;
		}
		int n = 0;
		int threshold = (auto + 255) / 256;
		Path dir = repo.getObjectsDirectory().toPath().resolve("17"); //$NON-NLS-1$
		if (!dir.toFile().exists()) {
			return false;
		}
		try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, file -> {
					Path fileName = file.getFileName();
					return file.toFile().isFile() && fileName != null
							&& PATTERN_LOOSE_OBJECT.matcher(fileName.toString())
									.matches();
				})) {
			for (Iterator<Path> iter = stream.iterator(); iter.hasNext(); iter
					.next()) {
				if (++n > threshold) {
					return true;
				}
			}
		} catch (IOException e) {
			LOG.error(e.getMessage(), e);
		}
		return false;
	}

	private int getLooseObjectLimit() {
		return repo.getConfig().getInt(ConfigConstants.CONFIG_GC_SECTION,
				ConfigConstants.CONFIG_KEY_AUTO, DEFAULT_AUTOLIMIT);
	}

	private class PidLock implements AutoCloseable {

		private static final String GC_PID = "gc.pid"; //$NON-NLS-1$

		private final Path pidFile;

		private LockToken token;

		private FileLock lock;

		private RandomAccessFile f;

		private FileChannel channel;

		private Thread cleanupHook;

		PidLock() {
			pidFile = repo.getDirectory().toPath().resolve(GC_PID);
		}

		boolean lock() throws IOException {
			if (Files.exists(pidFile)) {
				Instant mtime = FS.DETECTED
						.lastModifiedInstant(pidFile.toFile());
				Instant twelveHoursAgo = Instant.now().minus(12,
						ChronoUnit.HOURS);
				if (mtime.compareTo(twelveHoursAgo) > 0) {
					gcAlreadyRunning();
					return false;
				}
				LOG.warn(MessageFormat.format(JGitText.get().stalePidLock,
						pidFile, mtime));
			}
			try {
				token = FS.DETECTED.createNewFileAtomic(pidFile.toFile());
				f = new RandomAccessFile(pidFile.toFile(), "rw"); //$NON-NLS-1$
				channel = f.getChannel();
				lock = channel.tryLock();
				if (lock == null || !lock.isValid()) {
					gcAlreadyRunning();
					return false;
				}
				channel.write(ByteBuffer
						.wrap(getProcDesc().getBytes(StandardCharsets.UTF_8)));
				try {
					Runtime.getRuntime().addShutdownHook(
							cleanupHook = new Thread(() -> close()));
				} catch (IllegalStateException e) {
					// ignore - the VM is already shutting down
				}
			} catch (IOException | OverlappingFileLockException e) {
				try {
					failedToLock();
				} catch (Exception e1) {
					LOG.error(
							MessageFormat.format(
									JGitText.get().closePidLockFailed, pidFile),
							e1);
				}
				throw e;
			}
			return true;
		}

		private void failedToLock() {
			close();
			LOG.error(MessageFormat.format(JGitText.get().failedPidLock,
					pidFile));
		}

		private void gcAlreadyRunning() {
			close();
			Optional<String> s;
			try (Stream<String> lines = Files.lines(pidFile)) {
				s = lines.findFirst();
				String machine = null;
				String pid = null;
				if (s.isPresent()) {
					String[] c = s.get().split("\\s+"); //$NON-NLS-1$
					pid = c[0];
					machine = c[1];
				}
				if (!StringUtils.isEmptyOrNull(machine)
						&& !StringUtils.isEmptyOrNull(pid)) {
					LOG.error(MessageFormat.format(
							JGitText.get().gcAlreadyRunning, machine, pid));
					return;
				}
			} catch (IOException e) {
				// ignore
			}
			LOG.error(MessageFormat.format(JGitText.get().failedPidLock,
					pidFile));
		}

		private String getProcDesc() {
			StringBuffer s = new StringBuffer(Long.toString(getPID()));
			s.append(' ');
			s.append(getHostName());
			return s.toString();
		}

		private long getPID() {
			return ProcessHandle.current().pid();
		}

		private String getHostName() {
			try {
				return InetAddress.getLocalHost().getHostName();
			} catch (UnknownHostException e) {
				return ""; //$NON-NLS-1$
			}
		}

		@Override
		public void close() {
			boolean wasLocked = false;
			try {
				if (cleanupHook != null) {
					try {
						Runtime.getRuntime().removeShutdownHook(cleanupHook);
					} catch (IllegalStateException e) {
						// ignore - the VM is already shutting down
					}
				}
				if (lock != null && lock.isValid()) {
					lock.release();
					wasLocked = true;
				}
				if (channel != null) {
					channel.close();
				}
				if (f != null) {
					f.close();
				}
				if (token != null) {
					token.close();
				}
				if (wasLocked) {
					FileUtils.delete(pidFile.toFile(), FileUtils.RETRY);
				}
			} catch (IOException e) {
				LOG.error(MessageFormat
						.format(JGitText.get().closePidLockFailed, pidFile), e);
			}
		}

	}
}
