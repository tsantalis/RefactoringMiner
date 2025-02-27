/*
 * Copyright (C) 2008-2009, Google Inc. and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0 which is available at
 * https://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package org.eclipse.jgit.dircache;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.text.MessageFormat;

import org.eclipse.jgit.errors.CorruptObjectException;
import org.eclipse.jgit.internal.JGitText;
import org.eclipse.jgit.junit.MockSystemReader;
import org.eclipse.jgit.junit.RepositoryTestCase;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.FileMode;
import org.eclipse.jgit.lib.ObjectInserter;
import org.eclipse.jgit.util.SystemReader;
import org.junit.Test;

public class DirCacheBasicTest extends RepositoryTestCase {
	@Test
	public void testReadMissing_RealIndex() throws Exception {
		final File idx = new File(db.getDirectory(), "index");
		assertFalse(idx.exists());

		final DirCache dc = db.readDirCache();
		assertNotNull(dc);
		assertEquals(0, dc.getEntryCount());
	}

	@Test
	public void testReadMissing_TempIndex() throws Exception {
		final File idx = new File(db.getDirectory(), "tmp_index");
		assertFalse(idx.exists());

		final DirCache dc = DirCache.read(idx, db.getFS());
		assertNotNull(dc);
		assertEquals(0, dc.getEntryCount());
	}

	@Test
	public void testLockMissing_RealIndex() throws Exception {
		final File idx = new File(db.getDirectory(), "index");
		final File lck = new File(db.getDirectory(), "index.lock");
		assertFalse(idx.exists());
		assertFalse(lck.exists());

		final DirCache dc = db.lockDirCache();
		assertNotNull(dc);
		assertFalse(idx.exists());
		assertTrue(lck.exists());
		assertEquals(0, dc.getEntryCount());

		dc.unlock();
		assertFalse(idx.exists());
		assertFalse(lck.exists());
	}

	@Test
	public void testLockMissing_TempIndex() throws Exception {
		final File idx = new File(db.getDirectory(), "tmp_index");
		final File lck = new File(db.getDirectory(), "tmp_index.lock");
		assertFalse(idx.exists());
		assertFalse(lck.exists());

		final DirCache dc = DirCache.lock(idx, db.getFS());
		assertNotNull(dc);
		assertFalse(idx.exists());
		assertTrue(lck.exists());
		assertEquals(0, dc.getEntryCount());

		dc.unlock();
		assertFalse(idx.exists());
		assertFalse(lck.exists());
	}

	@Test
	public void testWriteEmptyUnlock_RealIndex() throws Exception {
		final File idx = new File(db.getDirectory(), "index");
		final File lck = new File(db.getDirectory(), "index.lock");
		assertFalse(idx.exists());
		assertFalse(lck.exists());

		final DirCache dc = db.lockDirCache();
		assertEquals(0, lck.length());
		dc.write();
		assertEquals(12 + 20, lck.length());

		dc.unlock();
		assertFalse(idx.exists());
		assertFalse(lck.exists());
	}

	@Test
	public void testWriteEmptyCommit_RealIndex() throws Exception {
		final File idx = new File(db.getDirectory(), "index");
		final File lck = new File(db.getDirectory(), "index.lock");
		assertFalse(idx.exists());
		assertFalse(lck.exists());

		final DirCache dc = db.lockDirCache();
		assertEquals(0, lck.length());
		dc.write();
		assertEquals(12 + 20, lck.length());

		assertTrue(dc.commit());
		assertTrue(idx.exists());
		assertFalse(lck.exists());
		assertEquals(12 + 20, idx.length());
	}

	@Test
	public void testWriteEmptyReadEmpty_RealIndex() throws Exception {
		final File idx = new File(db.getDirectory(), "index");
		final File lck = new File(db.getDirectory(), "index.lock");
		assertFalse(idx.exists());
		assertFalse(lck.exists());
		{
			final DirCache dc = db.lockDirCache();
			dc.write();
			assertTrue(dc.commit());
			assertTrue(idx.exists());
		}
		{
			final DirCache dc = db.readDirCache();
			assertEquals(0, dc.getEntryCount());
		}
	}

	@Test
	public void testWriteEmptyLockEmpty_RealIndex() throws Exception {
		final File idx = new File(db.getDirectory(), "index");
		final File lck = new File(db.getDirectory(), "index.lock");
		assertFalse(idx.exists());
		assertFalse(lck.exists());
		{
			final DirCache dc = db.lockDirCache();
			dc.write();
			assertTrue(dc.commit());
			assertTrue(idx.exists());
		}
		{
			final DirCache dc = db.lockDirCache();
			assertEquals(0, dc.getEntryCount());
			assertTrue(idx.exists());
			assertTrue(lck.exists());
			dc.unlock();
		}
	}

	@Test
	public void testBuildThenClear() throws Exception {
		final DirCache dc = db.readDirCache();

		final String[] paths = { "a-", "a.b", "a/b", "a0b" };
		final DirCacheEntry[] ents = new DirCacheEntry[paths.length];
		for (int i = 0; i < paths.length; i++) {
			ents[i] = new DirCacheEntry(paths[i]);
			ents[i].setFileMode(FileMode.REGULAR_FILE);
		}

		final DirCacheBuilder b = dc.builder();
		for (DirCacheEntry ent : ents) {
			b.add(ent);
		}
		b.finish();
		assertFalse(dc.hasUnmergedPaths());

		assertEquals(paths.length, dc.getEntryCount());
		dc.clear();
		assertEquals(0, dc.getEntryCount());
		assertFalse(dc.hasUnmergedPaths());
	}

	@Test
	public void testDetectUnmergedPaths() throws Exception {
		final DirCache dc = db.readDirCache();
		final DirCacheEntry[] ents = new DirCacheEntry[3];

		ents[0] = new DirCacheEntry("a", 1);
		ents[0].setFileMode(FileMode.REGULAR_FILE);
		ents[1] = new DirCacheEntry("a", 2);
		ents[1].setFileMode(FileMode.REGULAR_FILE);
		ents[2] = new DirCacheEntry("a", 3);
		ents[2].setFileMode(FileMode.REGULAR_FILE);

		final DirCacheBuilder b = dc.builder();
		for (DirCacheEntry ent : ents) {
			b.add(ent);
		}
		b.finish();
		assertTrue(dc.hasUnmergedPaths());
	}

	@Test
	public void testFindOnEmpty() throws Exception {
		final DirCache dc = DirCache.newInCore();
		final byte[] path = Constants.encode("a");
		assertEquals(-1, dc.findEntry(path, path.length));
	}

	@Test
	public void testRejectInvalidWindowsPaths() throws Exception {
		SystemReader.setInstance(new MockSystemReader() {
			{
				setUnix();
			}
		});

		String path = "src/con.txt";
		DirCache dc = db.lockDirCache();
		DirCacheBuilder b = dc.builder();
		DirCacheEntry e = new DirCacheEntry(path);
		e.setFileMode(FileMode.REGULAR_FILE);
		try (ObjectInserter.Formatter formatter = new ObjectInserter.Formatter()) {
			e.setObjectId(formatter.idFor(
					Constants.OBJ_BLOB,
					Constants.encode(path)));
		}
		b.add(e);
		b.commit();
		db.readDirCache();

		SystemReader.setInstance(new MockSystemReader() {
			{
				setWindows();
			}
		});

		try {
			db.readDirCache();
			fail("should have rejected " + path);
		} catch (CorruptObjectException err) {
			assertEquals(MessageFormat.format(JGitText.get().invalidPath, path),
					err.getMessage());
			assertNotNull(err.getCause());
			assertEquals("invalid name 'CON'", err.getCause().getMessage());
		}
	}
}
