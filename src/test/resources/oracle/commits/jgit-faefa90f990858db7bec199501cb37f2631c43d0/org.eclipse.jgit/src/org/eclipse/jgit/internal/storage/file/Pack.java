/*
 * Copyright (C) 2008-2009, Google Inc.
 * Copyright (C) 2007, Robin Rosenberg <robin.rosenberg@dewire.com>
 * Copyright (C) 2006-2008, Shawn O. Pearce <spearce@spearce.org> and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0 which is available at
 * https://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package org.eclipse.jgit.internal.storage.file;

import static org.eclipse.jgit.internal.storage.pack.PackExt.INDEX;
import static org.eclipse.jgit.internal.storage.pack.PackExt.KEEP;

import java.io.EOFException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel.MapMode;
import java.nio.file.AccessDeniedException;
import java.nio.file.NoSuchFileException;
import java.text.MessageFormat;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.CRC32;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

import org.eclipse.jgit.annotations.Nullable;
import org.eclipse.jgit.errors.CorruptObjectException;
import org.eclipse.jgit.errors.LargeObjectException;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.errors.NoPackSignatureException;
import org.eclipse.jgit.errors.PackInvalidException;
import org.eclipse.jgit.errors.PackMismatchException;
import org.eclipse.jgit.errors.StoredObjectRepresentationNotAvailableException;
import org.eclipse.jgit.errors.UnpackException;
import org.eclipse.jgit.errors.UnsupportedPackIndexVersionException;
import org.eclipse.jgit.errors.UnsupportedPackVersionException;
import org.eclipse.jgit.internal.JGitText;
import org.eclipse.jgit.internal.storage.pack.BinaryDelta;
import org.eclipse.jgit.internal.storage.pack.PackOutputStream;
import org.eclipse.jgit.lib.AbbreviatedObjectId;
import org.eclipse.jgit.lib.AnyObjectId;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.util.LongList;
import org.eclipse.jgit.util.NB;
import org.eclipse.jgit.util.RawParseUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A Git version 2 pack file representation. A pack file contains Git objects in
 * delta packed format yielding high compression of lots of object where some
 * objects are similar.
 */
public class Pack implements Iterable<PackIndex.MutableEntry> {
	private static final Logger LOG = LoggerFactory.getLogger(Pack.class);

	/**
	 * Sorts PackFiles to be most recently created to least recently created.
	 */
	public static final Comparator<Pack> SORT = (a, b) -> b.packLastModified
			.compareTo(a.packLastModified);

	private final PackFile packFile;

	private PackFile keepFile;

	final int hash;

	private RandomAccessFile fd;

	/** Serializes reads performed against {@link #fd}. */
	private final Object readLock = new Object();

	long length;

	private int activeWindows;

	private int activeCopyRawData;

	Instant packLastModified;

	private PackFileSnapshot fileSnapshot;

	private volatile boolean invalid;

	private volatile Exception invalidatingCause;

	@Nullable
	private PackFile bitmapIdxFile;

	private AtomicInteger transientErrorCount = new AtomicInteger();

	private byte[] packChecksum;

	private volatile PackIndex loadedIdx;

	private PackReverseIndex reverseIdx;

	private PackBitmapIndex bitmapIdx;

	/**
	 * Objects we have tried to read, and discovered to be corrupt.
	 * <p>
	 * The list is allocated after the first corruption is found, and filled in
	 * as more entries are discovered. Typically this list is never used, as
	 * pack files do not usually contain corrupt objects.
	 */
	private volatile LongList corruptObjects;

	/**
	 * Construct a reader for an existing, pre-indexed packfile.
	 *
	 * @param packFile
	 *            path of the <code>.pack</code> file holding the data.
	 * @param bitmapIdxFile
	 *            existing bitmap index file with the same base as the pack
	 */
	public Pack(File packFile, @Nullable PackFile bitmapIdxFile) {
		this.packFile = new PackFile(packFile);
		this.fileSnapshot = PackFileSnapshot.save(packFile);
		this.packLastModified = fileSnapshot.lastModifiedInstant();
		this.bitmapIdxFile = bitmapIdxFile;

		// Multiply by 31 here so we can more directly combine with another
		// value in WindowCache.hash(), without doing the multiply there.
		//
		hash = System.identityHashCode(this) * 31;
		length = Long.MAX_VALUE;
	}

	private PackIndex idx() throws IOException {
		PackIndex idx = loadedIdx;
		if (idx == null) {
			synchronized (this) {
				idx = loadedIdx;
				if (idx == null) {
					if (invalid) {
						throw new PackInvalidException(packFile,
								invalidatingCause);
					}
					try {
						long start = System.currentTimeMillis();
						PackFile idxFile = packFile.create(INDEX);
						idx = PackIndex.open(idxFile);
						if (LOG.isDebugEnabled()) {
							LOG.debug(String.format(
									"Opening pack index %s, size %.3f MB took %d ms", //$NON-NLS-1$
									idxFile.getAbsolutePath(),
									Float.valueOf(idxFile.length()
											/ (1024f * 1024)),
									Long.valueOf(System.currentTimeMillis()
											- start)));
						}

						if (packChecksum == null) {
							packChecksum = idx.packChecksum;
							fileSnapshot.setChecksum(
									ObjectId.fromRaw(packChecksum));
						} else if (!Arrays.equals(packChecksum,
								idx.packChecksum)) {
							throw new PackMismatchException(MessageFormat
									.format(JGitText.get().packChecksumMismatch,
											packFile.getPath(),
											ObjectId.fromRaw(packChecksum)
													.name(),
											ObjectId.fromRaw(idx.packChecksum)
													.name()));
						}
						loadedIdx = idx;
					} catch (InterruptedIOException e) {
						// don't invalidate the pack, we are interrupted from
						// another thread
						throw e;
					} catch (IOException e) {
						invalid = true;
						invalidatingCause = e;
						throw e;
					}
				}
			}
		}
		return idx;
	}
	/**
	 * Get the File object which locates this pack on disk.
	 *
	 * @return the File object which locates this pack on disk.
	 */
	public PackFile getPackFile() {
		return packFile;
	}

	/**
	 * Get the index for this pack file.
	 *
	 * @return the index for this pack file.
	 * @throws java.io.IOException
	 *             if an IO error occurred
	 */
	public PackIndex getIndex() throws IOException {
		return idx();
	}

	/**
	 * Get name extracted from {@code pack-*.pack} pattern.
	 *
	 * @return name extracted from {@code pack-*.pack} pattern.
	 */
	public String getPackName() {
		return packFile.getId();
	}

	/**
	 * Determine if an object is contained within the pack file.
	 * <p>
	 * For performance reasons only the index file is searched; the main pack
	 * content is ignored entirely.
	 * </p>
	 *
	 * @param id
	 *            the object to look for. Must not be null.
	 * @return true if the object is in this pack; false otherwise.
	 * @throws java.io.IOException
	 *             the index file cannot be loaded into memory.
	 */
	public boolean hasObject(AnyObjectId id) throws IOException {
		final long offset = idx().findOffset(id);
		return 0 < offset && !isCorrupt(offset);
	}

	/**
	 * Determines whether a .keep file exists for this pack file.
	 *
	 * @return true if a .keep file exist.
	 */
	public boolean shouldBeKept() {
		if (keepFile == null) {
			keepFile = packFile.create(KEEP);
		}
		return keepFile.exists();
	}

	/**
	 * Get an object from this pack.
	 *
	 * @param curs
	 *            temporary working space associated with the calling thread.
	 * @param id
	 *            the object to obtain from the pack. Must not be null.
	 * @return the object loader for the requested object if it is contained in
	 *         this pack; null if the object was not found.
	 * @throws IOException
	 *             the pack file or the index could not be read.
	 */
	ObjectLoader get(WindowCursor curs, AnyObjectId id)
			throws IOException {
		final long offset = idx().findOffset(id);
		return 0 < offset && !isCorrupt(offset) ? load(curs, offset) : null;
	}

	void resolve(Set<ObjectId> matches, AbbreviatedObjectId id, int matchLimit)
			throws IOException {
		idx().resolve(matches, id, matchLimit);
	}

	/**
	 * Close the resources utilized by this repository
	 */
	public void close() {
		WindowCache.purge(this);
		synchronized (this) {
			loadedIdx = null;
			reverseIdx = null;
		}
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * Provide iterator over entries in associated pack index, that should also
	 * exist in this pack file. Objects returned by such iterator are mutable
	 * during iteration.
	 * <p>
	 * Iterator returns objects in SHA-1 lexicographical order.
	 * </p>
	 *
	 * @see PackIndex#iterator()
	 */
	@Override
	public Iterator<PackIndex.MutableEntry> iterator() {
		try {
			return idx().iterator();
		} catch (IOException e) {
			return Collections.<PackIndex.MutableEntry> emptyList().iterator();
		}
	}

	/**
	 * Obtain the total number of objects available in this pack. This method
	 * relies on pack index, giving number of effectively available objects.
	 *
	 * @return number of objects in index of this pack, likewise in this pack
	 * @throws IOException
	 *             the index file cannot be loaded into memory.
	 */
	long getObjectCount() throws IOException {
		return idx().getObjectCount();
	}

	/**
	 * Search for object id with the specified start offset in associated pack
	 * (reverse) index.
	 *
	 * @param offset
	 *            start offset of object to find
	 * @return object id for this offset, or null if no object was found
	 * @throws IOException
	 *             the index file cannot be loaded into memory.
	 */
	ObjectId findObjectForOffset(long offset) throws IOException {
		return getReverseIdx().findObject(offset);
	}

	/**
	 * Return the @{@link FileSnapshot} associated to the underlying packfile
	 * that has been used when the object was created.
	 *
	 * @return the packfile @{@link FileSnapshot} that the object is loaded from.
	 */
	PackFileSnapshot getFileSnapshot() {
		return fileSnapshot;
	}

	AnyObjectId getPackChecksum() {
		return ObjectId.fromRaw(packChecksum);
	}

	private final byte[] decompress(final long position, final int sz,
			final WindowCursor curs) throws IOException, DataFormatException {
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

		if (curs.inflate(this, position, dstbuf, false) != sz)
			throw new EOFException(MessageFormat.format(
					JGitText.get().shortCompressedStreamAt,
					Long.valueOf(position)));
		return dstbuf;
	}

	void copyPackAsIs(PackOutputStream out, WindowCursor curs)
			throws IOException {
		// Pin the first window, this ensures the length is accurate.
		curs.pin(this, 0);
		curs.copyPackAsIs(this, length, out);
	}

	final void copyAsIs(PackOutputStream out, LocalObjectToPack src,
			boolean validate, WindowCursor curs) throws IOException,
			StoredObjectRepresentationNotAvailableException {
		beginCopyAsIs();
		try {
			copyAsIs2(out, src, validate, curs);
		} finally {
			endCopyAsIs();
		}
	}

	private void copyAsIs2(PackOutputStream out, LocalObjectToPack src,
			boolean validate, WindowCursor curs) throws IOException,
			StoredObjectRepresentationNotAvailableException {
		final CRC32 crc1 = validate ? new CRC32() : null;
		final CRC32 crc2 = validate ? new CRC32() : null;
		final byte[] buf = out.getCopyBuffer();

		// Rip apart the header so we can discover the size.
		//
		readFully(src.offset, buf, 0, 20, curs);
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

			readFully(src.offset + headerCnt, buf, 0, 20, curs);
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
		final ByteArrayWindow quickCopy;

		// Verify the object isn't corrupt before sending. If it is,
		// we report it missing instead.
		//
		try {
			quickCopy = curs.quickCopy(this, dataOffset, dataLength);

			if (validate && idx().hasCRC32Support()) {
				assert(crc1 != null);
				// Index has the CRC32 code cached, validate the object.
				//
				expectedCRC = idx().findCRC32(src);
				if (quickCopy != null) {
					quickCopy.crc32(crc1, dataOffset, (int) dataLength);
				} else {
					long pos = dataOffset;
					long cnt = dataLength;
					while (cnt > 0) {
						final int n = (int) Math.min(cnt, buf.length);
						readFully(pos, buf, 0, n, curs);
						crc1.update(buf, 0, n);
						pos += n;
						cnt -= n;
					}
				}
				if (crc1.getValue() != expectedCRC) {
					setCorrupt(src.offset);
					throw new CorruptObjectException(MessageFormat.format(
							JGitText.get().objectAtHasBadZlibStream,
							Long.valueOf(src.offset), getPackFile()));
				}
			} else if (validate) {
				// We don't have a CRC32 code in the index, so compute it
				// now while inflating the raw data to get zlib to tell us
				// whether or not the data is safe.
				//
				Inflater inf = curs.inflater();
				byte[] tmp = new byte[1024];
				if (quickCopy != null) {
					quickCopy.check(inf, tmp, dataOffset, (int) dataLength);
				} else {
					assert(crc1 != null);
					long pos = dataOffset;
					long cnt = dataLength;
					while (cnt > 0) {
						final int n = (int) Math.min(cnt, buf.length);
						readFully(pos, buf, 0, n, curs);
						crc1.update(buf, 0, n);
						inf.setInput(buf, 0, n);
						while (inf.inflate(tmp, 0, tmp.length) > 0)
							continue;
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
				assert(crc1 != null);
				expectedCRC = crc1.getValue();
			} else {
				expectedCRC = -1;
			}
		} catch (DataFormatException dataFormat) {
			setCorrupt(src.offset);

			CorruptObjectException corruptObject = new CorruptObjectException(
					MessageFormat.format(
							JGitText.get().objectAtHasBadZlibStream,
							Long.valueOf(src.offset), getPackFile()),
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
					readFully(pos, buf, 0, n, curs);
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
				readFully(pos, buf, 0, n, curs);
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
							Long.valueOf(src.offset), getPackFile()));
				}
			}
		}
	}

	boolean invalid() {
		return invalid;
	}

	void setInvalid() {
		invalid = true;
	}

	int incrementTransientErrorCount() {
		return transientErrorCount.incrementAndGet();
	}

	void resetTransientErrorCount() {
		transientErrorCount.set(0);
	}

	private void readFully(final long position, final byte[] dstbuf,
			int dstoff, final int cnt, final WindowCursor curs)
			throws IOException {
		if (curs.copy(this, position, dstbuf, dstoff, cnt) != cnt)
			throw new EOFException();
	}

	private synchronized void beginCopyAsIs()
			throws StoredObjectRepresentationNotAvailableException {
		if (++activeCopyRawData == 1 && activeWindows == 0) {
			try {
				doOpen();
			} catch (IOException thisPackNotValid) {
				throw new StoredObjectRepresentationNotAvailableException(
						thisPackNotValid);
			}
		}
	}

	private synchronized void endCopyAsIs() {
		if (--activeCopyRawData == 0 && activeWindows == 0)
			doClose();
	}

	synchronized boolean beginWindowCache() throws IOException {
		if (++activeWindows == 1) {
			if (activeCopyRawData == 0)
				doOpen();
			return true;
		}
		return false;
	}

	synchronized boolean endWindowCache() {
		final boolean r = --activeWindows == 0;
		if (r && activeCopyRawData == 0)
			doClose();
		return r;
	}

	private void doOpen() throws IOException {
		if (invalid) {
			openFail(true, invalidatingCause);
			throw new PackInvalidException(packFile, invalidatingCause);
		}
		try {
			synchronized (readLock) {
				fd = new RandomAccessFile(packFile, "r"); //$NON-NLS-1$
				length = fd.length();
				onOpenPack();
			}
		} catch (InterruptedIOException e) {
			// don't invalidate the pack, we are interrupted from another thread
			openFail(false, e);
			throw e;
		} catch (FileNotFoundException fn) {
			// don't invalidate the pack if opening an existing file failed
			// since it may be related to a temporary lack of resources (e.g.
			// max open files)
			openFail(!packFile.exists(), fn);
			throw fn;
		} catch (EOFException | AccessDeniedException | NoSuchFileException
				| CorruptObjectException | NoPackSignatureException
				| PackMismatchException | UnpackException
				| UnsupportedPackIndexVersionException
				| UnsupportedPackVersionException pe) {
			// exceptions signaling permanent problems with a pack
			openFail(true, pe);
			throw pe;
		} catch (IOException | RuntimeException ge) {
			// generic exceptions could be transient so we should not mark the
			// pack invalid to avoid false MissingObjectExceptions
			openFail(false, ge);
			throw ge;
		}
	}

	private void openFail(boolean invalidate, Exception cause) {
		activeWindows = 0;
		activeCopyRawData = 0;
		invalid = invalidate;
		invalidatingCause = cause;
		doClose();
	}

	private void doClose() {
		synchronized (readLock) {
			if (fd != null) {
				try {
					fd.close();
				} catch (IOException err) {
					// Ignore a close event. We had it open only for reading.
					// There should not be errors related to network buffers
					// not flushed, etc.
				}
				fd = null;
			}
		}
	}

	ByteArrayWindow read(long pos, int size) throws IOException {
		synchronized (readLock) {
			if (invalid || fd == null) {
				// Due to concurrency between a read and another packfile invalidation thread
				// one thread could come up to this point and then fail with NPE.
				// Detect the situation and throw a proper exception so that can be properly
				// managed by the main packfile search loop and the Git client won't receive
				// any failures.
				throw new PackInvalidException(packFile, invalidatingCause);
			}
			if (length < pos + size)
				size = (int) (length - pos);
			final byte[] buf = new byte[size];
			fd.seek(pos);
			fd.readFully(buf, 0, size);
			return new ByteArrayWindow(this, pos, buf);
		}
	}

	ByteWindow mmap(long pos, int size) throws IOException {
		synchronized (readLock) {
			if (length < pos + size)
				size = (int) (length - pos);

			MappedByteBuffer map;
			try {
				map = fd.getChannel().map(MapMode.READ_ONLY, pos, size);
			} catch (IOException ioe1) {
				// The most likely reason this failed is the JVM has run out
				// of virtual memory. We need to discard quickly, and try to
				// force the GC to finalize and release any existing mappings.
				//
				System.gc();
				System.runFinalization();
				map = fd.getChannel().map(MapMode.READ_ONLY, pos, size);
			}

			if (map.hasArray())
				return new ByteArrayWindow(this, pos, map.array());
			return new ByteBufferWindow(this, pos, map);
		}
	}

	private void onOpenPack() throws IOException {
		final PackIndex idx = idx();
		final byte[] buf = new byte[20];

		fd.seek(0);
		fd.readFully(buf, 0, 12);
		if (RawParseUtils.match(buf, 0, Constants.PACK_SIGNATURE) != 4) {
			throw new NoPackSignatureException(JGitText.get().notAPACKFile);
		}
		final long vers = NB.decodeUInt32(buf, 4);
		final long packCnt = NB.decodeUInt32(buf, 8);
		if (vers != 2 && vers != 3) {
			throw new UnsupportedPackVersionException(vers);
		}

		if (packCnt != idx.getObjectCount()) {
			throw new PackMismatchException(MessageFormat.format(
					JGitText.get().packObjectCountMismatch,
					Long.valueOf(packCnt), Long.valueOf(idx.getObjectCount()),
					getPackFile()));
		}

		fd.seek(length - 20);
		fd.readFully(buf, 0, 20);
		if (!Arrays.equals(buf, packChecksum)) {
			throw new PackMismatchException(MessageFormat.format(
					JGitText.get().packChecksumMismatch,
					getPackFile(),
					ObjectId.fromRaw(buf).name(),
					ObjectId.fromRaw(idx.packChecksum).name()));
		}
	}

	ObjectLoader load(WindowCursor curs, long pos)
			throws IOException, LargeObjectException {
		try {
			final byte[] ib = curs.tempId;
			Delta delta = null;
			byte[] data = null;
			int type = Constants.OBJ_BAD;
			boolean cached = false;

			SEARCH: for (;;) {
				readFully(pos, ib, 0, 20, curs);
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
					if (delta != null || sz < curs.getStreamFileThreshold()) {
						data = decompress(pos + p, (int) sz, curs);
					}

					if (delta != null) {
						type = typeCode;
						break SEARCH;
					}

					if (data != null) {
						return new ObjectLoader.SmallObject(typeCode, data);
					}
					return new LargePackedWholeObject(typeCode, sz, pos, p,
							this, curs.db);
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
					if (sz != delta.deltaSize)
						break SEARCH;

					DeltaBaseCache.Entry e = curs.getDeltaBaseCache().get(this, base);
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
					readFully(pos + p, ib, 0, 20, curs);
					long base = findDeltaBase(ObjectId.fromRaw(ib));
					delta = new Delta(delta, pos, (int) sz, p + 20, base);
					if (sz != delta.deltaSize)
						break SEARCH;

					DeltaBaseCache.Entry e = curs.getDeltaBaseCache().get(this, base);
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
							JGitText.get().unknownObjectType,
							Integer.valueOf(typeCode)));
				}
			}

			// At this point there is at least one delta to apply to data.
			// (Whole objects with no deltas to apply return early above.)

			if (data == null)
				throw new IOException(JGitText.get().inMemoryBufferLimitExceeded);

			assert(delta != null);
			do {
				// Cache only the base immediately before desired object.
				if (cached)
					cached = false;
				else if (delta.next == null)
					curs.getDeltaBaseCache().store(this, delta.basePos, data, type);

				pos = delta.deltaPos;

				final byte[] cmds = decompress(pos + delta.hdrLen,
						delta.deltaSize, curs);
				if (cmds == null) {
					data = null; // Discard base in case of OutOfMemoryError
					throw new LargeObjectException.OutOfMemory(new OutOfMemoryError());
				}

				final long sz = BinaryDelta.getResultSize(cmds);
				if (Integer.MAX_VALUE <= sz)
					throw new LargeObjectException.ExceedsByteArrayLimit();

				final byte[] result;
				try {
					result = new byte[(int) sz];
				} catch (OutOfMemoryError tooBig) {
					data = null; // Discard base in case of OutOfMemoryError
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
							JGitText.get().objectAtHasBadZlibStream,
							Long.valueOf(pos), getPackFile()),
					dfe);
		}
	}

	private long findDeltaBase(ObjectId baseId) throws IOException,
			MissingObjectException {
		long ofs = idx().findOffset(baseId);
		if (ofs < 0)
			throw new MissingObjectException(baseId,
					JGitText.get().missingDeltaBase);
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

	byte[] getDeltaHeader(WindowCursor wc, long pos)
			throws IOException, DataFormatException {
		// The delta stream starts as two variable length integers. If we
		// assume they are 64 bits each, we need 16 bytes to encode them,
		// plus 2 extra bytes for the variable length overhead. So 18 is
		// the longest delta instruction header.
		//
		final byte[] hdr = new byte[18];
		wc.inflate(this, pos, hdr, true /* headerOnly */);
		return hdr;
	}

	int getObjectType(WindowCursor curs, long pos) throws IOException {
		final byte[] ib = curs.tempId;
		for (;;) {
			readFully(pos, ib, 0, 20, curs);
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
				while ((c & 0x80) != 0)
					c = ib[p++] & 0xff;
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
				while ((c & 0x80) != 0)
					c = ib[p++] & 0xff;
				readFully(pos + p, ib, 0, 20, curs);
				pos = findDeltaBase(ObjectId.fromRaw(ib));
				continue;
			}

			default:
				throw new IOException(
						MessageFormat.format(JGitText.get().unknownObjectType,
								Integer.valueOf(type)));
			}
		}
	}

	long getObjectSize(WindowCursor curs, AnyObjectId id)
			throws IOException {
		final long offset = idx().findOffset(id);
		return 0 < offset ? getObjectSize(curs, offset) : -1;
	}

	long getObjectSize(WindowCursor curs, long pos)
			throws IOException {
		final byte[] ib = curs.tempId;
		readFully(pos, ib, 0, 20, curs);
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
			while ((c & 128) != 0)
				c = ib[p++] & 0xff;
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
			return BinaryDelta.getResultSize(getDeltaHeader(curs, deltaAt));
		} catch (DataFormatException e) {
			throw new CorruptObjectException(MessageFormat.format(
					JGitText.get().objectAtHasBadZlibStream, Long.valueOf(pos),
					getPackFile()), e);
		}
	}

	LocalObjectRepresentation representation(final WindowCursor curs,
			final AnyObjectId objectId) throws IOException {
		final long pos = idx().findOffset(objectId);
		if (pos < 0)
			return null;

		final byte[] ib = curs.tempId;
		readFully(pos, ib, 0, 20, curs);
		int c = ib[0] & 0xff;
		int p = 1;
		final int typeCode = (c >> 4) & 7;
		while ((c & 0x80) != 0)
			c = ib[p++] & 0xff;

		long len = (findEndOffset(pos) - pos);
		switch (typeCode) {
		case Constants.OBJ_COMMIT:
		case Constants.OBJ_TREE:
		case Constants.OBJ_BLOB:
		case Constants.OBJ_TAG:
			return LocalObjectRepresentation.newWhole(this, pos, len - p);

		case Constants.OBJ_OFS_DELTA: {
			c = ib[p++] & 0xff;
			long ofs = c & 127;
			while ((c & 128) != 0) {
				ofs += 1;
				c = ib[p++] & 0xff;
				ofs <<= 7;
				ofs += (c & 127);
			}
			ofs = pos - ofs;
			return LocalObjectRepresentation.newDelta(this, pos, len - p, ofs);
		}

		case Constants.OBJ_REF_DELTA: {
			len -= p;
			len -= Constants.OBJECT_ID_LENGTH;
			readFully(pos + p, ib, 0, 20, curs);
			ObjectId id = ObjectId.fromRaw(ib);
			return LocalObjectRepresentation.newDelta(this, pos, len, id);
		}

		default:
			throw new IOException(
					MessageFormat.format(JGitText.get().unknownObjectType,
							Integer.valueOf(typeCode)));
		}
	}

	private long findEndOffset(long startOffset)
			throws IOException, CorruptObjectException {
		final long maxOffset = length - 20;
		return getReverseIdx().findNextOffset(startOffset, maxOffset);
	}

	synchronized PackBitmapIndex getBitmapIndex() throws IOException {
		if (invalid || bitmapIdxFile == null) {
			return null;
		}
		if (bitmapIdx == null) {
			final PackBitmapIndex idx;
			try {
				idx = PackBitmapIndex.open(bitmapIdxFile, idx(),
						getReverseIdx());
			} catch (FileNotFoundException e) {
				// Once upon a time this bitmap file existed. Now it
				// has been removed. Most likely an external gc  has
				// removed this packfile and the bitmap
				bitmapIdxFile = null;
				return null;
			}

			// At this point, idx() will have set packChecksum.
			if (Arrays.equals(packChecksum, idx.packChecksum)) {
				bitmapIdx = idx;
			} else {
				bitmapIdxFile = null;
			}
		}
		return bitmapIdx;
	}

	private synchronized PackReverseIndex getReverseIdx() throws IOException {
		if (reverseIdx == null)
			reverseIdx = PackReverseIndex.computeFromIndex(idx());
		return reverseIdx;
	}

	private boolean isCorrupt(long offset) {
		LongList list = corruptObjects;
		if (list == null)
			return false;
		synchronized (list) {
			return list.contains(offset);
		}
	}

	private void setCorrupt(long offset) {
		LongList list = corruptObjects;
		if (list == null) {
			synchronized (readLock) {
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

	@SuppressWarnings("nls")
	@Override
	public String toString() {
		return "Pack [packFileName=" + packFile.getName() + ", length="
				+ packFile.length() + ", packChecksum="
				+ ObjectId.fromRaw(packChecksum).name() + "]";
	}
}
