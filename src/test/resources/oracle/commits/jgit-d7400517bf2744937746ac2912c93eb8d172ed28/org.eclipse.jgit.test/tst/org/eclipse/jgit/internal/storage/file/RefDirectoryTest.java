/*
 * Copyright (C) 2010, 2013 Google Inc. and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0 which is available at
 * https://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package org.eclipse.jgit.internal.storage.file;

import static org.eclipse.jgit.lib.Constants.HEAD;
import static org.eclipse.jgit.lib.Constants.R_HEADS;
import static org.eclipse.jgit.lib.Constants.R_TAGS;
import static org.eclipse.jgit.lib.Ref.Storage.LOOSE;
import static org.eclipse.jgit.lib.Ref.Storage.NEW;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.jgit.errors.LockFailedException;
import org.eclipse.jgit.events.ListenerHandle;
import org.eclipse.jgit.events.RefsChangedEvent;
import org.eclipse.jgit.junit.LocalDiskRepositoryTestCase;
import org.eclipse.jgit.junit.Repeat;
import org.eclipse.jgit.junit.TestRepository;
import org.eclipse.jgit.lib.AnyObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Ref.Storage;
import org.eclipse.jgit.lib.RefDatabase;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTag;
import org.eclipse.jgit.util.FS;
import org.junit.Before;
import org.junit.Test;

@SuppressWarnings("boxing")
public class RefDirectoryTest extends LocalDiskRepositoryTestCase {
	private Repository diskRepo;

	private TestRepository<Repository> repo;

	private RefDirectory refdir;

	private RevCommit A;

	private RevCommit B;

	private RevTag v1_0;

	@Override
	@Before
	public void setUp() throws Exception {
		super.setUp();
		refDirectorySetup();
	}

	public void refDirectorySetup() throws Exception {
		diskRepo = createBareRepository();
		refdir = (RefDirectory) diskRepo.getRefDatabase();

		repo = new TestRepository<>(diskRepo);
		A = repo.commit().create();
		B = repo.commit(repo.getRevWalk().parseCommit(A));
		v1_0 = repo.tag("v1_0", B);
		repo.getRevWalk().parseBody(v1_0);
	}

	@Test
	public void testCreate() throws IOException {
		// setUp above created the directory. We just have to test it.
		File d = diskRepo.getDirectory();
		assertSame(diskRepo, refdir.getRepository());

		assertTrue(new File(d, "refs").isDirectory());
		assertTrue(new File(d, "logs").isDirectory());
		assertTrue(new File(d, "logs/refs").isDirectory());
		assertFalse(new File(d, "packed-refs").exists());

		assertTrue(new File(d, "refs/heads").isDirectory());
		assertTrue(new File(d, "refs/tags").isDirectory());
		assertEquals(2, new File(d, "refs").list().length);
		assertEquals(0, new File(d, "refs/heads").list().length);
		assertEquals(0, new File(d, "refs/tags").list().length);

		assertTrue(new File(d, "logs/refs/heads").isDirectory());
		assertFalse(new File(d, "logs/HEAD").exists());
		assertEquals(0, new File(d, "logs/refs/heads").list().length);

		assertEquals("ref: refs/heads/master\n", read(new File(d, HEAD)));
	}

	@Test(expected = UnsupportedOperationException.class)
	public void testVersioningNotImplemented_exactRef() throws IOException {
		assertFalse(refdir.hasVersioning());

		Ref ref = refdir.exactRef(HEAD);
		assertNotNull(ref);
		ref.getUpdateIndex(); // Not implemented on FS
	}

	@Test
	public void testVersioningNotImplemented_getRefs() throws Exception {
		assertFalse(refdir.hasVersioning());

		RevCommit C = repo.commit().parent(B).create();
		repo.update("master", C);
		List<Ref> refs = refdir.getRefs();

		for (Ref ref : refs) {
			try {
				ref.getUpdateIndex();
				fail("FS doesn't implement ref versioning");
			} catch (UnsupportedOperationException e) {
				// ok
			}
		}
	}

	@Test
	public void testGetRefs_EmptyDatabase() throws IOException {
		Map<String, Ref> all;

		all = refdir.getRefs(RefDatabase.ALL);
		assertTrue("no references", all.isEmpty());

		all = refdir.getRefs(R_HEADS);
		assertTrue("no references", all.isEmpty());

		all = refdir.getRefs(R_TAGS);
		assertTrue("no references", all.isEmpty());
	}

	@Test
	public void testGetRefs_HeadOnOneBranch() throws IOException {
		Map<String, Ref> all;
		Ref head, master;

		writeLooseRef("refs/heads/master", A);

		all = refdir.getRefs(RefDatabase.ALL);
		assertEquals(2, all.size());
		assertTrue("has HEAD", all.containsKey(HEAD));
		assertTrue("has master", all.containsKey("refs/heads/master"));

		head = all.get(HEAD);
		master = all.get("refs/heads/master");

		assertEquals(HEAD, head.getName());
		assertTrue(head.isSymbolic());
		assertSame(LOOSE, head.getStorage());
		assertSame("uses same ref as target", master, head.getTarget());

		assertEquals("refs/heads/master", master.getName());
		assertFalse(master.isSymbolic());
		assertSame(LOOSE, master.getStorage());
		assertEquals(A, master.getObjectId());
	}

	@Test
	public void testGetRefs_DeatchedHead1() throws IOException {
		Map<String, Ref> all;
		Ref head;

		writeLooseRef(HEAD, A);

		all = refdir.getRefs(RefDatabase.ALL);
		assertEquals(1, all.size());
		assertTrue("has HEAD", all.containsKey(HEAD));

		head = all.get(HEAD);

		assertEquals(HEAD, head.getName());
		assertFalse(head.isSymbolic());
		assertSame(LOOSE, head.getStorage());
		assertEquals(A, head.getObjectId());
	}

	@Test
	public void testGetRefs_DeatchedHead2() throws IOException {
		Map<String, Ref> all;
		Ref head, master;

		writeLooseRef(HEAD, A);
		writeLooseRef("refs/heads/master", B);

		all = refdir.getRefs(RefDatabase.ALL);
		assertEquals(2, all.size());

		head = all.get(HEAD);
		master = all.get("refs/heads/master");

		assertEquals(HEAD, head.getName());
		assertFalse(head.isSymbolic());
		assertSame(LOOSE, head.getStorage());
		assertEquals(A, head.getObjectId());

		assertEquals("refs/heads/master", master.getName());
		assertFalse(master.isSymbolic());
		assertSame(LOOSE, master.getStorage());
		assertEquals(B, master.getObjectId());
	}

	@Test
	public void testGetRefs_DeeplyNestedBranch() throws IOException {
		String name = "refs/heads/a/b/c/d/e/f/g/h/i/j/k";
		Map<String, Ref> all;
		Ref r;

		writeLooseRef(name, A);

		all = refdir.getRefs(RefDatabase.ALL);
		assertEquals(1, all.size());

		r = all.get(name);
		assertEquals(name, r.getName());
		assertFalse(r.isSymbolic());
		assertSame(LOOSE, r.getStorage());
		assertEquals(A, r.getObjectId());
	}

	@Test
	public void testGetRefs_HeadBranchNotBorn() throws IOException {
		Map<String, Ref> all;
		Ref a, b;

		writeLooseRef("refs/heads/A", A);
		writeLooseRef("refs/heads/B", B);

		all = refdir.getRefs(RefDatabase.ALL);
		assertEquals(2, all.size());
		assertFalse("no HEAD", all.containsKey(HEAD));

		a = all.get("refs/heads/A");
		b = all.get("refs/heads/B");

		assertEquals(A, a.getObjectId());
		assertEquals(B, b.getObjectId());

		assertEquals("refs/heads/A", a.getName());
		assertEquals("refs/heads/B", b.getName());
	}

	@Test
	public void testGetRefs_LooseOverridesPacked() throws IOException {
		Map<String, Ref> heads;
		Ref a;

		writeLooseRef("refs/heads/master", B);
		writePackedRef("refs/heads/master", A);

		heads = refdir.getRefs(R_HEADS);
		assertEquals(1, heads.size());

		a = heads.get("master");
		assertEquals("refs/heads/master", a.getName());
		assertEquals(B, a.getObjectId());
	}

	@Test
	public void testGetRefs_IgnoresGarbageRef1() throws IOException {
		Map<String, Ref> heads;
		Ref a;

		writeLooseRef("refs/heads/A", A);
		write(new File(diskRepo.getDirectory(), "refs/heads/bad"), "FAIL\n");

		heads = refdir.getRefs(RefDatabase.ALL);
		assertEquals(1, heads.size());

		a = heads.get("refs/heads/A");
		assertEquals("refs/heads/A", a.getName());
		assertEquals(A, a.getObjectId());
	}

	@Test
	public void testGetRefs_IgnoresGarbageRef2() throws IOException {
		Map<String, Ref> heads;
		Ref a;

		writeLooseRef("refs/heads/A", A);
		write(new File(diskRepo.getDirectory(), "refs/heads/bad"), "");

		heads = refdir.getRefs(RefDatabase.ALL);
		assertEquals(1, heads.size());

		a = heads.get("refs/heads/A");
		assertEquals("refs/heads/A", a.getName());
		assertEquals(A, a.getObjectId());
	}

	@Test
	public void testGetRefs_IgnoresGarbageRef3() throws IOException {
		Map<String, Ref> heads;
		Ref a;

		writeLooseRef("refs/heads/A", A);
		write(new File(diskRepo.getDirectory(), "refs/heads/bad"), "\n");

		heads = refdir.getRefs(RefDatabase.ALL);
		assertEquals(1, heads.size());

		a = heads.get("refs/heads/A");
		assertEquals("refs/heads/A", a.getName());
		assertEquals(A, a.getObjectId());
	}

	@Test
	public void testGetRefs_IgnoresGarbageRef4() throws IOException {
		Map<String, Ref> heads;
		Ref a, b, c;

		writeLooseRef("refs/heads/A", A);
		writeLooseRef("refs/heads/B", B);
		writeLooseRef("refs/heads/C", A);
		heads = refdir.getRefs(RefDatabase.ALL);
		assertEquals(3, heads.size());
		assertTrue(heads.containsKey("refs/heads/A"));
		assertTrue(heads.containsKey("refs/heads/B"));
		assertTrue(heads.containsKey("refs/heads/C"));

		writeLooseRef("refs/heads/B", "FAIL\n");

		heads = refdir.getRefs(RefDatabase.ALL);
		assertEquals(2, heads.size());

		a = heads.get("refs/heads/A");
		b = heads.get("refs/heads/B");
		c = heads.get("refs/heads/C");

		assertEquals("refs/heads/A", a.getName());
		assertEquals(A, a.getObjectId());

		assertNull("no refs/heads/B", b);

		assertEquals("refs/heads/C", c.getName());
		assertEquals(A, c.getObjectId());
	}

	@Test
	public void testGetRefs_ExcludingPrefixes() throws IOException {
		writeLooseRef("refs/heads/A", A);
		writeLooseRef("refs/heads/B", B);
		writeLooseRef("refs/tags/tag", A);
		writeLooseRef("refs/something/something", B);
		writeLooseRef("refs/aaa/aaa", A);

		Set<String> toExclude = new HashSet<>();
		toExclude.add("refs/aaa/");
		toExclude.add("refs/heads/");
		List<Ref> refs = refdir.getRefsByPrefixWithExclusions(RefDatabase.ALL, toExclude);

		assertEquals(2, refs.size());
		assertTrue(refs.contains(refdir.exactRef("refs/tags/tag")));
		assertTrue(refs.contains(refdir.exactRef("refs/something/something")));
	}

	@Test
	public void testFirstExactRef_IgnoresGarbageRef() throws IOException {
		writeLooseRef("refs/heads/A", A);
		write(new File(diskRepo.getDirectory(), "refs/heads/bad"), "FAIL\n");

		Ref a = refdir.firstExactRef("refs/heads/bad", "refs/heads/A");
		assertEquals("refs/heads/A", a.getName());
		assertEquals(A, a.getObjectId());
	}

	@Test
	public void testExactRef_IgnoresGarbageRef() throws IOException {
		writeLooseRef("refs/heads/A", A);
		write(new File(diskRepo.getDirectory(), "refs/heads/bad"), "FAIL\n");

		Map<String, Ref> refs =
				refdir.exactRef("refs/heads/bad", "refs/heads/A");

		assertNull("no refs/heads/bad", refs.get("refs/heads/bad"));

		Ref a = refs.get("refs/heads/A");
		assertEquals("refs/heads/A", a.getName());
		assertEquals(A, a.getObjectId());

		assertEquals(1, refs.size());
	}

	@Test
	public void testGetRefs_InvalidName() throws IOException {
		writeLooseRef("refs/heads/A", A);

		assertTrue("empty refs/heads", refdir.getRefs("refs/heads").isEmpty());
		assertTrue("empty objects", refdir.getRefs("objects").isEmpty());
		assertTrue("empty objects/", refdir.getRefs("objects/").isEmpty());
	}

	@Test
	public void testReadNotExistingBranchConfig() throws IOException {
		assertNull("find branch config", refdir.findRef("config"));
		assertNull("find branch config", refdir.findRef("refs/heads/config"));
	}

	@Test
	public void testReadBranchConfig() throws IOException {
		writeLooseRef("refs/heads/config", A);

		assertNotNull("find branch config", refdir.findRef("config"));
	}

	@Test
	public void testGetRefs_HeadsOnly_AllLoose() throws IOException {
		Map<String, Ref> heads;
		Ref a, b;

		writeLooseRef("refs/heads/A", A);
		writeLooseRef("refs/heads/B", B);
		writeLooseRef("refs/tags/v1.0", v1_0);

		heads = refdir.getRefs(R_HEADS);
		assertEquals(2, heads.size());

		a = heads.get("A");
		b = heads.get("B");

		assertEquals("refs/heads/A", a.getName());
		assertEquals("refs/heads/B", b.getName());

		assertEquals(A, a.getObjectId());
		assertEquals(B, b.getObjectId());
	}

	@Test
	public void testGetRefs_HeadsOnly_AllPacked1() throws IOException {
		Map<String, Ref> heads;
		Ref a;

		deleteLooseRef(HEAD);
		writePackedRef("refs/heads/A", A);

		heads = refdir.getRefs(R_HEADS);
		assertEquals(1, heads.size());

		a = heads.get("A");

		assertEquals("refs/heads/A", a.getName());
		assertEquals(A, a.getObjectId());
	}

	@Test
	public void testGetRefs_HeadsOnly_SymrefToPacked() throws IOException {
		Map<String, Ref> heads;
		Ref master, other;

		writeLooseRef("refs/heads/other", "ref: refs/heads/master\n");
		writePackedRef("refs/heads/master", A);

		heads = refdir.getRefs(R_HEADS);
		assertEquals(2, heads.size());

		master = heads.get("master");
		other = heads.get("other");

		assertEquals("refs/heads/master", master.getName());
		assertEquals(A, master.getObjectId());

		assertEquals("refs/heads/other", other.getName());
		assertEquals(A, other.getObjectId());
		assertSame(master, other.getTarget());
	}

	@Test
	public void testGetRefs_HeadsOnly_Mixed() throws IOException {
		Map<String, Ref> heads;
		Ref a, b;

		writeLooseRef("refs/heads/A", A);
		writeLooseRef("refs/heads/B", B);
		writePackedRef("refs/tags/v1.0", v1_0);

		heads = refdir.getRefs(R_HEADS);
		assertEquals(2, heads.size());

		a = heads.get("A");
		b = heads.get("B");

		assertEquals("refs/heads/A", a.getName());
		assertEquals("refs/heads/B", b.getName());

		assertEquals(A, a.getObjectId());
		assertEquals(B, b.getObjectId());
	}

	@Test
	public void testFirstExactRef_Mixed() throws IOException {
		writeLooseRef("refs/heads/A", A);
		writePackedRef("refs/tags/v1.0", v1_0);

		Ref a = refdir.firstExactRef("refs/heads/A", "refs/tags/v1.0");
		Ref one = refdir.firstExactRef("refs/tags/v1.0", "refs/heads/A");

		assertEquals("refs/heads/A", a.getName());
		assertEquals("refs/tags/v1.0", one.getName());

		assertEquals(A, a.getObjectId());
		assertEquals(v1_0, one.getObjectId());
	}

	@Test
	public void testGetRefs_TagsOnly_AllLoose() throws IOException {
		Map<String, Ref> tags;
		Ref a;

		writeLooseRef("refs/heads/A", A);
		writeLooseRef("refs/tags/v1.0", v1_0);

		tags = refdir.getRefs(R_TAGS);
		assertEquals(1, tags.size());

		a = tags.get("v1.0");

		assertEquals("refs/tags/v1.0", a.getName());
		assertEquals(v1_0, a.getObjectId());
	}

	@Test
	public void testGetRefs_LooseSortedCorrectly() throws IOException {
		Map<String, Ref> refs;

		writeLooseRef("refs/heads/project1/A", A);
		writeLooseRef("refs/heads/project1-B", B);

		refs = refdir.getRefs(RefDatabase.ALL);
		assertEquals(2, refs.size());
		assertEquals(A, refs.get("refs/heads/project1/A").getObjectId());
		assertEquals(B, refs.get("refs/heads/project1-B").getObjectId());
	}

	@Test
	public void testGetRefs_LooseSorting_Bug_348834() throws IOException {
		Map<String, Ref> refs;
		final int[] count = new int[1];

		ListenerHandle listener = Repository.getGlobalListenerList()
				.addRefsChangedListener((RefsChangedEvent event) -> {
					count[0]++;
				});

		// RefsChangedEvent on the first attempt to read a ref is not expected
		// to be triggered (See Iea3a5035b0a1410b80b09cf53387b22b78b18018), so
		// create an update and fire pending events to ensure subsequent events
		// are fired.
		writeLooseRef("refs/heads/test", A);
		refs = refdir.getRefs(RefDatabase.ALL);
		count[0] = 0;
		int origSize = refs.size();

		writeLooseRef("refs/heads/my/a+b", A);
		writeLooseRef("refs/heads/my/a/b/c", B);

		refs = refdir.getRefs(RefDatabase.ALL);
		assertEquals(1, count[0]);
		assertEquals(2, refs.size() - origSize);
		assertEquals(A, refs.get("refs/heads/my/a+b").getObjectId());
		assertEquals(B, refs.get("refs/heads/my/a/b/c").getObjectId());

		refs = refdir.getRefs(RefDatabase.ALL);
		assertEquals(1, count[0]); // Bug 348834 multiple RefsChangedEvents
		listener.remove();
	}

	@Test
	public void testGetRefs_TagsOnly_AllPacked() throws IOException {
		Map<String, Ref> tags;
		Ref a;

		deleteLooseRef(HEAD);
		writePackedRef("refs/tags/v1.0", v1_0);

		tags = refdir.getRefs(R_TAGS);
		assertEquals(1, tags.size());

		a = tags.get("v1.0");

		assertEquals("refs/tags/v1.0", a.getName());
		assertEquals(v1_0, a.getObjectId());
	}

	@Test
	public void testGetRefs_DiscoversNewLoose1() throws IOException {
		Map<String, Ref> orig, next;
		Ref orig_r, next_r;

		writeLooseRef("refs/heads/master", A);
		orig = refdir.getRefs(RefDatabase.ALL);

		writeLooseRef("refs/heads/next", B);
		next = refdir.getRefs(RefDatabase.ALL);

		assertEquals(2, orig.size());
		assertEquals(3, next.size());

		assertFalse(orig.containsKey("refs/heads/next"));
		assertTrue(next.containsKey("refs/heads/next"));

		orig_r = orig.get("refs/heads/master");
		next_r = next.get("refs/heads/master");
		assertEquals(A, orig_r.getObjectId());
		assertSame("uses cached instance", orig_r, next_r);
		assertSame("same HEAD", orig_r, orig.get(HEAD).getTarget());
		assertSame("same HEAD", orig_r, next.get(HEAD).getTarget());

		next_r = next.get("refs/heads/next");
		assertSame(LOOSE, next_r.getStorage());
		assertEquals(B, next_r.getObjectId());
	}

	@Test
	public void testGetRefs_DiscoversNewLoose2() throws IOException {
		Map<String, Ref> orig, next, news;

		writeLooseRef("refs/heads/pu", A);
		orig = refdir.getRefs(RefDatabase.ALL);

		writeLooseRef("refs/heads/new/B", B);
		news = refdir.getRefs("refs/heads/new/");
		next = refdir.getRefs(RefDatabase.ALL);

		assertEquals(1, orig.size());
		assertEquals(2, next.size());
		assertEquals(1, news.size());

		assertTrue(orig.containsKey("refs/heads/pu"));
		assertTrue(next.containsKey("refs/heads/pu"));
		assertFalse(news.containsKey("refs/heads/pu"));

		assertFalse(orig.containsKey("refs/heads/new/B"));
		assertTrue(next.containsKey("refs/heads/new/B"));
		assertTrue(news.containsKey("B"));
	}

	@Test
	public void testGetRefs_DiscoversModifiedLoose() throws IOException {
		Map<String, Ref> all;

		writeLooseRef("refs/heads/master", A);
		all = refdir.getRefs(RefDatabase.ALL);
		assertEquals(A, all.get(HEAD).getObjectId());

		writeLooseRef("refs/heads/master", B);
		all = refdir.getRefs(RefDatabase.ALL);
		assertEquals(B, all.get(HEAD).getObjectId());
	}

	@Repeat(n = 100, abortOnFailure = false)
	@Test
	public void testFindRef_DiscoversModifiedLoose() throws IOException {
		Map<String, Ref> all;

		writeLooseRef("refs/heads/master", A);
		all = refdir.getRefs(RefDatabase.ALL);
		assertEquals(A, all.get(HEAD).getObjectId());

		writeLooseRef("refs/heads/master", B);

		Ref master = refdir.findRef("refs/heads/master");
		assertEquals(B, master.getObjectId());
	}

	@Test
	public void testGetRefs_DiscoversDeletedLoose1() throws IOException {
		Map<String, Ref> orig, next;
		Ref orig_r, next_r;

		writeLooseRef("refs/heads/B", B);
		writeLooseRef("refs/heads/master", A);
		orig = refdir.getRefs(RefDatabase.ALL);

		deleteLooseRef("refs/heads/B");
		next = refdir.getRefs(RefDatabase.ALL);

		assertEquals(3, orig.size());
		assertEquals(2, next.size());

		assertTrue(orig.containsKey("refs/heads/B"));
		assertFalse(next.containsKey("refs/heads/B"));

		orig_r = orig.get("refs/heads/master");
		next_r = next.get("refs/heads/master");
		assertEquals(A, orig_r.getObjectId());
		assertSame("uses cached instance", orig_r, next_r);
		assertSame("same HEAD", orig_r, orig.get(HEAD).getTarget());
		assertSame("same HEAD", orig_r, next.get(HEAD).getTarget());

		orig_r = orig.get("refs/heads/B");
		assertSame(LOOSE, orig_r.getStorage());
		assertEquals(B, orig_r.getObjectId());
	}

	@Test
	public void testFindRef_DiscoversDeletedLoose() throws IOException {
		Map<String, Ref> all;

		writeLooseRef("refs/heads/master", A);
		all = refdir.getRefs(RefDatabase.ALL);
		assertEquals(A, all.get(HEAD).getObjectId());

		deleteLooseRef("refs/heads/master");
		assertNull(refdir.findRef("refs/heads/master"));
		assertTrue(refdir.getRefs(RefDatabase.ALL).isEmpty());
	}

	@Test
	public void testGetRefs_DiscoversDeletedLoose2() throws IOException {
		Map<String, Ref> orig, next;

		writeLooseRef("refs/heads/master", A);
		writeLooseRef("refs/heads/pu", B);
		orig = refdir.getRefs(RefDatabase.ALL);

		deleteLooseRef("refs/heads/pu");
		next = refdir.getRefs(RefDatabase.ALL);

		assertEquals(3, orig.size());
		assertEquals(2, next.size());

		assertTrue(orig.containsKey("refs/heads/pu"));
		assertFalse(next.containsKey("refs/heads/pu"));
	}

	@Test
	public void testGetRefs_DiscoversDeletedLoose3() throws IOException {
		Map<String, Ref> orig, next;

		writeLooseRef("refs/heads/master", A);
		writeLooseRef("refs/heads/next", B);
		writeLooseRef("refs/heads/pu", B);
		writeLooseRef("refs/tags/v1.0", v1_0);
		orig = refdir.getRefs(RefDatabase.ALL);

		deleteLooseRef("refs/heads/pu");
		deleteLooseRef("refs/heads/next");
		next = refdir.getRefs(RefDatabase.ALL);

		assertEquals(5, orig.size());
		assertEquals(3, next.size());

		assertTrue(orig.containsKey("refs/heads/pu"));
		assertTrue(orig.containsKey("refs/heads/next"));
		assertFalse(next.containsKey("refs/heads/pu"));
		assertFalse(next.containsKey("refs/heads/next"));
	}

	@Test
	public void testGetRefs_DiscoversDeletedLoose4() throws IOException {
		Map<String, Ref> orig, next;
		Ref orig_r, next_r;

		writeLooseRef("refs/heads/B", B);
		writeLooseRef("refs/heads/master", A);
		orig = refdir.getRefs(RefDatabase.ALL);

		deleteLooseRef("refs/heads/master");
		next = refdir.getRefs("refs/heads/");

		assertEquals(3, orig.size());
		assertEquals(1, next.size());

		assertTrue(orig.containsKey("refs/heads/B"));
		assertTrue(orig.containsKey("refs/heads/master"));
		assertTrue(next.containsKey("B"));
		assertFalse(next.containsKey("master"));

		orig_r = orig.get("refs/heads/B");
		next_r = next.get("B");
		assertEquals(B, orig_r.getObjectId());
		assertSame("uses cached instance", orig_r, next_r);
	}

	@Test
	public void testGetRefs_DiscoversDeletedLoose5() throws IOException {
		Map<String, Ref> orig, next;

		writeLooseRef("refs/heads/master", A);
		writeLooseRef("refs/heads/pu", B);
		orig = refdir.getRefs(RefDatabase.ALL);

		deleteLooseRef("refs/heads/pu");
		writeLooseRef("refs/tags/v1.0", v1_0);
		next = refdir.getRefs(RefDatabase.ALL);

		assertEquals(3, orig.size());
		assertEquals(3, next.size());

		assertTrue(orig.containsKey("refs/heads/pu"));
		assertFalse(orig.containsKey("refs/tags/v1.0"));
		assertFalse(next.containsKey("refs/heads/pu"));
		assertTrue(next.containsKey("refs/tags/v1.0"));
	}

	@Test
	public void testGetRefs_SkipsLockFiles() throws IOException {
		Map<String, Ref> all;

		writeLooseRef("refs/heads/master", A);
		writeLooseRef("refs/heads/pu.lock", B);
		all = refdir.getRefs(RefDatabase.ALL);

		assertEquals(2, all.size());

		assertTrue(all.containsKey(HEAD));
		assertTrue(all.containsKey("refs/heads/master"));
		assertFalse(all.containsKey("refs/heads/pu.lock"));
	}

	@Test
	public void testGetRefs_CycleInSymbolicRef() throws IOException {
		Map<String, Ref> all;
		Ref r;

		writeLooseRef("refs/1", "ref: refs/2\n");
		writeLooseRef("refs/2", "ref: refs/3\n");
		writeLooseRef("refs/3", "ref: refs/4\n");
		writeLooseRef("refs/4", "ref: refs/5\n");
		writeLooseRef("refs/5", "ref: refs/end\n");
		writeLooseRef("refs/end", A);

		all = refdir.getRefs(RefDatabase.ALL);
		r = all.get("refs/1");
		assertNotNull("has 1", r);

		assertEquals("refs/1", r.getName());
		assertEquals(A, r.getObjectId());
		assertTrue(r.isSymbolic());

		r = r.getTarget();
		assertEquals("refs/2", r.getName());
		assertEquals(A, r.getObjectId());
		assertTrue(r.isSymbolic());

		r = r.getTarget();
		assertEquals("refs/3", r.getName());
		assertEquals(A, r.getObjectId());
		assertTrue(r.isSymbolic());

		r = r.getTarget();
		assertEquals("refs/4", r.getName());
		assertEquals(A, r.getObjectId());
		assertTrue(r.isSymbolic());

		r = r.getTarget();
		assertEquals("refs/5", r.getName());
		assertEquals(A, r.getObjectId());
		assertTrue(r.isSymbolic());

		r = r.getTarget();
		assertEquals("refs/end", r.getName());
		assertEquals(A, r.getObjectId());
		assertFalse(r.isSymbolic());

		writeLooseRef("refs/5", "ref: refs/6\n");
		writeLooseRef("refs/6", "ref: refs/end\n");
		all = refdir.getRefs(RefDatabase.ALL);
		r = all.get("refs/1");
		assertNull("mising 1 due to cycle", r);
	}

	@Test
	public void testFindRef_CycleInSymbolicRef() throws IOException {
		Ref r;

		writeLooseRef("refs/1", "ref: refs/2\n");
		writeLooseRef("refs/2", "ref: refs/3\n");
		writeLooseRef("refs/3", "ref: refs/4\n");
		writeLooseRef("refs/4", "ref: refs/5\n");
		writeLooseRef("refs/5", "ref: refs/end\n");
		writeLooseRef("refs/end", A);

		r = refdir.findRef("1");
		assertEquals("refs/1", r.getName());
		assertEquals(A, r.getObjectId());
		assertTrue(r.isSymbolic());

		writeLooseRef("refs/5", "ref: refs/6\n");
		writeLooseRef("refs/6", "ref: refs/end\n");

		r = refdir.findRef("1");
		assertNull("missing 1 due to cycle", r);

		writeLooseRef("refs/heads/1", B);

		r = refdir.findRef("1");
		assertEquals("refs/heads/1", r.getName());
		assertEquals(B, r.getObjectId());
		assertFalse(r.isSymbolic());
	}

	@Test
	public void testGetRefs_PackedNotPeeled_Sorted() throws IOException {
		Map<String, Ref> all;

		writePackedRefs("" + //
				A.name() + " refs/heads/master\n" + //
				B.name() + " refs/heads/other\n" + //
				v1_0.name() + " refs/tags/v1.0\n");
		all = refdir.getRefs(RefDatabase.ALL);

		assertEquals(4, all.size());
		final Ref head = all.get(HEAD);
		final Ref master = all.get("refs/heads/master");
		final Ref other = all.get("refs/heads/other");
		final Ref tag = all.get("refs/tags/v1.0");

		assertEquals(A, master.getObjectId());
		assertFalse(master.isPeeled());
		assertNull(master.getPeeledObjectId());

		assertEquals(B, other.getObjectId());
		assertFalse(other.isPeeled());
		assertNull(other.getPeeledObjectId());

		assertSame(master, head.getTarget());
		assertEquals(A, head.getObjectId());
		assertFalse(head.isPeeled());
		assertNull(head.getPeeledObjectId());

		assertEquals(v1_0, tag.getObjectId());
		assertFalse(tag.isPeeled());
		assertNull(tag.getPeeledObjectId());
	}

	@Test
	public void testFindRef_PackedNotPeeled_WrongSort() throws IOException {
		writePackedRefs("" + //
				v1_0.name() + " refs/tags/v1.0\n" + //
				B.name() + " refs/heads/other\n" + //
				A.name() + " refs/heads/master\n");

		final Ref head = refdir.findRef(HEAD);
		final Ref master = refdir.findRef("refs/heads/master");
		final Ref other = refdir.findRef("refs/heads/other");
		final Ref tag = refdir.findRef("refs/tags/v1.0");

		assertEquals(A, master.getObjectId());
		assertFalse(master.isPeeled());
		assertNull(master.getPeeledObjectId());

		assertEquals(B, other.getObjectId());
		assertFalse(other.isPeeled());
		assertNull(other.getPeeledObjectId());

		assertSame(master, head.getTarget());
		assertEquals(A, head.getObjectId());
		assertFalse(head.isPeeled());
		assertNull(head.getPeeledObjectId());

		assertEquals(v1_0, tag.getObjectId());
		assertFalse(tag.isPeeled());
		assertNull(tag.getPeeledObjectId());
	}

	@Test
	public void testGetRefs_PackedWithPeeled() throws IOException {
		Map<String, Ref> all;

		writePackedRefs("# pack-refs with: peeled \n" + //
				A.name() + " refs/heads/master\n" + //
				B.name() + " refs/heads/other\n" + //
				v1_0.name() + " refs/tags/v1.0\n" + //
				"^" + v1_0.getObject().name() + "\n");
		all = refdir.getRefs(RefDatabase.ALL);

		assertEquals(4, all.size());
		final Ref head = all.get(HEAD);
		final Ref master = all.get("refs/heads/master");
		final Ref other = all.get("refs/heads/other");
		final Ref tag = all.get("refs/tags/v1.0");

		assertEquals(A, master.getObjectId());
		assertTrue(master.isPeeled());
		assertNull(master.getPeeledObjectId());

		assertEquals(B, other.getObjectId());
		assertTrue(other.isPeeled());
		assertNull(other.getPeeledObjectId());

		assertSame(master, head.getTarget());
		assertEquals(A, head.getObjectId());
		assertTrue(head.isPeeled());
		assertNull(head.getPeeledObjectId());

		assertEquals(v1_0, tag.getObjectId());
		assertTrue(tag.isPeeled());
		assertEquals(v1_0.getObject(), tag.getPeeledObjectId());
	}

	@Test
	public void test_repack() throws Exception {
		Map<String, Ref> all;

		writePackedRefs("# pack-refs with: peeled \n" + //
				A.name() + " refs/heads/master\n" + //
				B.name() + " refs/heads/other\n" + //
				v1_0.name() + " refs/tags/v1.0\n" + //
				"^" + v1_0.getObject().name() + "\n");
		all = refdir.getRefs(RefDatabase.ALL);

		assertEquals(4, all.size());
		assertEquals(Storage.LOOSE, all.get(HEAD).getStorage());
		assertEquals(Storage.PACKED, all.get("refs/heads/master").getStorage());
		assertEquals(A.getId(), all.get("refs/heads/master").getObjectId());
		assertEquals(Storage.PACKED, all.get("refs/heads/other").getStorage());
		assertEquals(Storage.PACKED, all.get("refs/tags/v1.0").getStorage());

		repo.update("refs/heads/master", B.getId());
		RevTag v0_1 = repo.tag("v0.1", A);
		repo.update("refs/tags/v0.1", v0_1);

		all = refdir.getRefs(RefDatabase.ALL);
		assertEquals(5, all.size());
		assertEquals(Storage.LOOSE, all.get(HEAD).getStorage());
		// Why isn't the next ref LOOSE_PACKED?
		assertEquals(Storage.LOOSE, all.get("refs/heads/master")
				.getStorage());
		assertEquals(B.getId(), all.get("refs/heads/master").getObjectId());
		assertEquals(Storage.PACKED, all.get("refs/heads/other").getStorage());
		assertEquals(Storage.PACKED, all.get("refs/tags/v1.0").getStorage());
		assertEquals(Storage.LOOSE, all.get("refs/tags/v0.1").getStorage());
		assertEquals(v0_1.getId(), all.get("refs/tags/v0.1").getObjectId());

		all = refdir.getRefs(RefDatabase.ALL);
		refdir.pack(new ArrayList<>(all.keySet()));

		all = refdir.getRefs(RefDatabase.ALL);
		assertEquals(5, all.size());
		assertEquals(Storage.LOOSE, all.get(HEAD).getStorage());
		// Why isn't the next ref LOOSE_PACKED?
		assertEquals(Storage.PACKED, all.get("refs/heads/master").getStorage());
		assertEquals(B.getId(), all.get("refs/heads/master").getObjectId());
		assertEquals(Storage.PACKED, all.get("refs/heads/other").getStorage());
		assertEquals(Storage.PACKED, all.get("refs/tags/v1.0").getStorage());
		assertEquals(Storage.PACKED, all.get("refs/tags/v0.1").getStorage());
		assertEquals(v0_1.getId(), all.get("refs/tags/v0.1").getObjectId());
	}

	@Test
	public void testFindRef_EmptyDatabase() throws IOException {
		Ref r;

		r = refdir.findRef(HEAD);
		assertTrue(r.isSymbolic());
		assertSame(LOOSE, r.getStorage());
		assertEquals("refs/heads/master", r.getTarget().getName());
		assertSame(NEW, r.getTarget().getStorage());
		assertNull(r.getTarget().getObjectId());

		assertNull(refdir.findRef("refs/heads/master"));
		assertNull(refdir.findRef("refs/tags/v1.0"));
		assertNull(refdir.findRef("FETCH_HEAD"));
		assertNull(refdir.findRef("NOT.A.REF.NAME"));
		assertNull(refdir.findRef("master"));
		assertNull(refdir.findRef("v1.0"));
	}

	@Test
	public void testExactRef_EmptyDatabase() throws IOException {
		Ref r;

		r = refdir.exactRef(HEAD);
		assertTrue(r.isSymbolic());
		assertSame(LOOSE, r.getStorage());
		assertEquals("refs/heads/master", r.getTarget().getName());
		assertSame(NEW, r.getTarget().getStorage());
		assertNull(r.getTarget().getObjectId());

		assertNull(refdir.exactRef("refs/heads/master"));
		assertNull(refdir.exactRef("refs/tags/v1.0"));
		assertNull(refdir.exactRef("FETCH_HEAD"));
		assertNull(refdir.exactRef("NOT.A.REF.NAME"));
		assertNull(refdir.exactRef("master"));
		assertNull(refdir.exactRef("v1.0"));
	}

	@Test
	public void testGetAdditionalRefs_OrigHead() throws IOException {
		writeLooseRef("ORIG_HEAD", A);

		List<Ref> refs = refdir.getAdditionalRefs();
		assertEquals(1, refs.size());

		Ref r = refs.get(0);
		assertFalse(r.isSymbolic());
		assertEquals(A, r.getObjectId());
		assertEquals("ORIG_HEAD", r.getName());
		assertFalse(r.isPeeled());
		assertNull(r.getPeeledObjectId());
	}

	@Test
	public void testGetAdditionalRefs_OrigHeadBranch() throws IOException {
		writeLooseRef("refs/heads/ORIG_HEAD", A);
		List<Ref> refs = refdir.getAdditionalRefs();
		assertArrayEquals(new Ref[0], refs.toArray());
	}

	@Test
	public void testFindRef_FetchHead() throws IOException {
		// This is an odd special case where we need to make sure we read
		// exactly the first 40 bytes of the file and nothing further on
		// that line, or the remainder of the file.
		write(new File(diskRepo.getDirectory(), "FETCH_HEAD"), A.name()
				+ "\tnot-for-merge"
				+ "\tbranch 'master' of git://egit.eclipse.org/jgit\n");

		Ref r = refdir.findRef("FETCH_HEAD");
		assertFalse(r.isSymbolic());
		assertEquals(A, r.getObjectId());
		assertEquals("FETCH_HEAD", r.getName());
		assertFalse(r.isPeeled());
		assertNull(r.getPeeledObjectId());
	}

	@Test
	public void testExactRef_FetchHead() throws IOException {
		// This is an odd special case where we need to make sure we read
		// exactly the first 40 bytes of the file and nothing further on
		// that line, or the remainder of the file.
		write(new File(diskRepo.getDirectory(), "FETCH_HEAD"), A.name()
				+ "\tnot-for-merge"
				+ "\tbranch 'master' of git://egit.eclipse.org/jgit\n");

		Ref r = refdir.exactRef("FETCH_HEAD");
		assertFalse(r.isSymbolic());
		assertEquals(A, r.getObjectId());
		assertEquals("FETCH_HEAD", r.getName());
		assertFalse(r.isPeeled());
		assertNull(r.getPeeledObjectId());
	}

	@Test
	public void testFindRef_AnyHeadWithGarbage() throws IOException {
		write(new File(diskRepo.getDirectory(), "refs/heads/A"), A.name()
				+ "012345 . this is not a standard reference\n"
				+ "#and even more junk\n");

		Ref r = refdir.findRef("refs/heads/A");
		assertFalse(r.isSymbolic());
		assertEquals(A, r.getObjectId());
		assertEquals("refs/heads/A", r.getName());
		assertFalse(r.isPeeled());
		assertNull(r.getPeeledObjectId());
	}

	@Test
	public void testGetRefs_CorruptSymbolicReference() throws IOException {
		String name = "refs/heads/A";
		writeLooseRef(name, "ref: \n");
		assertTrue(refdir.getRefs(RefDatabase.ALL).isEmpty());
	}

	@Test
	public void testFindRef_CorruptSymbolicReference() throws IOException {
		String name = "refs/heads/A";
		writeLooseRef(name, "ref: \n");
		try {
			refdir.findRef(name);
			fail("read an invalid reference");
		} catch (IOException err) {
			String msg = err.getMessage();
			assertEquals("Not a ref: " + name + ": ref:", msg);
		}
	}

	@Test
	public void testGetRefs_CorruptObjectIdReference() throws IOException {
		String name = "refs/heads/A";
		String content = "zoo" + A.name();
		writeLooseRef(name, content + "\n");
		assertTrue(refdir.getRefs(RefDatabase.ALL).isEmpty());
	}

	@Test
	public void testFindRef_CorruptObjectIdReference() throws IOException {
		String name = "refs/heads/A";
		String content = "zoo" + A.name();
		writeLooseRef(name, content + "\n");
		try {
			refdir.findRef(name);
			fail("read an invalid reference");
		} catch (IOException err) {
			String msg = err.getMessage();
			assertEquals("Not a ref: " + name + ": " + content, msg);
		}
	}

	@Test
	public void testIsNameConflicting() throws IOException {
		writeLooseRef("refs/heads/a/b", A);
		writePackedRef("refs/heads/q", B);

		// new references cannot replace an existing container
		assertTrue(refdir.isNameConflicting("refs"));
		assertTrue(refdir.isNameConflicting("refs/heads"));
		assertTrue(refdir.isNameConflicting("refs/heads/a"));

		// existing reference is not conflicting
		assertFalse(refdir.isNameConflicting("refs/heads/a/b"));

		// new references are not conflicting
		assertFalse(refdir.isNameConflicting("refs/heads/a/d"));
		assertFalse(refdir.isNameConflicting("refs/heads/master"));

		// existing reference must not be used as a container
		assertTrue(refdir.isNameConflicting("refs/heads/a/b/c"));
		assertTrue(refdir.isNameConflicting("refs/heads/q/master"));
	}

	@Test
	public void testPeelLooseTag() throws IOException {
		writeLooseRef("refs/tags/v1_0", v1_0);
		writeLooseRef("refs/tags/current", "ref: refs/tags/v1_0\n");

		final Ref tag = refdir.findRef("refs/tags/v1_0");
		final Ref cur = refdir.findRef("refs/tags/current");

		assertEquals(v1_0, tag.getObjectId());
		assertFalse(tag.isSymbolic());
		assertFalse(tag.isPeeled());
		assertNull(tag.getPeeledObjectId());

		assertEquals(v1_0, cur.getObjectId());
		assertTrue(cur.isSymbolic());
		assertFalse(cur.isPeeled());
		assertNull(cur.getPeeledObjectId());

		final Ref tag_p = refdir.peel(tag);
		final Ref cur_p = refdir.peel(cur);

		assertNotSame(tag, tag_p);
		assertFalse(tag_p.isSymbolic());
		assertTrue(tag_p.isPeeled());
		assertEquals(v1_0, tag_p.getObjectId());
		assertEquals(v1_0.getObject(), tag_p.getPeeledObjectId());
		assertSame(tag_p, refdir.peel(tag_p));

		assertNotSame(cur, cur_p);
		assertEquals("refs/tags/current", cur_p.getName());
		assertTrue(cur_p.isSymbolic());
		assertEquals("refs/tags/v1_0", cur_p.getTarget().getName());
		assertTrue(cur_p.isPeeled());
		assertEquals(v1_0, cur_p.getObjectId());
		assertEquals(v1_0.getObject(), cur_p.getPeeledObjectId());

		// reuses cached peeling later, but not immediately due to
		// the implementation so we have to fetch it once.
		final Ref tag_p2 = refdir.findRef("refs/tags/v1_0");
		assertFalse(tag_p2.isSymbolic());
		assertTrue(tag_p2.isPeeled());
		assertEquals(v1_0, tag_p2.getObjectId());
		assertEquals(v1_0.getObject(), tag_p2.getPeeledObjectId());

		assertSame(tag_p2, refdir.findRef("refs/tags/v1_0"));
		assertSame(tag_p2, refdir.findRef("refs/tags/current").getTarget());
		assertSame(tag_p2, refdir.peel(tag_p2));
	}

	@Test
	public void testPeelCommit() throws IOException {
		writeLooseRef("refs/heads/master", A);

		Ref master = refdir.findRef("refs/heads/master");
		assertEquals(A, master.getObjectId());
		assertFalse(master.isPeeled());
		assertNull(master.getPeeledObjectId());

		Ref master_p = refdir.peel(master);
		assertNotSame(master, master_p);
		assertEquals(A, master_p.getObjectId());
		assertTrue(master_p.isPeeled());
		assertNull(master_p.getPeeledObjectId());

		// reuses cached peeling later, but not immediately due to
		// the implementation so we have to fetch it once.
		Ref master_p2 = refdir.findRef("refs/heads/master");
		assertNotSame(master, master_p2);
		assertEquals(A, master_p2.getObjectId());
		assertTrue(master_p2.isPeeled());
		assertNull(master_p2.getPeeledObjectId());
		assertSame(master_p2, refdir.peel(master_p2));
	}

	@Test
	public void testRefsChangedStackOverflow() throws Exception {
		final FileRepository newRepo = createBareRepository();
		final RefDatabase refDb = newRepo.getRefDatabase();
		File packedRefs = new File(newRepo.getDirectory(), "packed-refs");
		assertTrue(packedRefs.createNewFile());
		final AtomicReference<StackOverflowError> error = new AtomicReference<>();
		final AtomicReference<IOException> exception = new AtomicReference<>();
		final AtomicInteger changeCount = new AtomicInteger();
		newRepo.getListenerList()
				.addRefsChangedListener((RefsChangedEvent event) -> {
					try {
						refDb.getRefsByPrefix("ref");
						changeCount.incrementAndGet();
					} catch (StackOverflowError soe) {
						error.set(soe);
					} catch (IOException ioe) {
						exception.set(ioe);
					}
				});
		refDb.getRefsByPrefix("ref");
		refDb.getRefsByPrefix("ref");
		assertNull(error.get());
		assertNull(exception.get());
		assertEquals(1, changeCount.get());
	}

	@Test
	public void testPackedRefsLockFailure() throws Exception {
		writeLooseRef("refs/heads/master", A);
		refdir.setRetrySleepMs(Arrays.asList(0, 0));
		LockFile myLock = refdir.lockPackedRefs();
		try {
			refdir.pack(Arrays.asList("refs/heads/master"));
			fail("expected LockFailedException");
		} catch (LockFailedException e) {
			assertEquals(refdir.packedRefsFile.getPath(), e.getFile().getPath());
		} finally {
			myLock.unlock();
		}
		Ref ref = refdir.findRef("refs/heads/master");
		assertEquals(Storage.LOOSE, ref.getStorage());
	}

	private void writeLooseRef(String name, AnyObjectId id) throws IOException {
		writeLooseRef(name, id.name() + "\n");
	}

	private void writeLooseRef(String name, String content) throws IOException {
		write(new File(diskRepo.getDirectory(), name), content);
	}

	private void writePackedRef(String name, AnyObjectId id) throws IOException {
		writePackedRefs(id.name() + " " + name + "\n");
	}

	private void writePackedRefs(String content) throws IOException {
		File pr = new File(diskRepo.getDirectory(), "packed-refs");
		write(pr, content);
		FS fs = diskRepo.getFS();
		fs.setLastModified(pr.toPath(), Instant.now().minusSeconds(3600));
	}

	private void deleteLooseRef(String name) {
		File path = new File(diskRepo.getDirectory(), name);
		assertTrue("deleted " + name, path.delete());
	}
}
