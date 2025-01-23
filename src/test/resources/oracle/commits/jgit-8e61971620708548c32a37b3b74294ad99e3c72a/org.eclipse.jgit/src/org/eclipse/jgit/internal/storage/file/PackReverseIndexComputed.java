/*
 * Copyright (C) 2023, Google LLC and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0 which is available at
 * https://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package org.eclipse.jgit.internal.storage.file;

import java.text.MessageFormat;

import org.eclipse.jgit.errors.CorruptObjectException;
import org.eclipse.jgit.internal.JGitText;
import org.eclipse.jgit.internal.storage.file.PackIndex.MutableEntry;
import org.eclipse.jgit.lib.ObjectId;

/**
 * Reverse index for forward pack index which is computed from the forward pack
 * index.
 * <p>
 * Creating an instance uses an insertion sort of the entries in the forward
 * index, so it runs in quadratic time on average.
 */
final class PackReverseIndexComputed implements PackReverseIndex {
	/**
	 * Index we were created from, and that has our ObjectId data.
	 */
	private final PackIndex index;

	/**
	 * The number of bytes per entry in the offsetIndex.
	 */
	private final long bucketSize;

	/**
	 * An index into the nth mapping, where the value is the position after the
	 * the last index that contains the values of the bucket. For example given
	 * offset o (and bucket = o / bucketSize), the offset will be contained in
	 * the range nth[offsetIndex[bucket - 1]] inclusive to
	 * nth[offsetIndex[bucket]] exclusive.
	 * <p>
	 * See {@link #binarySearch}
	 */
	private final int[] offsetIndex;

	/**
	 * Mapping from indices in offset order to indices in SHA-1 order.
	 */
	private final int[] nth;

	/**
	 * Create reverse index from straight/forward pack index, by indexing all
	 * its entries.
	 *
	 * @param packIndex
	 *            forward index - entries to (reverse) index.
	 */
	PackReverseIndexComputed(PackIndex packIndex) {
		index = packIndex;

		final long cnt = index.getObjectCount();
		if (cnt + 1 > Integer.MAX_VALUE) {
			throw new IllegalArgumentException(
					JGitText.get().hugeIndexesAreNotSupportedByJgitYet);
		}

		if (cnt == 0) {
			bucketSize = Long.MAX_VALUE;
			offsetIndex = new int[1];
			nth = new int[0];
			return;
		}

		final long[] offsetsBySha1 = new long[(int) cnt];

		long maxOffset = 0;
		int ith = 0;
		for (MutableEntry me : index) {
			final long o = me.getOffset();
			offsetsBySha1[ith++] = o;
			if (o > maxOffset) {
				maxOffset = o;
			}
		}

		bucketSize = maxOffset / cnt + 1;
		int[] bucketIndex = new int[(int) cnt];
		int[] bucketValues = new int[(int) cnt + 1];
		for (int oi = 0; oi < offsetsBySha1.length; oi++) {
			final long o = offsetsBySha1[oi];
			final int bucket = (int) (o / bucketSize);
			final int bucketValuesPos = oi + 1;
			final int current = bucketIndex[bucket];
			bucketIndex[bucket] = bucketValuesPos;
			bucketValues[bucketValuesPos] = current;
		}

		int nthByOffset = 0;
		nth = new int[offsetsBySha1.length];
		offsetIndex = bucketIndex; // Reuse the allocation
		for (int bi = 0; bi < bucketIndex.length; bi++) {
			final int start = nthByOffset;
			// Insertion sort of the values in the bucket.
			for (int vi = bucketIndex[bi]; vi > 0; vi = bucketValues[vi]) {
				final int nthBySha1 = vi - 1;
				final long o = offsetsBySha1[nthBySha1];
				int insertion = nthByOffset++;
				for (; start < insertion; insertion--) {
					if (o > offsetsBySha1[nth[insertion - 1]]) {
						break;
					}
					nth[insertion] = nth[insertion - 1];
				}
				nth[insertion] = nthBySha1;
			}
			offsetIndex[bi] = nthByOffset;
		}
	}

	@Override
	public ObjectId findObject(long offset) {
		final int ith = binarySearch(offset);
		if (ith < 0) {
			return null;
		}
		return index.getObjectId(nth[ith]);
	}

	@Override
	public long findNextOffset(long offset, long maxOffset)
			throws CorruptObjectException {
		final int ith = binarySearch(offset);
		if (ith < 0) {
			throw new CorruptObjectException(MessageFormat.format(JGitText
					.get().cantFindObjectInReversePackIndexForTheSpecifiedOffset,
					Long.valueOf(offset)));
		}

		if (ith + 1 == nth.length) {
			return maxOffset;
		}
		return index.getOffset(nth[ith + 1]);
	}

	@Override
	public int findPosition(long offset) {
		return binarySearch(offset);
	}

	private int binarySearch(long offset) {
		int bucket = (int) (offset / bucketSize);
		int low = bucket == 0 ? 0 : offsetIndex[bucket - 1];
		int high = offsetIndex[bucket];
		while (low < high) {
			final int mid = (low + high) >>> 1;
			final long o = index.getOffset(nth[mid]);
			if (offset < o) {
				high = mid;
			} else if (offset == o) {
				return mid;
			} else {
				low = mid + 1;
			}
		}
		return -1;
	}

	@Override
	public ObjectId findObjectByPosition(int nthPosition) {
		return index.getObjectId(nth[nthPosition]);
	}
}
