/*
 * Copyright (C) 2010, Google Inc.
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

import java.io.EOFException;
import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.Deflater;

import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.LargeObjectException;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.ProgressMonitor;
import org.eclipse.jgit.storage.pack.PackConfig;
import org.eclipse.jgit.util.TemporaryBuffer;

final class DeltaWindow {
	private static final boolean NEXT_RES = false;
	private static final boolean NEXT_SRC = true;

	private final PackConfig config;
	private final DeltaCache deltaCache;
	private final ObjectReader reader;
	private final ProgressMonitor monitor;
	private final long bytesPerUnit;
	private long bytesProcessed;

	/** Maximum number of bytes to admit to the window at once. */
	private final long maxMemory;

	/** Maximum depth we should create for any delta chain. */
	private final int maxDepth;

	private final ObjectToPack[] toSearch;
	private int cur;
	private int end;

	/** Amount of memory we have loaded right now. */
	private long loaded;

	// The object we are currently considering needs a lot of state:

	/** Window entry of the object we are currently considering. */
	private DeltaWindowEntry res;

	/** If we have chosen a base, the window entry it was created from. */
	private DeltaWindowEntry bestBase;
	private int deltaLen;
	private Object deltaBuf;

	/** Used to compress cached deltas. */
	private Deflater deflater;

	DeltaWindow(PackConfig pc, DeltaCache dc, ObjectReader or,
			ProgressMonitor pm, long bpu,
			ObjectToPack[] in, int beginIndex, int endIndex) {
		config = pc;
		deltaCache = dc;
		reader = or;
		monitor = pm;
		bytesPerUnit = bpu;
		toSearch = in;
		cur = beginIndex;
		end = endIndex;

		maxMemory = Math.max(0, config.getDeltaSearchMemoryLimit());
		maxDepth = config.getMaxDeltaDepth();
		res = DeltaWindowEntry.createWindow(config.getDeltaSearchWindowSize());
	}

	synchronized DeltaTask.Slice remaining() {
		int e = end;
		int halfRemaining = (e - cur) >>> 1;
		if (0 == halfRemaining)
			return null;

		int split = e - halfRemaining;
		int h = toSearch[split].getPathHash();

		// Attempt to split on the next path after the 50% split point.
		for (int n = split + 1; n < e; n++) {
			if (h != toSearch[n].getPathHash())
				return new DeltaTask.Slice(n, e);
		}

		if (h != toSearch[cur].getPathHash()) {
			// Try to split on the path before the 50% split point.
			// Do not split the path currently being processed.
			for (int p = split - 1; cur < p; p--) {
				if (h != toSearch[p].getPathHash())
					return new DeltaTask.Slice(p + 1, e);
			}
		}
		return null;
	}

	synchronized boolean tryStealWork(DeltaTask.Slice s) {
		if (s.beginIndex <= cur || end <= s.beginIndex)
			return false;
		end = s.beginIndex;
		return true;
	}

	void search() throws IOException {
		try {
			for (;;) {
				ObjectToPack next;
				synchronized (this) {
					if (end <= cur)
						break;
					next = toSearch[cur++];
				}
				if (maxMemory != 0) {
					clear(res);
					final long need = estimateSize(next);
					DeltaWindowEntry n = res.next;
					for (; maxMemory < loaded + need && n != res; n = n.next)
						clear(n);
				}
				res.set(next);

				if (res.object.isEdge() || res.object.doNotAttemptDelta()) {
					// We don't actually want to make a delta for
					// them, just need to push them into the window
					// so they can be read by other objects.
					keepInWindow();
				} else {
					// Search for a delta for the current window slot.
					if (bytesPerUnit <= (bytesProcessed += next.getWeight())) {
						int d = (int) (bytesProcessed / bytesPerUnit);
						monitor.update(d);
						bytesProcessed -= d * bytesPerUnit;
					}
					searchInWindow();
				}
			}
		} finally {
			if (deflater != null)
				deflater.end();
		}
	}

	private static long estimateSize(ObjectToPack ent) {
		return DeltaIndex.estimateIndexSize(ent.getWeight());
	}

	private static long estimateIndexSize(DeltaWindowEntry ent) {
		if (ent.buffer == null)
			return estimateSize(ent.object);

		int len = ent.buffer.length;
		return DeltaIndex.estimateIndexSize(len) - len;
	}

	private void clear(DeltaWindowEntry ent) {
		if (ent.index != null)
			loaded -= ent.index.getIndexSize();
		else if (ent.buffer != null)
			loaded -= ent.buffer.length;
		ent.set(null);
	}

	private void searchInWindow() throws IOException {
		// Loop through the window backwards, considering every entry.
		// This lets us look at the bigger objects that came before.
		for (DeltaWindowEntry src = res.prev; src != res; src = src.prev) {
			if (src.empty())
				break;
			if (delta(src) /* == NEXT_SRC */)
				continue;
			bestBase = null;
			deltaBuf = null;
			return;
		}

		// We couldn't find a suitable delta for this object, but it may
		// still be able to act as a base for another one.
		if (bestBase == null) {
			keepInWindow();
			return;
		}

		// Select this best matching delta as the base for the object.
		//
		ObjectToPack srcObj = bestBase.object;
		ObjectToPack resObj = res.object;
		if (srcObj.isEdge()) {
			// The source (the delta base) is an edge object outside of the
			// pack. Its part of the common base set that the peer already
			// has on hand, so we don't want to send it. We have to store
			// an ObjectId and *NOT* an ObjectToPack for the base to ensure
			// the base isn't included in the outgoing pack file.
			resObj.setDeltaBase(srcObj.copy());
		} else {
			// The base is part of the pack we are sending, so it should be
			// a direct pointer to the base.
			resObj.setDeltaBase(srcObj);
		}

		int depth = srcObj.getDeltaDepth() + 1;
		resObj.setDeltaDepth(depth);
		resObj.clearReuseAsIs();
		cacheDelta(srcObj, resObj);

		if (depth < maxDepth) {
			// Reorder the window so that the best base will be tested
			// first for the next object, and the current object will
			// be the second candidate to consider before any others.
			res.makeNext(bestBase);
			res = bestBase.next;
		}

		bestBase = null;
		deltaBuf = null;
	}

	private boolean delta(final DeltaWindowEntry src)
			throws IOException {
		// Objects must use only the same type as their delta base.
		if (src.type() != res.type()) {
			keepInWindow();
			return NEXT_RES;
		}

		// If the sizes are radically different, this is a bad pairing.
		if (res.size() < src.size() >>> 4)
			return NEXT_SRC;

		int msz = deltaSizeLimit(src);
		if (msz <= 8) // Nearly impossible to fit useful delta.
			return NEXT_SRC;

		// If we have to insert a lot to make this work, find another.
		if (res.size() - src.size() > msz)
			return NEXT_SRC;

		DeltaIndex srcIndex;
		try {
			srcIndex = index(src);
		} catch (LargeObjectException tooBig) {
			// If the source is too big to work on, skip it.
			return NEXT_SRC;
		} catch (IOException notAvailable) {
			if (src.object.isEdge()) // Missing edges are OK.
				return NEXT_SRC;
			throw notAvailable;
		}

		byte[] resBuf;
		try {
			resBuf = buffer(res);
		} catch (LargeObjectException tooBig) {
			// If its too big, move on to another item.
			return NEXT_RES;
		}

		try {
			OutputStream delta = msz <= (8 << 10)
				? new ArrayStream(msz)
				: new TemporaryBuffer.Heap(msz);
			if (srcIndex.encode(delta, resBuf, msz))
				selectDeltaBase(src, delta);
		} catch (IOException deltaTooBig) {
			// Unlikely, encoder should see limit and return false.
		}
		return NEXT_SRC;
	}

	private void selectDeltaBase(DeltaWindowEntry src, OutputStream delta) {
		bestBase = src;

		if (delta instanceof ArrayStream) {
			ArrayStream a = (ArrayStream) delta;
			deltaBuf = a.buf;
			deltaLen = a.cnt;
		} else {
			TemporaryBuffer.Heap b = (TemporaryBuffer.Heap) delta;
			deltaBuf = b;
			deltaLen = (int) b.length();
		}
	}

	private int deltaSizeLimit(DeltaWindowEntry src) {
		if (bestBase == null) {
			// Any delta should be no more than 50% of the original size
			// (for text files deflate of whole form should shrink 50%).
			int n = res.size() >>> 1;

			// Evenly distribute delta size limits over allowed depth.
			// If src is non-delta (depth = 0), delta <= 50% of original.
			// If src is almost at limit (9/10), delta <= 10% of original.
			return n * (maxDepth - src.depth()) / maxDepth;
		}

		// With a delta base chosen any new delta must be "better".
		// Retain the distribution described above.
		int d = bestBase.depth();
		int n = deltaLen;

		// If src is whole (depth=0) and base is near limit (depth=9/10)
		// any delta using src can be 10x larger and still be better.
		//
		// If src is near limit (depth=9/10) and base is whole (depth=0)
		// a new delta dependent on src must be 1/10th the size.
		return n * (maxDepth - src.depth()) / (maxDepth - d);
	}

	private void cacheDelta(ObjectToPack srcObj, ObjectToPack resObj) {
		if (deltaCache.canCache(deltaLen, srcObj, resObj)) {
			try {
				byte[] zbuf = new byte[deflateBound(deltaLen)];
				ZipStream zs = new ZipStream(deflater(), zbuf);
				if (deltaBuf instanceof byte[])
					zs.write((byte[]) deltaBuf, 0, deltaLen);
				else
					((TemporaryBuffer.Heap) deltaBuf).writeTo(zs, null);
				deltaBuf = null;
				int len = zs.finish();

				resObj.setCachedDelta(deltaCache.cache(zbuf, len, deltaLen));
				resObj.setCachedSize(deltaLen);
			} catch (IOException err) {
				deltaCache.credit(deltaLen);
			} catch (OutOfMemoryError err) {
				deltaCache.credit(deltaLen);
			}
		}
	}

	private static int deflateBound(int insz) {
		return insz + ((insz + 7) >> 3) + ((insz + 63) >> 6) + 11;
	}

	private void keepInWindow() {
		res = res.next;
	}

	private DeltaIndex index(DeltaWindowEntry ent)
			throws MissingObjectException, IncorrectObjectTypeException,
			IOException, LargeObjectException {
		DeltaIndex idx = ent.index;
		if (idx == null) {
			checkLoadable(ent, estimateIndexSize(ent));

			try {
				idx = new DeltaIndex(buffer(ent));
			} catch (OutOfMemoryError noMemory) {
				LargeObjectException.OutOfMemory e;
				e = new LargeObjectException.OutOfMemory(noMemory);
				e.setObjectId(ent.object);
				throw e;
			}
			if (maxMemory != 0)
				loaded += idx.getIndexSize() - idx.getSourceSize();
			ent.index = idx;
		}
		return idx;
	}

	private byte[] buffer(DeltaWindowEntry ent) throws MissingObjectException,
			IncorrectObjectTypeException, IOException, LargeObjectException {
		byte[] buf = ent.buffer;
		if (buf == null) {
			checkLoadable(ent, ent.size());

			buf = PackWriter.buffer(config, reader, ent.object);
			if (maxMemory != 0)
				loaded += buf.length;
			ent.buffer = buf;
		}
		return buf;
	}

	private void checkLoadable(DeltaWindowEntry ent, long need) {
		if (maxMemory == 0)
			return;

		DeltaWindowEntry n = res.next;
		for (; maxMemory < loaded + need; n = n.next) {
			clear(n);
			if (n == ent)
				throw new LargeObjectException.ExceedsLimit(
						maxMemory, loaded + need);
		}
	}

	private Deflater deflater() {
		if (deflater == null)
			deflater = new Deflater(config.getCompressionLevel());
		else
			deflater.reset();
		return deflater;
	}

	static final class ZipStream extends OutputStream {
		private final Deflater deflater;

		private final byte[] zbuf;

		private int outPtr;

		ZipStream(Deflater deflater, byte[] zbuf) {
			this.deflater = deflater;
			this.zbuf = zbuf;
		}

		int finish() throws IOException {
			deflater.finish();
			for (;;) {
				if (outPtr == zbuf.length)
					throw new EOFException();

				int n = deflater.deflate(zbuf, outPtr, zbuf.length - outPtr);
				if (n == 0) {
					if (deflater.finished())
						return outPtr;
					throw new IOException();
				}
				outPtr += n;
			}
		}

		@Override
		public void write(byte[] b, int off, int len) throws IOException {
			deflater.setInput(b, off, len);
			for (;;) {
				if (outPtr == zbuf.length)
					throw new EOFException();

				int n = deflater.deflate(zbuf, outPtr, zbuf.length - outPtr);
				if (n == 0) {
					if (deflater.needsInput())
						break;
					throw new IOException();
				}
				outPtr += n;
			}
		}

		@Override
		public void write(int b) throws IOException {
			throw new UnsupportedOperationException();
		}
	}

	static final class ArrayStream extends OutputStream {
		final byte[] buf;
		int cnt;

		ArrayStream(int max) {
			buf = new byte[max];
		}

		@Override
		public void write(int b) throws IOException {
			if (cnt == buf.length)
				throw new IOException();
			buf[cnt++] = (byte) b;
		}

		@Override
		public void write(byte[] b, int off, int len) throws IOException {
			if (len > buf.length - cnt)
				throw new IOException();
			System.arraycopy(b, off, buf, cnt, len);
			cnt += len;
		}
	}
}
