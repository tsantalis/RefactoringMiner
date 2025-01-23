/*
 * Copyright (C) 2010, Google Inc. and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0 which is available at
 * https://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package org.eclipse.jgit.internal.storage.file;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.zip.Deflater;

import org.eclipse.jgit.errors.LargeObjectException;
import org.eclipse.jgit.internal.JGitText;
import org.eclipse.jgit.internal.storage.pack.DeltaEncoder;
import org.eclipse.jgit.internal.storage.pack.PackExt;
import org.eclipse.jgit.junit.JGitTestUtil;
import org.eclipse.jgit.junit.LocalDiskRepositoryTestCase;
import org.eclipse.jgit.junit.TestRepository;
import org.eclipse.jgit.junit.TestRng;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.NullProgressMonitor;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectInserter;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.ObjectStream;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevBlob;
import org.eclipse.jgit.storage.file.WindowCacheConfig;
import org.eclipse.jgit.transport.PackParser;
import org.eclipse.jgit.transport.PackedObjectInfo;
import org.eclipse.jgit.util.IO;
import org.eclipse.jgit.util.NB;
import org.eclipse.jgit.util.TemporaryBuffer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class PackTest extends LocalDiskRepositoryTestCase {
	private int streamThreshold = 16 * 1024;

	private TestRng rng;

	private FileRepository repo;

	private TestRepository<Repository> tr;

	private WindowCursor wc;

	private TestRng getRng() {
		if (rng == null)
			rng = new TestRng(JGitTestUtil.getName());
		return rng;
	}

	@Override
	@Before
	public void setUp() throws Exception {
		super.setUp();

		WindowCacheConfig cfg = new WindowCacheConfig();
		cfg.setStreamFileThreshold(streamThreshold);
		cfg.install();

		repo = createBareRepository();
		tr = new TestRepository<>(repo);
		wc = (WindowCursor) repo.newObjectReader();
	}

	@Override
	@After
	public void tearDown() throws Exception {
		if (wc != null)
			wc.close();
		new WindowCacheConfig().install();
		super.tearDown();
	}

	@Test
	public void testWhole_SmallObject() throws Exception {
		final int type = Constants.OBJ_BLOB;
		byte[] data = getRng().nextBytes(300);
		RevBlob id = tr.blob(data);
		tr.branch("master").commit().add("A", id).create();
		tr.packAndPrune();
		assertTrue("has blob", wc.has(id));

		ObjectLoader ol = wc.open(id);
		assertNotNull("created loader", ol);
		assertEquals(type, ol.getType());
		assertEquals(data.length, ol.getSize());
		assertFalse("is not large", ol.isLarge());
		assertTrue("same content", Arrays.equals(data, ol.getCachedBytes()));

		try (ObjectStream in = ol.openStream()) {
			assertNotNull("have stream", in);
			assertEquals(type, in.getType());
			assertEquals(data.length, in.getSize());
			byte[] data2 = new byte[data.length];
			IO.readFully(in, data2, 0, data.length);
			assertTrue("same content", Arrays.equals(data2, data));
			assertEquals("stream at EOF", -1, in.read());
		}
	}

	@Test
	public void testWhole_LargeObject() throws Exception {
		final int type = Constants.OBJ_BLOB;
		byte[] data = getRng().nextBytes(streamThreshold + 5);
		RevBlob id = tr.blob(data);
		tr.branch("master").commit().add("A", id).create();
		tr.packAndPrune();
		assertTrue("has blob", wc.has(id));

		ObjectLoader ol = wc.open(id);
		assertNotNull("created loader", ol);
		assertEquals(type, ol.getType());
		assertEquals(data.length, ol.getSize());
		assertTrue("is large", ol.isLarge());
		try {
			ol.getCachedBytes();
			fail("Should have thrown LargeObjectException");
		} catch (LargeObjectException tooBig) {
			assertEquals(MessageFormat.format(
					JGitText.get().largeObjectException, id.name()), tooBig
					.getMessage());
		}

		try (ObjectStream in = ol.openStream()) {
			assertNotNull("have stream", in);
			assertEquals(type, in.getType());
			assertEquals(data.length, in.getSize());
			byte[] data2 = new byte[data.length];
			IO.readFully(in, data2, 0, data.length);
			assertTrue("same content", Arrays.equals(data2, data));
			assertEquals("stream at EOF", -1, in.read());
		}
	}

	@Test
	public void testDelta_SmallObjectChain() throws Exception {
		try (ObjectInserter.Formatter fmt = new ObjectInserter.Formatter()) {
			byte[] data0 = new byte[512];
			Arrays.fill(data0, (byte) 0xf3);
			ObjectId id0 = fmt.idFor(Constants.OBJ_BLOB, data0);

			TemporaryBuffer.Heap pack = new TemporaryBuffer.Heap(64 * 1024);
			packHeader(pack, 4);
			objectHeader(pack, Constants.OBJ_BLOB, data0.length);
			deflate(pack, data0);

			byte[] data1 = clone(0x01, data0);
			byte[] delta1 = delta(data0, data1);
			ObjectId id1 = fmt.idFor(Constants.OBJ_BLOB, data1);
			objectHeader(pack, Constants.OBJ_REF_DELTA, delta1.length);
			id0.copyRawTo(pack);
			deflate(pack, delta1);

			byte[] data2 = clone(0x02, data1);
			byte[] delta2 = delta(data1, data2);
			ObjectId id2 = fmt.idFor(Constants.OBJ_BLOB, data2);
			objectHeader(pack, Constants.OBJ_REF_DELTA, delta2.length);
			id1.copyRawTo(pack);
			deflate(pack, delta2);

			byte[] data3 = clone(0x03, data2);
			byte[] delta3 = delta(data2, data3);
			ObjectId id3 = fmt.idFor(Constants.OBJ_BLOB, data3);
			objectHeader(pack, Constants.OBJ_REF_DELTA, delta3.length);
			id2.copyRawTo(pack);
			deflate(pack, delta3);

			digest(pack);
			PackParser ip = index(pack.toByteArray());
			ip.setAllowThin(true);
			ip.parse(NullProgressMonitor.INSTANCE);

			assertTrue("has blob", wc.has(id3));

			ObjectLoader ol = wc.open(id3);
			assertNotNull("created loader", ol);
			assertEquals(Constants.OBJ_BLOB, ol.getType());
			assertEquals(data3.length, ol.getSize());
			assertFalse("is large", ol.isLarge());
			assertNotNull(ol.getCachedBytes());
			assertArrayEquals(data3, ol.getCachedBytes());

			try (ObjectStream in = ol.openStream()) {
				assertNotNull("have stream", in);
				assertEquals(Constants.OBJ_BLOB, in.getType());
				assertEquals(data3.length, in.getSize());
				byte[] act = new byte[data3.length];
				IO.readFully(in, act, 0, data3.length);
				assertTrue("same content", Arrays.equals(act, data3));
				assertEquals("stream at EOF", -1, in.read());
			}
		}
	}

	@Test
	public void testDelta_FailsOver2GiB() throws Exception {
		try (ObjectInserter.Formatter fmt = new ObjectInserter.Formatter()) {
			byte[] base = new byte[] { 'a' };
			ObjectId idA = fmt.idFor(Constants.OBJ_BLOB, base);
			ObjectId idB = fmt.idFor(Constants.OBJ_BLOB, new byte[] { 'b' });

			PackedObjectInfo a = new PackedObjectInfo(idA);
			PackedObjectInfo b = new PackedObjectInfo(idB);

			TemporaryBuffer.Heap packContents = new TemporaryBuffer.Heap(64 * 1024);
			packHeader(packContents, 2);
			a.setOffset(packContents.length());
			objectHeader(packContents, Constants.OBJ_BLOB, base.length);
			deflate(packContents, base);

			ByteArrayOutputStream tmp = new ByteArrayOutputStream();
			DeltaEncoder de = new DeltaEncoder(tmp, base.length, 3L << 30);
			de.copy(0, 1);
			byte[] delta = tmp.toByteArray();
			b.setOffset(packContents.length());
			objectHeader(packContents, Constants.OBJ_REF_DELTA, delta.length);
			idA.copyRawTo(packContents);
			deflate(packContents, delta);
			byte[] footer = digest(packContents);

			File dir = new File(repo.getObjectDatabase().getDirectory(),
					"pack");
			PackFile packName = new PackFile(dir, idA.name() + ".pack");
			PackFile idxName = packName.create(PackExt.INDEX);

			try (FileOutputStream f = new FileOutputStream(packName)) {
				f.write(packContents.toByteArray());
			}

			try (FileOutputStream f = new FileOutputStream(idxName)) {
				List<PackedObjectInfo> list = new ArrayList<>();
				list.add(a);
				list.add(b);
				Collections.sort(list);
				new PackIndexWriterV1(f).write(list, footer);
			}

			Pack pack = new Pack(repo.getConfig(), packName, null);
			try {
				pack.get(wc, b);
				fail("expected LargeObjectException.ExceedsByteArrayLimit");
			} catch (LargeObjectException.ExceedsByteArrayLimit bad) {
				assertNull(bad.getObjectId());
			} finally {
				pack.close();
			}
		}
	}

	@Test
	public void testConfigurableStreamFileThreshold() throws Exception {
		byte[] data = getRng().nextBytes(300);
		RevBlob id = tr.blob(data);
		tr.branch("master").commit().add("A", id).create();
		tr.packAndPrune();
		assertTrue("has blob", wc.has(id));

		ObjectLoader ol = wc.open(id);
		try (ObjectStream in = ol.openStream()) {
			assertTrue(in instanceof ObjectStream.SmallStream);
			assertEquals(300, in.available());
		}

		wc.setStreamFileThreshold(299);
		ol = wc.open(id);
		try (ObjectStream in = ol.openStream()) {
			assertTrue(in instanceof ObjectStream.Filter);
			assertEquals(1, in.available());
		}
	}

	private static byte[] clone(int first, byte[] base) {
		byte[] r = new byte[base.length];
		System.arraycopy(base, 1, r, 1, r.length - 1);
		r[0] = (byte) first;
		return r;
	}

	private static byte[] delta(byte[] base, byte[] dest) throws IOException {
		ByteArrayOutputStream tmp = new ByteArrayOutputStream();
		DeltaEncoder de = new DeltaEncoder(tmp, base.length, dest.length);
		de.insert(dest, 0, 1);
		de.copy(1, base.length - 1);
		return tmp.toByteArray();
	}

	private static void packHeader(TemporaryBuffer.Heap pack, int cnt)
			throws IOException {
		final byte[] hdr = new byte[8];
		NB.encodeInt32(hdr, 0, 2);
		NB.encodeInt32(hdr, 4, cnt);
		pack.write(Constants.PACK_SIGNATURE);
		pack.write(hdr, 0, 8);
	}

	private static void objectHeader(TemporaryBuffer.Heap pack, int type, int sz)
			throws IOException {
		byte[] buf = new byte[8];
		int nextLength = sz >>> 4;
		buf[0] = (byte) ((nextLength > 0 ? 0x80 : 0x00) | (type << 4) | (sz & 0x0F));
		sz = nextLength;
		int n = 1;
		while (sz > 0) {
			nextLength >>>= 7;
			buf[n++] = (byte) ((nextLength > 0 ? 0x80 : 0x00) | (sz & 0x7F));
			sz = nextLength;
		}
		pack.write(buf, 0, n);
	}

	private static void deflate(TemporaryBuffer.Heap pack, byte[] content)
			throws IOException {
		final Deflater deflater = new Deflater();
		final byte[] buf = new byte[128];
		deflater.setInput(content, 0, content.length);
		deflater.finish();
		do {
			final int n = deflater.deflate(buf, 0, buf.length);
			if (n > 0)
				pack.write(buf, 0, n);
		} while (!deflater.finished());
		deflater.end();
	}

	private static byte[] digest(TemporaryBuffer.Heap buf)
			throws IOException {
		MessageDigest md = Constants.newMessageDigest();
		md.update(buf.toByteArray());
		byte[] footer = md.digest();
		buf.write(footer);
		return footer;
	}

	private ObjectInserter inserter;

	@After
	public void release() {
		if (inserter != null) {
			inserter.close();
		}
	}

	private PackParser index(byte[] raw) throws IOException {
		if (inserter == null)
			inserter = repo.newObjectInserter();
		return inserter.newPackParser(new ByteArrayInputStream(raw));
	}
}
