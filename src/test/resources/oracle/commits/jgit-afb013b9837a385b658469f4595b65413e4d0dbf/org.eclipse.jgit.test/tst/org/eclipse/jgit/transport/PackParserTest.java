/*
 * Copyright (C) 2009, Google Inc.
 * Copyright (C) 2008, Imran M Yousuf <imyousuf@smartitengineering.com>
 * Copyright (C) 2007-2008, Robin Rosenberg <robin.rosenberg@dewire.com>
 * Copyright (C) 2008, Shawn O. Pearce <spearce@spearce.org> and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0 which is available at
 * https://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package org.eclipse.jgit.transport;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jgit.errors.TooLargeObjectInPackException;
import org.eclipse.jgit.internal.JGitText;
import org.eclipse.jgit.internal.storage.file.ObjectDirectoryPackParser;
import org.eclipse.jgit.internal.storage.file.Pack;
import org.eclipse.jgit.junit.JGitTestUtil;
import org.eclipse.jgit.junit.RepositoryTestCase;
import org.eclipse.jgit.junit.TestRepository;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.NullProgressMonitor;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectInserter;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevBlob;
import org.eclipse.jgit.util.io.UnionInputStream;
import org.junit.After;
import org.junit.Test;

/**
 * Test indexing of git packs. A pack is read from a stream, copied
 * to a new pack and an index is created. Then the packs are tested
 * to make sure they contain the expected objects (well we don't test
 * for all of them unless the packs are very small).
 */
public class PackParserTest extends RepositoryTestCase {
	/**
	 * Test indexing one of the test packs in the egit repo. It has deltas.
	 *
	 * @throws IOException
	 */
	@Test
	public void test1() throws  IOException {
		File packFile = JGitTestUtil.getTestResourceFile("pack-34be9032ac282b11fa9babdc2b2a93ca996c9c2f.pack");
		try (InputStream is = new FileInputStream(packFile)) {
			ObjectDirectoryPackParser p = (ObjectDirectoryPackParser) index(is);
			p.parse(NullProgressMonitor.INSTANCE);
			Pack pack = p.getPack();

			assertTrue(pack.hasObject(ObjectId.fromString("4b825dc642cb6eb9a060e54bf8d69288fbee4904")));
			assertTrue(pack.hasObject(ObjectId.fromString("540a36d136cf413e4b064c2b0e0a4db60f77feab")));
			assertTrue(pack.hasObject(ObjectId.fromString("5b6e7c66c276e7610d4a73c70ec1a1f7c1003259")));
			assertTrue(pack.hasObject(ObjectId.fromString("6ff87c4664981e4397625791c8ea3bbb5f2279a3")));
			assertTrue(pack.hasObject(ObjectId.fromString("82c6b885ff600be425b4ea96dee75dca255b69e7")));
			assertTrue(pack.hasObject(ObjectId.fromString("902d5476fa249b7abc9d84c611577a81381f0327")));
			assertTrue(pack.hasObject(ObjectId.fromString("aabf2ffaec9b497f0950352b3e582d73035c2035")));
			assertTrue(pack.hasObject(ObjectId.fromString("c59759f143fb1fe21c197981df75a7ee00290799")));
		}
	}

	@Test
	public void testParsePack1ReadsObjectSizes() throws IOException {
		File packFile = JGitTestUtil.getTestResourceFile(
				"pack-34be9032ac282b11fa9babdc2b2a93ca996c9c2f.pack");

		// Sizes from git cat-file -s after unpacking in a local repo
		Map<String, Long> expected = new HashMap<>();
		// Commits
		expected.put("540a36d136cf413e4b064c2b0e0a4db60f77feab",
				Long.valueOf(191));
		expected.put("c59759f143fb1fe21c197981df75a7ee00290799",
				Long.valueOf(240));
		expected.put("82c6b885ff600be425b4ea96dee75dca255b69e7",
				Long.valueOf(245));

		// Trees
		expected.put("4b825dc642cb6eb9a060e54bf8d69288fbee4904",
				Long.valueOf(0)); // empty
		expected.put("902d5476fa249b7abc9d84c611577a81381f0327",
				Long.valueOf(35));
		expected.put("aabf2ffaec9b497f0950352b3e582d73035c2035",
				Long.valueOf(35));

		// Blobs
		expected.put("6ff87c4664981e4397625791c8ea3bbb5f2279a3",
				Long.valueOf(18787));

		// Deltas
		expected.put("5b6e7c66c276e7610d4a73c70ec1a1f7c1003259",
				Long.valueOf(18009)); // delta-oid blob


		try (InputStream is = new FileInputStream(packFile)) {
			ObjectDirectoryPackParser p = (ObjectDirectoryPackParser) index(is);
			p.parse(NullProgressMonitor.INSTANCE);
			List<PackedObjectInfo> parsedObjects = p.getSortedObjectList(null);
			for (PackedObjectInfo objInfo: parsedObjects) {
				assertEquals(objInfo.getName(), objInfo.getFullSize(),
						expected.get(objInfo.getName()).longValue());
			}
		}
	}

	/**
	 * This is just another pack. It so happens that we have two convenient pack to
	 * test with in the repository.
	 *
	 * @throws IOException
	 */
	@Test
	public void test2() throws  IOException {
		File packFile = JGitTestUtil.getTestResourceFile("pack-df2982f284bbabb6bdb59ee3fcc6eb0983e20371.pack");
		try (InputStream is = new FileInputStream(packFile)) {
			ObjectDirectoryPackParser p = (ObjectDirectoryPackParser) index(is);
			p.parse(NullProgressMonitor.INSTANCE);
			Pack pack = p.getPack();

			assertTrue(pack.hasObject(ObjectId.fromString("02ba32d3649e510002c21651936b7077aa75ffa9")));
			assertTrue(pack.hasObject(ObjectId.fromString("0966a434eb1a025db6b71485ab63a3bfbea520b6")));
			assertTrue(pack.hasObject(ObjectId.fromString("09efc7e59a839528ac7bda9fa020dc9101278680")));
			assertTrue(pack.hasObject(ObjectId.fromString("0a3d7772488b6b106fb62813c4d6d627918d9181")));
			assertTrue(pack.hasObject(ObjectId.fromString("1004d0d7ac26fbf63050a234c9b88a46075719d3")));
			assertTrue(pack.hasObject(ObjectId.fromString("10da5895682013006950e7da534b705252b03be6")));
			assertTrue(pack.hasObject(ObjectId.fromString("1203b03dc816ccbb67773f28b3c19318654b0bc8")));
			assertTrue(pack.hasObject(ObjectId.fromString("15fae9e651043de0fd1deef588aa3fbf5a7a41c6")));
			assertTrue(pack.hasObject(ObjectId.fromString("16f9ec009e5568c435f473ba3a1df732d49ce8c3")));
			assertTrue(pack.hasObject(ObjectId.fromString("1fd7d579fb6ae3fe942dc09c2c783443d04cf21e")));
			assertTrue(pack.hasObject(ObjectId.fromString("20a8ade77639491ea0bd667bf95de8abf3a434c8")));
			assertTrue(pack.hasObject(ObjectId.fromString("2675188fd86978d5bc4d7211698b2118ae3bf658")));
			// and lots more...
		}
	}

	@Test
	public void testParsePack2ReadsObjectSizes() throws IOException {
		File packFile = JGitTestUtil.getTestResourceFile(
				"pack-df2982f284bbabb6bdb59ee3fcc6eb0983e20371.pack");
		Map<String, Long> expected = new HashMap<>();
		// Deltified commit
		expected.put("d0114ab8ac326bab30e3a657a0397578c5a1af88",
				Long.valueOf(222));
		// Delta of delta of commit
		expected.put("f73b95671f326616d66b2afb3bdfcdbbce110b44",
				Long.valueOf(221));
		// Deltified tree
		expected.put("be9b45333b66013bde1c7314efc50fabd9b39c6d",
				Long.valueOf(94));

		try (InputStream is = new FileInputStream(packFile)) {
			ObjectDirectoryPackParser p = (ObjectDirectoryPackParser) index(is);
			p.parse(NullProgressMonitor.INSTANCE);
			List<PackedObjectInfo> parsedObjects = p.getSortedObjectList(null);
			// Check only the interesting objects
			int assertedObjs = 0;
			for (PackedObjectInfo objInfo : parsedObjects) {
				if (!expected.containsKey(objInfo.getName())) {
					continue;
				}
				assertEquals(objInfo.getName(), objInfo.getFullSize(),
						expected.get(objInfo.getName()).longValue());
				assertedObjs += 1;
			}
			assertEquals(assertedObjs, expected.size());
		}
	}

	@Test
	public void testTinyThinPack() throws Exception {
		RevBlob a;
		try (TestRepository d = new TestRepository<Repository>(db)) {
			db.incrementOpen();
			a = d.blob("a");
		}

		InMemoryPack pack = new InMemoryPack();
		pack.header(1);
		pack.write((Constants.OBJ_REF_DELTA) << 4 | 4);
		pack.copyRaw(a);
		pack.deflate(new byte[] { 0x1, 0x1, 0x1, 'b' });
		pack.digest();

		PackParser p = index(pack.toInputStream());
		p.setAllowThin(true);
		p.parse(NullProgressMonitor.INSTANCE);
	}

	@Test
	public void testPackWithDuplicateBlob() throws Exception {
		final byte[] data = Constants.encode("0123456789abcdefg");
		try (TestRepository<Repository> d = new TestRepository<>(db)) {
			db.incrementOpen();
			assertTrue(db.getObjectDatabase().has(d.blob(data)));
		}

		InMemoryPack pack = new InMemoryPack();
		pack.header(1);
		pack.write((Constants.OBJ_BLOB) << 4 | 0x80 | 1);
		pack.write(1);
		pack.deflate(data);
		pack.digest();

		PackParser p = index(pack.toInputStream());
		p.setAllowThin(false);
		p.parse(NullProgressMonitor.INSTANCE);
	}

	@Test
	public void testParseOfsDeltaFullSize() throws Exception {
		final byte[] data = Constants.encode("0123456789");
		try (TestRepository<Repository> d = new TestRepository<>(db)) {
			db.incrementOpen();
			assertTrue(db.getObjectDatabase().has(d.blob(data)));
		}

		InMemoryPack pack = new InMemoryPack();
		pack.header(2);
		pack.write((Constants.OBJ_BLOB) << 4 | 10); // offset 12
		pack.deflate(data);
		pack.write((Constants.OBJ_OFS_DELTA) << 4 | 4); // offset 31
		pack.write(19);
		pack.deflate(new byte[] { 0xA, 0xB, 0x1, 'b' });
		pack.digest();

		PackParser p = index(pack.toInputStream());
		p.parse(NullProgressMonitor.INSTANCE);

		List<PackedObjectInfo> sortedObjectList = p.getSortedObjectList(null);
		assertEquals(sortedObjectList.size(), 2);

		// Deltified comes first because they are sorted by SHA1
		PackedObjectInfo deltifiedObj = sortedObjectList.get(0);
		assertEquals(deltifiedObj.getName(),
				"16646543f87fb53e30b032eec7dfc88f2e717966");
		assertEquals(deltifiedObj.getOffset(), 31);
		assertEquals(deltifiedObj.getType(), Constants.OBJ_BLOB);
		assertEquals(deltifiedObj.getFullSize(), 11);

		PackedObjectInfo baseObj = sortedObjectList.get(1);
		assertEquals(baseObj.getName(),
				"ad471007bd7f5983d273b9584e5629230150fd54");
		assertEquals(baseObj.getOffset(), 12);
		assertEquals(baseObj.getType(), Constants.OBJ_BLOB);
		assertEquals(baseObj.getFullSize(), 10);
	}

	@Test
	public void testPackWithTrailingGarbage() throws Exception {
		RevBlob a;
		try (TestRepository d = new TestRepository<Repository>(db)) {
			db.incrementOpen();
			a = d.blob("a");
		}

		InMemoryPack pack = new InMemoryPack();
		pack.header(1);
		pack.write((Constants.OBJ_REF_DELTA) << 4 | 4);
		pack.copyRaw(a);
		pack.deflate(new byte[] { 0x1, 0x1, 0x1, 'b' });
		pack.digest();

		PackParser p = index(new UnionInputStream(
				pack.toInputStream(),
				new ByteArrayInputStream(new byte[] { 0x7e })));
		p.setAllowThin(true);
		p.setCheckEofAfterPackFooter(true);
		try {
			p.parse(NullProgressMonitor.INSTANCE);
			fail("Pack with trailing garbage was accepted");
		} catch (IOException err) {
			assertEquals(
					MessageFormat.format(JGitText.get().expectedEOFReceived, "\\x7e"),
					err.getMessage());
		}
	}

	@Test
	public void testMaxObjectSizeFullBlob() throws Exception {
		final byte[] data = Constants.encode("0123456789");
		try (TestRepository d = new TestRepository<Repository>(db)) {
			db.incrementOpen();
			d.blob(data);
		}

		InMemoryPack pack = new InMemoryPack();
		pack.header(1);
		pack.write((Constants.OBJ_BLOB) << 4 | 10);
		pack.deflate(data);
		pack.digest();

		PackParser p = index(pack.toInputStream());
		p.setMaxObjectSizeLimit(11);
		p.parse(NullProgressMonitor.INSTANCE);

		p = index(pack.toInputStream());
		p.setMaxObjectSizeLimit(10);
		p.parse(NullProgressMonitor.INSTANCE);

		p = index(pack.toInputStream());
		p.setMaxObjectSizeLimit(9);
		try {
			p.parse(NullProgressMonitor.INSTANCE);
			fail("PackParser should have failed");
		} catch (TooLargeObjectInPackException e) {
			assertTrue(e.getMessage().contains("10")); // obj size
			assertTrue(e.getMessage().contains("9")); // max obj size
		}
	}

	@Test
	public void testMaxObjectSizeDeltaBlock() throws Exception {
		RevBlob a;
		try (TestRepository d = new TestRepository<Repository>(db)) {
			db.incrementOpen();
			a = d.blob("a");
		}

		InMemoryPack pack = new InMemoryPack();
		pack.header(1);
		pack.write((Constants.OBJ_REF_DELTA) << 4 | 14);
		pack.copyRaw(a);
		pack.deflate(new byte[] { 1, 11, 11, 'a', '0', '1', '2', '3', '4',
				'5', '6', '7', '8', '9' });
		pack.digest();

		PackParser p = index(pack.toInputStream());
		p.setAllowThin(true);
		p.setMaxObjectSizeLimit(14);
		p.parse(NullProgressMonitor.INSTANCE);

		p = index(pack.toInputStream());
		p.setAllowThin(true);
		p.setMaxObjectSizeLimit(13);
		try {
			p.parse(NullProgressMonitor.INSTANCE);
			fail("PackParser should have failed");
		} catch (TooLargeObjectInPackException e) {
			assertTrue(e.getMessage().contains("13")); // max obj size
			assertTrue(e.getMessage().contains("14")); // delta size
		}
	}

	@Test
	public void testMaxObjectSizeDeltaResultSize() throws Exception {
		RevBlob a;
		try (TestRepository d = new TestRepository<Repository>(db)) {
			db.incrementOpen();
			a = d.blob("0123456789");
		}

		InMemoryPack pack = new InMemoryPack();
		pack.header(1);
		pack.write((Constants.OBJ_REF_DELTA) << 4 | 4);
		pack.copyRaw(a);
		pack.deflate(new byte[] { 10, 11, 1, 'a' });
		pack.digest();

		PackParser p = index(pack.toInputStream());
		p.setAllowThin(true);
		p.setMaxObjectSizeLimit(11);
		p.parse(NullProgressMonitor.INSTANCE);

		p = index(pack.toInputStream());
		p.setAllowThin(true);
		p.setMaxObjectSizeLimit(10);
		try {
			p.parse(NullProgressMonitor.INSTANCE);
			fail("PackParser should have failed");
		} catch (TooLargeObjectInPackException e) {
			assertTrue(e.getMessage().contains("11")); // result obj size
			assertTrue(e.getMessage().contains("10")); // max obj size
		}
	}

	@Test
	public void testNonMarkingInputStream() throws Exception {
		RevBlob a;
		try (TestRepository d = new TestRepository<Repository>(db)) {
			db.incrementOpen();
			a = d.blob("a");
		}

		InMemoryPack pack = new InMemoryPack();
		pack.header(1);
		pack.write((Constants.OBJ_REF_DELTA) << 4 | 4);
		pack.copyRaw(a);
		pack.deflate(new byte[] { 0x1, 0x1, 0x1, 'b' });
		pack.digest();

		InputStream in = new ByteArrayInputStream(pack.toByteArray()) {
			@Override
			public boolean markSupported() {
				return false;
			}

			@Override
			public void mark(int maxlength) {
				fail("Mark should not be called");
			}
		};

		PackParser p = index(in);
		p.setAllowThin(true);
		p.setCheckEofAfterPackFooter(false);
		p.setExpectDataAfterPackFooter(true);

		try {
			p.parse(NullProgressMonitor.INSTANCE);
			fail("PackParser should have failed");
		} catch (IOException e) {
			assertEquals(e.getMessage(),
					JGitText.get().inputStreamMustSupportMark);
		}
	}

	@Test
	public void testDataAfterPackFooterSingleRead() throws Exception {
		RevBlob a;
		try (TestRepository d = new TestRepository<Repository>(db)) {
			db.incrementOpen();
			a = d.blob("a");
		}

		InMemoryPack pack = new InMemoryPack();
		pack.header(1);
		pack.write((Constants.OBJ_REF_DELTA) << 4 | 4);
		pack.copyRaw(a);
		pack.deflate(new byte[] { 0x1, 0x1, 0x1, 'b' });
		pack.digest();

		byte packData[] = pack.toByteArray();
		byte streamData[] = new byte[packData.length + 1];
		System.arraycopy(packData, 0, streamData, 0, packData.length);
		streamData[packData.length] = 0x7e;

		InputStream in = new ByteArrayInputStream(streamData);
		PackParser p = index(in);
		p.setAllowThin(true);
		p.setCheckEofAfterPackFooter(false);
		p.setExpectDataAfterPackFooter(true);

		p.parse(NullProgressMonitor.INSTANCE);

		assertEquals(0x7e, in.read());
	}

	@Test
	public void testDataAfterPackFooterSplitObjectRead() throws Exception {
		final byte[] data = Constants.encode("0123456789");

		// Build a pack ~17k
		int objects = 900;
		InMemoryPack pack = new InMemoryPack(32 * 1024);
		pack.header(objects);

		for (int i = 0; i < objects; i++) {
			pack.write((Constants.OBJ_BLOB) << 4 | 10);
			pack.deflate(data);
		}
		pack.digest();

		byte packData[] = pack.toByteArray();
		byte streamData[] = new byte[packData.length + 1];
		System.arraycopy(packData, 0, streamData, 0, packData.length);
		streamData[packData.length] = 0x7e;
		InputStream in = new ByteArrayInputStream(streamData);
		PackParser p = index(in);
		p.setAllowThin(true);
		p.setCheckEofAfterPackFooter(false);
		p.setExpectDataAfterPackFooter(true);

		p.parse(NullProgressMonitor.INSTANCE);

		assertEquals(0x7e, in.read());
	}

	@Test
	public void testDataAfterPackFooterSplitHeaderRead() throws Exception {
		final byte[] data = Constants.encode("a");
		RevBlob b;
		try (TestRepository d = new TestRepository<Repository>(db)) {
			db.incrementOpen();
			b = d.blob(data);
		}

		int objects = 248;
		InMemoryPack pack = new InMemoryPack(32 * 1024);
		pack.header(objects + 1);

		int offset = 13;
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < offset; i++)
			sb.append(i);
		offset = sb.toString().length();
		int lenByte = (Constants.OBJ_BLOB) << 4 | (offset & 0x0F);
		offset >>= 4;
		if (offset > 0)
			lenByte |= 1 << 7;
		pack.write(lenByte);
		while (offset > 0) {
			lenByte = offset & 0x7F;
			offset >>= 6;
			if (offset > 0)
				lenByte |= 1 << 7;
			pack.write(lenByte);
		}
		pack.deflate(Constants.encode(sb.toString()));

		for (int i = 0; i < objects; i++) {
			// The last pack header written falls across the 8192 byte boundary
			// between [8189:8210]
			pack.write((Constants.OBJ_REF_DELTA) << 4 | 4);
			pack.copyRaw(b);
			pack.deflate(new byte[] { 0x1, 0x1, 0x1, 'b' });
		}
		pack.digest();

		byte packData[] = pack.toByteArray();
		byte streamData[] = new byte[packData.length + 1];
		System.arraycopy(packData, 0, streamData, 0, packData.length);
		streamData[packData.length] = 0x7e;
		InputStream in = new ByteArrayInputStream(streamData);
		PackParser p = index(in);
		p.setAllowThin(true);
		p.setCheckEofAfterPackFooter(false);
		p.setExpectDataAfterPackFooter(true);

		p.parse(NullProgressMonitor.INSTANCE);

		assertEquals(0x7e, in.read());
	}

	private ObjectInserter inserter;

	@After
	public void release() {
		if (inserter != null) {
			inserter.close();
		}
	}

	private PackParser index(InputStream in) throws IOException {
		if (inserter == null)
			inserter = db.newObjectInserter();
		return inserter.newPackParser(in);
	}
}
