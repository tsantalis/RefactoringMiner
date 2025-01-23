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

/**
 * Factory for creating instances of {@link PackReverseIndex}.
 */
public final class PackReverseIndexFactory {
	/**
	 * Compute an in-memory pack reverse index from the in-memory pack forward
	 * index. This computation uses insertion sort, which has a quadratic
	 * runtime on average.
	 *
	 * @param packIndex
	 *            the forward index to compute from
	 * @return the reverse index instance
	 */
	public static PackReverseIndex computeFromIndex(PackIndex packIndex) {
		return new PackReverseIndexComputed(packIndex);
	}
}
