/*
 * Copyright (C) 2008, Marek Zawirski <marek.zawirski@gmail.com> and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0 which is available at
 * https://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package org.eclipse.jgit.internal.storage.file;

import org.eclipse.jgit.errors.CorruptObjectException;
import org.eclipse.jgit.lib.ObjectId;

/**
 * <p>
 * Reverse index for forward pack index. Provides operations based on offset
 * instead of object id. Such offset-based reverse lookups are performed in
 * O(log n) time.
 * </p>
 *
 * @see PackIndex
 * @see Pack
 */
public interface PackReverseIndex {
	/**
	 * Search for object id with the specified start offset in this pack
	 * (reverse) index.
	 *
	 * @param offset
	 *            start offset of object to find.
	 * @return object id for this offset, or null if no object was found.
	 */
	ObjectId findObject(long offset);

	/**
	 * Search for the next offset to the specified offset in this pack (reverse)
	 * index.
	 *
	 * @param offset
	 *            start offset of previous object (must be valid-existing
	 *            offset).
	 * @param maxOffset
	 *            maximum offset in a pack (returned when there is no next
	 *            offset).
	 * @return offset of the next object in a pack or maxOffset if provided
	 *         offset was the last one.
	 * @throws org.eclipse.jgit.errors.CorruptObjectException
	 *             when there is no object with the provided offset.
	 */
	long findNextOffset(long offset, long maxOffset)
			throws CorruptObjectException;

	/**
	 * Find the position in the primary index of the object at the given pack
	 * offset.
	 *
	 * @param offset
	 *            the pack offset of the object
	 * @return the position in the primary index of the object
	 */
	int findPosition(long offset);

	/**
	 * Find the object that is in the given position in the primary index.
	 *
	 * @param nthPosition
	 *            the position of the object in the primary index
	 * @return the object in that position
	 */
	ObjectId findObjectByPosition(int nthPosition);
}
