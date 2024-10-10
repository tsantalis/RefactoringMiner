/*
 * Copyright (C) 2008-2010, Google Inc.
 * Copyright (C) 2008, Marek Zawirski <marek.zawirski@gmail.com>
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

package org.eclipse.jgit.internal.storage.pack;

import static org.eclipse.jgit.internal.storage.pack.StoredObjectRepresentation.PACK_DELTA;
import static org.eclipse.jgit.internal.storage.pack.StoredObjectRepresentation.PACK_WHOLE;
import static org.eclipse.jgit.lib.Constants.OBJECT_ID_LENGTH;
import static org.eclipse.jgit.lib.Constants.OBJ_BLOB;
import static org.eclipse.jgit.lib.Constants.OBJ_COMMIT;
import static org.eclipse.jgit.lib.Constants.OBJ_TAG;
import static org.eclipse.jgit.lib.Constants.OBJ_TREE;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.security.MessageDigest;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.zip.CRC32;
import java.util.zip.CheckedOutputStream;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;

import org.eclipse.jgit.errors.CorruptObjectException;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.LargeObjectException;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.errors.StoredObjectRepresentationNotAvailableException;
import org.eclipse.jgit.internal.JGitText;
import org.eclipse.jgit.internal.storage.file.PackBitmapIndexBuilder;
import org.eclipse.jgit.internal.storage.file.PackBitmapIndexWriterV1;
import org.eclipse.jgit.internal.storage.file.PackIndexWriter;
import org.eclipse.jgit.lib.AnyObjectId;
import org.eclipse.jgit.lib.AsyncObjectSizeQueue;
import org.eclipse.jgit.lib.BatchingProgressMonitor;
import org.eclipse.jgit.lib.BitmapIndex;
import org.eclipse.jgit.lib.BitmapIndex.BitmapBuilder;
import org.eclipse.jgit.lib.BitmapObject;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.NullProgressMonitor;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectIdOwnerMap;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.ProgressMonitor;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.ThreadSafeProgressMonitor;
import org.eclipse.jgit.revwalk.AsyncRevObjectQueue;
import org.eclipse.jgit.revwalk.DepthWalk;
import org.eclipse.jgit.revwalk.ObjectWalk;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevFlag;
import org.eclipse.jgit.revwalk.RevObject;
import org.eclipse.jgit.revwalk.RevSort;
import org.eclipse.jgit.revwalk.RevTag;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.storage.pack.PackConfig;
import org.eclipse.jgit.util.BlockList;
import org.eclipse.jgit.util.TemporaryBuffer;

/**
 * <p>
 * PackWriter class is responsible for generating pack files from specified set
 * of objects from repository. This implementation produce pack files in format
 * version 2.
 * </p>
 * <p>
 * Source of objects may be specified in two ways:
 * <ul>
 * <li>(usually) by providing sets of interesting and uninteresting objects in
 * repository - all interesting objects and their ancestors except uninteresting
 * objects and their ancestors will be included in pack, or</li>
 * <li>by providing iterator of {@link RevObject} specifying exact list and
 * order of objects in pack</li>
 * </ul>
 * Typical usage consists of creating instance intended for some pack,
 * configuring options, preparing the list of objects by calling
 * {@link #preparePack(Iterator)} or
 * {@link #preparePack(ProgressMonitor, Collection, Collection)}, and finally
 * producing the stream with {@link #writePack(ProgressMonitor, ProgressMonitor, OutputStream)}.
 * </p>
 * <p>
 * Class provide set of configurable options and {@link ProgressMonitor}
 * support, as operations may take a long time for big repositories. Deltas
 * searching algorithm is <b>NOT IMPLEMENTED</b> yet - this implementation
 * relies only on deltas and objects reuse.
 * </p>
 * <p>
 * This class is not thread safe, it is intended to be used in one thread, with
 * one instance per created pack. Subsequent calls to writePack result in
 * undefined behavior.
 * </p>
 */
public class PackWriter {
	private static final int PACK_VERSION_GENERATED = 2;

	/** A collection of object ids. */
	public interface ObjectIdSet {
		/**
		 * Returns true if the objectId is contained within the collection.
		 *
		 * @param objectId
		 *            the objectId to find
		 * @return whether the collection contains the objectId.
		 */
		boolean contains(AnyObjectId objectId);
	}

	private static final Map<WeakReference<PackWriter>, Boolean> instances =
			new ConcurrentHashMap<WeakReference<PackWriter>, Boolean>();

	private static final Iterable<PackWriter> instancesIterable = new Iterable<PackWriter>() {
		public Iterator<PackWriter> iterator() {
			return new Iterator<PackWriter>() {
				private final Iterator<WeakReference<PackWriter>> it =
						instances.keySet().iterator();
				private PackWriter next;

				public boolean hasNext() {
					if (next != null)
						return true;
					while (it.hasNext()) {
						WeakReference<PackWriter> ref = it.next();
						next = ref.get();
						if (next != null)
							return true;
						it.remove();
					}
					return false;
				}

				public PackWriter next() {
					if (hasNext()) {
						PackWriter result = next;
						next = null;
						return result;
					}
					throw new NoSuchElementException();
				}

				public void remove() {
					throw new UnsupportedOperationException();
				}
			};
		}
	};

	/** @return all allocated, non-released PackWriters instances. */
	public static Iterable<PackWriter> getInstances() {
		return instancesIterable;
	}

	@SuppressWarnings("unchecked")
	private final BlockList<ObjectToPack> objectsLists[] = new BlockList[OBJ_TAG + 1];
	{
		objectsLists[OBJ_COMMIT] = new BlockList<ObjectToPack>();
		objectsLists[OBJ_TREE] = new BlockList<ObjectToPack>();
		objectsLists[OBJ_BLOB] = new BlockList<ObjectToPack>();
		objectsLists[OBJ_TAG] = new BlockList<ObjectToPack>();
	}

	private final ObjectIdOwnerMap<ObjectToPack> objectsMap = new ObjectIdOwnerMap<ObjectToPack>();

	// edge objects for thin packs
	private List<ObjectToPack> edgeObjects = new BlockList<ObjectToPack>();

	// Objects the client is known to have already.
	private BitmapBuilder haveObjects;

	private List<CachedPack> cachedPacks = new ArrayList<CachedPack>(2);

	private Set<ObjectId> tagTargets = Collections.emptySet();

	private ObjectIdSet[] excludeInPacks;

	private ObjectIdSet excludeInPackLast;

	private Deflater myDeflater;

	private final ObjectReader reader;

	/** {@link #reader} recast to the reuse interface, if it supports it. */
	private final ObjectReuseAsIs reuseSupport;

	private final PackConfig config;

	private final Statistics stats;

	private final MutableState state;

	private final WeakReference<PackWriter> selfRef;

	private Statistics.ObjectType typeStats;

	private List<ObjectToPack> sortedByName;

	private byte packcsum[];

	private boolean deltaBaseAsOffset;

	private boolean reuseDeltas;

	private boolean reuseDeltaCommits;

	private boolean reuseValidate;

	private boolean thin;

	private boolean useCachedPacks;

	private boolean useBitmaps;

	private boolean ignoreMissingUninteresting = true;

	private boolean pruneCurrentObjectList;

	private boolean shallowPack;

	private boolean canBuildBitmaps;

	private boolean indexDisabled;

	private int depth;

	private Collection<? extends ObjectId> unshallowObjects;

	private PackBitmapIndexBuilder writeBitmaps;

	private CRC32 crc32;

	/**
	 * Create writer for specified repository.
	 * <p>
	 * Objects for packing are specified in {@link #preparePack(Iterator)} or
	 * {@link #preparePack(ProgressMonitor, Collection, Collection)}.
	 *
	 * @param repo
	 *            repository where objects are stored.
	 */
	public PackWriter(final Repository repo) {
		this(repo, repo.newObjectReader());
	}

	/**
	 * Create a writer to load objects from the specified reader.
	 * <p>
	 * Objects for packing are specified in {@link #preparePack(Iterator)} or
	 * {@link #preparePack(ProgressMonitor, Collection, Collection)}.
	 *
	 * @param reader
	 *            reader to read from the repository with.
	 */
	public PackWriter(final ObjectReader reader) {
		this(new PackConfig(), reader);
	}

	/**
	 * Create writer for specified repository.
	 * <p>
	 * Objects for packing are specified in {@link #preparePack(Iterator)} or
	 * {@link #preparePack(ProgressMonitor, Collection, Collection)}.
	 *
	 * @param repo
	 *            repository where objects are stored.
	 * @param reader
	 *            reader to read from the repository with.
	 */
	public PackWriter(final Repository repo, final ObjectReader reader) {
		this(new PackConfig(repo), reader);
	}

	/**
	 * Create writer with a specified configuration.
	 * <p>
	 * Objects for packing are specified in {@link #preparePack(Iterator)} or
	 * {@link #preparePack(ProgressMonitor, Collection, Collection)}.
	 *
	 * @param config
	 *            configuration for the pack writer.
	 * @param reader
	 *            reader to read from the repository with.
	 */
	public PackWriter(final PackConfig config, final ObjectReader reader) {
		this.config = config;
		this.reader = reader;
		if (reader instanceof ObjectReuseAsIs)
			reuseSupport = ((ObjectReuseAsIs) reader);
		else
			reuseSupport = null;

		deltaBaseAsOffset = config.isDeltaBaseAsOffset();
		reuseDeltas = config.isReuseDeltas();
		reuseValidate = true; // be paranoid by default
		stats = new Statistics();
		state = new MutableState();
		selfRef = new WeakReference<PackWriter>(this);
		instances.put(selfRef, Boolean.TRUE);
	}

	/**
	 * Check whether writer can store delta base as an offset (new style
	 * reducing pack size) or should store it as an object id (legacy style,
	 * compatible with old readers).
	 *
	 * Default setting: {@value PackConfig#DEFAULT_DELTA_BASE_AS_OFFSET}
	 *
	 * @return true if delta base is stored as an offset; false if it is stored
	 *         as an object id.
	 */
	public boolean isDeltaBaseAsOffset() {
		return deltaBaseAsOffset;
	}

	/**
	 * Set writer delta base format. Delta base can be written as an offset in a
	 * pack file (new approach reducing file size) or as an object id (legacy
	 * approach, compatible with old readers).
	 *
	 * Default setting: {@value PackConfig#DEFAULT_DELTA_BASE_AS_OFFSET}
	 *
	 * @param deltaBaseAsOffset
	 *            boolean indicating whether delta base can be stored as an
	 *            offset.
	 */
	public void setDeltaBaseAsOffset(boolean deltaBaseAsOffset) {
		this.deltaBaseAsOffset = deltaBaseAsOffset;
	}

	/**
	 * Check if the writer will reuse commits that are already stored as deltas.
	 *
	 * @return true if the writer would reuse commits stored as deltas, assuming
	 *         delta reuse is already enabled.
	 */
	public boolean isReuseDeltaCommits() {
		return reuseDeltaCommits;
	}

	/**
	 * Set the writer to reuse existing delta versions of commits.
	 *
	 * @param reuse
	 *            if true, the writer will reuse any commits stored as deltas.
	 *            By default the writer does not reuse delta commits.
	 */
	public void setReuseDeltaCommits(boolean reuse) {
		reuseDeltaCommits = reuse;
	}

	/**
	 * Check if the writer validates objects before copying them.
	 *
	 * @return true if validation is enabled; false if the reader will handle
	 *         object validation as a side-effect of it consuming the output.
	 */
	public boolean isReuseValidatingObjects() {
		return reuseValidate;
	}

	/**
	 * Enable (or disable) object validation during packing.
	 *
	 * @param validate
	 *            if true the pack writer will validate an object before it is
	 *            put into the output. This additional validation work may be
	 *            necessary to avoid propagating corruption from one local pack
	 *            file to another local pack file.
	 */
	public void setReuseValidatingObjects(boolean validate) {
		reuseValidate = validate;
	}

	/** @return true if this writer is producing a thin pack. */
	public boolean isThin() {
		return thin;
	}

	/**
	 * @param packthin
	 *            a boolean indicating whether writer may pack objects with
	 *            delta base object not within set of objects to pack, but
	 *            belonging to party repository (uninteresting/boundary) as
	 *            determined by set; this kind of pack is used only for
	 *            transport; true - to produce thin pack, false - otherwise.
	 */
	public void setThin(final boolean packthin) {
		thin = packthin;
	}

	/** @return true to reuse cached packs. If true index creation isn't available. */
	public boolean isUseCachedPacks() {
		return useCachedPacks;
	}

	/**
	 * @param useCached
	 *            if set to true and a cached pack is present, it will be
	 *            appended onto the end of a thin-pack, reducing the amount of
	 *            working set space and CPU used by PackWriter. Enabling this
	 *            feature prevents PackWriter from creating an index for the
	 *            newly created pack, so its only suitable for writing to a
	 *            network client, where the client will make the index.
	 */
	public void setUseCachedPacks(boolean useCached) {
		useCachedPacks = useCached;
	}

	/** @return true to use bitmaps for ObjectWalks, if available. */
	public boolean isUseBitmaps() {
		return useBitmaps;
	}

	/**
	 * @param useBitmaps
	 *            if set to true, bitmaps will be used when preparing a pack.
	 */
	public void setUseBitmaps(boolean useBitmaps) {
		this.useBitmaps = useBitmaps;
	}

	/** @return true if the index file cannot be created by this PackWriter. */
	public boolean isIndexDisabled() {
		return indexDisabled || !cachedPacks.isEmpty();
	}

	/**
	 * @param noIndex
	 *            true to disable creation of the index file.
	 */
	public void setIndexDisabled(boolean noIndex) {
		this.indexDisabled = noIndex;
	}

	/**
	 * @return true to ignore objects that are uninteresting and also not found
	 *         on local disk; false to throw a {@link MissingObjectException}
	 *         out of {@link #preparePack(ProgressMonitor, Collection, Collection)} if an
	 *         uninteresting object is not in the source repository. By default,
	 *         true, permitting gracefully ignoring of uninteresting objects.
	 */
	public boolean isIgnoreMissingUninteresting() {
		return ignoreMissingUninteresting;
	}

	/**
	 * @param ignore
	 *            true if writer should ignore non existing uninteresting
	 *            objects during construction set of objects to pack; false
	 *            otherwise - non existing uninteresting objects may cause
	 *            {@link MissingObjectException}
	 */
	public void setIgnoreMissingUninteresting(final boolean ignore) {
		ignoreMissingUninteresting = ignore;
	}

	/**
	 * Set the tag targets that should be hoisted earlier during packing.
	 * <p>
	 * Callers may put objects into this set before invoking any of the
	 * preparePack methods to influence where an annotated tag's target is
	 * stored within the resulting pack. Typically these will be clustered
	 * together, and hoisted earlier in the file even if they are ancient
	 * revisions, allowing readers to find tag targets with better locality.
	 *
	 * @param objects
	 *            objects that annotated tags point at.
	 */
	public void setTagTargets(Set<ObjectId> objects) {
		tagTargets = objects;
	}

	/**
	 * Configure this pack for a shallow clone.
	 *
	 * @param depth
	 *            maximum depth to traverse the commit graph
	 * @param unshallow
	 *            objects which used to be shallow on the client, but are being
	 *            extended as part of this fetch
	 */
	public void setShallowPack(int depth,
			Collection<? extends ObjectId> unshallow) {
		this.shallowPack = true;
		this.depth = depth;
		this.unshallowObjects = unshallow;
	}

	/**
	 * Returns objects number in a pack file that was created by this writer.
	 *
	 * @return number of objects in pack.
	 * @throws IOException
	 *             a cached pack cannot supply its object count.
	 */
	public long getObjectCount() throws IOException {
		if (stats.totalObjects == 0) {
			long objCnt = 0;

			objCnt += objectsLists[OBJ_COMMIT].size();
			objCnt += objectsLists[OBJ_TREE].size();
			objCnt += objectsLists[OBJ_BLOB].size();
			objCnt += objectsLists[OBJ_TAG].size();

			for (CachedPack pack : cachedPacks)
				objCnt += pack.getObjectCount();
			return objCnt;
		}
		return stats.totalObjects;
	}

	/**
	 * Returns the object ids in the pack file that was created by this writer.
	 *
	 * This method can only be invoked after
	 * {@link #writePack(ProgressMonitor, ProgressMonitor, OutputStream)} has
	 * been invoked and completed successfully.
	 *
	 * @return number of objects in pack.
	 * @throws IOException
	 *             a cached pack cannot supply its object ids.
	 */
	public ObjectIdOwnerMap<ObjectIdOwnerMap.Entry> getObjectSet()
			throws IOException {
		if (!cachedPacks.isEmpty())
			throw new IOException(
					JGitText.get().cachedPacksPreventsListingObjects);

		ObjectIdOwnerMap<ObjectIdOwnerMap.Entry> objs = new ObjectIdOwnerMap<
				ObjectIdOwnerMap.Entry>();
		for (BlockList<ObjectToPack> objList : objectsLists) {
			if (objList != null) {
				for (ObjectToPack otp : objList)
					objs.add(new ObjectIdOwnerMap.Entry(otp) {
						// A new entry that copies the ObjectId
					});
			}
		}
		return objs;
	}

	/**
	 * Add a pack index whose contents should be excluded from the result.
	 *
	 * @param idx
	 *            objects in this index will not be in the output pack.
	 */
	public void excludeObjects(ObjectIdSet idx) {
		if (excludeInPacks == null) {
			excludeInPacks = new ObjectIdSet[] { idx };
			excludeInPackLast = idx;
		} else {
			int cnt = excludeInPacks.length;
			ObjectIdSet[] newList = new ObjectIdSet[cnt + 1];
			System.arraycopy(excludeInPacks, 0, newList, 0, cnt);
			newList[cnt] = idx;
			excludeInPacks = newList;
		}
	}

	/**
	 * Prepare the list of objects to be written to the pack stream.
	 * <p>
	 * Iterator <b>exactly</b> determines which objects are included in a pack
	 * and order they appear in pack (except that objects order by type is not
	 * needed at input). This order should conform general rules of ordering
	 * objects in git - by recency and path (type and delta-base first is
	 * internally secured) and responsibility for guaranteeing this order is on
	 * a caller side. Iterator must return each id of object to write exactly
	 * once.
	 * </p>
	 *
	 * @param objectsSource
	 *            iterator of object to store in a pack; order of objects within
	 *            each type is important, ordering by type is not needed;
	 *            allowed types for objects are {@link Constants#OBJ_COMMIT},
	 *            {@link Constants#OBJ_TREE}, {@link Constants#OBJ_BLOB} and
	 *            {@link Constants#OBJ_TAG}; objects returned by iterator may be
	 *            later reused by caller as object id and type are internally
	 *            copied in each iteration.
	 * @throws IOException
	 *             when some I/O problem occur during reading objects.
	 */
	public void preparePack(final Iterator<RevObject> objectsSource)
			throws IOException {
		while (objectsSource.hasNext()) {
			addObject(objectsSource.next());
		}
	}

	/**
	 * Prepare the list of objects to be written to the pack stream.
	 * <p>
	 * Basing on these 2 sets, another set of objects to put in a pack file is
	 * created: this set consists of all objects reachable (ancestors) from
	 * interesting objects, except uninteresting objects and their ancestors.
	 * This method uses class {@link ObjectWalk} extensively to find out that
	 * appropriate set of output objects and their optimal order in output pack.
	 * Order is consistent with general git in-pack rules: sort by object type,
	 * recency, path and delta-base first.
	 * </p>
	 *
	 * @param countingMonitor
	 *            progress during object enumeration.
	 * @param want
	 *            collection of objects to be marked as interesting (start
	 *            points of graph traversal).
	 * @param have
	 *            collection of objects to be marked as uninteresting (end
	 *            points of graph traversal).
	 * @throws IOException
	 *             when some I/O problem occur during reading objects.
	 * @deprecated to be removed in 2.0; use the Set version of this method.
	 */
	@Deprecated
	public void preparePack(ProgressMonitor countingMonitor,
			final Collection<? extends ObjectId> want,
			final Collection<? extends ObjectId> have) throws IOException {
		preparePack(countingMonitor, ensureSet(want), ensureSet(have));
	}

	/**
	 * Prepare the list of objects to be written to the pack stream.
	 * <p>
	 * Basing on these 2 sets, another set of objects to put in a pack file is
	 * created: this set consists of all objects reachable (ancestors) from
	 * interesting objects, except uninteresting objects and their ancestors.
	 * This method uses class {@link ObjectWalk} extensively to find out that
	 * appropriate set of output objects and their optimal order in output pack.
	 * Order is consistent with general git in-pack rules: sort by object type,
	 * recency, path and delta-base first.
	 * </p>
	 *
	 * @param countingMonitor
	 *            progress during object enumeration.
	 * @param walk
	 *            ObjectWalk to perform enumeration.
	 * @param interestingObjects
	 *            collection of objects to be marked as interesting (start
	 *            points of graph traversal).
	 * @param uninterestingObjects
	 *            collection of objects to be marked as uninteresting (end
	 *            points of graph traversal).
	 * @throws IOException
	 *             when some I/O problem occur during reading objects.
	 * @deprecated to be removed in 2.0; use the Set version of this method.
	 */
	@Deprecated
	public void preparePack(ProgressMonitor countingMonitor,
			ObjectWalk walk,
			final Collection<? extends ObjectId> interestingObjects,
			final Collection<? extends ObjectId> uninterestingObjects)
			throws IOException {
		preparePack(countingMonitor, walk,
				ensureSet(interestingObjects),
				ensureSet(uninterestingObjects));
	}

	@SuppressWarnings("unchecked")
	private static Set<ObjectId> ensureSet(Collection<? extends ObjectId> objs) {
		Set<ObjectId> set;
		if (objs instanceof Set<?>)
			set = (Set<ObjectId>) objs;
		else if (objs == null)
			set = Collections.emptySet();
		else
			set = new HashSet<ObjectId>(objs);
		return set;
	}

	/**
	 * Prepare the list of objects to be written to the pack stream.
	 * <p>
	 * Basing on these 2 sets, another set of objects to put in a pack file is
	 * created: this set consists of all objects reachable (ancestors) from
	 * interesting objects, except uninteresting objects and their ancestors.
	 * This method uses class {@link ObjectWalk} extensively to find out that
	 * appropriate set of output objects and their optimal order in output pack.
	 * Order is consistent with general git in-pack rules: sort by object type,
	 * recency, path and delta-base first.
	 * </p>
	 *
	 * @param countingMonitor
	 *            progress during object enumeration.
	 * @param want
	 *            collection of objects to be marked as interesting (start
	 *            points of graph traversal).
	 * @param have
	 *            collection of objects to be marked as uninteresting (end
	 *            points of graph traversal).
	 * @throws IOException
	 *             when some I/O problem occur during reading objects.
	 */
	public void preparePack(ProgressMonitor countingMonitor,
			Set<? extends ObjectId> want,
			Set<? extends ObjectId> have) throws IOException {
		ObjectWalk ow;
		if (shallowPack)
			ow = new DepthWalk.ObjectWalk(reader, depth);
		else
			ow = new ObjectWalk(reader);
		preparePack(countingMonitor, ow, want, have);
	}

	/**
	 * Prepare the list of objects to be written to the pack stream.
	 * <p>
	 * Basing on these 2 sets, another set of objects to put in a pack file is
	 * created: this set consists of all objects reachable (ancestors) from
	 * interesting objects, except uninteresting objects and their ancestors.
	 * This method uses class {@link ObjectWalk} extensively to find out that
	 * appropriate set of output objects and their optimal order in output pack.
	 * Order is consistent with general git in-pack rules: sort by object type,
	 * recency, path and delta-base first.
	 * </p>
	 *
	 * @param countingMonitor
	 *            progress during object enumeration.
	 * @param walk
	 *            ObjectWalk to perform enumeration.
	 * @param interestingObjects
	 *            collection of objects to be marked as interesting (start
	 *            points of graph traversal).
	 * @param uninterestingObjects
	 *            collection of objects to be marked as uninteresting (end
	 *            points of graph traversal).
	 * @throws IOException
	 *             when some I/O problem occur during reading objects.
	 */
	public void preparePack(ProgressMonitor countingMonitor,
			ObjectWalk walk,
			final Set<? extends ObjectId> interestingObjects,
			final Set<? extends ObjectId> uninterestingObjects)
			throws IOException {
		if (countingMonitor == null)
			countingMonitor = NullProgressMonitor.INSTANCE;
		if (shallowPack && !(walk instanceof DepthWalk.ObjectWalk))
			walk = new DepthWalk.ObjectWalk(reader, depth);
		findObjectsToPack(countingMonitor, walk, interestingObjects,
				uninterestingObjects);
	}

	/**
	 * Determine if the pack file will contain the requested object.
	 *
	 * @param id
	 *            the object to test the existence of.
	 * @return true if the object will appear in the output pack file.
	 * @throws IOException
	 *             a cached pack cannot be examined.
	 */
	public boolean willInclude(final AnyObjectId id) throws IOException {
		ObjectToPack obj = objectsMap.get(id);
		return obj != null && !obj.isEdge();
	}

	/**
	 * Lookup the ObjectToPack object for a given ObjectId.
	 *
	 * @param id
	 *            the object to find in the pack.
	 * @return the object we are packing, or null.
	 */
	public ObjectToPack get(AnyObjectId id) {
		ObjectToPack obj = objectsMap.get(id);
		return obj != null && !obj.isEdge() ? obj : null;
	}

	/**
	 * Computes SHA-1 of lexicographically sorted objects ids written in this
	 * pack, as used to name a pack file in repository.
	 *
	 * @return ObjectId representing SHA-1 name of a pack that was created.
	 */
	public ObjectId computeName() {
		final byte[] buf = new byte[OBJECT_ID_LENGTH];
		final MessageDigest md = Constants.newMessageDigest();
		for (ObjectToPack otp : sortByName()) {
			otp.copyRawTo(buf, 0);
			md.update(buf, 0, OBJECT_ID_LENGTH);
		}
		return ObjectId.fromRaw(md.digest());
	}

	/**
	 * Returns the index format version that will be written.
	 * <p>
	 * This method can only be invoked after
	 * {@link #writePack(ProgressMonitor, ProgressMonitor, OutputStream)} has
	 * been invoked and completed successfully.
	 *
	 * @return the index format version.
	 */
	public int getIndexVersion() {
		int indexVersion = config.getIndexVersion();
		if (indexVersion <= 0) {
			for (BlockList<ObjectToPack> objs : objectsLists)
				indexVersion = Math.max(indexVersion,
						PackIndexWriter.oldestPossibleFormat(objs));
		}
		return indexVersion;
	}

	/**
	 * Create an index file to match the pack file just written.
	 * <p>
	 * This method can only be invoked after
	 * {@link #writePack(ProgressMonitor, ProgressMonitor, OutputStream)} has
	 * been invoked and completed successfully. Writing a corresponding index is
	 * an optional feature that not all pack users may require.
	 *
	 * @param indexStream
	 *            output for the index data. Caller is responsible for closing
	 *            this stream.
	 * @throws IOException
	 *             the index data could not be written to the supplied stream.
	 */
	public void writeIndex(final OutputStream indexStream) throws IOException {
		if (isIndexDisabled())
			throw new IOException(JGitText.get().cachedPacksPreventsIndexCreation);

		long writeStart = System.currentTimeMillis();
		final PackIndexWriter iw = PackIndexWriter.createVersion(
				indexStream, getIndexVersion());
		iw.write(sortByName(), packcsum);
		stats.timeWriting += System.currentTimeMillis() - writeStart;
	}

	/**
	 * Create a bitmap index file to match the pack file just written.
	 * <p>
	 * This method can only be invoked after
	 * {@link #prepareBitmapIndex(ProgressMonitor)} has been invoked and
	 * completed successfully. Writing a corresponding bitmap index is an
	 * optional feature that not all pack users may require.
	 *
	 * @param bitmapIndexStream
	 *            output for the bitmap index data. Caller is responsible for
	 *            closing this stream.
	 * @throws IOException
	 *             the index data could not be written to the supplied stream.
	 */
	public void writeBitmapIndex(final OutputStream bitmapIndexStream)
			throws IOException {
		if (writeBitmaps == null)
			throw new IOException(JGitText.get().bitmapsMustBePrepared);

		long writeStart = System.currentTimeMillis();
		final PackBitmapIndexWriterV1 iw = new PackBitmapIndexWriterV1(bitmapIndexStream);
		iw.write(writeBitmaps, packcsum);
		stats.timeWriting += System.currentTimeMillis() - writeStart;
	}

	private List<ObjectToPack> sortByName() {
		if (sortedByName == null) {
			int cnt = 0;
			cnt += objectsLists[OBJ_COMMIT].size();
			cnt += objectsLists[OBJ_TREE].size();
			cnt += objectsLists[OBJ_BLOB].size();
			cnt += objectsLists[OBJ_TAG].size();

			sortedByName = new BlockList<ObjectToPack>(cnt);
			sortedByName.addAll(objectsLists[OBJ_COMMIT]);
			sortedByName.addAll(objectsLists[OBJ_TREE]);
			sortedByName.addAll(objectsLists[OBJ_BLOB]);
			sortedByName.addAll(objectsLists[OBJ_TAG]);
			Collections.sort(sortedByName);
		}
		return sortedByName;
	}

	private void beginPhase(PackingPhase phase, ProgressMonitor monitor,
			long cnt) {
		state.phase = phase;
		String task;
		switch (phase) {
		case COUNTING:
			task = JGitText.get().countingObjects;
			break;
		case GETTING_SIZES:
			task = JGitText.get().searchForSizes;
			break;
		case FINDING_SOURCES:
			task = JGitText.get().searchForReuse;
			break;
		case COMPRESSING:
			task = JGitText.get().compressingObjects;
			break;
		case WRITING:
			task = JGitText.get().writingObjects;
			break;
		case BUILDING_BITMAPS:
			task = JGitText.get().buildingBitmaps;
			break;
		default:
			throw new IllegalArgumentException(
					MessageFormat.format(JGitText.get().illegalPackingPhase, phase));
		}
		monitor.beginTask(task, (int) cnt);
	}

	private void endPhase(ProgressMonitor monitor) {
		monitor.endTask();
	}

	/**
	 * Write the prepared pack to the supplied stream.
	 * <p>
	 * At first, this method collects and sorts objects to pack, then deltas
	 * search is performed if set up accordingly, finally pack stream is
	 * written.
	 * </p>
	 * <p>
	 * All reused objects data checksum (Adler32/CRC32) is computed and
	 * validated against existing checksum.
	 * </p>
	 *
	 * @param compressMonitor
	 *            progress monitor to report object compression work.
	 * @param writeMonitor
	 *            progress monitor to report the number of objects written.
	 * @param packStream
	 *            output stream of pack data. The stream should be buffered by
	 *            the caller. The caller is responsible for closing the stream.
	 * @throws IOException
	 *             an error occurred reading a local object's data to include in
	 *             the pack, or writing compressed object data to the output
	 *             stream.
	 */
	public void writePack(ProgressMonitor compressMonitor,
			ProgressMonitor writeMonitor, OutputStream packStream)
			throws IOException {
		if (compressMonitor == null)
			compressMonitor = NullProgressMonitor.INSTANCE;
		if (writeMonitor == null)
			writeMonitor = NullProgressMonitor.INSTANCE;

		excludeInPacks = null;
		excludeInPackLast = null;

		boolean needSearchForReuse = reuseSupport != null && (
				   reuseDeltas
				|| config.isReuseObjects()
				|| !cachedPacks.isEmpty());

		if (compressMonitor instanceof BatchingProgressMonitor) {
			long delay = 1000;
			if (needSearchForReuse && config.isDeltaCompress())
				delay = 500;
			((BatchingProgressMonitor) compressMonitor).setDelayStart(
					delay,
					TimeUnit.MILLISECONDS);
		}

		if (needSearchForReuse)
			searchForReuse(compressMonitor);
		if (config.isDeltaCompress())
			searchForDeltas(compressMonitor);

		crc32 = new CRC32();
		final PackOutputStream out = new PackOutputStream(
			writeMonitor,
			isIndexDisabled()
				? packStream
				: new CheckedOutputStream(packStream, crc32),
			this);

		long objCnt = getObjectCount();
		stats.totalObjects = objCnt;
		beginPhase(PackingPhase.WRITING, writeMonitor, objCnt);
		long writeStart = System.currentTimeMillis();

		out.writeFileHeader(PACK_VERSION_GENERATED, objCnt);
		out.flush();

		writeObjects(out);
		if (!edgeObjects.isEmpty() || !cachedPacks.isEmpty()) {
			for (Statistics.ObjectType typeStat : stats.objectTypes) {
				if (typeStat == null)
					continue;
				stats.thinPackBytes += typeStat.bytes;
			}
		}

		for (CachedPack pack : cachedPacks) {
			long deltaCnt = pack.getDeltaCount();
			stats.reusedObjects += pack.getObjectCount();
			stats.reusedDeltas += deltaCnt;
			stats.totalDeltas += deltaCnt;
			reuseSupport.copyPackAsIs(out, pack, reuseValidate);
		}
		writeChecksum(out);
		out.flush();
		stats.timeWriting = System.currentTimeMillis() - writeStart;
		stats.totalBytes = out.length();
		stats.reusedPacks = Collections.unmodifiableList(cachedPacks);
		stats.depth = depth;

		for (Statistics.ObjectType typeStat : stats.objectTypes) {
			if (typeStat == null)
				continue;
			typeStat.cntDeltas += typeStat.reusedDeltas;

			stats.reusedObjects += typeStat.reusedObjects;
			stats.reusedDeltas += typeStat.reusedDeltas;
			stats.totalDeltas += typeStat.cntDeltas;
		}

		reader.release();
		endPhase(writeMonitor);
	}

	/**
	 * @return description of what this PackWriter did in order to create the
	 *         final pack stream. The object is only available to callers after
	 *         {@link #writePack(ProgressMonitor, ProgressMonitor, OutputStream)}
	 */
	public Statistics getStatistics() {
		return stats;
	}

	/** @return snapshot of the current state of this PackWriter. */
	public State getState() {
		return state.snapshot();
	}

	/** Release all resources used by this writer. */
	public void release() {
		reader.release();
		if (myDeflater != null) {
			myDeflater.end();
			myDeflater = null;
		}
		instances.remove(selfRef);
	}

	private void searchForReuse(ProgressMonitor monitor) throws IOException {
		long cnt = 0;
		cnt += objectsLists[OBJ_COMMIT].size();
		cnt += objectsLists[OBJ_TREE].size();
		cnt += objectsLists[OBJ_BLOB].size();
		cnt += objectsLists[OBJ_TAG].size();

		long start = System.currentTimeMillis();
		beginPhase(PackingPhase.FINDING_SOURCES, monitor, cnt);
		if (cnt <= 4096) {
			// For small object counts, do everything as one list.
			BlockList<ObjectToPack> tmp = new BlockList<ObjectToPack>((int) cnt);
			tmp.addAll(objectsLists[OBJ_TAG]);
			tmp.addAll(objectsLists[OBJ_COMMIT]);
			tmp.addAll(objectsLists[OBJ_TREE]);
			tmp.addAll(objectsLists[OBJ_BLOB]);
			searchForReuse(monitor, tmp);
			if (pruneCurrentObjectList) {
				// If the list was pruned, we need to re-prune the main lists.
				pruneEdgesFromObjectList(objectsLists[OBJ_COMMIT]);
				pruneEdgesFromObjectList(objectsLists[OBJ_TREE]);
				pruneEdgesFromObjectList(objectsLists[OBJ_BLOB]);
				pruneEdgesFromObjectList(objectsLists[OBJ_TAG]);
			}
		} else {
			searchForReuse(monitor, objectsLists[OBJ_TAG]);
			searchForReuse(monitor, objectsLists[OBJ_COMMIT]);
			searchForReuse(monitor, objectsLists[OBJ_TREE]);
			searchForReuse(monitor, objectsLists[OBJ_BLOB]);
		}
		endPhase(monitor);
		stats.timeSearchingForReuse = System.currentTimeMillis() - start;

		if (config.isReuseDeltas() && config.getCutDeltaChains()) {
			cutDeltaChains(objectsLists[OBJ_TREE]);
			cutDeltaChains(objectsLists[OBJ_BLOB]);
		}
	}

	private void searchForReuse(ProgressMonitor monitor, List<ObjectToPack> list)
			throws IOException, MissingObjectException {
		pruneCurrentObjectList = false;
		reuseSupport.selectObjectRepresentation(this, monitor, list);
		if (pruneCurrentObjectList)
			pruneEdgesFromObjectList(list);
	}

	private void cutDeltaChains(BlockList<ObjectToPack> list)
			throws IOException {
		int max = config.getMaxDeltaDepth();
		for (int idx = list.size() - 1; idx >= 0; idx--) {
			int d = 0;
			ObjectToPack b = list.get(idx).getDeltaBase();
			while (b != null) {
				if (d < b.getChainLength())
					break;
				b.setChainLength(++d);
				if (d >= max && b.isDeltaRepresentation()) {
					reselectNonDelta(b);
					break;
				}
				b = b.getDeltaBase();
			}
		}
		if (config.isDeltaCompress()) {
			for (ObjectToPack otp : list)
				otp.clearChainLength();
		}
	}

	private void searchForDeltas(ProgressMonitor monitor)
			throws MissingObjectException, IncorrectObjectTypeException,
			IOException {
		// Commits and annotated tags tend to have too many differences to
		// really benefit from delta compression. Consequently just don't
		// bother examining those types here.
		//
		ObjectToPack[] list = new ObjectToPack[
				  objectsLists[OBJ_TREE].size()
				+ objectsLists[OBJ_BLOB].size()
				+ edgeObjects.size()];
		int cnt = 0;
		cnt = findObjectsNeedingDelta(list, cnt, OBJ_TREE);
		cnt = findObjectsNeedingDelta(list, cnt, OBJ_BLOB);
		if (cnt == 0)
			return;
		int nonEdgeCnt = cnt;

		// Queue up any edge objects that we might delta against.  We won't
		// be sending these as we assume the other side has them, but we need
		// them in the search phase below.
		//
		for (ObjectToPack eo : edgeObjects) {
			eo.setWeight(0);
			list[cnt++] = eo;
		}

		// Compute the sizes of the objects so we can do a proper sort.
		// We let the reader skip missing objects if it chooses. For
		// some readers this can be a huge win. We detect missing objects
		// by having set the weights above to 0 and allowing the delta
		// search code to discover the missing object and skip over it, or
		// abort with an exception if we actually had to have it.
		//
		final long sizingStart = System.currentTimeMillis();
		beginPhase(PackingPhase.GETTING_SIZES, monitor, cnt);
		AsyncObjectSizeQueue<ObjectToPack> sizeQueue = reader.getObjectSize(
				Arrays.<ObjectToPack> asList(list).subList(0, cnt), false);
		try {
			final long limit = Math.min(
					config.getBigFileThreshold(),
					Integer.MAX_VALUE);
			for (;;) {
				try {
					if (!sizeQueue.next())
						break;
				} catch (MissingObjectException notFound) {
					monitor.update(1);
					if (ignoreMissingUninteresting) {
						ObjectToPack otp = sizeQueue.getCurrent();
						if (otp != null && otp.isEdge()) {
							otp.setDoNotDelta();
							continue;
						}

						otp = objectsMap.get(notFound.getObjectId());
						if (otp != null && otp.isEdge()) {
							otp.setDoNotDelta();
							continue;
						}
					}
					throw notFound;
				}

				ObjectToPack otp = sizeQueue.getCurrent();
				if (otp == null)
					otp = objectsMap.get(sizeQueue.getObjectId());

				long sz = sizeQueue.getSize();
				if (DeltaIndex.BLKSZ < sz && sz < limit)
					otp.setWeight((int) sz);
				else
					otp.setDoNotDelta(); // too small, or too big
				monitor.update(1);
			}
		} finally {
			sizeQueue.release();
		}
		endPhase(monitor);
		stats.timeSearchingForSizes = System.currentTimeMillis() - sizingStart;

		// Sort the objects by path hash so like files are near each other,
		// and then by size descending so that bigger files are first. This
		// applies "Linus' Law" which states that newer files tend to be the
		// bigger ones, because source files grow and hardly ever shrink.
		//
		Arrays.sort(list, 0, cnt, new Comparator<ObjectToPack>() {
			public int compare(ObjectToPack a, ObjectToPack b) {
				int cmp = (a.isDoNotDelta() ? 1 : 0)
						- (b.isDoNotDelta() ? 1 : 0);
				if (cmp != 0)
					return cmp;

				cmp = a.getType() - b.getType();
				if (cmp != 0)
					return cmp;

				cmp = (a.getPathHash() >>> 1) - (b.getPathHash() >>> 1);
				if (cmp != 0)
					return cmp;

				cmp = (a.getPathHash() & 1) - (b.getPathHash() & 1);
				if (cmp != 0)
					return cmp;

				cmp = (a.isEdge() ? 0 : 1) - (b.isEdge() ? 0 : 1);
				if (cmp != 0)
					return cmp;

				return b.getWeight() - a.getWeight();
			}
		});

		// Above we stored the objects we cannot delta onto the end.
		// Remove them from the list so we don't waste time on them.
		while (0 < cnt && list[cnt - 1].isDoNotDelta()) {
			if (!list[cnt - 1].isEdge())
				nonEdgeCnt--;
			cnt--;
		}
		if (cnt == 0)
			return;

		final long searchStart = System.currentTimeMillis();
		searchForDeltas(monitor, list, cnt);
		stats.deltaSearchNonEdgeObjects = nonEdgeCnt;
		stats.timeCompressing = System.currentTimeMillis() - searchStart;

		for (int i = 0; i < cnt; i++)
			if (!list[i].isEdge() && list[i].isDeltaRepresentation())
				stats.deltasFound++;
	}

	private int findObjectsNeedingDelta(ObjectToPack[] list, int cnt, int type) {
		for (ObjectToPack otp : objectsLists[type]) {
			if (otp.isDoNotDelta()) // delta is disabled for this path
				continue;
			if (otp.isDeltaRepresentation()) // already reusing a delta
				continue;
			otp.setWeight(0);
			list[cnt++] = otp;
		}
		return cnt;
	}

	private void reselectNonDelta(ObjectToPack otp) throws IOException {
		otp.clearDeltaBase();
		otp.clearReuseAsIs();
		boolean old = reuseDeltas;
		reuseDeltas = false;
		reuseSupport.selectObjectRepresentation(this,
				NullProgressMonitor.INSTANCE,
				Collections.singleton(otp));
		reuseDeltas = old;
	}

	private void searchForDeltas(final ProgressMonitor monitor,
			final ObjectToPack[] list, final int cnt)
			throws MissingObjectException, IncorrectObjectTypeException,
			LargeObjectException, IOException {
		int threads = config.getThreads();
		if (threads == 0)
			threads = Runtime.getRuntime().availableProcessors();
		if (threads <= 1 || cnt <= config.getDeltaSearchWindowSize())
			singleThreadDeltaSearch(monitor, list, cnt);
		else
			parallelDeltaSearch(monitor, list, cnt, threads);
	}

	private void singleThreadDeltaSearch(ProgressMonitor monitor,
			ObjectToPack[] list, int cnt) throws IOException {
		long totalWeight = 0;
		for (int i = 0; i < cnt; i++) {
			ObjectToPack o = list[i];
			if (!o.isEdge() && !o.doNotAttemptDelta())
				totalWeight += o.getWeight();
		}

		long bytesPerUnit = 1;
		while (DeltaTask.MAX_METER <= (totalWeight / bytesPerUnit))
			bytesPerUnit <<= 10;
		int cost = (int) (totalWeight / bytesPerUnit);
		if (totalWeight % bytesPerUnit != 0)
			cost++;

		beginPhase(PackingPhase.COMPRESSING, monitor, cost);
		new DeltaWindow(config, new DeltaCache(config), reader,
				monitor, bytesPerUnit,
				list, 0, cnt).search();
		endPhase(monitor);
	}

	private void parallelDeltaSearch(ProgressMonitor monitor,
			ObjectToPack[] list, int cnt, int threads) throws IOException {
		DeltaCache dc = new ThreadSafeDeltaCache(config);
		ThreadSafeProgressMonitor pm = new ThreadSafeProgressMonitor(monitor);
		DeltaTask.Block taskBlock = new DeltaTask.Block(threads, config,
				reader, dc, pm,
				list, 0, cnt);
		taskBlock.partitionTasks();
		beginPhase(PackingPhase.COMPRESSING, monitor, taskBlock.cost());
		pm.startWorkers(taskBlock.tasks.size());

		Executor executor = config.getExecutor();
		final List<Throwable> errors =
				Collections.synchronizedList(new ArrayList<Throwable>(threads));
		if (executor instanceof ExecutorService) {
			// Caller supplied us a service, use it directly.
			runTasks((ExecutorService) executor, pm, taskBlock, errors);
		} else if (executor == null) {
			// Caller didn't give us a way to run the tasks, spawn up a
			// temporary thread pool and make sure it tears down cleanly.
			ExecutorService pool = Executors.newFixedThreadPool(threads);
			try {
				runTasks(pool, pm, taskBlock, errors);
			} finally {
				pool.shutdown();
				for (;;) {
					try {
						if (pool.awaitTermination(60, TimeUnit.SECONDS))
							break;
					} catch (InterruptedException e) {
						throw new IOException(
								JGitText.get().packingCancelledDuringObjectsWriting);
					}
				}
			}
		} else {
			// The caller gave us an executor, but it might not do
			// asynchronous execution.  Wrap everything and hope it
			// can schedule these for us.
			for (final DeltaTask task : taskBlock.tasks) {
				executor.execute(new Runnable() {
					public void run() {
						try {
							task.call();
						} catch (Throwable failure) {
							errors.add(failure);
						}
					}
				});
			}
			try {
				pm.waitForCompletion();
			} catch (InterruptedException ie) {
				// We can't abort the other tasks as we have no handle.
				// Cross our fingers and just break out anyway.
				//
				throw new IOException(
						JGitText.get().packingCancelledDuringObjectsWriting);
			}
		}

		// If any task threw an error, try to report it back as
		// though we weren't using a threaded search algorithm.
		//
		if (!errors.isEmpty()) {
			Throwable err = errors.get(0);
			if (err instanceof Error)
				throw (Error) err;
			if (err instanceof RuntimeException)
				throw (RuntimeException) err;
			if (err instanceof IOException)
				throw (IOException) err;

			IOException fail = new IOException(err.getMessage());
			fail.initCause(err);
			throw fail;
		}
		endPhase(monitor);
	}

	private static void runTasks(ExecutorService pool,
			ThreadSafeProgressMonitor pm,
			DeltaTask.Block tb, List<Throwable> errors) throws IOException {
		List<Future<?>> futures = new ArrayList<Future<?>>(tb.tasks.size());
		for (DeltaTask task : tb.tasks)
			futures.add(pool.submit(task));

		try {
			pm.waitForCompletion();
			for (Future<?> f : futures) {
				try {
					f.get();
				} catch (ExecutionException failed) {
					errors.add(failed.getCause());
				}
			}
		} catch (InterruptedException ie) {
			for (Future<?> f : futures)
				f.cancel(true);
			throw new IOException(
					JGitText.get().packingCancelledDuringObjectsWriting);
		}
	}

	private void writeObjects(PackOutputStream out) throws IOException {
		writeObjects(out, objectsLists[OBJ_COMMIT]);
		writeObjects(out, objectsLists[OBJ_TAG]);
		writeObjects(out, objectsLists[OBJ_TREE]);
		writeObjects(out, objectsLists[OBJ_BLOB]);
	}

	private void writeObjects(PackOutputStream out, List<ObjectToPack> list)
			throws IOException {
		if (list.isEmpty())
			return;

		typeStats = stats.objectTypes[list.get(0).getType()];
		long beginOffset = out.length();

		if (reuseSupport != null) {
			reuseSupport.writeObjects(out, list);
		} else {
			for (ObjectToPack otp : list)
				out.writeObject(otp);
		}

		typeStats.bytes += out.length() - beginOffset;
		typeStats.cntObjects = list.size();
	}

	void writeObject(PackOutputStream out, ObjectToPack otp) throws IOException {
		if (!otp.isWritten())
			writeObjectImpl(out, otp);
	}

	private void writeObjectImpl(PackOutputStream out, ObjectToPack otp)
			throws IOException {
		if (otp.wantWrite()) {
			// A cycle exists in this delta chain. This should only occur if a
			// selected object representation disappeared during writing
			// (for example due to a concurrent repack) and a different base
			// was chosen, forcing a cycle. Select something other than a
			// delta, and write this object.
			reselectNonDelta(otp);
		}
		otp.markWantWrite();

		while (otp.isReuseAsIs()) {
			writeBase(out, otp.getDeltaBase());
			if (otp.isWritten())
				return; // Delta chain cycle caused this to write already.

			crc32.reset();
			otp.setOffset(out.length());
			try {
				reuseSupport.copyObjectAsIs(out, otp, reuseValidate);
				out.endObject();
				otp.setCRC((int) crc32.getValue());
				typeStats.reusedObjects++;
				if (otp.isDeltaRepresentation()) {
					typeStats.reusedDeltas++;
					typeStats.deltaBytes += out.length() - otp.getOffset();
				}
				return;
			} catch (StoredObjectRepresentationNotAvailableException gone) {
				if (otp.getOffset() == out.length()) {
					otp.setOffset(0);
					otp.clearDeltaBase();
					otp.clearReuseAsIs();
					reuseSupport.selectObjectRepresentation(this,
							NullProgressMonitor.INSTANCE,
							Collections.singleton(otp));
					continue;
				} else {
					// Object writing already started, we cannot recover.
					//
					CorruptObjectException coe;
					coe = new CorruptObjectException(otp, ""); //$NON-NLS-1$
					coe.initCause(gone);
					throw coe;
				}
			}
		}

		// If we reached here, reuse wasn't possible.
		//
		if (otp.isDeltaRepresentation())
			writeDeltaObjectDeflate(out, otp);
		else
			writeWholeObjectDeflate(out, otp);
		out.endObject();
		otp.setCRC((int) crc32.getValue());
	}

	private void writeBase(PackOutputStream out, ObjectToPack base)
			throws IOException {
		if (base != null && !base.isWritten() && !base.isEdge())
			writeObjectImpl(out, base);
	}

	private void writeWholeObjectDeflate(PackOutputStream out,
			final ObjectToPack otp) throws IOException {
		final Deflater deflater = deflater();
		final ObjectLoader ldr = reader.open(otp, otp.getType());

		crc32.reset();
		otp.setOffset(out.length());
		out.writeHeader(otp, ldr.getSize());

		deflater.reset();
		DeflaterOutputStream dst = new DeflaterOutputStream(out, deflater);
		ldr.copyTo(dst);
		dst.finish();
	}

	private void writeDeltaObjectDeflate(PackOutputStream out,
			final ObjectToPack otp) throws IOException {
		writeBase(out, otp.getDeltaBase());

		crc32.reset();
		otp.setOffset(out.length());

		DeltaCache.Ref ref = otp.popCachedDelta();
		if (ref != null) {
			byte[] zbuf = ref.get();
			if (zbuf != null) {
				out.writeHeader(otp, otp.getCachedSize());
				out.write(zbuf);
				return;
			}
		}

		TemporaryBuffer.Heap delta = delta(otp);
		out.writeHeader(otp, delta.length());

		Deflater deflater = deflater();
		deflater.reset();
		DeflaterOutputStream dst = new DeflaterOutputStream(out, deflater);
		delta.writeTo(dst, null);
		dst.finish();
		typeStats.cntDeltas++;
		typeStats.deltaBytes += out.length() - otp.getOffset();
	}

	private TemporaryBuffer.Heap delta(final ObjectToPack otp)
			throws IOException {
		DeltaIndex index = new DeltaIndex(buffer(otp.getDeltaBaseId()));
		byte[] res = buffer(otp);

		// We never would have proposed this pair if the delta would be
		// larger than the unpacked version of the object. So using it
		// as our buffer limit is valid: we will never reach it.
		//
		TemporaryBuffer.Heap delta = new TemporaryBuffer.Heap(res.length);
		index.encode(delta, res);
		return delta;
	}

	private byte[] buffer(AnyObjectId objId) throws IOException {
		return buffer(config, reader, objId);
	}

	static byte[] buffer(PackConfig config, ObjectReader or, AnyObjectId objId)
			throws IOException {
		// PackWriter should have already pruned objects that
		// are above the big file threshold, so our chances of
		// the object being below it are very good. We really
		// shouldn't be here, unless the implementation is odd.

		return or.open(objId).getCachedBytes(config.getBigFileThreshold());
	}

	private Deflater deflater() {
		if (myDeflater == null)
			myDeflater = new Deflater(config.getCompressionLevel());
		return myDeflater;
	}

	private void writeChecksum(PackOutputStream out) throws IOException {
		packcsum = out.getDigest();
		out.write(packcsum);
	}

	private void findObjectsToPack(final ProgressMonitor countingMonitor,
			final ObjectWalk walker, final Set<? extends ObjectId> want,
			Set<? extends ObjectId> have)
			throws MissingObjectException, IOException,
			IncorrectObjectTypeException {
		final long countingStart = System.currentTimeMillis();
		beginPhase(PackingPhase.COUNTING, countingMonitor, ProgressMonitor.UNKNOWN);

		if (have == null)
			have = Collections.emptySet();

		stats.interestingObjects = Collections.unmodifiableSet(new HashSet<ObjectId>(want));
		stats.uninterestingObjects = Collections.unmodifiableSet(new HashSet<ObjectId>(have));

		walker.setRetainBody(false);

		canBuildBitmaps = config.isBuildBitmaps()
				&& !shallowPack
				&& have.isEmpty()
				&& (excludeInPacks == null || excludeInPacks.length == 0);
		if (!shallowPack && useBitmaps) {
			BitmapIndex bitmapIndex = reader.getBitmapIndex();
			if (bitmapIndex != null) {
				PackWriterBitmapWalker bitmapWalker = new PackWriterBitmapWalker(
						walker, bitmapIndex, countingMonitor);
				findObjectsToPackUsingBitmaps(bitmapWalker, want, have);
				endPhase(countingMonitor);
				stats.timeCounting = System.currentTimeMillis() - countingStart;
				return;
			}
		}

		List<ObjectId> all = new ArrayList<ObjectId>(want.size() + have.size());
		all.addAll(want);
		all.addAll(have);

		final RevFlag include = walker.newFlag("include"); //$NON-NLS-1$
		final RevFlag added = walker.newFlag("added"); //$NON-NLS-1$

		walker.carry(include);

		int haveEst = have.size();
		if (have.isEmpty()) {
			walker.sort(RevSort.COMMIT_TIME_DESC);
		} else {
			walker.sort(RevSort.TOPO);
			if (thin)
				walker.sort(RevSort.BOUNDARY, true);
		}

		List<RevObject> wantObjs = new ArrayList<RevObject>(want.size());
		List<RevObject> haveObjs = new ArrayList<RevObject>(haveEst);
		List<RevTag> wantTags = new ArrayList<RevTag>(want.size());

		AsyncRevObjectQueue q = walker.parseAny(all, true);
		try {
			for (;;) {
				try {
					RevObject o = q.next();
					if (o == null)
						break;
					if (have.contains(o))
						haveObjs.add(o);
					if (want.contains(o)) {
						o.add(include);
						wantObjs.add(o);
						if (o instanceof RevTag)
							wantTags.add((RevTag) o);
					}
				} catch (MissingObjectException e) {
					if (ignoreMissingUninteresting
							&& have.contains(e.getObjectId()))
						continue;
					throw e;
				}
			}
		} finally {
			q.release();
		}

		if (!wantTags.isEmpty()) {
			all = new ArrayList<ObjectId>(wantTags.size());
			for (RevTag tag : wantTags)
				all.add(tag.getObject());
			q = walker.parseAny(all, true);
			try {
				while (q.next() != null) {
					// Just need to pop the queue item to parse the object.
				}
			} finally {
				q.release();
			}
		}

		if (walker instanceof DepthWalk.ObjectWalk) {
			DepthWalk.ObjectWalk depthWalk = (DepthWalk.ObjectWalk) walker;
			for (RevObject obj : wantObjs)
				depthWalk.markRoot(obj);
			if (unshallowObjects != null) {
				for (ObjectId id : unshallowObjects)
					depthWalk.markUnshallow(walker.parseAny(id));
			}
		} else {
			for (RevObject obj : wantObjs)
				walker.markStart(obj);
		}
		for (RevObject obj : haveObjs)
			walker.markUninteresting(obj);

		final int maxBases = config.getDeltaSearchWindowSize();
		Set<RevTree> baseTrees = new HashSet<RevTree>();
		BlockList<RevCommit> commits = new BlockList<RevCommit>();
		RevCommit c;
		while ((c = walker.next()) != null) {
			if (exclude(c))
				continue;
			if (c.has(RevFlag.UNINTERESTING)) {
				if (baseTrees.size() <= maxBases)
					baseTrees.add(c.getTree());
				continue;
			}

			commits.add(c);
			countingMonitor.update(1);
		}

		if (shallowPack) {
			for (RevCommit cmit : commits) {
				addObject(cmit, 0);
			}
		} else {
			int commitCnt = 0;
			boolean putTagTargets = false;
			for (RevCommit cmit : commits) {
				if (!cmit.has(added)) {
					cmit.add(added);
					addObject(cmit, 0);
					commitCnt++;
				}

				for (int i = 0; i < cmit.getParentCount(); i++) {
					RevCommit p = cmit.getParent(i);
					if (!p.has(added) && !p.has(RevFlag.UNINTERESTING)
							&& !exclude(p)) {
						p.add(added);
						addObject(p, 0);
						commitCnt++;
					}
				}

				if (!putTagTargets && 4096 < commitCnt) {
					for (ObjectId id : tagTargets) {
						RevObject obj = walker.lookupOrNull(id);
						if (obj instanceof RevCommit
								&& obj.has(include)
								&& !obj.has(RevFlag.UNINTERESTING)
								&& !obj.has(added)) {
							obj.add(added);
							addObject(obj, 0);
						}
					}
					putTagTargets = true;
				}
			}
		}
		commits = null;

		if (thin && !baseTrees.isEmpty()) {
			BaseSearch bases = new BaseSearch(countingMonitor, baseTrees, //
					objectsMap, edgeObjects, reader);
			RevObject o;
			while ((o = walker.nextObject()) != null) {
				if (o.has(RevFlag.UNINTERESTING))
					continue;
				if (exclude(o))
					continue;

				int pathHash = walker.getPathHashCode();
				byte[] pathBuf = walker.getPathBuffer();
				int pathLen = walker.getPathLength();
				bases.addBase(o.getType(), pathBuf, pathLen, pathHash);
				addObject(o, pathHash);
				countingMonitor.update(1);
			}
		} else {
			RevObject o;
			while ((o = walker.nextObject()) != null) {
				if (o.has(RevFlag.UNINTERESTING))
					continue;
				if (exclude(o))
					continue;
				addObject(o, walker.getPathHashCode());
				countingMonitor.update(1);
			}
		}

		for (CachedPack pack : cachedPacks)
			countingMonitor.update((int) pack.getObjectCount());
		endPhase(countingMonitor);
		stats.timeCounting = System.currentTimeMillis() - countingStart;
	}

	private void findObjectsToPackUsingBitmaps(
			PackWriterBitmapWalker bitmapWalker, Set<? extends ObjectId> want,
			Set<? extends ObjectId> have)
			throws MissingObjectException, IncorrectObjectTypeException,
			IOException {
		BitmapBuilder haveBitmap = bitmapWalker.findObjects(have, null);
		bitmapWalker.reset();
		BitmapBuilder wantBitmap = bitmapWalker.findObjects(want, haveBitmap);
		BitmapBuilder needBitmap = wantBitmap.andNot(haveBitmap);

		if (useCachedPacks && reuseSupport != null
				&& (excludeInPacks == null || excludeInPacks.length == 0))
			cachedPacks.addAll(
					reuseSupport.getCachedPacksAndUpdate(needBitmap));

		for (BitmapObject obj : needBitmap) {
			ObjectId objectId = obj.getObjectId();
			if (exclude(objectId)) {
				needBitmap.remove(objectId);
				continue;
			}
			addObject(objectId, obj.getType(), 0);
		}

		if (thin)
			haveObjects = haveBitmap;
	}

	private static void pruneEdgesFromObjectList(List<ObjectToPack> list) {
		final int size = list.size();
		int src = 0;
		int dst = 0;

		for (; src < size; src++) {
			ObjectToPack obj = list.get(src);
			if (obj.isEdge())
				continue;
			if (dst != src)
				list.set(dst, obj);
			dst++;
		}

		while (dst < list.size())
			list.remove(list.size() - 1);
	}

	/**
	 * Include one object to the output file.
	 * <p>
	 * Objects are written in the order they are added. If the same object is
	 * added twice, it may be written twice, creating a larger than necessary
	 * file.
	 *
	 * @param object
	 *            the object to add.
	 * @throws IncorrectObjectTypeException
	 *             the object is an unsupported type.
	 */
	public void addObject(final RevObject object)
			throws IncorrectObjectTypeException {
		if (!exclude(object))
			addObject(object, 0);
	}

	private void addObject(final RevObject object, final int pathHashCode) {
		addObject(object, object.getType(), pathHashCode);
	}

	private void addObject(
			final AnyObjectId src, final int type, final int pathHashCode) {
		final ObjectToPack otp;
		if (reuseSupport != null)
			otp = reuseSupport.newObjectToPack(src, type);
		else
			otp = new ObjectToPack(src, type);
		otp.setPathHash(pathHashCode);
		objectsLists[type].add(otp);
		objectsMap.add(otp);
	}

	private boolean exclude(AnyObjectId objectId) {
		if (excludeInPacks == null)
			return false;
		if (excludeInPackLast.contains(objectId))
			return true;
		for (ObjectIdSet idx : excludeInPacks) {
			if (idx.contains(objectId)) {
				excludeInPackLast = idx;
				return true;
			}
		}
		return false;
	}

	/**
	 * Select an object representation for this writer.
	 * <p>
	 * An {@link ObjectReader} implementation should invoke this method once for
	 * each representation available for an object, to allow the writer to find
	 * the most suitable one for the output.
	 *
	 * @param otp
	 *            the object being packed.
	 * @param next
	 *            the next available representation from the repository.
	 */
	public void select(ObjectToPack otp, StoredObjectRepresentation next) {
		int nFmt = next.getFormat();

		if (!cachedPacks.isEmpty()) {
			if (otp.isEdge())
				return;
			if ((nFmt == PACK_WHOLE) | (nFmt == PACK_DELTA)) {
				for (CachedPack pack : cachedPacks) {
					if (pack.hasObject(otp, next)) {
						otp.setEdge();
						otp.clearDeltaBase();
						otp.clearReuseAsIs();
						pruneCurrentObjectList = true;
						return;
					}
				}
			}
		}

		if (nFmt == PACK_DELTA && reuseDeltas && reuseDeltaFor(otp)) {
			ObjectId baseId = next.getDeltaBase();
			ObjectToPack ptr = objectsMap.get(baseId);
			if (ptr != null && !ptr.isEdge()) {
				otp.setDeltaBase(ptr);
				otp.setReuseAsIs();
			} else if (thin && have(ptr, baseId)) {
				otp.setDeltaBase(baseId);
				otp.setReuseAsIs();
			} else {
				otp.clearDeltaBase();
				otp.clearReuseAsIs();
			}
		} else if (nFmt == PACK_WHOLE && config.isReuseObjects()) {
			int nWeight = next.getWeight();
			if (otp.isReuseAsIs() && !otp.isDeltaRepresentation()) {
				// We've chosen another PACK_WHOLE format for this object,
				// choose the one that has the smaller compressed size.
				//
				if (otp.getWeight() <= nWeight)
					return;
			}
			otp.clearDeltaBase();
			otp.setReuseAsIs();
			otp.setWeight(nWeight);
		} else {
			otp.clearDeltaBase();
			otp.clearReuseAsIs();
		}

		otp.setDeltaAttempted(reuseDeltas & next.wasDeltaAttempted());
		otp.select(next);
	}

	private final boolean have(ObjectToPack ptr, AnyObjectId objectId) {
		return (ptr != null && ptr.isEdge())
				|| (haveObjects != null && haveObjects.contains(objectId));
	}

	/**
	 * Prepares the bitmaps to be written to the pack index. Bitmaps can be used
	 * to speed up fetches and clones by storing the entire object graph at
	 * selected commits.
	 *
	 * This method can only be invoked after
	 * {@link #writePack(ProgressMonitor, ProgressMonitor, OutputStream)} has
	 * been invoked and completed successfully. Writing a corresponding bitmap
	 * index is an optional feature that not all pack users may require.
	 *
	 * @param pm
	 *            progress monitor to report bitmap building work.
	 * @return whether a bitmap index may be written.
	 * @throws IOException
	 *             when some I/O problem occur during reading objects.
	 */
	public boolean prepareBitmapIndex(ProgressMonitor pm) throws IOException {
		if (!canBuildBitmaps || getObjectCount() > Integer.MAX_VALUE
				|| !cachedPacks.isEmpty())
			return false;

		if (pm == null)
			pm = NullProgressMonitor.INSTANCE;

		writeBitmaps = new PackBitmapIndexBuilder(sortByName());
		PackWriterBitmapPreparer bitmapPreparer = new PackWriterBitmapPreparer(
				reader, writeBitmaps, pm, stats.interestingObjects);

		int numCommits = objectsLists[OBJ_COMMIT].size();
		Collection<PackWriterBitmapPreparer.BitmapCommit> selectedCommits =
				bitmapPreparer.doCommitSelection(numCommits);

		beginPhase(PackingPhase.BUILDING_BITMAPS, pm, selectedCommits.size());

		PackWriterBitmapWalker walker = bitmapPreparer.newBitmapWalker();
		AnyObjectId last = null;
		for (PackWriterBitmapPreparer.BitmapCommit cmit : selectedCommits) {
			if (cmit.isReuseWalker())
				walker.reset();
			else
				walker = bitmapPreparer.newBitmapWalker();

			BitmapBuilder bitmap = walker.findObjects(
					Collections.singleton(cmit), null);

			if (last != null && cmit.isReuseWalker() && !bitmap.contains(last))
				throw new IllegalStateException(MessageFormat.format(
						JGitText.get().bitmapMissingObject, cmit.name(),
						last.name()));
			last = cmit;
			writeBitmaps.addBitmap(cmit, bitmap.build(), cmit.getFlags());

			pm.update(1);
		}

		endPhase(pm);
		return true;
	}

	private boolean reuseDeltaFor(ObjectToPack otp) {
		int type = otp.getType();
		if ((type & 2) != 0) // OBJ_TREE(2) or OBJ_BLOB(3)
			return true;
		if (type == OBJ_COMMIT)
			return reuseDeltaCommits;
		if (type == OBJ_TAG)
			return false;
		return true;
	}

	/** Summary of how PackWriter created the pack. */
	public static class Statistics {
		/** Statistics about a single class of object. */
		public static class ObjectType {
			long cntObjects;

			long cntDeltas;

			long reusedObjects;

			long reusedDeltas;

			long bytes;

			long deltaBytes;

			/**
			 * @return total number of objects output. This total includes the
			 *         value of {@link #getDeltas()}.
			 */
			public long getObjects() {
				return cntObjects;
			}

			/**
			 * @return total number of deltas output. This may be lower than the
			 *         actual number of deltas if a cached pack was reused.
			 */
			public long getDeltas() {
				return cntDeltas;
			}

			/**
			 * @return number of objects whose existing representation was
			 *         reused in the output. This count includes
			 *         {@link #getReusedDeltas()}.
			 */
			public long getReusedObjects() {
				return reusedObjects;
			}

			/**
			 * @return number of deltas whose existing representation was reused
			 *         in the output, as their base object was also output or
			 *         was assumed present for a thin pack. This may be lower
			 *         than the actual number of reused deltas if a cached pack
			 *         was reused.
			 */
			public long getReusedDeltas() {
				return reusedDeltas;
			}

			/**
			 * @return total number of bytes written. This size includes the
			 *         object headers as well as the compressed data. This size
			 *         also includes all of {@link #getDeltaBytes()}.
			 */
			public long getBytes() {
				return bytes;
			}

			/**
			 * @return number of delta bytes written. This size includes the
			 *         object headers for the delta objects.
			 */
			public long getDeltaBytes() {
				return deltaBytes;
			}
		}

		Set<ObjectId> interestingObjects;

		Set<ObjectId> uninterestingObjects;

		Collection<CachedPack> reusedPacks;

		int depth;

		int deltaSearchNonEdgeObjects;

		int deltasFound;

		long totalObjects;

		long totalDeltas;

		long reusedObjects;

		long reusedDeltas;

		long totalBytes;

		long thinPackBytes;

		long timeCounting;

		long timeSearchingForReuse;

		long timeSearchingForSizes;

		long timeCompressing;

		long timeWriting;

		ObjectType[] objectTypes;

		{
			objectTypes = new ObjectType[5];
			objectTypes[OBJ_COMMIT] = new ObjectType();
			objectTypes[OBJ_TREE] = new ObjectType();
			objectTypes[OBJ_BLOB] = new ObjectType();
			objectTypes[OBJ_TAG] = new ObjectType();
		}

		/**
		 * @return unmodifiable collection of objects to be included in the
		 *         pack. May be null if the pack was hand-crafted in a unit
		 *         test.
		 */
		public Set<ObjectId> getInterestingObjects() {
			return interestingObjects;
		}

		/**
		 * @return unmodifiable collection of objects that should be excluded
		 *         from the pack, as the peer that will receive the pack already
		 *         has these objects.
		 */
		public Set<ObjectId> getUninterestingObjects() {
			return uninterestingObjects;
		}

		/**
		 * @return unmodifiable collection of the cached packs that were reused
		 *         in the output, if any were selected for reuse.
		 */
		public Collection<CachedPack> getReusedPacks() {
			return reusedPacks;
		}

		/**
		 * @return number of objects in the output pack that went through the
		 *         delta search process in order to find a potential delta base.
		 */
		public int getDeltaSearchNonEdgeObjects() {
			return deltaSearchNonEdgeObjects;
		}

		/**
		 * @return number of objects in the output pack that went through delta
		 *         base search and found a suitable base. This is a subset of
		 *         {@link #getDeltaSearchNonEdgeObjects()}.
		 */
		public int getDeltasFound() {
			return deltasFound;
		}

		/**
		 * @return total number of objects output. This total includes the value
		 *         of {@link #getTotalDeltas()}.
		 */
		public long getTotalObjects() {
			return totalObjects;
		}

		/**
		 * @return total number of deltas output. This may be lower than the
		 *         actual number of deltas if a cached pack was reused.
		 */
		public long getTotalDeltas() {
			return totalDeltas;
		}

		/**
		 * @return number of objects whose existing representation was reused in
		 *         the output. This count includes {@link #getReusedDeltas()}.
		 */
		public long getReusedObjects() {
			return reusedObjects;
		}

		/**
		 * @return number of deltas whose existing representation was reused in
		 *         the output, as their base object was also output or was
		 *         assumed present for a thin pack. This may be lower than the
		 *         actual number of reused deltas if a cached pack was reused.
		 */
		public long getReusedDeltas() {
			return reusedDeltas;
		}

		/**
		 * @return total number of bytes written. This size includes the pack
		 *         header, trailer, thin pack, and reused cached pack(s).
		 */
		public long getTotalBytes() {
			return totalBytes;
		}

		/**
		 * @return size of the thin pack in bytes, if a thin pack was generated.
		 *         A thin pack is created when the client already has objects
		 *         and some deltas are created against those objects, or if a
		 *         cached pack is being used and some deltas will reference
		 *         objects in the cached pack. This size does not include the
		 *         pack header or trailer.
		 */
		public long getThinPackBytes() {
			return thinPackBytes;
		}

		/**
		 * @param typeCode
		 *            object type code, e.g. OBJ_COMMIT or OBJ_TREE.
		 * @return information about this type of object in the pack.
		 */
		public ObjectType byObjectType(int typeCode) {
			return objectTypes[typeCode];
		}

		/** @return true if the resulting pack file was a shallow pack. */
		public boolean isShallow() {
			return depth > 0;
		}

		/** @return depth (in commits) the pack includes if shallow. */
		public int getDepth() {
			return depth;
		}

		/**
		 * @return time in milliseconds spent enumerating the objects that need
		 *         to be included in the output. This time includes any restarts
		 *         that occur when a cached pack is selected for reuse.
		 */
		public long getTimeCounting() {
			return timeCounting;
		}

		/**
		 * @return time in milliseconds spent matching existing representations
		 *         against objects that will be transmitted, or that the client
		 *         can be assumed to already have.
		 */
		public long getTimeSearchingForReuse() {
			return timeSearchingForReuse;
		}

		/**
		 * @return time in milliseconds spent finding the sizes of all objects
		 *         that will enter the delta compression search window. The
		 *         sizes need to be known to better match similar objects
		 *         together and improve delta compression ratios.
		 */
		public long getTimeSearchingForSizes() {
			return timeSearchingForSizes;
		}

		/**
		 * @return time in milliseconds spent on delta compression. This is
		 *         observed wall-clock time and does not accurately track CPU
		 *         time used when multiple threads were used to perform the
		 *         delta compression.
		 */
		public long getTimeCompressing() {
			return timeCompressing;
		}

		/**
		 * @return time in milliseconds spent writing the pack output, from
		 *         start of header until end of trailer. The transfer speed can
		 *         be approximated by dividing {@link #getTotalBytes()} by this
		 *         value.
		 */
		public long getTimeWriting() {
			return timeWriting;
		}

		/** @return total time spent processing this pack. */
		public long getTimeTotal() {
			return timeCounting
				+ timeSearchingForReuse
				+ timeSearchingForSizes
				+ timeCompressing
				+ timeWriting;
		}

		/**
		 * @return get the average output speed in terms of bytes-per-second.
		 *         {@code getTotalBytes() / (getTimeWriting() / 1000.0)}.
		 */
		public double getTransferRate() {
			return getTotalBytes() / (getTimeWriting() / 1000.0);
		}

		/** @return formatted message string for display to clients. */
		public String getMessage() {
			return MessageFormat.format(JGitText.get().packWriterStatistics, //
					Long.valueOf(totalObjects), Long.valueOf(totalDeltas), //
					Long.valueOf(reusedObjects), Long.valueOf(reusedDeltas));
		}
	}

	private class MutableState {
		/** Estimated size of a single ObjectToPack instance. */
		// Assume 64-bit pointers, since this is just an estimate.
		private static final long OBJECT_TO_PACK_SIZE =
				(2 * 8)               // Object header
				+ (2 * 8) + (2 * 8)   // ObjectToPack fields
				+ (8 + 8)             // PackedObjectInfo fields
				+ 8                   // ObjectIdOwnerMap fields
				+ 40                  // AnyObjectId fields
				+ 8;                  // Reference in BlockList

		private final long totalDeltaSearchBytes;

		private volatile PackingPhase phase;

		MutableState() {
			phase = PackingPhase.COUNTING;
			if (config.isDeltaCompress()) {
				int threads = config.getThreads();
				if (threads <= 0)
					threads = Runtime.getRuntime().availableProcessors();
				totalDeltaSearchBytes = (threads * config.getDeltaSearchMemoryLimit())
						+ config.getBigFileThreshold();
			} else
				totalDeltaSearchBytes = 0;
		}

		State snapshot() {
			long objCnt = 0;
			objCnt += objectsLists[OBJ_COMMIT].size();
			objCnt += objectsLists[OBJ_TREE].size();
			objCnt += objectsLists[OBJ_BLOB].size();
			objCnt += objectsLists[OBJ_TAG].size();
			// Exclude CachedPacks.

			long bytesUsed = OBJECT_TO_PACK_SIZE * objCnt;
			PackingPhase curr = phase;
			if (curr == PackingPhase.COMPRESSING)
				bytesUsed += totalDeltaSearchBytes;
			return new State(curr, bytesUsed);
		}
	}

	/** Possible states that a PackWriter can be in. */
	public static enum PackingPhase {
		/** Counting objects phase. */
		COUNTING,

		/** Getting sizes phase. */
		GETTING_SIZES,

		/** Finding sources phase. */
		FINDING_SOURCES,

		/** Compressing objects phase. */
		COMPRESSING,

		/** Writing objects phase. */
		WRITING,

		/** Building bitmaps phase. */
		BUILDING_BITMAPS;
	}

	/** Summary of the current state of a PackWriter. */
	public class State {
		private final PackingPhase phase;

		private final long bytesUsed;

		State(PackingPhase phase, long bytesUsed) {
			this.phase = phase;
			this.bytesUsed = bytesUsed;
		}

		/** @return the PackConfig used to build the writer. */
		public PackConfig getConfig() {
			return config;
		}

		/** @return the current phase of the writer. */
		public PackingPhase getPhase() {
			return phase;
		}

		/** @return an estimate of the total memory used by the writer. */
		public long estimateBytesUsed() {
			return bytesUsed;
		}

		@SuppressWarnings("nls")
		@Override
		public String toString() {
			return "PackWriter.State[" + phase + ", memory=" + bytesUsed + "]";
		}
	}
}
