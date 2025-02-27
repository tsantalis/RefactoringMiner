/*
 * Copyright (C) 2008, 2009 Google Inc.
 * Copyright (C) 2008, 2020 Shawn O. Pearce <spearce@spearce.org> and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0 which is available at
 * https://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package org.eclipse.jgit.internal.storage.file;

import java.io.IOException;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;
import java.util.Collections;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.concurrent.atomic.LongAdder;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import org.eclipse.jgit.annotations.NonNull;
import org.eclipse.jgit.internal.JGitText;
import org.eclipse.jgit.storage.file.WindowCacheConfig;
import org.eclipse.jgit.storage.file.WindowCacheStats;
import org.eclipse.jgit.util.Monitoring;

/**
 * Caches slices of a {@link org.eclipse.jgit.internal.storage.file.Pack} in
 * memory for faster read access.
 * <p>
 * The WindowCache serves as a Java based "buffer cache", loading segments of a
 * PackFile into the JVM heap prior to use. As JGit often wants to do reads of
 * only tiny slices of a file, the WindowCache tries to smooth out these tiny
 * reads into larger block-sized IO operations.
 * <p>
 * Whenever a cache miss occurs, {@link #load(Pack, long)} is invoked by
 * exactly one thread for the given <code>(PackFile,position)</code> key tuple.
 * This is ensured by an array of locks, with the tuple hashed to a lock
 * instance.
 * <p>
 * During a miss, older entries are evicted from the cache so long as
 * {@link #isFull()} returns true.
 * <p>
 * Its too expensive during object access to be 100% accurate with a least
 * recently used (LRU) algorithm. Strictly ordering every read is a lot of
 * overhead that typically doesn't yield a corresponding benefit to the
 * application.
 * <p>
 * This cache implements a loose LRU policy by randomly picking a window
 * comprised of roughly 10% of the cache, and evicting the oldest accessed entry
 * within that window.
 * <p>
 * Entities created by the cache are held under SoftReferences if option
 * {@code core.packedGitUseStrongRefs} is set to {@code false} in the git config
 * (this is the default) or by calling
 * {@link WindowCacheConfig#setPackedGitUseStrongRefs(boolean)}, permitting the
 * Java runtime's garbage collector to evict entries when heap memory gets low.
 * Most JREs implement a loose least recently used algorithm for this eviction.
 * When this option is set to {@code true} strong references are used which
 * means that Java gc cannot evict the WindowCache to reclaim memory. On the
 * other hand this provides more predictable performance since the cache isn't
 * flushed when used heap comes close to the maximum heap size.
 * <p>
 * The internal hash table does not expand at runtime, instead it is fixed in
 * size at cache creation time. The internal lock table used to gate load
 * invocations is also fixed in size.
 * <p>
 * The key tuple is passed through to methods as a pair of parameters rather
 * than as a single Object, thus reducing the transient memory allocations of
 * callers. It is more efficient to avoid the allocation, as we can't be 100%
 * sure that a JIT would be able to stack-allocate a key tuple.
 * <p>
 * This cache has an implementation rule such that:
 * <ul>
 * <li>{@link #load(Pack, long)} is invoked by at most one thread at a time
 * for a given <code>(PackFile,position)</code> tuple.</li>
 * <li>For every <code>load()</code> invocation there is exactly one
 * {@link #createRef(Pack, long, ByteWindow)} invocation to wrap a
 * SoftReference or a StrongReference around the cached entity.</li>
 * <li>For every Reference created by <code>createRef()</code> there will be
 * exactly one call to {@link #clear(PageRef)} to cleanup any resources associated
 * with the (now expired) cached entity.</li>
 * </ul>
 * <p>
 * Therefore, it is safe to perform resource accounting increments during the
 * {@link #load(Pack, long)} or
 * {@link #createRef(Pack, long, ByteWindow)} methods, and matching
 * decrements during {@link #clear(PageRef)}. Implementors may need to override
 * {@link #createRef(Pack, long, ByteWindow)} in order to embed additional
 * accounting information into an implementation specific
 * {@link org.eclipse.jgit.internal.storage.file.WindowCache.PageRef} subclass, as
 * the cached entity may have already been evicted by the JRE's garbage
 * collector.
 * <p>
 * To maintain higher concurrency workloads, during eviction only one thread
 * performs the eviction work, while other threads can continue to insert new
 * objects in parallel. This means that the cache can be temporarily over limit,
 * especially if the nominated eviction thread is being starved relative to the
 * other threads.
 */
public class WindowCache {

	/**
	 * Record statistics for a cache
	 */
	static interface StatsRecorder {
		/**
		 * Record cache hits. Called when cache returns a cached entry.
		 *
		 * @param count
		 *            number of cache hits to record
		 */
		void recordHits(int count);

		/**
		 * Record cache misses. Called when the cache returns an entry which had
		 * to be loaded.
		 *
		 * @param count
		 *            number of cache misses to record
		 */
		void recordMisses(int count);

		/**
		 * Record a successful load of a cache entry
		 *
		 * @param loadTimeNanos
		 *            time to load a cache entry
		 */
		void recordLoadSuccess(long loadTimeNanos);

		/**
		 * Record a failed load of a cache entry
		 *
		 * @param loadTimeNanos
		 *            time used trying to load a cache entry
		 */
		void recordLoadFailure(long loadTimeNanos);

		/**
		 * Record cache evictions due to the cache evictions strategy
		 *
		 * @param count
		 *            number of evictions to record
		 */
		void recordEvictions(int count);

		/**
		 * Record files opened by cache
		 *
		 * @param delta
		 *            delta of number of files opened by cache
		 */
		void recordOpenFiles(int delta);

		/**
		 * Record cached bytes
		 *
		 * @param pack
		 *            pack file the bytes are read from
		 *
		 * @param delta
		 *            delta of cached bytes
		 */
		void recordOpenBytes(Pack pack, int delta);

		/**
		 * Returns a snapshot of this recorder's stats. Note that this may be an
		 * inconsistent view, as it may be interleaved with update operations.
		 *
		 * @return a snapshot of this recorder's stats
		 */
		@NonNull
		WindowCacheStats getStats();
	}

	static class StatsRecorderImpl
			implements StatsRecorder, WindowCacheStats {
		private final LongAdder hitCount;
		private final LongAdder missCount;
		private final LongAdder loadSuccessCount;
		private final LongAdder loadFailureCount;
		private final LongAdder totalLoadTime;
		private final LongAdder evictionCount;
		private final LongAdder openFileCount;
		private final LongAdder openByteCount;
		private final Map<String, LongAdder> openByteCountPerRepository;

		/**
		 * Constructs an instance with all counts initialized to zero.
		 */
		public StatsRecorderImpl() {
			hitCount = new LongAdder();
			missCount = new LongAdder();
			loadSuccessCount = new LongAdder();
			loadFailureCount = new LongAdder();
			totalLoadTime = new LongAdder();
			evictionCount = new LongAdder();
			openFileCount = new LongAdder();
			openByteCount = new LongAdder();
			openByteCountPerRepository = new ConcurrentHashMap<>();
		}

		@Override
		public void recordHits(int count) {
			hitCount.add(count);
		}

		@Override
		public void recordMisses(int count) {
			missCount.add(count);
		}

		@Override
		public void recordLoadSuccess(long loadTimeNanos) {
			loadSuccessCount.increment();
			totalLoadTime.add(loadTimeNanos);
		}

		@Override
		public void recordLoadFailure(long loadTimeNanos) {
			loadFailureCount.increment();
			totalLoadTime.add(loadTimeNanos);
		}

		@Override
		public void recordEvictions(int count) {
			evictionCount.add(count);
		}

		@Override
		public void recordOpenFiles(int delta) {
			openFileCount.add(delta);
		}

		@Override
		public void recordOpenBytes(Pack pack, int delta) {
			openByteCount.add(delta);
			String repositoryId = repositoryId(pack);
			LongAdder la = openByteCountPerRepository
					.computeIfAbsent(repositoryId, k -> new LongAdder());
			la.add(delta);
			if (delta < 0) {
				openByteCountPerRepository.computeIfPresent(repositoryId,
						(k, v) -> v.longValue() == 0 ? null : v);
			}
		}

		private static String repositoryId(Pack pack) {
			// use repository's gitdir since Pack doesn't know its repository
			return pack.getPackFile().getParentFile().getParentFile()
					.getParent();
		}

		@Override
		public WindowCacheStats getStats() {
			return this;
		}

		@Override
		public long getHitCount() {
			return hitCount.sum();
		}

		@Override
		public long getMissCount() {
			return missCount.sum();
		}

		@Override
		public long getLoadSuccessCount() {
			return loadSuccessCount.sum();
		}

		@Override
		public long getLoadFailureCount() {
			return loadFailureCount.sum();
		}

		@Override
		public long getEvictionCount() {
			return evictionCount.sum();
		}

		@Override
		public long getTotalLoadTime() {
			return totalLoadTime.sum();
		}

		@Override
		public long getOpenFileCount() {
			return openFileCount.sum();
		}

		@Override
		public long getOpenByteCount() {
			return openByteCount.sum();
		}

		@Override
		public void resetCounters() {
			hitCount.reset();
			missCount.reset();
			loadSuccessCount.reset();
			loadFailureCount.reset();
			totalLoadTime.reset();
			evictionCount.reset();
		}

		@Override
		public Map<String, Long> getOpenByteCountPerRepository() {
			return Collections.unmodifiableMap(
					openByteCountPerRepository.entrySet().stream()
							.collect(Collectors.toMap(Map.Entry::getKey,
									e -> Long.valueOf(e.getValue().sum()),
									(u, v) -> v)));
		}
	}

	private static final int bits(int newSize) {
		if (newSize < 4096)
			throw new IllegalArgumentException(JGitText.get().invalidWindowSize);
		if (Integer.bitCount(newSize) != 1)
			throw new IllegalArgumentException(JGitText.get().windowSizeMustBePowerOf2);
		return Integer.numberOfTrailingZeros(newSize);
	}

	private static final Random rng = new Random();

	private static volatile WindowCache cache;

	private static volatile int streamFileThreshold;

	static {
		reconfigure(new WindowCacheConfig());
	}

	/**
	 * Modify the configuration of the window cache.
	 * <p>
	 * The new configuration is applied immediately. If the new limits are
	 * smaller than what is currently cached, older entries will be purged
	 * as soon as possible to allow the cache to meet the new limit.
	 *
	 * @deprecated use {@code cfg.install()} to avoid internal reference.
	 * @param cfg
	 *            the new window cache configuration.
	 * @throws java.lang.IllegalArgumentException
	 *             the cache configuration contains one or more invalid
	 *             settings, usually too low of a limit.
	 */
	@Deprecated
	public static void reconfigure(WindowCacheConfig cfg) {
		final WindowCache nc = new WindowCache(cfg);
		final WindowCache oc = cache;
		if (oc != null)
			oc.removeAll();
		cache = nc;
		streamFileThreshold = cfg.getStreamFileThreshold();
		DeltaBaseCache.reconfigure(cfg);
	}

	static int getStreamFileThreshold() {
		return streamFileThreshold;
	}

	/**
	 * @return the cached instance.
	 */
	public static WindowCache getInstance() {
		return cache.publishMBeanIfNeeded();
	}

	static final ByteWindow get(Pack pack, long offset)
			throws IOException {
		final WindowCache c = cache;
		final ByteWindow r = c.getOrLoad(pack, c.toStart(offset));
		if (c != cache.publishMBeanIfNeeded()) {
			// The cache was reconfigured while we were using the old one
			// to load this window. The window is still valid, but our
			// cache may think its still live. Ensure the window is removed
			// from the old cache so resources can be released.
			//
			c.removeAll();
		}
		return r;
	}

	static final void purge(Pack pack) {
		cache.removeAll(pack);
	}

	/** cleanup released and/or garbage collected windows. */
	private final CleanupQueue queue;

	/** Number of entries in {@link #table}. */
	private final int tableSize;

	/** Access clock for loose LRU. */
	private final AtomicLong clock;

	/** Hash bucket directory; entries are chained below. */
	private final AtomicReferenceArray<Entry> table;

	/** Locks to prevent concurrent loads for same (PackFile,position). */
	private final Lock[] locks;

	/** Lock to elect the eviction thread after a load occurs. */
	private final ReentrantLock evictLock;

	/** Number of {@link #table} buckets to scan for an eviction window. */
	private final int evictBatch;

	private final int maxFiles;

	private final long maxBytes;

	private final boolean mmap;

	private final int windowSizeShift;

	private final int windowSize;

	private final StatsRecorder statsRecorder;

	private final StatsRecorderImpl mbean;

	private final AtomicBoolean publishMBean = new AtomicBoolean();

	private final boolean useStrongRefs;

	private final boolean useStrongIndexRefs;

	private WindowCache(WindowCacheConfig cfg) {
		tableSize = tableSize(cfg);
		final int lockCount = lockCount(cfg);
		if (tableSize < 1)
			throw new IllegalArgumentException(JGitText.get().tSizeMustBeGreaterOrEqual1);
		if (lockCount < 1)
			throw new IllegalArgumentException(JGitText.get().lockCountMustBeGreaterOrEqual1);

		clock = new AtomicLong(1);
		table = new AtomicReferenceArray<>(tableSize);
		locks = new Lock[lockCount];
		for (int i = 0; i < locks.length; i++)
			locks[i] = new Lock();
		evictLock = new ReentrantLock();

		int eb = (int) (tableSize * .1);
		if (64 < eb)
			eb = 64;
		else if (eb < 4)
			eb = 4;
		if (tableSize < eb)
			eb = tableSize;
		evictBatch = eb;

		maxFiles = cfg.getPackedGitOpenFiles();
		maxBytes = cfg.getPackedGitLimit();
		mmap = cfg.isPackedGitMMAP();
		windowSizeShift = bits(cfg.getPackedGitWindowSize());
		windowSize = 1 << windowSizeShift;
		useStrongRefs = cfg.isPackedGitUseStrongRefs();
		useStrongIndexRefs = cfg.isPackedIndexGitUseStrongRefs();
		queue = useStrongRefs ? new StrongCleanupQueue(this)
				: new SoftCleanupQueue(this);

		mbean = new StatsRecorderImpl();
		statsRecorder = mbean;
		publishMBean.set(cfg.getExposeStatsViaJmx());

		if (maxFiles < 1)
			throw new IllegalArgumentException(JGitText.get().openFilesMustBeAtLeast1);
		if (maxBytes < windowSize)
			throw new IllegalArgumentException(JGitText.get().windowSizeMustBeLesserThanLimit);
	}

	private WindowCache publishMBeanIfNeeded() {
		if (publishMBean.getAndSet(false)) {
			Monitoring.registerMBean(mbean, "block_cache"); //$NON-NLS-1$
		}
		return this;
	}

	/**
	 * @return cache statistics for the WindowCache
	 */
	public WindowCacheStats getStats() {
		return statsRecorder.getStats();
	}

	/**
	 * Reset stats. Does not reset open bytes and open files stats.
	 */
	public void resetStats() {
		mbean.resetCounters();
	}

	private int hash(int packHash, long off) {
		return packHash + (int) (off >>> windowSizeShift);
	}

	private ByteWindow load(Pack pack, long offset) throws IOException {
		long startTime = System.nanoTime();
		if (pack.beginWindowCache())
			statsRecorder.recordOpenFiles(1);
		try {
			if (mmap)
				return pack.mmap(offset, windowSize);
			ByteArrayWindow w = pack.read(offset, windowSize);
			statsRecorder.recordLoadSuccess(System.nanoTime() - startTime);
			return w;
		} catch (IOException | RuntimeException | Error e) {
			close(pack);
			statsRecorder.recordLoadFailure(System.nanoTime() - startTime);
			throw e;
		} finally {
			statsRecorder.recordMisses(1);
		}
	}

	private PageRef<ByteWindow> createRef(Pack p, long o, ByteWindow v) {
		final PageRef<ByteWindow> ref = useStrongRefs
				? new StrongRef(p, o, v, queue)
				: new SoftRef(p, o, v, (SoftCleanupQueue) queue);
		statsRecorder.recordOpenBytes(ref.getPack(), ref.getSize());
		return ref;
	}

	private void clear(PageRef<ByteWindow> ref) {
		statsRecorder.recordOpenBytes(ref.getPack(), -ref.getSize());
		statsRecorder.recordEvictions(1);
		close(ref.getPack());
	}

	private void close(Pack pack) {
		if (pack.endWindowCache()) {
			statsRecorder.recordOpenFiles(-1);
		}
	}

	private boolean isFull() {
		return maxFiles < mbean.getOpenFileCount()
				|| maxBytes < mbean.getOpenByteCount();
	}

	private long toStart(long offset) {
		return (offset >>> windowSizeShift) << windowSizeShift;
	}

	private static int tableSize(WindowCacheConfig cfg) {
		final int wsz = cfg.getPackedGitWindowSize();
		final long limit = cfg.getPackedGitLimit();
		if (wsz <= 0)
			throw new IllegalArgumentException(JGitText.get().invalidWindowSize);
		if (limit < wsz)
			throw new IllegalArgumentException(JGitText.get().windowSizeMustBeLesserThanLimit);
		return (int) Math.min(5 * (limit / wsz) / 2, 2000000000);
	}

	private static int lockCount(WindowCacheConfig cfg) {
		return Math.max(cfg.getPackedGitOpenFiles(), 32);
	}

	/**
	 * Lookup a cached object, creating and loading it if it doesn't exist.
	 *
	 * @param pack
	 *            the pack that "contains" the cached object.
	 * @param position
	 *            offset within <code>pack</code> of the object.
	 * @return the object reference.
	 * @throws IOException
	 *             the object reference was not in the cache and could not be
	 *             obtained by {@link #load(Pack, long)}.
	 */
	private ByteWindow getOrLoad(Pack pack, long position)
			throws IOException {
		final int slot = slot(pack, position);
		final Entry e1 = table.get(slot);
		ByteWindow v = scan(e1, pack, position);
		if (v != null) {
			statsRecorder.recordHits(1);
			return v;
		}

		synchronized (lock(pack, position)) {
			Entry e2 = table.get(slot);
			if (e2 != e1) {
				v = scan(e2, pack, position);
				if (v != null) {
					statsRecorder.recordHits(1);
					return v;
				}
			}

			v = load(pack, position);
			final PageRef<ByteWindow> ref = createRef(pack, position, v);
			hit(ref);
			for (;;) {
				final Entry n = new Entry(clean(e2), ref);
				if (table.compareAndSet(slot, e2, n))
					break;
				e2 = table.get(slot);
			}
		}

		if (evictLock.tryLock()) {
			try {
				gc();
				evict();
			} finally {
				evictLock.unlock();
			}
		}

		return v;
	}

	private ByteWindow scan(Entry n, Pack pack, long position) {
		for (; n != null; n = n.next) {
			final PageRef<ByteWindow> r = n.ref;
			if (r.getPack() == pack && r.getPosition() == position) {
				final ByteWindow v = r.get();
				if (v != null) {
					hit(r);
					return v;
				}
				n.kill();
				break;
			}
		}
		return null;
	}

	private void hit(PageRef r) {
		// We don't need to be 100% accurate here. Its sufficient that at least
		// one thread performs the increment. Any other concurrent access at
		// exactly the same time can simply use the same clock value.
		//
		// Consequently we attempt the set, but we don't try to recover should
		// it fail. This is why we don't use getAndIncrement() here.
		//
		final long c = clock.get();
		clock.compareAndSet(c, c + 1);
		r.setLastAccess(c);
	}

	private void evict() {
		while (isFull()) {
			int ptr = rng.nextInt(tableSize);
			Entry old = null;
			int slot = 0;
			for (int b = evictBatch - 1; b >= 0; b--, ptr++) {
				if (tableSize <= ptr)
					ptr = 0;
				for (Entry e = table.get(ptr); e != null; e = e.next) {
					if (e.dead)
						continue;
					if (old == null || e.ref.getLastAccess() < old.ref
							.getLastAccess()) {
						old = e;
						slot = ptr;
					}
				}
			}
			if (old != null) {
				old.kill();
				gc();
				final Entry e1 = table.get(slot);
				table.compareAndSet(slot, e1, clean(e1));
			}
		}
	}

	/**
	 * Clear every entry from the cache.
	 * <p>
	 * This is a last-ditch effort to clear out the cache, such as before it
	 * gets replaced by another cache that is configured differently. This
	 * method tries to force every cached entry through {@link #clear(PageRef)} to
	 * ensure that resources are correctly accounted for and cleaned up by the
	 * subclass. A concurrent reader loading entries while this method is
	 * running may cause resource accounting failures.
	 */
	private void removeAll() {
		for (int s = 0; s < tableSize; s++) {
			Entry e1;
			do {
				e1 = table.get(s);
				for (Entry e = e1; e != null; e = e.next)
					e.kill();
			} while (!table.compareAndSet(s, e1, null));
		}
		gc();
	}

	/**
	 * Clear all entries related to a single file.
	 * <p>
	 * Typically this method is invoked during {@link Pack#close()}, when we
	 * know the pack is never going to be useful to us again (for example, it no
	 * longer exists on disk). A concurrent reader loading an entry from this
	 * same pack may cause the pack to become stuck in the cache anyway.
	 *
	 * @param pack
	 *            the file to purge all entries of.
	 */
	private void removeAll(Pack pack) {
		for (int s = 0; s < tableSize; s++) {
			final Entry e1 = table.get(s);
			boolean hasDead = false;
			for (Entry e = e1; e != null; e = e.next) {
				if (e.ref.getPack() == pack) {
					e.kill();
					hasDead = true;
				} else if (e.dead)
					hasDead = true;
			}
			if (hasDead)
				table.compareAndSet(s, e1, clean(e1));
		}
		gc();
	}

	private void gc() {
		queue.gc();
	}

	private int slot(Pack pack, long position) {
		return (hash(pack.hash, position) >>> 1) % tableSize;
	}

	private Lock lock(Pack pack, long position) {
		return locks[(hash(pack.hash, position) >>> 1) % locks.length];
	}

	private static Entry clean(Entry top) {
		while (top != null && top.dead) {
			top.ref.kill();
			top = top.next;
		}
		if (top == null)
			return null;
		final Entry n = clean(top.next);
		return n == top.next ? top : new Entry(n, top.ref);
	}

	boolean isPackedIndexGitUseStrongRefs() {
		return useStrongIndexRefs;
	}

	private static class Entry {
		/** Next entry in the hash table's chain list. */
		final Entry next;

		/** The referenced object. */
		final PageRef<ByteWindow> ref;

		/**
		 * Marked true when ref.get() returns null and the ref is dead.
		 * <p>
		 * A true here indicates that the ref is no longer accessible, and that
		 * we therefore need to eventually purge this Entry object out of the
		 * bucket's chain.
		 */
		volatile boolean dead;

		Entry(Entry n, PageRef<ByteWindow> r) {
			next = n;
			ref = r;
		}

		final void kill() {
			dead = true;
			ref.kill();
		}
	}

	private static interface PageRef<T> {
		/**
		 * Returns this reference object's referent. If this reference object
		 * has been cleared, either by the program or by the garbage collector,
		 * then this method returns <code>null</code>.
		 *
		 * @return The object to which this reference refers, or
		 *         <code>null</code> if this reference object has been cleared
		 */
		T get();

	    /**
		 * Kill this ref
		 *
		 * @return <code>true</code> if this reference object was successfully
		 *         killed; <code>false</code> if it was already killed
		 */
		boolean kill();

		/**
		 * Get the {@link org.eclipse.jgit.internal.storage.file.Pack} the
		 * referenced cache page is allocated for
		 *
		 * @return the {@link org.eclipse.jgit.internal.storage.file.Pack} the
		 *         referenced cache page is allocated for
		 */
		Pack getPack();

		/**
		 * Get the position of the referenced cache page in the
		 * {@link org.eclipse.jgit.internal.storage.file.Pack}
		 *
		 * @return the position of the referenced cache page in the
		 *         {@link org.eclipse.jgit.internal.storage.file.Pack}
		 */
		long getPosition();

		/**
		 * Get size of cache page
		 *
		 * @return size of cache page
		 */
		int getSize();

		/**
		 * Get pseudo time of last access to this cache page
		 *
		 * @return pseudo time of last access to this cache page
		 */
		long getLastAccess();

		/**
		 * Set pseudo time of last access to this cache page
		 *
		 * @param time
		 *            pseudo time of last access to this cache page
		 */
		void setLastAccess(long time);

		/**
		 * Whether this is a strong reference.
		 * @return {@code true} if this is a strong reference
		 */
		boolean isStrongRef();
	}

	/** A soft reference wrapped around a cached object. */
	private static class SoftRef extends SoftReference<ByteWindow>
			implements PageRef<ByteWindow> {
		private final Pack pack;

		private final long position;

		private final int size;

		private long lastAccess;

		protected SoftRef(final Pack pack, final long position,
				final ByteWindow v, final SoftCleanupQueue queue) {
			super(v, queue);
			this.pack = pack;
			this.position = position;
			this.size = v.size();
		}

		@Override
		public Pack getPack() {
			return pack;
		}

		@Override
		public long getPosition() {
			return position;
		}

		@Override
		public int getSize() {
			return size;
		}

		@Override
		public long getLastAccess() {
			return lastAccess;
		}

		@Override
		public void setLastAccess(long time) {
			this.lastAccess = time;
		}

		@Override
		public boolean kill() {
			return enqueue();
		}

		@Override
		public boolean isStrongRef() {
			return false;
		}
	}

	/** A strong reference wrapped around a cached object. */
	private static class StrongRef implements PageRef<ByteWindow> {
		private ByteWindow referent;

		private final Pack pack;

		private final long position;

		private final int size;

		private long lastAccess;

		private CleanupQueue queue;

		protected StrongRef(final Pack pack, final long position,
				final ByteWindow v, final CleanupQueue queue) {
			this.pack = pack;
			this.position = position;
			this.referent = v;
			this.size = v.size();
			this.queue = queue;
		}

		@Override
		public Pack getPack() {
			return pack;
		}

		@Override
		public long getPosition() {
			return position;
		}

		@Override
		public int getSize() {
			return size;
		}

		@Override
		public long getLastAccess() {
			return lastAccess;
		}

		@Override
		public void setLastAccess(long time) {
			this.lastAccess = time;
		}

		@Override
		public ByteWindow get() {
			return referent;
		}

		@Override
		public boolean kill() {
			if (referent == null) {
				return false;
			}
			referent = null;
			return queue.enqueue(this);
		}

		@Override
		public boolean isStrongRef() {
			return true;
		}
	}

	private static interface CleanupQueue {
		boolean enqueue(PageRef<ByteWindow> r);
		void gc();
	}

	private static class SoftCleanupQueue extends ReferenceQueue<ByteWindow>
			implements CleanupQueue {
		private final WindowCache wc;

		SoftCleanupQueue(WindowCache cache) {
			this.wc = cache;
		}

		@Override
		public boolean enqueue(PageRef<ByteWindow> r) {
			// no need to explicitly add soft references which are enqueued by
			// the JVM
			return false;
		}

		@Override
		public void gc() {
			SoftRef r;
			while ((r = (SoftRef) poll()) != null) {
				wc.clear(r);

				final int s = wc.slot(r.getPack(), r.getPosition());
				final Entry e1 = wc.table.get(s);
				for (Entry n = e1; n != null; n = n.next) {
					if (n.ref == r) {
						n.dead = true;
						wc.table.compareAndSet(s, e1, clean(e1));
						break;
					}
				}
			}
		}
	}

	private static class StrongCleanupQueue implements CleanupQueue {
		private final WindowCache wc;

		private final ConcurrentLinkedQueue<PageRef<ByteWindow>> queue = new ConcurrentLinkedQueue<>();

		StrongCleanupQueue(WindowCache wc) {
			this.wc = wc;
		}

		@Override
		public boolean enqueue(PageRef<ByteWindow> r) {
			if (queue.contains(r)) {
				return false;
			}
			return queue.add(r);
		}

		@Override
		public void gc() {
			PageRef<ByteWindow> r;
			while ((r = queue.poll()) != null) {
				wc.clear(r);

				final int s = wc.slot(r.getPack(), r.getPosition());
				final Entry e1 = wc.table.get(s);
				for (Entry n = e1; n != null; n = n.next) {
					if (n.ref == r) {
						n.dead = true;
						wc.table.compareAndSet(s, e1, clean(e1));
						break;
					}
				}
			}
		}
	}

	private static final class Lock {
		// Used only for its implicit monitor.
	}
}
