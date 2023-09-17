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

package org.eclipse.jgit.storage.pack;

import static org.eclipse.jgit.storage.pack.StoredObjectRepresentation.PACK_DELTA;
import static org.eclipse.jgit.storage.pack.StoredObjectRepresentation.PACK_WHOLE;

import java.io.IOException;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;

import org.eclipse.jgit.JGitText;
import org.eclipse.jgit.errors.CorruptObjectException;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.LargeObjectException;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.errors.StoredObjectRepresentationNotAvailableException;
import org.eclipse.jgit.lib.AnyObjectId;
import org.eclipse.jgit.lib.AsyncObjectSizeQueue;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.NullProgressMonitor;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectIdSubclassMap;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.ProgressMonitor;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.ThreadSafeProgressMonitor;
import org.eclipse.jgit.revwalk.AsyncRevObjectQueue;
import org.eclipse.jgit.revwalk.ObjectWalk;
import org.eclipse.jgit.revwalk.RevFlag;
import org.eclipse.jgit.revwalk.RevObject;
import org.eclipse.jgit.revwalk.RevSort;
import org.eclipse.jgit.storage.file.PackIndexWriter;
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

	@SuppressWarnings("unchecked")
	private final List<ObjectToPack> objectsLists[] = new List[Constants.OBJ_TAG + 1];
	{
		objectsLists[0] = Collections.<ObjectToPack> emptyList();
		objectsLists[Constants.OBJ_COMMIT] = new ArrayList<ObjectToPack>();
		objectsLists[Constants.OBJ_TREE] = new ArrayList<ObjectToPack>();
		objectsLists[Constants.OBJ_BLOB] = new ArrayList<ObjectToPack>();
		objectsLists[Constants.OBJ_TAG] = new ArrayList<ObjectToPack>();
	}

	private final ObjectIdSubclassMap<ObjectToPack> objectsMap = new ObjectIdSubclassMap<ObjectToPack>();

	// edge objects for thin packs
	private final ObjectIdSubclassMap<ObjectToPack> edgeObjects = new ObjectIdSubclassMap<ObjectToPack>();

	private Deflater myDeflater;

	private final ObjectReader reader;

	/** {@link #reader} recast to the reuse interface, if it supports it. */
	private final ObjectReuseAsIs reuseSupport;

	private final PackConfig config;

	private List<ObjectToPack> sortedByName;

	private byte packcsum[];

	private boolean deltaBaseAsOffset;

	private boolean reuseDeltas;

	private boolean thin;

	private boolean ignoreMissingUninteresting = true;

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
	 * Returns objects number in a pack file that was created by this writer.
	 *
	 * @return number of objects in pack.
	 */
	public int getObjectsNumber() {
		return objectsMap.size();
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
	 * <p>
	 * When iterator returns object that has {@link RevFlag#UNINTERESTING} flag,
	 * this object won't be included in an output pack. Instead, it is recorded
	 * as edge-object (known to remote repository) for thin-pack. In such a case
	 * writer may pack objects with delta base object not within set of objects
	 * to pack, but belonging to party repository - those marked with
	 * {@link RevFlag#UNINTERESTING} flag. This type of pack is used only for
	 * transport.
	 * </p>
	 *
	 * @param objectsSource
	 *            iterator of object to store in a pack; order of objects within
	 *            each type is important, ordering by type is not needed;
	 *            allowed types for objects are {@link Constants#OBJ_COMMIT},
	 *            {@link Constants#OBJ_TREE}, {@link Constants#OBJ_BLOB} and
	 *            {@link Constants#OBJ_TAG}; objects returned by iterator may
	 *            be later reused by caller as object id and type are internally
	 *            copied in each iteration; if object returned by iterator has
	 *            {@link RevFlag#UNINTERESTING} flag set, it won't be included
	 *            in a pack, but is considered as edge-object for thin-pack.
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
			final Collection<? extends ObjectId> interestingObjects,
			final Collection<? extends ObjectId> uninterestingObjects)
			throws IOException {
		if (countingMonitor == null)
			countingMonitor = NullProgressMonitor.INSTANCE;
		ObjectWalk walker = setUpWalker(interestingObjects,
				uninterestingObjects);
		findObjectsToPack(countingMonitor, walker);
	}

	/**
	 * Determine if the pack file will contain the requested object.
	 *
	 * @param id
	 *            the object to test the existence of.
	 * @return true if the object will appear in the output pack file.
	 */
	public boolean willInclude(final AnyObjectId id) {
		return get(id) != null;
	}

	/**
	 * Lookup the ObjectToPack object for a given ObjectId.
	 *
	 * @param id
	 *            the object to find in the pack.
	 * @return the object we are packing, or null.
	 */
	public ObjectToPack get(AnyObjectId id) {
		return objectsMap.get(id);
	}

	/**
	 * Computes SHA-1 of lexicographically sorted objects ids written in this
	 * pack, as used to name a pack file in repository.
	 *
	 * @return ObjectId representing SHA-1 name of a pack that was created.
	 */
	public ObjectId computeName() {
		final byte[] buf = new byte[Constants.OBJECT_ID_LENGTH];
		final MessageDigest md = Constants.newMessageDigest();
		for (ObjectToPack otp : sortByName()) {
			otp.copyRawTo(buf, 0);
			md.update(buf, 0, Constants.OBJECT_ID_LENGTH);
		}
		return ObjectId.fromRaw(md.digest());
	}

	/**
	 * Create an index file to match the pack file just written.
	 * <p>
	 * This method can only be invoked after {@link #preparePack(Iterator)} or
	 * {@link #preparePack(ProgressMonitor, Collection, Collection)} has been
	 * invoked and completed successfully. Writing a corresponding index is an
	 * optional feature that not all pack users may require.
	 *
	 * @param indexStream
	 *            output for the index data. Caller is responsible for closing
	 *            this stream.
	 * @throws IOException
	 *             the index data could not be written to the supplied stream.
	 */
	public void writeIndex(final OutputStream indexStream) throws IOException {
		final List<ObjectToPack> list = sortByName();
		final PackIndexWriter iw;
		int indexVersion = config.getIndexVersion();
		if (indexVersion <= 0)
			iw = PackIndexWriter.createOldestPossible(indexStream, list);
		else
			iw = PackIndexWriter.createVersion(indexStream, indexVersion);
		iw.write(list, packcsum);
	}

	private List<ObjectToPack> sortByName() {
		if (sortedByName == null) {
			sortedByName = new ArrayList<ObjectToPack>(objectsMap.size());
			for (List<ObjectToPack> list : objectsLists) {
				for (ObjectToPack otp : list)
					sortedByName.add(otp);
			}
			Collections.sort(sortedByName);
		}
		return sortedByName;
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

		if ((reuseDeltas || config.isReuseObjects()) && reuseSupport != null)
			searchForReuse(compressMonitor);
		if (config.isDeltaCompress())
			searchForDeltas(compressMonitor);

		final PackOutputStream out = new PackOutputStream(writeMonitor,
				packStream, this);

		int objCnt = getObjectsNumber();
		writeMonitor.beginTask(JGitText.get().writingObjects, objCnt);
		out.writeFileHeader(PACK_VERSION_GENERATED, objCnt);
		out.flush();
		writeObjects(out);
		writeChecksum(out);

		reader.release();
		writeMonitor.endTask();
	}

	/** Release all resources used by this writer. */
	public void release() {
		reader.release();
		if (myDeflater != null) {
			myDeflater.end();
			myDeflater = null;
		}
	}

	private void searchForReuse(ProgressMonitor monitor) throws IOException {
		monitor.beginTask(JGitText.get().searchForReuse, getObjectsNumber());
		for (List<ObjectToPack> list : objectsLists)
			reuseSupport.selectObjectRepresentation(this, monitor, list);
		monitor.endTask();
	}

	private void searchForDeltas(ProgressMonitor monitor)
			throws MissingObjectException, IncorrectObjectTypeException,
			IOException {
		// Commits and annotated tags tend to have too many differences to
		// really benefit from delta compression. Consequently just don't
		// bother examining those types here.
		//
		ObjectToPack[] list = new ObjectToPack[
				  objectsLists[Constants.OBJ_TREE].size()
				+ objectsLists[Constants.OBJ_BLOB].size()
				+ edgeObjects.size()];
		int cnt = 0;
		cnt = findObjectsNeedingDelta(list, cnt, Constants.OBJ_TREE);
		cnt = findObjectsNeedingDelta(list, cnt, Constants.OBJ_BLOB);
		if (cnt == 0)
			return;

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
		monitor.beginTask(JGitText.get().compressingObjects, cnt);
		AsyncObjectSizeQueue<ObjectToPack> sizeQueue = reader.getObjectSize(
				Arrays.<ObjectToPack> asList(list).subList(0, cnt), false);
		try {
			final long limit = config.getBigFileThreshold();
			for (;;) {
				monitor.update(1);

				try {
					if (!sizeQueue.next())
						break;
				} catch (MissingObjectException notFound) {
					if (ignoreMissingUninteresting) {
						ObjectToPack otp = sizeQueue.getCurrent();
						if (otp != null && otp.isEdge()) {
							otp.setDoNotDelta(true);
							continue;
						}

						otp = edgeObjects.get(notFound.getObjectId());
						if (otp != null) {
							otp.setDoNotDelta(true);
							continue;
						}
					}
					throw notFound;
				}

				ObjectToPack otp = sizeQueue.getCurrent();
				if (otp == null) {
					otp = objectsMap.get(sizeQueue.getObjectId());
					if (otp == null)
						otp = edgeObjects.get(sizeQueue.getObjectId());
				}

				long sz = sizeQueue.getSize();
				if (limit <= sz || Integer.MAX_VALUE <= sz)
					otp.setDoNotDelta(true); // too big, avoid costly files

				else if (sz <= DeltaIndex.BLKSZ)
					otp.setDoNotDelta(true); // too small, won't work

				else
					otp.setWeight((int) sz);
			}
		} finally {
			sizeQueue.release();
		}
		monitor.endTask();

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

				return b.getWeight() - a.getWeight();
			}
		});

		// Above we stored the objects we cannot delta onto the end.
		// Remove them from the list so we don't waste time on them.
		while (0 < cnt && list[cnt - 1].isDoNotDelta())
			cnt--;
		if (cnt == 0)
			return;

		monitor.beginTask(JGitText.get().compressingObjects, cnt);
		searchForDeltas(monitor, list, cnt);
		monitor.endTask();
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

	private void searchForDeltas(final ProgressMonitor monitor,
			final ObjectToPack[] list, final int cnt)
			throws MissingObjectException, IncorrectObjectTypeException,
			LargeObjectException, IOException {
		int threads = config.getThreads();
		if (threads == 0)
			threads = Runtime.getRuntime().availableProcessors();

		if (threads <= 1 || cnt <= 2 * config.getDeltaSearchWindowSize()) {
			DeltaCache dc = new DeltaCache(config);
			DeltaWindow dw = new DeltaWindow(config, dc, reader);
			dw.search(monitor, list, 0, cnt);
			return;
		}

		final DeltaCache dc = new ThreadSafeDeltaCache(config);
		final ThreadSafeProgressMonitor pm = new ThreadSafeProgressMonitor(monitor);

		// Guess at the size of batch we want. Because we don't really
		// have a way for a thread to steal work from another thread if
		// it ends early, we over partition slightly so the work units
		// are a bit smaller.
		//
		int estSize = cnt / (threads * 2);
		if (estSize < 2 * config.getDeltaSearchWindowSize())
			estSize = 2 * config.getDeltaSearchWindowSize();

		final List<DeltaTask> myTasks = new ArrayList<DeltaTask>(threads * 2);
		for (int i = 0; i < cnt;) {
			final int start = i;
			final int batchSize;

			if (cnt - i < estSize) {
				// If we don't have enough to fill the remaining block,
				// schedule what is left over as a single block.
				//
				batchSize = cnt - i;
			} else {
				// Try to split the block at the end of a path.
				//
				int end = start + estSize;
				while (end < cnt) {
					ObjectToPack a = list[end - 1];
					ObjectToPack b = list[end];
					if (a.getPathHash() == b.getPathHash())
						end++;
					else
						break;
				}
				batchSize = end - start;
			}
			i += batchSize;
			myTasks.add(new DeltaTask(config, reader, dc, pm, batchSize, start, list));
		}
		pm.startWorkers(myTasks.size());

		final Executor executor = config.getExecutor();
		final List<Throwable> errors = Collections
				.synchronizedList(new ArrayList<Throwable>());
		if (executor instanceof ExecutorService) {
			// Caller supplied us a service, use it directly.
			//
			runTasks((ExecutorService) executor, pm, myTasks, errors);

		} else if (executor == null) {
			// Caller didn't give us a way to run the tasks, spawn up a
			// temporary thread pool and make sure it tears down cleanly.
			//
			ExecutorService pool = Executors.newFixedThreadPool(threads);
			try {
				runTasks(pool, pm, myTasks, errors);
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
			//
			for (final DeltaTask task : myTasks) {
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
	}

	private void runTasks(ExecutorService pool, ThreadSafeProgressMonitor pm,
			List<DeltaTask> tasks, List<Throwable> errors) throws IOException {
		List<Future<?>> futures = new ArrayList<Future<?>>(tasks.size());
		for (DeltaTask task : tasks)
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
		if (reuseSupport != null) {
			for (List<ObjectToPack> list : objectsLists)
				reuseSupport.writeObjects(out, list);
		} else {
			for (List<ObjectToPack> list : objectsLists) {
				for (ObjectToPack otp : list)
					out.writeObject(otp);
			}
		}
	}

	void writeObject(PackOutputStream out, ObjectToPack otp) throws IOException {
		if (otp.isWritten())
			return; // We shouldn't be here.

		otp.markWantWrite();
		if (otp.isDeltaRepresentation())
			writeBaseFirst(out, otp);

		out.resetCRC32();
		otp.setOffset(out.length());

		while (otp.isReuseAsIs()) {
			try {
				reuseSupport.copyObjectAsIs(out, otp);
				out.endObject();
				otp.setCRC(out.getCRC32());
				return;
			} catch (StoredObjectRepresentationNotAvailableException gone) {
				if (otp.getOffset() == out.length()) {
					redoSearchForReuse(otp);
					continue;
				} else {
					// Object writing already started, we cannot recover.
					//
					CorruptObjectException coe;
					coe = new CorruptObjectException(otp, "");
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
		otp.setCRC(out.getCRC32());
	}

	private void writeBaseFirst(PackOutputStream out, final ObjectToPack otp)
			throws IOException {
		ObjectToPack baseInPack = otp.getDeltaBase();
		if (baseInPack != null) {
			if (!baseInPack.isWritten()) {
				if (baseInPack.wantWrite()) {
					// There is a cycle. Our caller is trying to write the
					// object we want as a base, and called us. Turn off
					// delta reuse so we can find another form.
					//
					reuseDeltas = false;
					redoSearchForReuse(otp);
					reuseDeltas = true;
				} else {
					writeObject(out, baseInPack);
				}
			}
		} else if (!thin) {
			// This should never occur, the base isn't in the pack and
			// the pack isn't allowed to reference base outside objects.
			// Write the object as a whole form, even if that is slow.
			//
			otp.clearDeltaBase();
			otp.clearReuseAsIs();
		}
	}

	private void redoSearchForReuse(final ObjectToPack otp) throws IOException,
			MissingObjectException {
		otp.clearDeltaBase();
		otp.clearReuseAsIs();
		reuseSupport.selectObjectRepresentation(this,
				NullProgressMonitor.INSTANCE, Collections.singleton(otp));
	}

	private void writeWholeObjectDeflate(PackOutputStream out,
			final ObjectToPack otp) throws IOException {
		final Deflater deflater = deflater();
		final ObjectLoader ldr = reader.open(otp, otp.getType());

		out.writeHeader(otp, ldr.getSize());

		deflater.reset();
		DeflaterOutputStream dst = new DeflaterOutputStream(out, deflater);
		ldr.copyTo(dst);
		dst.finish();
	}

	private void writeDeltaObjectDeflate(PackOutputStream out,
			final ObjectToPack otp) throws IOException {
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

	private ObjectWalk setUpWalker(
			final Collection<? extends ObjectId> interestingObjects,
			final Collection<? extends ObjectId> uninterestingObjects)
			throws MissingObjectException, IOException,
			IncorrectObjectTypeException {
		List<ObjectId> all = new ArrayList<ObjectId>(interestingObjects.size());
		for (ObjectId id : interestingObjects)
			all.add(id.copy());

		final Set<ObjectId> not;
		if (uninterestingObjects != null && !uninterestingObjects.isEmpty()) {
			not = new HashSet<ObjectId>();
			for (ObjectId id : uninterestingObjects)
				not.add(id.copy());
			all.addAll(not);
		} else
			not = Collections.emptySet();

		final ObjectWalk walker = new ObjectWalk(reader);
		walker.setRetainBody(false);
		if (not.isEmpty())
			walker.sort(RevSort.COMMIT_TIME_DESC);
		else
			walker.sort(RevSort.TOPO);
		if (thin && !not.isEmpty())
			walker.sort(RevSort.BOUNDARY, true);

		AsyncRevObjectQueue q = walker.parseAny(all, true);
		try {
			for (;;) {
				try {
					RevObject o = q.next();
					if (o == null)
						break;
					if (not.contains(o.copy()))
						walker.markUninteresting(o);
					else
						walker.markStart(o);
				} catch (MissingObjectException e) {
					if (ignoreMissingUninteresting
							&& not.contains(e.getObjectId()))
						continue;
					throw e;
				}
			}
		} finally {
			q.release();
		}
		return walker;
	}

	private void findObjectsToPack(final ProgressMonitor countingMonitor,
			final ObjectWalk walker) throws MissingObjectException,
			IncorrectObjectTypeException,			IOException {
		countingMonitor.beginTask(JGitText.get().countingObjects,
				ProgressMonitor.UNKNOWN);
		RevObject o;

		while ((o = walker.next()) != null) {
			addObject(o, 0);
			countingMonitor.update(1);
		}
		while ((o = walker.nextObject()) != null) {
			addObject(o, walker.getPathHashCode());
			countingMonitor.update(1);
		}
		countingMonitor.endTask();
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
		addObject(object, 0);
	}

	private void addObject(final RevObject object, final int pathHashCode)
			throws IncorrectObjectTypeException {
		if (object.has(RevFlag.UNINTERESTING)) {
			switch (object.getType()) {
			case Constants.OBJ_TREE:
			case Constants.OBJ_BLOB:
				ObjectToPack otp = new ObjectToPack(object);
				otp.setPathHash(pathHashCode);
				otp.setEdge();
				edgeObjects.add(otp);
				thin = true;
				break;
			}
			return;
		}

		final ObjectToPack otp;
		if (reuseSupport != null)
			otp = reuseSupport.newObjectToPack(object);
		else
			otp = new ObjectToPack(object);
		otp.setPathHash(pathHashCode);

		try {
			objectsLists[object.getType()].add(otp);
		} catch (ArrayIndexOutOfBoundsException x) {
			throw new IncorrectObjectTypeException(object,
					JGitText.get().incorrectObjectType_COMMITnorTREEnorBLOBnorTAG);
		} catch (UnsupportedOperationException x) {
			// index pointing to "dummy" empty list
			throw new IncorrectObjectTypeException(object,
					JGitText.get().incorrectObjectType_COMMITnorTREEnorBLOBnorTAG);
		}
		objectsMap.add(otp);
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
		int nWeight;
		if (otp.isReuseAsIs()) {
			// We've already chosen to reuse a packed form, if next
			// cannot beat that break out early.
			//
			if (PACK_WHOLE < nFmt)
				return; // next isn't packed
			else if (PACK_DELTA < nFmt && otp.isDeltaRepresentation())
				return; // next isn't a delta, but we are

			nWeight = next.getWeight();
			if (otp.getWeight() <= nWeight)
				return; // next would be bigger
		} else
			nWeight = next.getWeight();

		if (nFmt == PACK_DELTA && reuseDeltas) {
			ObjectId baseId = next.getDeltaBase();
			ObjectToPack ptr = objectsMap.get(baseId);
			if (ptr != null) {
				otp.setDeltaBase(ptr);
				otp.setReuseAsIs();
				otp.setWeight(nWeight);
			} else if (thin && edgeObjects.contains(baseId)) {
				otp.setDeltaBase(baseId);
				otp.setReuseAsIs();
				otp.setWeight(nWeight);
			} else {
				otp.clearDeltaBase();
				otp.clearReuseAsIs();
			}
		} else if (nFmt == PACK_WHOLE && config.isReuseObjects()) {
			otp.clearDeltaBase();
			otp.setReuseAsIs();
			otp.setWeight(nWeight);
		} else {
			otp.clearDeltaBase();
			otp.clearReuseAsIs();
		}

		otp.select(next);
	}
}
