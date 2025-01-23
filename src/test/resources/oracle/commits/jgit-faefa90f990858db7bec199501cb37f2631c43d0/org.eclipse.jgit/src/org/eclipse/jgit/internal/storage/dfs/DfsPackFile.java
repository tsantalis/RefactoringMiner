/*
 * Copyright (C) 2008-2011, Google Inc.
 * Copyright (C) 2007, Robin Rosenberg <robin.rosenberg@dewire.com>
 * Copyright (C) 2006-2008, Shawn O. Pearce <spearce@spearce.org> and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0 which is available at
 * https://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package org.eclipse.jgit.internal.storage.dfs;

import static org.eclipse.jgit.internal.storage.dfs.DfsObjDatabase.PackSource.UNREACHABLE_GARBAGE;
import static org.eclipse.jgit.internal.storage.pack.PackExt.BITMAP_INDEX;
import static org.eclipse.jgit.internal.storage.pack.PackExt.COMMIT_GRAPH;
import static org.eclipse.jgit.internal.storage.pack.PackExt.INDEX;
import static org.eclipse.jgit.internal.storage.pack.PackExt.PACK;
import static org.eclipse.jgit.internal.storage.pack.PackExt.REVERSE_INDEX;

import java.io.BufferedInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.text.MessageFormat;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.zip.CRC32;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

import org.eclipse.jgit.errors.CorruptObjectException;
import org.eclipse.jgit.errors.LargeObjectException;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.errors.PackInvalidException;
import org.eclipse.jgit.errors.StoredObjectRepresentationNotAvailableException;
import org.eclipse.jgit.internal.JGitText;
import org.eclipse.jgit.internal.storage.commitgraph.CommitGraph;
import org.eclipse.jgit.internal.storage.commitgraph.CommitGraphLoader;
import org.eclipse.jgit.internal.storage.file.PackBitmapIndex;
import org.eclipse.jgit.internal.storage.file.PackIndex;
import org.eclipse.jgit.internal.storage.file.PackReverseIndex;
import org.eclipse.jgit.internal.storage.pack.BinaryDelta;
import org.eclipse.jgit.internal.storage.pack.PackOutputStream;
import org.eclipse.jgit.internal.storage.pack.StoredObjectRepresentation;
import org.eclipse.jgit.lib.AbbreviatedObjectId;
import org.eclipse.jgit.lib.AnyObjectId;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.util.LongList;

/**
 * A Git version 2 pack file representation. A pack file contains Git objects in
 * delta packed format yielding high compression of lots of object where some
 * objects are similar.
 */
public final class DfsPackFile extends BlockBasedFile {
	private static final int REC_SIZE = Constants.OBJECT_ID_LENGTH + 8;
	private static final long REF_POSITION = 0;

	/** Index mapping {@link ObjectId} to position within the pack stream. */
	private volatile PackIndex index;

	/** Reverse version of {@link #index} mapping position to {@link ObjectId}. */
	private volatile PackReverseIndex reverseIndex;

	/** Index of compressed bitmap mapping entire object graph. */
	private volatile PackBitmapIndex bitmapIndex;

	/** Index of compressed commit graph mapping entire object graph. */
	private volatile CommitGraph commitGraph;

	/**
	 * Objects we have tried to read, and discovered to be corrupt.
	 * <p>
	 * The list is allocated after the first corruption is found, and filled in
	 * as more entries are discovered. Typically this list is never used, as
	 * pack files do not usually contain corrupt objects.
	 */
	private volatile LongList corruptObjects;

	/** Lock for {@link #corruptObjects}. */
	private final Object corruptObjectsLock = new Object();

	/**
	 * Construct a reader for an existing, packfile.
	 *
	 * @param cache
	 *            cache that owns the pack data.
	 * @param desc
	 *            description of the pack within the DFS.
	 */
	DfsPackFile(DfsBlockCache cache, DfsPackDescription desc) {
		super(cache, desc, PACK);

		int bs = desc.getBlockSize(PACK);
		if (bs > 0) {
			setBlockSize(bs);
		}

		long sz = desc.getFileSize(PACK);
		length = sz > 0 ? sz : -1;
	}

	/**
	 * Get description that was originally used to configure this pack file.
	 *
	 * @return description that was originally used to configure this pack file.
	 */
	public DfsPackDescription getPackDescription() {
		return desc;
	}

	/**
	 * Whether the pack index file is loaded and cached in memory.
	 *
	 * @return whether the pack index file is loaded and cached in memory.
	 */
	public boolean isIndexLoaded() {
		return index != null;
	}

	void setPackIndex(PackIndex idx) {
		long objCnt = idx.getObjectCount();
		int recSize = Constants.OBJECT_ID_LENGTH + 8;
		long sz = objCnt * recSize;
		cache.putRef(desc.getStreamKey(INDEX), sz, idx);
		index = idx;
	}

	/**
	 * Get the PackIndex for this PackFile.
	 *
	 * @param ctx
	 *            reader context to support reading from the backing store if
	 *            the index is not already loaded in memory.
	 * @return the PackIndex.
	 * @throws java.io.IOException
	 *             the pack index is not available, or is corrupt.
	 */
	public PackIndex getPackIndex(DfsReader ctx) throws IOException {
		return idx(ctx);
	}

	private PackIndex idx(DfsReader ctx) throws IOException {
		if (index != null) {
			return index;
		}

		if (invalid) {
			throw new PackInvalidException(getFileName(), invalidatingCause);
		}

		Repository.getGlobalListenerList()
				.dispatch(new BeforeDfsPackIndexLoadedEvent(this));
		try {
			DfsStreamKey idxKey = desc.getStreamKey(INDEX);
			AtomicBoolean cacheHit = new AtomicBoolean(true);
			DfsBlockCache.Ref<PackIndex> idxref = cache.getOrLoadRef(idxKey,
					REF_POSITION, () -> {
						cacheHit.set(false);
						return loadPackIndex(ctx, idxKey);
					});
			if (cacheHit.get()) {
				ctx.stats.idxCacheHit++;
			}
			PackIndex idx = idxref.get();
			if (index == null && idx != null) {
				index = idx;
			}
			return index;
		} catch (IOException e) {
			invalid = true;
			invalidatingCause = e;
			throw e;
		}
	}

	final boolean isGarbage() {
		return desc.getPackSource() == UNREACHABLE_GARBAGE;
	}

	/**
	 * Get the BitmapIndex for this PackFile.
	 *
	 * @param ctx
	 *            reader context to support reading from the backing store if
	 *            the index is not already loaded in memory.
	 * @return the BitmapIndex.
	 * @throws java.io.IOException
	 *             the bitmap index is not available, or is corrupt.
	 */
	public PackBitmapIndex getBitmapIndex(DfsReader ctx) throws IOException {
		if (invalid || isGarbage() || !desc.hasFileExt(BITMAP_INDEX)) {
			return null;
		}

		if (bitmapIndex != null) {
			return bitmapIndex;
		}

		DfsStreamKey bitmapKey = desc.getStreamKey(BITMAP_INDEX);
		AtomicBoolean cacheHit = new AtomicBoolean(true);
		DfsBlockCache.Ref<PackBitmapIndex> idxref = cache
				.getOrLoadRef(bitmapKey, REF_POSITION, () -> {
					cacheHit.set(false);
					return loadBitmapIndex(ctx, bitmapKey);
				});
		if (cacheHit.get()) {
			ctx.stats.bitmapCacheHit++;
		}
		PackBitmapIndex bmidx = idxref.get();
		if (bitmapIndex == null && bmidx != null) {
			bitmapIndex = bmidx;
		}
		return bitmapIndex;
	}

	/**
	 * Get the Commit Graph for this PackFile.
	 *
	 * @param ctx
	 *            reader context to support reading from the backing store if
	 *            the index is not already loaded in memory.
	 * @return {@link org.eclipse.jgit.internal.storage.commitgraph.CommitGraph},
	 *         null if pack doesn't have it.
	 * @throws java.io.IOException
	 *             the Commit Graph is not available, or is corrupt.
	 */
	public CommitGraph getCommitGraph(DfsReader ctx) throws IOException {
		if (invalid || isGarbage() || !desc.hasFileExt(COMMIT_GRAPH)) {
			return null;
		}

		if (commitGraph != null) {
			return commitGraph;
		}

		DfsStreamKey commitGraphKey = desc.getStreamKey(COMMIT_GRAPH);
		AtomicBoolean cacheHit = new AtomicBoolean(true);
		DfsBlockCache.Ref<CommitGraph> cgref = cache
				.getOrLoadRef(commitGraphKey, REF_POSITION, () -> {
					cacheHit.set(false);
					return loadCommitGraph(ctx, commitGraphKey);
				});
		if (cacheHit.get()) {
			ctx.stats.commitGraphCacheHit++;
		}
		CommitGraph cg = cgref.get();
		if (commitGraph == null && cg != null) {
			commitGraph = cg;
		}
		return commitGraph;
	}

	PackReverseIndex getReverseIdx(DfsReader ctx) throws IOException {
		if (reverseIndex != null) {
			return reverseIndex;
		}

		PackIndex idx = idx(ctx);
		DfsStreamKey revKey = desc.getStreamKey(REVERSE_INDEX);
		AtomicBoolean cacheHit = new AtomicBoolean(true);
		DfsBlockCache.Ref<PackReverseIndex> revref = cache.getOrLoadRef(revKey,
				REF_POSITION, () -> {
					cacheHit.set(false);
					return loadReverseIdx(ctx, revKey, idx);
				});
		if (cacheHit.get()) {
			ctx.stats.ridxCacheHit++;
		}
		PackReverseIndex revidx = revref.get();
		if (reverseIndex == null && revidx != null) {
			reverseIndex = revidx;
		}
		return reverseIndex;
	}

	/**
	 * Check if an object is stored within this pack.
	 *
	 * @param ctx
	 *            reader context to support reading from the backing store if
	 *            the index is not already loaded in memory.
	 * @param id
	 *            object to be located.
	 * @return true if the object exists in this pack; false if it does not.
	 * @throws java.io.IOException
	 *             the pack index is not available, or is corrupt.
	 */
	public boolean hasObject(DfsReader ctx, AnyObjectId id) throws IOException {
		final long offset = idx(ctx).findOffset(id);
		return 0 < offset && !isCorrupt(offset);
	}

	/**
	 * Get an object from this pack.
	 *
	 * @param ctx
	 *            temporary working space associated with the calling thread.
	 * @param id
	 *            the object to obtain from the pack. Must not be null.
	 * @return the object loader for the requested object if it is contained in
	 *         this pack; null if the object was not found.
	 * @throws IOException
	 *             the pack file or the index could not be read.
	 */
	ObjectLoader get(DfsReader ctx, AnyObjectId id)
			throws IOException {
		long offset = idx(ctx).findOffset(id);
		return 0 < offset && !isCorrupt(offset) ? load(ctx, offset) : null;
	}

	long findOffset(DfsReader ctx, AnyObjectId id) throws IOException {
		return idx(ctx).findOffset(id);
	}

	void resolve(DfsReader ctx, Set<ObjectId> matches, AbbreviatedObjectId id,
			int matchLimit) throws IOException {
		idx(ctx).resolve(matches, id, matchLimit);
	}

	/**
	 * Obtain the total number of objects available in this pack. This method
	 * relies on pack index, giving number of effectively available objects.
	 *
	 * @param ctx
	 *            current reader for the calling thread.
	 * @return number of objects in index of this pack, likewise in this pack
	 * @throws IOException
	 *             the index file cannot be loaded into memory.
	 */
	long getObjectCount(DfsReader ctx) throws IOException {
		return idx(ctx).getObjectCount();
	}

	private byte[] decompress(long position, int sz, DfsReader ctx)
			throws IOException, DataFormatException {
		byte[] dstbuf;
		try {
			dstbuf = new byte[sz];
		} catch (OutOfMemoryError noMemory) {
			// The size may be larger than our heap allows, return null to
			// let the caller know allocation isn't possible and it should
			// use the large object streaming approach instead.
			//
			// For example, this can occur when sz is 640 MB, and JRE
			// maximum heap size is only 256 MB. Even if the JRE has
			// 200 MB free, it cannot allocate a 640 MB byte array.
			return null;
		}

		if (ctx.inflate(this, position, dstbuf, false) != sz) {
			throw new EOFException(MessageFormat.format(
					JGitText.get().shortCompressedStreamAt,
					Long.valueOf(position)));
		}
		return dstbuf;
	}

	void copyPackAsIs(PackOutputStream out, DfsReader ctx) throws IOException {
		// If the length hasn't been determined yet, pin to set it.
		if (length == -1) {
			ctx.pin(this, 0);
			ctx.unpin();
		}
		try (ReadableChannel rc = ctx.db.openFile(desc, PACK)) {
			int sz = ctx.getOptions().getStreamPackBufferSize();
			if (sz > 0) {
				rc.setReadAheadBytes(sz);
			}
			if (cache.shouldCopyThroughCache(length)) {
				copyPackThroughCache(out, ctx, rc);
			} else {
				copyPackBypassCache(out, rc);
			}
		}
	}

	private void copyPackThroughCache(PackOutputStream out, DfsReader ctx,
			ReadableChannel rc) throws IOException {
		long position = 12;
		long remaining = length - (12 + 20);
		while (0 < remaining) {
			DfsBlock b = cache.getOrLoad(this, position, ctx, () -> rc);
			int ptr = (int) (position - b.start);
			if (b.size() <= ptr) {
				throw packfileIsTruncated();
			}
			int n = (int) Math.min(b.size() - ptr, remaining);
			b.write(out, position, n);
			position += n;
			remaining -= n;
		}
	}

	private long copyPackBypassCache(PackOutputStream out, ReadableChannel rc)
			throws IOException {
		ByteBuffer buf = newCopyBuffer(out, rc);
		long position = 12;
		long remaining = length - (12 + 20);
		boolean packHeadSkipped = false;
		while (0 < remaining) {
			DfsBlock b = cache.get(key, alignToBlock(position));
			if (b != null) {
				int ptr = (int) (position - b.start);
				if (b.size() <= ptr) {
					throw packfileIsTruncated();
				}
				int n = (int) Math.min(b.size() - ptr, remaining);
				b.write(out, position, n);
				position += n;
				remaining -= n;
				rc.position(position);
				packHeadSkipped = true;
				continue;
			}

			// Need to skip the 'PACK' header for the first read
			int ptr = packHeadSkipped ? 0 : 12;
			buf.position(0);
			int bufLen = read(rc, buf);
			if (bufLen <= ptr) {
				throw packfileIsTruncated();
			}
			int n = (int) Math.min(bufLen - ptr, remaining);
			out.write(buf.array(), ptr, n);
			position += n;
			remaining -= n;
			packHeadSkipped = true;
		}
		return position;
	}

	private ByteBuffer newCopyBuffer(PackOutputStream out, ReadableChannel rc) {
		int bs = blockSize(rc);
		byte[] copyBuf = out.getCopyBuffer();
		if (bs > copyBuf.length) {
			copyBuf = new byte[bs];
		}
		return ByteBuffer.wrap(copyBuf, 0, bs);
	}

	void copyAsIs(PackOutputStream out, DfsObjectToPack src,
			boolean validate, DfsReader ctx) throws IOException,
			StoredObjectRepresentationNotAvailableException {
		final CRC32 crc1 = validate ? new CRC32() : null;
		final CRC32 crc2 = validate ? new CRC32() : null;
		final byte[] buf = out.getCopyBuffer();

		// Rip apart the header so we can discover the size.
		//
		try {
			readFully(src.offset, buf, 0, 20, ctx);
		} catch (IOException ioError) {
			throw new StoredObjectRepresentationNotAvailableException(ioError);
		}
		int c = buf[0] & 0xff;
		final int typeCode = (c >> 4) & 7;
		long inflatedLength = c & 15;
		int shift = 4;
		int headerCnt = 1;
		while ((c & 0x80) != 0) {
			c = buf[headerCnt++] & 0xff;
			inflatedLength += ((long) (c & 0x7f)) << shift;
			shift += 7;
		}

		if (typeCode == Constants.OBJ_OFS_DELTA) {
			do {
				c = buf[headerCnt++] & 0xff;
			} while ((c & 128) != 0);
			if (validate) {
				assert(crc1 != null && crc2 != null);
				crc1.update(buf, 0, headerCnt);
				crc2.update(buf, 0, headerCnt);
			}
		} else if (typeCode == Constants.OBJ_REF_DELTA) {
			if (validate) {
				assert(crc1 != null && crc2 != null);
				crc1.update(buf, 0, headerCnt);
				crc2.update(buf, 0, headerCnt);
			}

			readFully(src.offset + headerCnt, buf, 0, 20, ctx);
			if (validate) {
				assert(crc1 != null && crc2 != null);
				crc1.update(buf, 0, 20);
				crc2.update(buf, 0, 20);
			}
			headerCnt += 20;
		} else if (validate) {
			assert(crc1 != null && crc2 != null);
			crc1.update(buf, 0, headerCnt);
			crc2.update(buf, 0, headerCnt);
		}

		final long dataOffset = src.offset + headerCnt;
		final long dataLength = src.length;
		final long expectedCRC;
		final DfsBlock quickCopy;

		// Verify the object isn't corrupt before sending. If it is,
		// we report it missing instead.
		//
		try {
			quickCopy = ctx.quickCopy(this, dataOffset, dataLength);

			if (validate && idx(ctx).hasCRC32Support()) {
				assert(crc1 != null);
				// Index has the CRC32 code cached, validate the object.
				//
				expectedCRC = idx(ctx).findCRC32(src);
				if (quickCopy != null) {
					quickCopy.crc32(crc1, dataOffset, (int) dataLength);
				} else {
					long pos = dataOffset;
					long cnt = dataLength;
					while (cnt > 0) {
						final int n = (int) Math.min(cnt, buf.length);
						readFully(pos, buf, 0, n, ctx);
						crc1.update(buf, 0, n);
						pos += n;
						cnt -= n;
					}
				}
				if (crc1.getValue() != expectedCRC) {
					setCorrupt(src.offset);
					throw new CorruptObjectException(MessageFormat.format(
							JGitText.get().objectAtHasBadZlibStream,
							Long.valueOf(src.offset), getFileName()));
				}
			} else if (validate) {
				assert(crc1 != null);
				// We don't have a CRC32 code in the index, so compute it
				// now while inflating the raw data to get zlib to tell us
				// whether or not the data is safe.
				//
				Inflater inf = ctx.inflater();
				byte[] tmp = new byte[1024];
				if (quickCopy != null) {
					quickCopy.check(inf, tmp, dataOffset, (int) dataLength);
				} else {
					long pos = dataOffset;
					long cnt = dataLength;
					while (cnt > 0) {
						final int n = (int) Math.min(cnt, buf.length);
						readFully(pos, buf, 0, n, ctx);
						crc1.update(buf, 0, n);
						inf.setInput(buf, 0, n);
						while (inf.inflate(tmp, 0, tmp.length) > 0) {
							continue;
						}
						pos += n;
						cnt -= n;
					}
				}
				if (!inf.finished() || inf.getBytesRead() != dataLength) {
					setCorrupt(src.offset);
					throw new EOFException(MessageFormat.format(
							JGitText.get().shortCompressedStreamAt,
							Long.valueOf(src.offset)));
				}
				expectedCRC = crc1.getValue();
			} else {
				expectedCRC = -1;
			}
		} catch (DataFormatException dataFormat) {
			setCorrupt(src.offset);

			CorruptObjectException corruptObject = new CorruptObjectException(
					MessageFormat.format(
							JGitText.get().objectAtHasBadZlibStream,
							Long.valueOf(src.offset), getFileName()),
					dataFormat);

			throw new StoredObjectRepresentationNotAvailableException(
					corruptObject);

		} catch (IOException ioError) {
			throw new StoredObjectRepresentationNotAvailableException(ioError);
		}

		if (quickCopy != null) {
			// The entire object fits into a single byte array window slice,
			// and we have it pinned.  Write this out without copying.
			//
			out.writeHeader(src, inflatedLength);
			quickCopy.write(out, dataOffset, (int) dataLength);

		} else if (dataLength <= buf.length) {
			// Tiny optimization: Lots of objects are very small deltas or
			// deflated commits that are likely to fit in the copy buffer.
			//
			if (!validate) {
				long pos = dataOffset;
				long cnt = dataLength;
				while (cnt > 0) {
					final int n = (int) Math.min(cnt, buf.length);
					readFully(pos, buf, 0, n, ctx);
					pos += n;
					cnt -= n;
				}
			}
			out.writeHeader(src, inflatedLength);
			out.write(buf, 0, (int) dataLength);
		} else {
			// Now we are committed to sending the object. As we spool it out,
			// check its CRC32 code to make sure there wasn't corruption between
			// the verification we did above, and us actually outputting it.
			//
			out.writeHeader(src, inflatedLength);
			long pos = dataOffset;
			long cnt = dataLength;
			while (cnt > 0) {
				final int n = (int) Math.min(cnt, buf.length);
				readFully(pos, buf, 0, n, ctx);
				if (validate) {
					assert(crc2 != null);
					crc2.update(buf, 0, n);
				}
				out.write(buf, 0, n);
				pos += n;
				cnt -= n;
			}
			if (validate) {
				assert(crc2 != null);
				if (crc2.getValue() != expectedCRC) {
					throw new CorruptObjectException(MessageFormat.format(
							JGitText.get().objectAtHasBadZlibStream,
							Long.valueOf(src.offset), getFileName()));
				}
			}
		}
	}

	private IOException packfileIsTruncated() {
		invalid = true;
		IOException exc = new IOException(MessageFormat.format(
				JGitText.get().packfileIsTruncated, getFileName()));
		invalidatingCause = exc;
		return exc;
	}

	private void readFully(long position, byte[] dstbuf, int dstoff, int cnt,
			DfsReader ctx) throws IOException {
		while (cnt > 0) {
			int copied = ctx.copy(this, position, dstbuf, dstoff, cnt);
			if (copied == 0) {
				throw new EOFException();
			}
			position += copied;
			dstoff += copied;
			cnt -= copied;
		}
	}

	ObjectLoader load(DfsReader ctx, long pos)
			throws IOException {
		try {
			final byte[] ib = ctx.tempId;
			Delta delta = null;
			byte[] data = null;
			int type = Constants.OBJ_BAD;
			boolean cached = false;

			SEARCH: for (;;) {
				readFully(pos, ib, 0, 20, ctx);
				int c = ib[0] & 0xff;
				final int typeCode = (c >> 4) & 7;
				long sz = c & 15;
				int shift = 4;
				int p = 1;
				while ((c & 0x80) != 0) {
					c = ib[p++] & 0xff;
					sz += ((long) (c & 0x7f)) << shift;
					shift += 7;
				}

				switch (typeCode) {
				case Constants.OBJ_COMMIT:
				case Constants.OBJ_TREE:
				case Constants.OBJ_BLOB:
				case Constants.OBJ_TAG: {
					if (delta != null) {
						data = decompress(pos + p, (int) sz, ctx);
						type = typeCode;
						break SEARCH;
					}

					if (sz < ctx.getStreamFileThreshold()) {
						data = decompress(pos + p, (int) sz, ctx);
						if (data != null) {
							return new ObjectLoader.SmallObject(typeCode, data);
						}
					}
					return new LargePackedWholeObject(typeCode, sz, pos, p, this, ctx.db);
				}

				case Constants.OBJ_OFS_DELTA: {
					c = ib[p++] & 0xff;
					long base = c & 127;
					while ((c & 128) != 0) {
						base += 1;
						c = ib[p++] & 0xff;
						base <<= 7;
						base += (c & 127);
					}
					base = pos - base;
					delta = new Delta(delta, pos, (int) sz, p, base);
					if (sz != delta.deltaSize) {
						break SEARCH;
					}

					DeltaBaseCache.Entry e = ctx.getDeltaBaseCache().get(key, base);
					if (e != null) {
						type = e.type;
						data = e.data;
						cached = true;
						break SEARCH;
					}
					pos = base;
					continue SEARCH;
				}

				case Constants.OBJ_REF_DELTA: {
					readFully(pos + p, ib, 0, 20, ctx);
					long base = findDeltaBase(ctx, ObjectId.fromRaw(ib));
					delta = new Delta(delta, pos, (int) sz, p + 20, base);
					if (sz != delta.deltaSize) {
						break SEARCH;
					}

					DeltaBaseCache.Entry e = ctx.getDeltaBaseCache().get(key, base);
					if (e != null) {
						type = e.type;
						data = e.data;
						cached = true;
						break SEARCH;
					}
					pos = base;
					continue SEARCH;
				}

				default:
					throw new IOException(MessageFormat.format(
							JGitText.get().unknownObjectType, Integer.valueOf(typeCode)));
				}
			}

			// At this point there is at least one delta to apply to data.
			// (Whole objects with no deltas to apply return early above.)

			if (data == null)
				throw new LargeObjectException();

			assert(delta != null);
			do {
				// Cache only the base immediately before desired object.
				if (cached) {
					cached = false;
				} else if (delta.next == null) {
					ctx.getDeltaBaseCache().put(key, delta.basePos, type, data);
				}

				pos = delta.deltaPos;

				byte[] cmds = decompress(pos + delta.hdrLen, delta.deltaSize, ctx);
				if (cmds == null) {
					data = null; // Discard base in case of OutOfMemoryError
					throw new LargeObjectException();
				}

				final long sz = BinaryDelta.getResultSize(cmds);
				if (Integer.MAX_VALUE <= sz) {
					throw new LargeObjectException.ExceedsByteArrayLimit();
				}

				final byte[] result;
				try {
					result = new byte[(int) sz];
				} catch (OutOfMemoryError tooBig) {
					data = null; // Discard base in case of OutOfMemoryError
					cmds = null;
					throw new LargeObjectException.OutOfMemory(tooBig);
				}

				BinaryDelta.apply(data, cmds, result);
				data = result;
				delta = delta.next;
			} while (delta != null);

			return new ObjectLoader.SmallObject(type, data);

		} catch (DataFormatException dfe) {
			throw new CorruptObjectException(
					MessageFormat.format(
							JGitText.get().objectAtHasBadZlibStream, Long.valueOf(pos),
							getFileName()),
					dfe);
		}
	}

	private long findDeltaBase(DfsReader ctx, ObjectId baseId)
			throws IOException, MissingObjectException {
		long ofs = idx(ctx).findOffset(baseId);
		if (ofs < 0) {
			throw new MissingObjectException(baseId,
					JGitText.get().missingDeltaBase);
		}
		return ofs;
	}

	private static class Delta {
		/** Child that applies onto this object. */
		final Delta next;

		/** Offset of the delta object. */
		final long deltaPos;

		/** Size of the inflated delta stream. */
		final int deltaSize;

		/** Total size of the delta's pack entry header (including base). */
		final int hdrLen;

		/** Offset of the base object this delta applies onto. */
		final long basePos;

		Delta(Delta next, long ofs, int sz, int hdrLen, long baseOffset) {
			this.next = next;
			this.deltaPos = ofs;
			this.deltaSize = sz;
			this.hdrLen = hdrLen;
			this.basePos = baseOffset;
		}
	}

	byte[] getDeltaHeader(DfsReader wc, long pos)
			throws IOException, DataFormatException {
		// The delta stream starts as two variable length integers. If we
		// assume they are 64 bits each, we need 16 bytes to encode them,
		// plus 2 extra bytes for the variable length overhead. So 18 is
		// the longest delta instruction header.
		//
		final byte[] hdr = new byte[32];
		wc.inflate(this, pos, hdr, true /* header only */);
		return hdr;
	}

	int getObjectType(DfsReader ctx, long pos) throws IOException {
		final byte[] ib = ctx.tempId;
		for (;;) {
			readFully(pos, ib, 0, 20, ctx);
			int c = ib[0] & 0xff;
			final int type = (c >> 4) & 7;

			switch (type) {
			case Constants.OBJ_COMMIT:
			case Constants.OBJ_TREE:
			case Constants.OBJ_BLOB:
			case Constants.OBJ_TAG:
				return type;

			case Constants.OBJ_OFS_DELTA: {
				int p = 1;
				while ((c & 0x80) != 0) {
					c = ib[p++] & 0xff;
				}
				c = ib[p++] & 0xff;
				long ofs = c & 127;
				while ((c & 128) != 0) {
					ofs += 1;
					c = ib[p++] & 0xff;
					ofs <<= 7;
					ofs += (c & 127);
				}
				pos = pos - ofs;
				continue;
			}

			case Constants.OBJ_REF_DELTA: {
				int p = 1;
				while ((c & 0x80) != 0) {
					c = ib[p++] & 0xff;
				}
				readFully(pos + p, ib, 0, 20, ctx);
				pos = findDeltaBase(ctx, ObjectId.fromRaw(ib));
				continue;
			}

			default:
				throw new IOException(MessageFormat.format(
						JGitText.get().unknownObjectType, Integer.valueOf(type)));
			}
		}
	}

	long getObjectSize(DfsReader ctx, AnyObjectId id) throws IOException {
		final long offset = idx(ctx).findOffset(id);
		return 0 < offset ? getObjectSize(ctx, offset) : -1;
	}

	long getObjectSize(DfsReader ctx, long pos)
			throws IOException {
		final byte[] ib = ctx.tempId;
		readFully(pos, ib, 0, 20, ctx);
		int c = ib[0] & 0xff;
		final int type = (c >> 4) & 7;
		long sz = c & 15;
		int shift = 4;
		int p = 1;
		while ((c & 0x80) != 0) {
			c = ib[p++] & 0xff;
			sz += ((long) (c & 0x7f)) << shift;
			shift += 7;
		}

		long deltaAt;
		switch (type) {
		case Constants.OBJ_COMMIT:
		case Constants.OBJ_TREE:
		case Constants.OBJ_BLOB:
		case Constants.OBJ_TAG:
			return sz;

		case Constants.OBJ_OFS_DELTA:
			c = ib[p++] & 0xff;
			while ((c & 128) != 0) {
				c = ib[p++] & 0xff;
			}
			deltaAt = pos + p;
			break;

		case Constants.OBJ_REF_DELTA:
			deltaAt = pos + p + 20;
			break;

		default:
			throw new IOException(MessageFormat.format(
					JGitText.get().unknownObjectType, Integer.valueOf(type)));
		}

		try {
			return BinaryDelta.getResultSize(getDeltaHeader(ctx, deltaAt));
		} catch (DataFormatException dfe) {
			throw new CorruptObjectException(
					MessageFormat.format(
							JGitText.get().objectAtHasBadZlibStream, Long.valueOf(pos),
							getFileName()),
					dfe);
		}
	}

	void representation(DfsObjectRepresentation r, final long pos,
			DfsReader ctx, PackReverseIndex rev)
			throws IOException {
		r.offset = pos;
		final byte[] ib = ctx.tempId;
		readFully(pos, ib, 0, 20, ctx);
		int c = ib[0] & 0xff;
		int p = 1;
		final int typeCode = (c >> 4) & 7;
		while ((c & 0x80) != 0) {
			c = ib[p++] & 0xff;
		}

		long len = rev.findNextOffset(pos, length - 20) - pos;
		switch (typeCode) {
		case Constants.OBJ_COMMIT:
		case Constants.OBJ_TREE:
		case Constants.OBJ_BLOB:
		case Constants.OBJ_TAG:
			r.format = StoredObjectRepresentation.PACK_WHOLE;
			r.baseId = null;
			r.length = len - p;
			return;

		case Constants.OBJ_OFS_DELTA: {
			c = ib[p++] & 0xff;
			long ofs = c & 127;
			while ((c & 128) != 0) {
				ofs += 1;
				c = ib[p++] & 0xff;
				ofs <<= 7;
				ofs += (c & 127);
			}
			r.format = StoredObjectRepresentation.PACK_DELTA;
			r.baseId = rev.findObject(pos - ofs);
			r.length = len - p;
			return;
		}

		case Constants.OBJ_REF_DELTA: {
			readFully(pos + p, ib, 0, 20, ctx);
			r.format = StoredObjectRepresentation.PACK_DELTA;
			r.baseId = ObjectId.fromRaw(ib);
			r.length = len - p - 20;
			return;
		}

		default:
			throw new IOException(MessageFormat.format(
					JGitText.get().unknownObjectType, Integer.valueOf(typeCode)));
		}
	}

	boolean isCorrupt(long offset) {
		LongList list = corruptObjects;
		if (list == null) {
			return false;
		}
		synchronized (list) {
			return list.contains(offset);
		}
	}

	private void setCorrupt(long offset) {
		LongList list = corruptObjects;
		if (list == null) {
			synchronized (corruptObjectsLock) {
				list = corruptObjects;
				if (list == null) {
					list = new LongList();
					corruptObjects = list;
				}
			}
		}
		synchronized (list) {
			list.add(offset);
		}
	}

	private DfsBlockCache.Ref<PackIndex> loadPackIndex(
			DfsReader ctx, DfsStreamKey idxKey) throws IOException {
		try {
			ctx.stats.readIdx++;
			long start = System.nanoTime();
			try (ReadableChannel rc = ctx.db.openFile(desc, INDEX)) {
				PackIndex idx = PackIndex.read(alignTo8kBlocks(rc));
				ctx.stats.readIdxBytes += rc.position();
				index = idx;
				return new DfsBlockCache.Ref<>(
						idxKey,
						REF_POSITION,
						idx.getObjectCount() * REC_SIZE,
						idx);
			} finally {
				ctx.stats.readIdxMicros += elapsedMicros(start);
			}
		} catch (EOFException e) {
			throw new IOException(MessageFormat.format(
					DfsText.get().shortReadOfIndex,
					desc.getFileName(INDEX)), e);
		} catch (IOException e) {
			throw new IOException(MessageFormat.format(
					DfsText.get().cannotReadIndex,
					desc.getFileName(INDEX)), e);
		}
	}

	private DfsBlockCache.Ref<PackReverseIndex> loadReverseIdx(
			DfsReader ctx, DfsStreamKey revKey, PackIndex idx) {
		ctx.stats.readReverseIdx++;
		long start = System.nanoTime();
		PackReverseIndex revidx = PackReverseIndex.computeFromIndex(idx);
		reverseIndex = revidx;
		ctx.stats.readReverseIdxMicros += elapsedMicros(start);
		return new DfsBlockCache.Ref<>(
				revKey,
				REF_POSITION,
				idx.getObjectCount() * 8,
				revidx);
	}

	private DfsBlockCache.Ref<PackBitmapIndex> loadBitmapIndex(DfsReader ctx,
			DfsStreamKey bitmapKey) throws IOException {
		ctx.stats.readBitmap++;
		long start = System.nanoTime();
		try (ReadableChannel rc = ctx.db.openFile(desc, BITMAP_INDEX)) {
			long size;
			PackBitmapIndex bmidx;
			try {
				bmidx = PackBitmapIndex.read(alignTo8kBlocks(rc),
						() -> idx(ctx), () -> getReverseIdx(ctx),
						ctx.getOptions().shouldLoadRevIndexInParallel());
			} finally {
				size = rc.position();
				ctx.stats.readBitmapIdxBytes += size;
				ctx.stats.readBitmapIdxMicros += elapsedMicros(start);
			}
			bitmapIndex = bmidx;
			return new DfsBlockCache.Ref<>(
					bitmapKey, REF_POSITION, size, bmidx);
		} catch (EOFException e) {
			throw new IOException(MessageFormat.format(
					DfsText.get().shortReadOfIndex,
					desc.getFileName(BITMAP_INDEX)), e);
		} catch (IOException e) {
			throw new IOException(MessageFormat.format(
					DfsText.get().cannotReadIndex,
					desc.getFileName(BITMAP_INDEX)), e);
		}
	}

	private DfsBlockCache.Ref<CommitGraph> loadCommitGraph(DfsReader ctx,
			DfsStreamKey cgkey) throws IOException {
		ctx.stats.readCommitGraph++;
		long start = System.nanoTime();
		try (ReadableChannel rc = ctx.db.openFile(desc, COMMIT_GRAPH)) {
			long size;
			CommitGraph cg;
			try {
				cg = CommitGraphLoader.read(alignTo8kBlocks(rc));
			} finally {
				size = rc.position();
				ctx.stats.readCommitGraphBytes += size;
				ctx.stats.readCommitGraphMicros += elapsedMicros(start);
			}
			commitGraph = cg;
			return new DfsBlockCache.Ref<>(cgkey, REF_POSITION, size, cg);
		} catch (IOException e) {
			throw new IOException(
					MessageFormat.format(DfsText.get().cannotReadCommitGraph,
							desc.getFileName(COMMIT_GRAPH)),
					e);
		}
	}

	private static InputStream alignTo8kBlocks(ReadableChannel rc) {
		// TODO(ifrade): This is not reading from DFS, so the channel should
		// know better the right blocksize. I don't know why this was done in
		// the first place, verify and remove if not needed.
		InputStream in = Channels.newInputStream(rc);
		int wantSize = 8192;
		int bs = rc.blockSize();
		if (0 < bs && bs < wantSize) {
			bs = (wantSize / bs) * bs;
		} else if (bs <= 0) {
			bs = wantSize;
		}
		return new BufferedInputStream(in, bs);
	}
}
