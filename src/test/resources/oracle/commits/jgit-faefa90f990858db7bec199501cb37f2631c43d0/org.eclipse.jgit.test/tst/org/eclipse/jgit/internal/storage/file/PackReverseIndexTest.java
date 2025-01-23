/*
 * Copyright (C) 2008, Imran M Yousuf <imyousuf@smartitengineering.com>
 * Copyright (C) 2008, Marek Zawirski <marek.zawirski@gmail.com> and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0 which is available at
 * https://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package org.eclipse.jgit.internal.storage.file;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.eclipse.jgit.errors.CorruptObjectException;
import org.eclipse.jgit.internal.storage.file.PackIndex.MutableEntry;
import org.eclipse.jgit.junit.JGitTestUtil;
import org.eclipse.jgit.junit.RepositoryTestCase;
import org.junit.Before;
import org.junit.Test;

public class PackReverseIndexTest extends RepositoryTestCase {

	private PackIndex idx;

	private PackReverseIndex reverseIdx;

	/**
	 * Set up tested class instance, test constructor by the way.
	 */
	@Override
	@Before
	public void setUp() throws Exception {
		super.setUp();
		// index with both small (< 2^31) and big offsets
		idx = PackIndex.open(JGitTestUtil.getTestResourceFile("pack-huge.idx"));
		reverseIdx = PackReverseIndex.computeFromIndex(idx);
	}

	/**
	 * Test findObject() for all index entries.
	 */
	@Test
	public void testFindObject() {
		for (MutableEntry me : idx)
			assertEquals(me.toObjectId(), reverseIdx.findObject(me.getOffset()));
	}

	/**
	 * Test findObject() with illegal argument.
	 */
	@Test
	public void testFindObjectWrongOffset() {
		assertNull(reverseIdx.findObject(0));
	}

	/**
	 * Test findNextOffset() for all index entries.
	 *
	 * @throws CorruptObjectException
	 */
	@Test
	public void testFindNextOffset() throws CorruptObjectException {
		long offset = findFirstOffset();
		assertTrue(offset > 0);
		for (int i = 0; i < idx.getObjectCount(); i++) {
			long newOffset = reverseIdx.findNextOffset(offset, Long.MAX_VALUE);
			assertTrue(newOffset > offset);
			if (i == idx.getObjectCount() - 1)
				assertEquals(newOffset, Long.MAX_VALUE);
			else
				assertEquals(newOffset, idx.findOffset(reverseIdx
						.findObject(newOffset)));
			offset = newOffset;
		}
	}

	/**
	 * Test findNextOffset() with wrong illegal argument as offset.
	 */
	@Test
	public void testFindNextOffsetWrongOffset() {
		try {
			reverseIdx.findNextOffset(0, Long.MAX_VALUE);
			fail("findNextOffset() should throw exception");
		} catch (CorruptObjectException x) {
			// expected
		}
	}

	private long findFirstOffset() {
		long min = Long.MAX_VALUE;
		for (MutableEntry me : idx)
			min = Math.min(min, me.getOffset());
		return min;
	}
}
