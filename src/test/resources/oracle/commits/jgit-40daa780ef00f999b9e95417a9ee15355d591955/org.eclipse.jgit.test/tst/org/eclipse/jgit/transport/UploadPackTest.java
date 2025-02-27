package org.eclipse.jgit.transport;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

import org.eclipse.jgit.dircache.DirCache;
import org.eclipse.jgit.dircache.DirCacheBuilder;
import org.eclipse.jgit.dircache.DirCacheEntry;
import org.eclipse.jgit.errors.TransportException;
import org.eclipse.jgit.internal.storage.dfs.DfsGarbageCollector;
import org.eclipse.jgit.internal.storage.dfs.DfsRepositoryDescription;
import org.eclipse.jgit.internal.storage.dfs.InMemoryRepository;
import org.eclipse.jgit.internal.storage.pack.CachedPack;
import org.eclipse.jgit.internal.storage.pack.CachedPackUriProvider;
import org.eclipse.jgit.junit.TestRepository;
import org.eclipse.jgit.lib.ConfigConstants;
import org.eclipse.jgit.lib.NullProgressMonitor;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectInserter;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.ProgressMonitor;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.RefDatabase;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.Sets;
import org.eclipse.jgit.lib.TextProgressMonitor;
import org.eclipse.jgit.revwalk.RevBlob;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTag;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.storage.pack.PackStatistics;
import org.eclipse.jgit.transport.UploadPack.RequestPolicy;
import org.eclipse.jgit.util.io.NullOutputStream;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests for server upload-pack utilities.
 */
public class UploadPackTest {
	private URIish uri;

	private TestProtocol<Object> testProtocol;

	private final Object ctx = new Object();

	private InMemoryRepository server;

	private InMemoryRepository client;

	private TestRepository<InMemoryRepository> remote;

	private PackStatistics stats;

	@Before
	public void setUp() throws Exception {
		server = newRepo("server");
		client = newRepo("client");

		remote = new TestRepository<>(server);
	}

	@After
	public void tearDown() {
		Transport.unregister(testProtocol);
	}

	private static InMemoryRepository newRepo(String name) {
		return new InMemoryRepository(new DfsRepositoryDescription(name));
	}

	private void generateBitmaps(InMemoryRepository repo) throws Exception {
		new DfsGarbageCollector(repo).pack(null);
		repo.scanForRepoChanges();
	}

	@Test
	public void testFetchParentOfShallowCommit() throws Exception {
		RevCommit commit0 = remote.commit().message("0").create();
		RevCommit commit1 = remote.commit().message("1").parent(commit0).create();
		RevCommit tip = remote.commit().message("2").parent(commit1).create();
		remote.update("master", tip);

		testProtocol = new TestProtocol<>((Object req, Repository db) -> {
			UploadPack up = new UploadPack(db);
			up.setRequestPolicy(RequestPolicy.REACHABLE_COMMIT);
			// assume client has a shallow commit
			up.getRevWalk()
					.assumeShallow(Collections.singleton(commit1.getId()));
			return up;
		}, null);
		uri = testProtocol.register(ctx, server);

		assertFalse(client.getObjectDatabase().has(commit0.toObjectId()));

		// Fetch of the parent of the shallow commit
		try (Transport tn = testProtocol.open(uri, client, "server")) {
			tn.fetch(NullProgressMonitor.INSTANCE,
					Collections.singletonList(new RefSpec(commit0.name())));
			assertTrue(client.getObjectDatabase().has(commit0.toObjectId()));
		}
	}

	@Test
	public void testFetchWithBlobZeroFilter() throws Exception {
		InMemoryRepository server2 = newRepo("server2");
		try (TestRepository<InMemoryRepository> remote2 = new TestRepository<>(
				server2)) {
			RevBlob blob1 = remote2.blob("foobar");
			RevBlob blob2 = remote2.blob("fooba");
			RevTree tree = remote2.tree(remote2.file("1", blob1),
					remote2.file("2", blob2));
			RevCommit commit = remote2.commit(tree);
			remote2.update("master", commit);

			server2.getConfig().setBoolean("uploadpack", null, "allowfilter",
					true);

			testProtocol = new TestProtocol<>((Object req, Repository db) -> {
				UploadPack up = new UploadPack(db);
				return up;
			}, null);
			uri = testProtocol.register(ctx, server2);

			try (Transport tn = testProtocol.open(uri, client, "server2")) {
				tn.setFilterSpec(FilterSpec.withBlobLimit(0));
				tn.fetch(NullProgressMonitor.INSTANCE,
						Collections.singletonList(new RefSpec(commit.name())));
				assertTrue(client.getObjectDatabase().has(tree.toObjectId()));
				assertFalse(client.getObjectDatabase().has(blob1.toObjectId()));
				assertFalse(client.getObjectDatabase().has(blob2.toObjectId()));
			}
		}
	}

	@Test
	public void testFetchExplicitBlobWithFilter() throws Exception {
		InMemoryRepository server2 = newRepo("server2");
		try (TestRepository<InMemoryRepository> remote2 = new TestRepository<>(
				server2)) {
			RevBlob blob1 = remote2.blob("foobar");
			RevBlob blob2 = remote2.blob("fooba");
			RevTree tree = remote2.tree(remote2.file("1", blob1),
					remote2.file("2", blob2));
			RevCommit commit = remote2.commit(tree);
			remote2.update("master", commit);
			remote2.update("a_blob", blob1);

			server2.getConfig().setBoolean("uploadpack", null, "allowfilter",
					true);

			testProtocol = new TestProtocol<>((Object req, Repository db) -> {
				UploadPack up = new UploadPack(db);
				return up;
			}, null);
			uri = testProtocol.register(ctx, server2);

			try (Transport tn = testProtocol.open(uri, client, "server2")) {
				tn.setFilterSpec(FilterSpec.withBlobLimit(0));
				tn.fetch(NullProgressMonitor.INSTANCE, Arrays.asList(
						new RefSpec(commit.name()), new RefSpec(blob1.name())));
				assertTrue(client.getObjectDatabase().has(tree.toObjectId()));
				assertTrue(client.getObjectDatabase().has(blob1.toObjectId()));
				assertFalse(client.getObjectDatabase().has(blob2.toObjectId()));
			}
		}
	}

	@Test
	public void testFetchWithBlobLimitFilter() throws Exception {
		InMemoryRepository server2 = newRepo("server2");
		try (TestRepository<InMemoryRepository> remote2 = new TestRepository<>(
				server2)) {
			RevBlob longBlob = remote2.blob("foobar");
			RevBlob shortBlob = remote2.blob("fooba");
			RevTree tree = remote2.tree(remote2.file("1", longBlob),
					remote2.file("2", shortBlob));
			RevCommit commit = remote2.commit(tree);
			remote2.update("master", commit);

			server2.getConfig().setBoolean("uploadpack", null, "allowfilter",
					true);

			testProtocol = new TestProtocol<>((Object req, Repository db) -> {
				UploadPack up = new UploadPack(db);
				return up;
			}, null);
			uri = testProtocol.register(ctx, server2);

			try (Transport tn = testProtocol.open(uri, client, "server2")) {
				tn.setFilterSpec(FilterSpec.withBlobLimit(5));
				tn.fetch(NullProgressMonitor.INSTANCE,
						Collections.singletonList(new RefSpec(commit.name())));
				assertFalse(
						client.getObjectDatabase().has(longBlob.toObjectId()));
				assertTrue(
						client.getObjectDatabase().has(shortBlob.toObjectId()));
			}
		}
	}

	@Test
	public void testFetchExplicitBlobWithFilterAndBitmaps() throws Exception {
		InMemoryRepository server2 = newRepo("server2");
		try (TestRepository<InMemoryRepository> remote2 = new TestRepository<>(
				server2)) {
			RevBlob blob1 = remote2.blob("foobar");
			RevBlob blob2 = remote2.blob("fooba");
			RevTree tree = remote2.tree(remote2.file("1", blob1),
					remote2.file("2", blob2));
			RevCommit commit = remote2.commit(tree);
			remote2.update("master", commit);
			remote2.update("a_blob", blob1);

			server2.getConfig().setBoolean("uploadpack", null, "allowfilter",
					true);

			// generate bitmaps
			new DfsGarbageCollector(server2).pack(null);
			server2.scanForRepoChanges();

			testProtocol = new TestProtocol<>((Object req, Repository db) -> {
				UploadPack up = new UploadPack(db);
				return up;
			}, null);
			uri = testProtocol.register(ctx, server2);

			try (Transport tn = testProtocol.open(uri, client, "server2")) {
				tn.setFilterSpec(FilterSpec.withBlobLimit(0));
				tn.fetch(NullProgressMonitor.INSTANCE, Arrays.asList(
						new RefSpec(commit.name()), new RefSpec(blob1.name())));
				assertTrue(client.getObjectDatabase().has(blob1.toObjectId()));
				assertFalse(client.getObjectDatabase().has(blob2.toObjectId()));
			}
		}
	}

	@Test
	public void testFetchWithBlobLimitFilterAndBitmaps() throws Exception {
		InMemoryRepository server2 = newRepo("server2");
		try (TestRepository<InMemoryRepository> remote2 = new TestRepository<>(
				server2)) {
			RevBlob longBlob = remote2.blob("foobar");
			RevBlob shortBlob = remote2.blob("fooba");
			RevTree tree = remote2.tree(remote2.file("1", longBlob),
					remote2.file("2", shortBlob));
			RevCommit commit = remote2.commit(tree);
			remote2.update("master", commit);

			server2.getConfig().setBoolean("uploadpack", null, "allowfilter",
					true);

			// generate bitmaps
			new DfsGarbageCollector(server2).pack(null);
			server2.scanForRepoChanges();

			testProtocol = new TestProtocol<>((Object req, Repository db) -> {
				UploadPack up = new UploadPack(db);
				return up;
			}, null);
			uri = testProtocol.register(ctx, server2);

			try (Transport tn = testProtocol.open(uri, client, "server2")) {
				tn.setFilterSpec(FilterSpec.withBlobLimit(5));
				tn.fetch(NullProgressMonitor.INSTANCE,
						Collections.singletonList(new RefSpec(commit.name())));
				assertFalse(
						client.getObjectDatabase().has(longBlob.toObjectId()));
				assertTrue(
						client.getObjectDatabase().has(shortBlob.toObjectId()));
			}
		}
	}

	@Test
	public void testFetchWithTreeZeroFilter() throws Exception {
		InMemoryRepository server2 = newRepo("server2");
		try (TestRepository<InMemoryRepository> remote2 = new TestRepository<>(
				server2)) {
			RevBlob blob1 = remote2.blob("foobar");
			RevBlob blob2 = remote2.blob("fooba");
			RevTree tree = remote2.tree(remote2.file("1", blob1),
					remote2.file("2", blob2));
			RevCommit commit = remote2.commit(tree);
			remote2.update("master", commit);

			server2.getConfig().setBoolean("uploadpack", null, "allowfilter",
					true);

			testProtocol = new TestProtocol<>((Object req, Repository db) -> {
				UploadPack up = new UploadPack(db);
				return up;
			}, null);
			uri = testProtocol.register(ctx, server2);

			try (Transport tn = testProtocol.open(uri, client, "server2")) {
				tn.setFilterSpec(FilterSpec.withTreeDepthLimit(0));
				tn.fetch(NullProgressMonitor.INSTANCE,
						Collections.singletonList(new RefSpec(commit.name())));
				assertFalse(client.getObjectDatabase().has(tree.toObjectId()));
				assertFalse(client.getObjectDatabase().has(blob1.toObjectId()));
				assertFalse(client.getObjectDatabase().has(blob2.toObjectId()));
			}
		}
	}

	@Test
	public void testFetchWithNonSupportingServer() throws Exception {
		InMemoryRepository server2 = newRepo("server2");
		try (TestRepository<InMemoryRepository> remote2 = new TestRepository<>(
				server2)) {
			RevBlob blob = remote2.blob("foo");
			RevTree tree = remote2.tree(remote2.file("1", blob));
			RevCommit commit = remote2.commit(tree);
			remote2.update("master", commit);

			server2.getConfig().setBoolean("uploadpack", null, "allowfilter",
					false);

			testProtocol = new TestProtocol<>((Object req, Repository db) -> {
				UploadPack up = new UploadPack(db);
				return up;
			}, null);
			uri = testProtocol.register(ctx, server2);

			try (Transport tn = testProtocol.open(uri, client, "server2")) {
				tn.setFilterSpec(FilterSpec.withBlobLimit(0));

				TransportException e = assertThrows(TransportException.class,
						() -> tn.fetch(NullProgressMonitor.INSTANCE, Collections
								.singletonList(new RefSpec(commit.name()))));
				assertThat(e.getMessage(), containsString(
						"filter requires server to advertise that capability"));
			}
		}
	}

	/*
	 * Invokes UploadPack with specified protocol version and sends it the given lines,
	 * and returns UploadPack's output stream.
	 */
	private ByteArrayInputStream uploadPackSetup(String version,
			Consumer<UploadPack> postConstructionSetup, String... inputLines)
			throws Exception {

		ByteArrayInputStream send = linesAsInputStream(inputLines);

		server.getConfig().setString(ConfigConstants.CONFIG_PROTOCOL_SECTION,
				null, ConfigConstants.CONFIG_KEY_VERSION, version);
		UploadPack up = new UploadPack(server);
		if (postConstructionSetup != null) {
			postConstructionSetup.accept(up);
		}
		up.setExtraParameters(Sets.of("version=".concat(version)));

		ByteArrayOutputStream recv = new ByteArrayOutputStream();
		up.upload(send, recv, null);
		stats = up.getStatistics();

		return new ByteArrayInputStream(recv.toByteArray());
	}

	private static ByteArrayInputStream linesAsInputStream(String... inputLines)
			throws IOException {
		try (ByteArrayOutputStream send = new ByteArrayOutputStream()) {
			PacketLineOut pckOut = new PacketLineOut(send);
			for (String line : inputLines) {
				Objects.requireNonNull(line);
				if (PacketLineIn.isEnd(line)) {
					pckOut.end();
				} else if (PacketLineIn.isDelimiter(line)) {
					pckOut.writeDelim();
				} else {
					pckOut.writeString(line);
				}
			}
			return new ByteArrayInputStream(send.toByteArray());
		}
	}

	/*
	 * Invokes UploadPack with protocol v1 and sends it the given lines.
	 * Returns UploadPack's output stream, not including the capability
	 * advertisement by the server.
	 */
	private ByteArrayInputStream uploadPackV1(
			Consumer<UploadPack> postConstructionSetup,
			String... inputLines)
			throws Exception {
		ByteArrayInputStream recvStream =
				uploadPackSetup("1", postConstructionSetup, inputLines);
		PacketLineIn pckIn = new PacketLineIn(recvStream);

		// drain capabilities
		while (!PacketLineIn.isEnd(pckIn.readString())) {
			// do nothing
		}
		return recvStream;
	}

	private ByteArrayInputStream uploadPackV1(String... inputLines) throws Exception {
		return uploadPackV1(null, inputLines);
	}

	/*
	 * Invokes UploadPack with protocol v2 and sends it the given lines.
	 * Returns UploadPack's output stream, not including the capability
	 * advertisement by the server.
	 */
	private ByteArrayInputStream uploadPackV2(
			Consumer<UploadPack> postConstructionSetup,
			String... inputLines)
			throws Exception {
		ByteArrayInputStream recvStream = uploadPackSetup(
				TransferConfig.ProtocolVersion.V2.version(),
				postConstructionSetup, inputLines);
		PacketLineIn pckIn = new PacketLineIn(recvStream);

		// drain capabilities
		while (!PacketLineIn.isEnd(pckIn.readString())) {
			// do nothing
		}
		return recvStream;
	}

	private ByteArrayInputStream uploadPackV2(String... inputLines) throws Exception {
		return uploadPackV2(null, inputLines);
	}

	private static class TestV2Hook implements ProtocolV2Hook {
		private CapabilitiesV2Request capabilitiesRequest;

		private LsRefsV2Request lsRefsRequest;

		private FetchV2Request fetchRequest;

		private ObjectInfoRequest objectInfoRequest;

		@Override
		public void onCapabilities(CapabilitiesV2Request req) {
			capabilitiesRequest = req;
		}

		@Override
		public void onLsRefs(LsRefsV2Request req) {
			lsRefsRequest = req;
		}

		@Override
		public void onFetch(FetchV2Request req) {
			fetchRequest = req;
		}

		@Override
		public void onObjectInfo(ObjectInfoRequest req) {
			objectInfoRequest = req;
		}
	}

	@Test
	public void testV2Capabilities() throws Exception {
		TestV2Hook hook = new TestV2Hook();
		ByteArrayInputStream recvStream = uploadPackSetup(
				TransferConfig.ProtocolVersion.V2.version(),
				(UploadPack up) -> {
					up.setProtocolV2Hook(hook);
				}, PacketLineIn.end());
		PacketLineIn pckIn = new PacketLineIn(recvStream);
		assertThat(hook.capabilitiesRequest, notNullValue());
		assertThat(pckIn.readString(), is("version 2"));
		assertThat(
				Arrays.asList(pckIn.readString(), pckIn.readString(),
						pckIn.readString()),
				// TODO(jonathantanmy) This check is written this way
				// to make it simple to see that we expect this list of
				// capabilities, but probably should be loosened to
				// allow additional commands to be added to the list,
				// and additional capabilities to be added to existing
				// commands without requiring test changes.
				hasItems("ls-refs", "fetch=shallow", "server-option"));
		assertTrue(PacketLineIn.isEnd(pckIn.readString()));
	}

	private void checkAdvertisedIfAllowed(String configSection, String configName,
			String fetchCapability) throws Exception {
		server.getConfig().setBoolean(configSection, null, configName, true);
		ByteArrayInputStream recvStream = uploadPackSetup(
				TransferConfig.ProtocolVersion.V2.version(), null,
				PacketLineIn.end());
		PacketLineIn pckIn = new PacketLineIn(recvStream);

		assertThat(pckIn.readString(), is("version 2"));

		ArrayList<String> lines = new ArrayList<>();
		String line;
		while (!PacketLineIn.isEnd((line = pckIn.readString()))) {
			if (line.startsWith("fetch=")) {
				assertThat(
					Arrays.asList(line.substring(6).split(" ")),
					containsInAnyOrder(fetchCapability, "shallow"));
				lines.add("fetch");
			} else {
				lines.add(line);
			}
		}
		assertThat(lines, containsInAnyOrder("ls-refs", "fetch", "server-option"));
	}

	private void checkUnadvertisedIfUnallowed(String configSection,
			String configName, String fetchCapability) throws Exception {
		server.getConfig().setBoolean(configSection, null, configName, false);
		ByteArrayInputStream recvStream = uploadPackSetup(
				TransferConfig.ProtocolVersion.V2.version(), null,
				PacketLineIn.end());
		PacketLineIn pckIn = new PacketLineIn(recvStream);

		assertThat(pckIn.readString(), is("version 2"));

		ArrayList<String> lines = new ArrayList<>();
		String line;
		while (!PacketLineIn.isEnd((line = pckIn.readString()))) {
			if (line.startsWith("fetch=")) {
				List<String> fetchItems = Arrays.asList(line.substring(6).split(" "));
				assertThat(fetchItems, hasItems("shallow"));
				assertFalse(fetchItems.contains(fetchCapability));
				lines.add("fetch");
			} else {
				lines.add(line);
			}
		}
		assertThat(lines, hasItems("ls-refs", "fetch", "server-option"));
	}

	@Test
	public void testV2CapabilitiesAllowFilter() throws Exception {
		checkAdvertisedIfAllowed("uploadpack", "allowfilter", "filter");
		checkUnadvertisedIfUnallowed("uploadpack", "allowfilter", "filter");
	}

	@Test
	public void testV2CapabilitiesRefInWant() throws Exception {
		checkAdvertisedIfAllowed("uploadpack", "allowrefinwant", "ref-in-want");
	}

	@Test
	public void testV2CapabilitiesRefInWantNotAdvertisedIfUnallowed() throws Exception {
		checkUnadvertisedIfUnallowed("uploadpack", "allowrefinwant",
				"ref-in-want");
	}

	@Test
	public void testV2CapabilitiesAdvertiseSidebandAll() throws Exception {
		server.getConfig().setBoolean("uploadpack", null, "allowsidebandall",
				true);
		checkAdvertisedIfAllowed("uploadpack", "advertisesidebandall",
				"sideband-all");
		checkUnadvertisedIfUnallowed("uploadpack", "advertisesidebandall",
				"sideband-all");
	}

	@Test
	public void testV2CapabilitiesRefInWantNotAdvertisedIfAdvertisingForbidden() throws Exception {
		server.getConfig().setBoolean("uploadpack", null, "allowrefinwant", true);
		server.getConfig().setBoolean("uploadpack", null, "advertiserefinwant", false);
		ByteArrayInputStream recvStream = uploadPackSetup(
				TransferConfig.ProtocolVersion.V2.version(), null,
				PacketLineIn.end());
		PacketLineIn pckIn = new PacketLineIn(recvStream);

		assertThat(pckIn.readString(), is("version 2"));
		assertThat(
				Arrays.asList(pckIn.readString(), pckIn.readString(),
						pckIn.readString()),
				hasItems("ls-refs", "fetch=shallow", "server-option"));
		assertTrue(PacketLineIn.isEnd(pckIn.readString()));
	}

	@Test
	public void testV2EmptyRequest() throws Exception {
		ByteArrayInputStream recvStream = uploadPackV2(PacketLineIn.end());
		// Verify that there is nothing more after the capability
		// advertisement.
		assertEquals(0, recvStream.available());
	}

	@Test
	public void testV2LsRefs() throws Exception {
		RevCommit tip = remote.commit().message("message").create();
		remote.update("master", tip);
		server.updateRef("HEAD").link("refs/heads/master");
		RevTag tag = remote.tag("tag", tip);
		remote.update("refs/tags/tag", tag);

		TestV2Hook hook = new TestV2Hook();
		ByteArrayInputStream recvStream = uploadPackV2(
				(UploadPack up) -> {up.setProtocolV2Hook(hook);},
				"command=ls-refs\n", PacketLineIn.end());
		PacketLineIn pckIn = new PacketLineIn(recvStream);

		assertThat(hook.lsRefsRequest, notNullValue());
		assertThat(pckIn.readString(), is(tip.toObjectId().getName() + " HEAD"));
		assertThat(pckIn.readString(), is(tip.toObjectId().getName() + " refs/heads/master"));
		assertThat(pckIn.readString(), is(tag.toObjectId().getName() + " refs/tags/tag"));
		assertTrue(PacketLineIn.isEnd(pckIn.readString()));
	}

	@Test
	public void testV2LsRefsSymrefs() throws Exception {
		RevCommit tip = remote.commit().message("message").create();
		remote.update("master", tip);
		server.updateRef("HEAD").link("refs/heads/master");
		RevTag tag = remote.tag("tag", tip);
		remote.update("refs/tags/tag", tag);

		ByteArrayInputStream recvStream = uploadPackV2("command=ls-refs\n",
				PacketLineIn.delimiter(), "symrefs", PacketLineIn.end());
		PacketLineIn pckIn = new PacketLineIn(recvStream);

		assertThat(pckIn.readString(), is(tip.toObjectId().getName() + " HEAD symref-target:refs/heads/master"));
		assertThat(pckIn.readString(), is(tip.toObjectId().getName() + " refs/heads/master"));
		assertThat(pckIn.readString(), is(tag.toObjectId().getName() + " refs/tags/tag"));
		assertTrue(PacketLineIn.isEnd(pckIn.readString()));
	}

	@Test
	public void testV2LsRefsPeel() throws Exception {
		RevCommit tip = remote.commit().message("message").create();
		remote.update("master", tip);
		server.updateRef("HEAD").link("refs/heads/master");
		RevTag tag = remote.tag("tag", tip);
		remote.update("refs/tags/tag", tag);

		ByteArrayInputStream recvStream = uploadPackV2("command=ls-refs\n",
				PacketLineIn.delimiter(), "peel", PacketLineIn.end());
		PacketLineIn pckIn = new PacketLineIn(recvStream);

		assertThat(pckIn.readString(), is(tip.toObjectId().getName() + " HEAD"));
		assertThat(pckIn.readString(), is(tip.toObjectId().getName() + " refs/heads/master"));
		assertThat(
			pckIn.readString(),
			is(tag.toObjectId().getName() + " refs/tags/tag peeled:"
				+ tip.toObjectId().getName()));
		assertTrue(PacketLineIn.isEnd(pckIn.readString()));
	}

	@Test
	public void testV2LsRefsMultipleCommands() throws Exception {
		RevCommit tip = remote.commit().message("message").create();
		remote.update("master", tip);
		server.updateRef("HEAD").link("refs/heads/master");
		RevTag tag = remote.tag("tag", tip);
		remote.update("refs/tags/tag", tag);

		ByteArrayInputStream recvStream = uploadPackV2(
				"command=ls-refs\n", PacketLineIn.delimiter(), "symrefs",
				"peel", PacketLineIn.end(), "command=ls-refs\n",
				PacketLineIn.delimiter(), PacketLineIn.end());
		PacketLineIn pckIn = new PacketLineIn(recvStream);

		assertThat(pckIn.readString(), is(tip.toObjectId().getName() + " HEAD symref-target:refs/heads/master"));
		assertThat(pckIn.readString(), is(tip.toObjectId().getName() + " refs/heads/master"));
		assertThat(
			pckIn.readString(),
			is(tag.toObjectId().getName() + " refs/tags/tag peeled:"
				+ tip.toObjectId().getName()));
		assertTrue(PacketLineIn.isEnd(pckIn.readString()));
		assertThat(pckIn.readString(), is(tip.toObjectId().getName() + " HEAD"));
		assertThat(pckIn.readString(), is(tip.toObjectId().getName() + " refs/heads/master"));
		assertThat(pckIn.readString(), is(tag.toObjectId().getName() + " refs/tags/tag"));
		assertTrue(PacketLineIn.isEnd(pckIn.readString()));
	}

	@Test
	public void testV2LsRefsRefPrefix() throws Exception {
		RevCommit tip = remote.commit().message("message").create();
		remote.update("master", tip);
		remote.update("other", tip);
		remote.update("yetAnother", tip);

		ByteArrayInputStream recvStream = uploadPackV2(
			"command=ls-refs\n",
			PacketLineIn.delimiter(),
			"ref-prefix refs/heads/maste",
			"ref-prefix refs/heads/other",
				PacketLineIn.end());
		PacketLineIn pckIn = new PacketLineIn(recvStream);

		assertThat(pckIn.readString(), is(tip.toObjectId().getName() + " refs/heads/master"));
		assertThat(pckIn.readString(), is(tip.toObjectId().getName() + " refs/heads/other"));
		assertTrue(PacketLineIn.isEnd(pckIn.readString()));
	}

	@Test
	public void testV2LsRefsRefPrefixNoSlash() throws Exception {
		RevCommit tip = remote.commit().message("message").create();
		remote.update("master", tip);
		remote.update("other", tip);

		ByteArrayInputStream recvStream = uploadPackV2(
			"command=ls-refs\n",
			PacketLineIn.delimiter(),
			"ref-prefix refs/heads/maste",
			"ref-prefix r",
				PacketLineIn.end());
		PacketLineIn pckIn = new PacketLineIn(recvStream);

		assertThat(pckIn.readString(), is(tip.toObjectId().getName() + " refs/heads/master"));
		assertThat(pckIn.readString(), is(tip.toObjectId().getName() + " refs/heads/other"));
		assertTrue(PacketLineIn.isEnd(pckIn.readString()));
	}

	@Test
	public void testV2LsRefsUnrecognizedArgument() throws Exception {
		UploadPackInternalServerErrorException e = assertThrows(
				UploadPackInternalServerErrorException.class,
				() -> uploadPackV2("command=ls-refs\n",
						PacketLineIn.delimiter(), "invalid-argument\n",
						PacketLineIn.end()));
		assertThat(e.getCause().getMessage(),
				containsString("unexpected invalid-argument"));
	}

	@Test
	public void testV2LsRefsServerOptions() throws Exception {
		String[] lines = { "command=ls-refs\n",
				"server-option=one\n", "server-option=two\n",
				PacketLineIn.delimiter(),
				PacketLineIn.end() };

		TestV2Hook testHook = new TestV2Hook();
		uploadPackSetup(TransferConfig.ProtocolVersion.V2.version(),
				(UploadPack up) -> {
					up.setProtocolV2Hook(testHook);
				}, lines);

		LsRefsV2Request req = testHook.lsRefsRequest;
		assertEquals(2, req.getServerOptions().size());
		assertThat(req.getServerOptions(), hasItems("one", "two"));
	}

	/*
	 * Parse multiplexed packfile output from upload-pack using protocol V2
	 * into the client repository.
	 */
	private ReceivedPackStatistics parsePack(ByteArrayInputStream recvStream) throws Exception {
		return parsePack(recvStream, NullProgressMonitor.INSTANCE);
	}

	private ReceivedPackStatistics parsePack(ByteArrayInputStream recvStream, ProgressMonitor pm)
			throws Exception {
		SideBandInputStream sb = new SideBandInputStream(
				recvStream, pm,
				new StringWriter(), NullOutputStream.INSTANCE);
		PackParser pp = client.newObjectInserter().newPackParser(sb);
		pp.parse(NullProgressMonitor.INSTANCE);

		// Ensure that there is nothing left in the stream.
		assertEquals(-1, recvStream.read());

		return pp.getReceivedPackStatistics();
	}

	@Test
	public void testV2FetchRequestPolicyAdvertised() throws Exception {
		RevCommit advertized = remote.commit().message("x").create();
		RevCommit unadvertized = remote.commit().message("y").create();
		remote.update("branch1", advertized);

		// This works
		uploadPackV2(
			(UploadPack up) -> {up.setRequestPolicy(RequestPolicy.ADVERTISED);},
			"command=fetch\n",
			PacketLineIn.delimiter(),
			"want " + advertized.name() + "\n",
			PacketLineIn.end());

		// This doesn't
		UploadPackInternalServerErrorException e = assertThrows(
				UploadPackInternalServerErrorException.class,
				() -> uploadPackV2(
						(UploadPack up) -> {up.setRequestPolicy(RequestPolicy.ADVERTISED);},
						"command=fetch\n", PacketLineIn.delimiter(),
						"want " + unadvertized.name() + "\n",
						PacketLineIn.end()));
		assertThat(e.getCause().getMessage(),
				containsString("want " + unadvertized.name() + " not valid"));
	}

	@Test
	public void testV2FetchRequestPolicyReachableCommit() throws Exception {
		RevCommit reachable = remote.commit().message("x").create();
		RevCommit advertized = remote.commit().message("x").parent(reachable)
				.create();
		RevCommit unreachable = remote.commit().message("y").create();
		remote.update("branch1", advertized);

		// This works
		uploadPackV2(
			(UploadPack up) -> {up.setRequestPolicy(RequestPolicy.REACHABLE_COMMIT);},
			"command=fetch\n",
			PacketLineIn.delimiter(),
			"want " + reachable.name() + "\n",
				PacketLineIn.end());

		// This doesn't
		UploadPackInternalServerErrorException e = assertThrows(
				UploadPackInternalServerErrorException.class,
				() -> uploadPackV2(
						(UploadPack up) -> {up.setRequestPolicy(RequestPolicy.REACHABLE_COMMIT);},
						"command=fetch\n", PacketLineIn.delimiter(),
						"want " + unreachable.name() + "\n",
						PacketLineIn.end()));
		assertThat(e.getCause().getMessage(),
				containsString("want " + unreachable.name() + " not valid"));
	}

	@Test
	public void testV2FetchRequestPolicyTip() throws Exception {
		RevCommit parentOfTip = remote.commit().message("x").create();
		RevCommit tip = remote.commit().message("y").parent(parentOfTip)
				.create();
		remote.update("secret", tip);

		// This works
		uploadPackV2(
			(UploadPack up) -> {
				up.setRequestPolicy(RequestPolicy.TIP);
				up.setRefFilter(new RejectAllRefFilter());
			},
			"command=fetch\n",
			PacketLineIn.delimiter(),
			"want " + tip.name() + "\n",
				PacketLineIn.end());

		// This doesn't
		UploadPackInternalServerErrorException e = assertThrows(
				UploadPackInternalServerErrorException.class,
				() -> uploadPackV2(
						(UploadPack up) -> {
							up.setRequestPolicy(RequestPolicy.TIP);
							up.setRefFilter(new RejectAllRefFilter());
						},
						"command=fetch\n", PacketLineIn.delimiter(),
						"want " + parentOfTip.name() + "\n",
						PacketLineIn.end()));
		assertThat(e.getCause().getMessage(),
				containsString("want " + parentOfTip.name() + " not valid"));
	}

	@Test
	public void testV2FetchRequestPolicyReachableCommitTip() throws Exception {
		RevCommit parentOfTip = remote.commit().message("x").create();
		RevCommit tip = remote.commit().message("y").parent(parentOfTip)
				.create();
		RevCommit unreachable = remote.commit().message("y").create();
		remote.update("secret", tip);

		// This works
		uploadPackV2(
				(UploadPack up) -> {
					up.setRequestPolicy(RequestPolicy.REACHABLE_COMMIT_TIP);
					up.setRefFilter(new RejectAllRefFilter());
				},
				"command=fetch\n",
				PacketLineIn.delimiter(), "want " + parentOfTip.name() + "\n",
				PacketLineIn.end());

		// This doesn't
		UploadPackInternalServerErrorException e = assertThrows(
				UploadPackInternalServerErrorException.class,
				() -> uploadPackV2(
						(UploadPack up) -> {
							up.setRequestPolicy(RequestPolicy.REACHABLE_COMMIT_TIP);
							up.setRefFilter(new RejectAllRefFilter());
						},
						"command=fetch\n",
						PacketLineIn.delimiter(),
						"want " + unreachable.name() + "\n",
						PacketLineIn.end()));
		assertThat(e.getCause().getMessage(),
				containsString("want " + unreachable.name() + " not valid"));
	}

	@Test
	public void testV2FetchRequestPolicyAny() throws Exception {
		RevCommit unreachable = remote.commit().message("y").create();

		// Exercise to make sure that even unreachable commits can be fetched
		uploadPackV2(
			(UploadPack up) -> {up.setRequestPolicy(RequestPolicy.ANY);},
			"command=fetch\n",
			PacketLineIn.delimiter(),
			"want " + unreachable.name() + "\n",
				PacketLineIn.end());
	}

	@Test
	public void testV2FetchServerDoesNotStopNegotiation() throws Exception {
		RevCommit fooParent = remote.commit().message("x").create();
		RevCommit fooChild = remote.commit().message("x").parent(fooParent).create();
		RevCommit barParent = remote.commit().message("y").create();
		RevCommit barChild = remote.commit().message("y").parent(barParent).create();
		remote.update("branch1", fooChild);
		remote.update("branch2", barChild);

		ByteArrayInputStream recvStream = uploadPackV2(
			"command=fetch\n",
			PacketLineIn.delimiter(),
			"want " + fooChild.toObjectId().getName() + "\n",
			"want " + barChild.toObjectId().getName() + "\n",
			"have " + fooParent.toObjectId().getName() + "\n",
				PacketLineIn.end());
		PacketLineIn pckIn = new PacketLineIn(recvStream);

		assertThat(pckIn.readString(), is("acknowledgments"));
		assertThat(pckIn.readString(), is("ACK " + fooParent.toObjectId().getName()));
		assertTrue(PacketLineIn.isEnd(pckIn.readString()));
	}

	@Test
	public void testV2FetchServerStopsNegotiation() throws Exception {
		RevCommit fooParent = remote.commit().message("x").create();
		RevCommit fooChild = remote.commit().message("x").parent(fooParent).create();
		RevCommit barParent = remote.commit().message("y").create();
		RevCommit barChild = remote.commit().message("y").parent(barParent).create();
		remote.update("branch1", fooChild);
		remote.update("branch2", barChild);

		ByteArrayInputStream recvStream = uploadPackV2(
			"command=fetch\n",
			PacketLineIn.delimiter(),
			"want " + fooChild.toObjectId().getName() + "\n",
			"want " + barChild.toObjectId().getName() + "\n",
			"have " + fooParent.toObjectId().getName() + "\n",
			"have " + barParent.toObjectId().getName() + "\n",
				PacketLineIn.end());
		PacketLineIn pckIn = new PacketLineIn(recvStream);

		assertThat(pckIn.readString(), is("acknowledgments"));
		assertThat(
			Arrays.asList(pckIn.readString(), pckIn.readString()),
			hasItems(
				"ACK " + fooParent.toObjectId().getName(),
				"ACK " + barParent.toObjectId().getName()));
		assertThat(pckIn.readString(), is("ready"));
		assertTrue(PacketLineIn.isDelimiter(pckIn.readString()));
		assertThat(pckIn.readString(), is("packfile"));
		parsePack(recvStream);
		assertFalse(client.getObjectDatabase().has(fooParent.toObjectId()));
		assertTrue(client.getObjectDatabase().has(fooChild.toObjectId()));
		assertFalse(client.getObjectDatabase().has(barParent.toObjectId()));
		assertTrue(client.getObjectDatabase().has(barChild.toObjectId()));
	}

	@Test
	public void testV2FetchClientStopsNegotiation() throws Exception {
		RevCommit fooParent = remote.commit().message("x").create();
		RevCommit fooChild = remote.commit().message("x").parent(fooParent).create();
		RevCommit barParent = remote.commit().message("y").create();
		RevCommit barChild = remote.commit().message("y").parent(barParent).create();
		remote.update("branch1", fooChild);
		remote.update("branch2", barChild);

		ByteArrayInputStream recvStream = uploadPackV2(
			"command=fetch\n",
			PacketLineIn.delimiter(),
			"want " + fooChild.toObjectId().getName() + "\n",
			"want " + barChild.toObjectId().getName() + "\n",
			"have " + fooParent.toObjectId().getName() + "\n",
			"done\n",
				PacketLineIn.end());
		PacketLineIn pckIn = new PacketLineIn(recvStream);

		assertThat(pckIn.readString(), is("packfile"));
		parsePack(recvStream);
		assertFalse(client.getObjectDatabase().has(fooParent.toObjectId()));
		assertTrue(client.getObjectDatabase().has(fooChild.toObjectId()));
		assertTrue(client.getObjectDatabase().has(barParent.toObjectId()));
		assertTrue(client.getObjectDatabase().has(barChild.toObjectId()));
	}

	@Test
	public void testV2FetchWithoutWaitForDoneReceivesPackfile()
			throws Exception {
		String commonInBlob = "abcdefghijklmnopqrstuvwxyz";

		RevBlob parentBlob = remote.blob(commonInBlob + "a");
		RevCommit parent = remote
				.commit(remote.tree(remote.file("foo", parentBlob)));
		remote.update("branch1", parent);

		RevCommit localParent = null;
		RevCommit localChild = null;
		try (TestRepository<InMemoryRepository> local = new TestRepository<>(
				client)) {
			RevBlob localParentBlob = local.blob(commonInBlob + "a");
			localParent = local
					.commit(local.tree(local.file("foo", localParentBlob)));
			RevBlob localChildBlob = local.blob(commonInBlob + "b");
			localChild = local.commit(
					local.tree(local.file("foo", localChildBlob)), localParent);
			local.update("branch1", localChild);
		}

		ByteArrayInputStream recvStream = uploadPackV2("command=fetch\n",
				PacketLineIn.delimiter(),
				"have " + localParent.toObjectId().getName() + "\n",
				"have " + localChild.toObjectId().getName() + "\n",
				PacketLineIn.end());
		PacketLineIn pckIn = new PacketLineIn(recvStream);
		assertThat(pckIn.readString(), is("acknowledgments"));
		assertThat(Arrays.asList(pckIn.readString()),
				hasItems("ACK " + parent.toObjectId().getName()));
		assertThat(pckIn.readString(), is("ready"));
		assertTrue(PacketLineIn.isDelimiter(pckIn.readString()));
		assertThat(pckIn.readString(), is("packfile"));
		parsePack(recvStream);
	}

	@Test
	public void testV2FetchWithWaitForDoneOnlyDoesNegotiation()
			throws Exception {
		String commonInBlob = "abcdefghijklmnopqrstuvwxyz";

		RevBlob parentBlob = remote.blob(commonInBlob + "a");
		RevCommit parent = remote
				.commit(remote.tree(remote.file("foo", parentBlob)));
		remote.update("branch1", parent);

		RevCommit localParent = null;
		RevCommit localChild = null;
		try (TestRepository<InMemoryRepository> local = new TestRepository<>(
				client)) {
			RevBlob localParentBlob = local.blob(commonInBlob + "a");
			localParent = local
					.commit(local.tree(local.file("foo", localParentBlob)));
			RevBlob localChildBlob = local.blob(commonInBlob + "b");
			localChild = local.commit(
					local.tree(local.file("foo", localChildBlob)), localParent);
			local.update("branch1", localChild);
		}

		ByteArrayInputStream recvStream = uploadPackV2("command=fetch\n",
				PacketLineIn.delimiter(), "wait-for-done\n",
				"have " + localParent.toObjectId().getName() + "\n",
				"have " + localChild.toObjectId().getName() + "\n",
				PacketLineIn.end());
		PacketLineIn pckIn = new PacketLineIn(recvStream);
		assertThat(pckIn.readString(), is("acknowledgments"));
		assertThat(Arrays.asList(pckIn.readString()),
				hasItems("ACK " + parent.toObjectId().getName()));
		assertTrue(PacketLineIn.isEnd(pckIn.readString()));
	}

	@Test
	public void testV2FetchWithWaitForDoneOnlyDoesNegotiationAndNothingToAck()
			throws Exception {
		String commonInBlob = "abcdefghijklmnopqrstuvwxyz";

		RevCommit localParent = null;
		RevCommit localChild = null;
		try (TestRepository<InMemoryRepository> local = new TestRepository<>(
				client)) {
			RevBlob localParentBlob = local.blob(commonInBlob + "a");
			localParent = local
					.commit(local.tree(local.file("foo", localParentBlob)));
			RevBlob localChildBlob = local.blob(commonInBlob + "b");
			localChild = local.commit(
					local.tree(local.file("foo", localChildBlob)), localParent);
			local.update("branch1", localChild);
		}

		ByteArrayInputStream recvStream = uploadPackV2("command=fetch\n",
				PacketLineIn.delimiter(), "wait-for-done\n",
				"have " + localParent.toObjectId().getName() + "\n",
				"have " + localChild.toObjectId().getName() + "\n",
				PacketLineIn.end());
		PacketLineIn pckIn = new PacketLineIn(recvStream);
		assertThat(pckIn.readString(), is("acknowledgments"));
		assertThat(pckIn.readString(), is("NAK"));
		assertTrue(PacketLineIn.isEnd(pckIn.readString()));
	}

	@Test
	public void testV2FetchServerStopsNegotiationForRefWithoutParents()
			throws Exception {
		RevCommit fooCommit = remote.commit().message("x").create();
		RevCommit barCommit = remote.commit().message("y").create();
		remote.update("refs/changes/01/1/1", fooCommit);
		remote.update("refs/changes/02/2/1", barCommit);

		ByteArrayInputStream recvStream = uploadPackV2("command=fetch\n",
				PacketLineIn.delimiter(),
				"want " + fooCommit.toObjectId().getName() + "\n",
				"have " + barCommit.toObjectId().getName() + "\n",
				PacketLineIn.end());
		PacketLineIn pckIn = new PacketLineIn(recvStream);

		assertThat(pckIn.readString(), is("acknowledgments"));
		assertThat(pckIn.readString(),
				is("ACK " + barCommit.toObjectId().getName()));
		assertThat(pckIn.readString(), is("ready"));
		assertTrue(PacketLineIn.isDelimiter(pckIn.readString()));
		assertThat(pckIn.readString(), is("packfile"));
		parsePack(recvStream);
		assertTrue(client.getObjectDatabase().has(fooCommit.toObjectId()));
	}

	@Test
	public void testV2FetchServerDoesNotStopNegotiationWhenOneRefWithoutParentAndOtherWithParents()
			throws Exception {
		RevCommit fooCommit = remote.commit().message("x").create();
		RevCommit barParent = remote.commit().message("y").create();
		RevCommit barChild = remote.commit().message("y").parent(barParent)
				.create();
		RevCommit fooBarParent = remote.commit().message("z").create();
		RevCommit fooBarChild = remote.commit().message("y")
				.parent(fooBarParent)
				.create();
		remote.update("refs/changes/01/1/1", fooCommit);
		remote.update("refs/changes/02/2/1", barChild);
		remote.update("refs/changes/03/3/1", fooBarChild);

		ByteArrayInputStream recvStream = uploadPackV2("command=fetch\n",
				PacketLineIn.delimiter(),
				"want " + fooCommit.toObjectId().getName() + "\n",
				"want " + barChild.toObjectId().getName() + "\n",
				"want " + fooBarChild.toObjectId().getName() + "\n",
				"have " + fooBarParent.toObjectId().getName() + "\n",
				PacketLineIn.end());
		PacketLineIn pckIn = new PacketLineIn(recvStream);

		assertThat(pckIn.readString(), is("acknowledgments"));
		assertThat(pckIn.readString(),
				is("ACK " + fooBarParent.toObjectId().getName()));
		assertTrue(PacketLineIn.isEnd(pckIn.readString()));
	}

	@Test
	public void testV2FetchThinPack() throws Exception {
		String commonInBlob = "abcdefghijklmnopqrstuvwxyz";

		RevBlob parentBlob = remote.blob(commonInBlob + "a");
		RevCommit parent = remote
				.commit(remote.tree(remote.file("foo", parentBlob)));
		RevBlob childBlob = remote.blob(commonInBlob + "b");
		RevCommit child = remote
				.commit(remote.tree(remote.file("foo", childBlob)), parent);
		remote.update("branch1", child);

		// Pretend that we have parent to get a thin pack based on it.
		ByteArrayInputStream recvStream = uploadPackV2("command=fetch\n",
				PacketLineIn.delimiter(),
				"want " + child.toObjectId().getName() + "\n",
				"have " + parent.toObjectId().getName() + "\n", "thin-pack\n",
				"done\n", PacketLineIn.end());
		PacketLineIn pckIn = new PacketLineIn(recvStream);

		assertThat(pckIn.readString(), is("packfile"));

		// Verify that we received a thin pack by trying to apply it
		// against the client repo, which does not have parent.
		IOException e = assertThrows(IOException.class,
				() -> parsePack(recvStream));
		assertThat(e.getMessage(),
				containsString("pack has unresolved deltas"));
	}

	@Test
	public void testV2FetchNoProgress() throws Exception {
		RevCommit commit = remote.commit().message("x").create();
		remote.update("branch1", commit);

		// Without no-progress, progress is reported.
		StringWriter sw = new StringWriter();
		ByteArrayInputStream recvStream = uploadPackV2(
			"command=fetch\n",
			PacketLineIn.delimiter(),
			"want " + commit.toObjectId().getName() + "\n",
			"done\n",
				PacketLineIn.end());
		PacketLineIn pckIn = new PacketLineIn(recvStream);
		assertThat(pckIn.readString(), is("packfile"));
		parsePack(recvStream, new TextProgressMonitor(sw));
		assertFalse(sw.toString().isEmpty());

		// With no-progress, progress is not reported.
		sw = new StringWriter();
		recvStream = uploadPackV2(
			"command=fetch\n",
			PacketLineIn.delimiter(),
			"want " + commit.toObjectId().getName() + "\n",
			"no-progress\n",
			"done\n",
				PacketLineIn.end());
		pckIn = new PacketLineIn(recvStream);
		assertThat(pckIn.readString(), is("packfile"));
		parsePack(recvStream, new TextProgressMonitor(sw));
		assertTrue(sw.toString().isEmpty());
	}

	@Test
	public void testV2FetchIncludeTag() throws Exception {
		RevCommit commit = remote.commit().message("x").create();
		RevTag tag = remote.tag("tag", commit);
		remote.update("branch1", commit);
		remote.update("refs/tags/tag", tag);

		// Without include-tag.
		ByteArrayInputStream recvStream = uploadPackV2(
			"command=fetch\n",
			PacketLineIn.delimiter(),
			"want " + commit.toObjectId().getName() + "\n",
			"done\n",
				PacketLineIn.end());
		PacketLineIn pckIn = new PacketLineIn(recvStream);
		assertThat(pckIn.readString(), is("packfile"));
		parsePack(recvStream);
		assertFalse(client.getObjectDatabase().has(tag.toObjectId()));

		// With tag.
		recvStream = uploadPackV2(
			"command=fetch\n",
			PacketLineIn.delimiter(),
			"want " + commit.toObjectId().getName() + "\n",
			"include-tag\n",
			"done\n",
				PacketLineIn.end());
		pckIn = new PacketLineIn(recvStream);
		assertThat(pckIn.readString(), is("packfile"));
		parsePack(recvStream);
		assertTrue(client.getObjectDatabase().has(tag.toObjectId()));
	}

	@Test
	public void testUploadNewBytes() throws Exception {
		String commonInBlob = "abcdefghijklmnopqrstuvwx";

		RevBlob parentBlob = remote.blob(commonInBlob + "a");
		RevCommit parent = remote.commit(remote.tree(remote.file("foo", parentBlob)));
		RevBlob childBlob = remote.blob(commonInBlob + "b");
		RevCommit child = remote.commit(remote.tree(remote.file("foo", childBlob)), parent);
		remote.update("branch1", child);

		ByteArrayInputStream recvStream = uploadPackV2(
			"command=fetch\n",
			PacketLineIn.delimiter(),
			"want " + child.toObjectId().getName() + "\n",
			"ofs-delta\n",
			"done\n",
				PacketLineIn.end());
		PacketLineIn pckIn = new PacketLineIn(recvStream);
		assertThat(pckIn.readString(), is("packfile"));
		ReceivedPackStatistics receivedStats = parsePack(recvStream);
		assertTrue(receivedStats.getNumBytesDuplicated() == 0);
		assertTrue(receivedStats.getNumObjectsDuplicated() == 0);
	}

	@Test
	public void testUploadRedundantBytes() throws Exception {
		String commonInBlob = "abcdefghijklmnopqrstuvwxyz";

		RevBlob parentBlob = remote.blob(commonInBlob + "a");
		RevCommit parent = remote.commit(remote.tree(remote.file("foo", parentBlob)));
		RevBlob childBlob = remote.blob(commonInBlob + "b");
		RevCommit child = remote.commit(remote.tree(remote.file("foo", childBlob)), parent);
		remote.update("branch1", child);

		try (TestRepository<InMemoryRepository> local = new TestRepository<>(
				client)) {
			RevBlob localParentBlob = local.blob(commonInBlob + "a");
			RevCommit localParent = local
					.commit(local.tree(local.file("foo", localParentBlob)));
			RevBlob localChildBlob = local.blob(commonInBlob + "b");
			RevCommit localChild = local.commit(
					local.tree(local.file("foo", localChildBlob)), localParent);
			local.update("branch1", localChild);
		}

		ByteArrayInputStream recvStream = uploadPackV2(
				"command=fetch\n",
				PacketLineIn.delimiter(),
				"want " + child.toObjectId().getName() + "\n",
				"ofs-delta\n",
				"done\n",
					PacketLineIn.end());
		PacketLineIn pckIn = new PacketLineIn(recvStream);
		assertThat(pckIn.readString(), is("packfile"));
		ReceivedPackStatistics receivedStats = parsePack(recvStream);

		long sizeOfHeader = 12;
		long sizeOfTrailer = 20;
		long expectedSize = receivedStats.getNumBytesRead() - sizeOfHeader
				- sizeOfTrailer;
		assertTrue(receivedStats.getNumBytesDuplicated() == expectedSize);
		assertTrue(receivedStats.getNumObjectsDuplicated() == 6);
	}

	@Test
	public void testV2FetchOfsDelta() throws Exception {
		String commonInBlob = "abcdefghijklmnopqrstuvwxyz";

		RevBlob parentBlob = remote.blob(commonInBlob + "a");
		RevCommit parent = remote.commit(remote.tree(remote.file("foo", parentBlob)));
		RevBlob childBlob = remote.blob(commonInBlob + "b");
		RevCommit child = remote.commit(remote.tree(remote.file("foo", childBlob)), parent);
		remote.update("branch1", child);

		// Without ofs-delta.
		ByteArrayInputStream recvStream = uploadPackV2(
			"command=fetch\n",
			PacketLineIn.delimiter(),
			"want " + child.toObjectId().getName() + "\n",
			"done\n",
				PacketLineIn.end());
		PacketLineIn pckIn = new PacketLineIn(recvStream);
		assertThat(pckIn.readString(), is("packfile"));
		ReceivedPackStatistics receivedStats = parsePack(recvStream);
		assertTrue(receivedStats.getNumOfsDelta() == 0);

		// With ofs-delta.
		recvStream = uploadPackV2(
			"command=fetch\n",
			PacketLineIn.delimiter(),
			"want " + child.toObjectId().getName() + "\n",
			"ofs-delta\n",
			"done\n",
				PacketLineIn.end());
		pckIn = new PacketLineIn(recvStream);
		assertThat(pckIn.readString(), is("packfile"));
		receivedStats = parsePack(recvStream);
		assertTrue(receivedStats.getNumOfsDelta() != 0);
	}

	@Test
	public void testV2FetchShallow() throws Exception {
		RevCommit commonParent = remote.commit().message("parent").create();
		RevCommit fooChild = remote.commit().message("x").parent(commonParent).create();
		RevCommit barChild = remote.commit().message("y").parent(commonParent).create();
		remote.update("branch1", barChild);

		// Without shallow, the server thinks that we have
		// commonParent, so it doesn't send it.
		ByteArrayInputStream recvStream = uploadPackV2(
			"command=fetch\n",
			PacketLineIn.delimiter(),
			"want " + barChild.toObjectId().getName() + "\n",
			"have " + fooChild.toObjectId().getName() + "\n",
			"done\n",
				PacketLineIn.end());
		PacketLineIn pckIn = new PacketLineIn(recvStream);
		assertThat(pckIn.readString(), is("packfile"));
		parsePack(recvStream);
		assertTrue(client.getObjectDatabase().has(barChild.toObjectId()));
		assertFalse(client.getObjectDatabase().has(commonParent.toObjectId()));

		// With shallow, the server knows that we don't have
		// commonParent, so it sends it.
		recvStream = uploadPackV2(
			"command=fetch\n",
			PacketLineIn.delimiter(),
			"want " + barChild.toObjectId().getName() + "\n",
			"have " + fooChild.toObjectId().getName() + "\n",
			"shallow " + fooChild.toObjectId().getName() + "\n",
			"done\n",
				PacketLineIn.end());
		pckIn = new PacketLineIn(recvStream);
		assertThat(pckIn.readString(), is("packfile"));
		parsePack(recvStream);
		assertTrue(client.getObjectDatabase().has(commonParent.toObjectId()));
	}

	@Test
	public void testV2FetchDeepenAndDone() throws Exception {
		RevCommit parent = remote.commit().message("parent").create();
		RevCommit child = remote.commit().message("x").parent(parent).create();
		remote.update("branch1", child);

		// "deepen 1" sends only the child.
		ByteArrayInputStream recvStream = uploadPackV2(
			"command=fetch\n",
			PacketLineIn.delimiter(),
			"want " + child.toObjectId().getName() + "\n",
			"deepen 1\n",
			"done\n",
				PacketLineIn.end());
		PacketLineIn pckIn = new PacketLineIn(recvStream);
		assertThat(pckIn.readString(), is("shallow-info"));
		assertThat(pckIn.readString(), is("shallow " + child.toObjectId().getName()));
		assertTrue(PacketLineIn.isDelimiter(pckIn.readString()));
		assertThat(pckIn.readString(), is("packfile"));
		parsePack(recvStream);
		assertTrue(client.getObjectDatabase().has(child.toObjectId()));
		assertFalse(client.getObjectDatabase().has(parent.toObjectId()));

		// Without that, the parent is sent too.
		recvStream = uploadPackV2(
			"command=fetch\n",
			PacketLineIn.delimiter(),
			"want " + child.toObjectId().getName() + "\n",
			"done\n",
				PacketLineIn.end());
		pckIn = new PacketLineIn(recvStream);
		assertThat(pckIn.readString(), is("packfile"));
		parsePack(recvStream);
		assertTrue(client.getObjectDatabase().has(parent.toObjectId()));
	}

	@Test
	public void testV2FetchDeepenWithoutDone() throws Exception {
		RevCommit parent = remote.commit().message("parent").create();
		RevCommit child = remote.commit().message("x").parent(parent).create();
		remote.update("branch1", child);

		ByteArrayInputStream recvStream = uploadPackV2(
			"command=fetch\n",
			PacketLineIn.delimiter(),
			"want " + child.toObjectId().getName() + "\n",
			"deepen 1\n",
				PacketLineIn.end());
		PacketLineIn pckIn = new PacketLineIn(recvStream);

		// Verify that only the correct section is sent. "shallow-info"
		// is not sent because, according to the specification, it is
		// sent only if a packfile is sent.
		assertThat(pckIn.readString(), is("acknowledgments"));
		assertThat(pckIn.readString(), is("NAK"));
		assertTrue(PacketLineIn.isEnd(pckIn.readString()));
	}

	@Test
	public void testV2FetchShallowSince() throws Exception {
		PersonIdent person = new PersonIdent(remote.getRepository());

		RevCommit beyondBoundary = remote.commit()
			.committer(new PersonIdent(person, 1510000000, 0)).create();
		RevCommit boundary = remote.commit().parent(beyondBoundary)
			.committer(new PersonIdent(person, 1520000000, 0)).create();
		RevCommit tooOld = remote.commit()
			.committer(new PersonIdent(person, 1500000000, 0)).create();
		RevCommit merge = remote.commit().parent(boundary).parent(tooOld)
			.committer(new PersonIdent(person, 1530000000, 0)).create();

		remote.update("branch1", merge);

		// Report that we only have "boundary" as a shallow boundary.
		ByteArrayInputStream recvStream = uploadPackV2(
			"command=fetch\n",
			PacketLineIn.delimiter(),
			"shallow " + boundary.toObjectId().getName() + "\n",
			"deepen-since 1510000\n",
			"want " + merge.toObjectId().getName() + "\n",
			"have " + boundary.toObjectId().getName() + "\n",
			"done\n",
				PacketLineIn.end());
		PacketLineIn pckIn = new PacketLineIn(recvStream);
		assertThat(pckIn.readString(), is("shallow-info"));

		// "merge" is shallow because one of its parents is committed
		// earlier than the given deepen-since time.
		assertThat(pckIn.readString(), is("shallow " + merge.toObjectId().getName()));

		// "boundary" is unshallow because its parent committed at or
		// later than the given deepen-since time.
		assertThat(pckIn.readString(), is("unshallow " + boundary.toObjectId().getName()));

		assertTrue(PacketLineIn.isDelimiter(pckIn.readString()));
		assertThat(pckIn.readString(), is("packfile"));
		parsePack(recvStream);

		// The server does not send this because it is committed
		// earlier than the given deepen-since time.
		assertFalse(client.getObjectDatabase().has(tooOld.toObjectId()));

		// The server does not send this because the client claims to
		// have it.
		assertFalse(client.getObjectDatabase().has(boundary.toObjectId()));

		// The server sends both these commits.
		assertTrue(client.getObjectDatabase().has(beyondBoundary.toObjectId()));
		assertTrue(client.getObjectDatabase().has(merge.toObjectId()));
	}

	@Test
	public void testV2FetchShallowSince_excludedParentWithMultipleChildren() throws Exception {
		PersonIdent person = new PersonIdent(remote.getRepository());

		RevCommit base = remote.commit()
			.committer(new PersonIdent(person, 1500000000, 0)).create();
		RevCommit child1 = remote.commit().parent(base)
			.committer(new PersonIdent(person, 1510000000, 0)).create();
		RevCommit child2 = remote.commit().parent(base)
			.committer(new PersonIdent(person, 1520000000, 0)).create();

		remote.update("branch1", child1);
		remote.update("branch2", child2);

		ByteArrayInputStream recvStream = uploadPackV2(
			"command=fetch\n",
			PacketLineIn.delimiter(),
			"deepen-since 1510000\n",
			"want " + child1.toObjectId().getName() + "\n",
			"want " + child2.toObjectId().getName() + "\n",
			"done\n",
				PacketLineIn.end());
		PacketLineIn pckIn = new PacketLineIn(recvStream);
		assertThat(pckIn.readString(), is("shallow-info"));

		// "base" is excluded, so its children are shallow.
		assertThat(
			Arrays.asList(pckIn.readString(), pckIn.readString()),
			hasItems(
				"shallow " + child1.toObjectId().getName(),
				"shallow " + child2.toObjectId().getName()));

		assertTrue(PacketLineIn.isDelimiter(pckIn.readString()));
		assertThat(pckIn.readString(), is("packfile"));
		parsePack(recvStream);

		// Only the children are sent.
		assertFalse(client.getObjectDatabase().has(base.toObjectId()));
		assertTrue(client.getObjectDatabase().has(child1.toObjectId()));
		assertTrue(client.getObjectDatabase().has(child2.toObjectId()));
	}

	@Test
	public void testV2FetchShallowSince_noCommitsSelected() throws Exception {
		PersonIdent person = new PersonIdent(remote.getRepository());

		RevCommit tooOld = remote.commit()
				.committer(new PersonIdent(person, 1500000000, 0)).create();

		remote.update("branch1", tooOld);

		UploadPackInternalServerErrorException e = assertThrows(
				UploadPackInternalServerErrorException.class,
				() -> uploadPackV2("command=fetch\n", PacketLineIn.delimiter(),
						"deepen-since 1510000\n",
						"want " + tooOld.toObjectId().getName() + "\n",
						"done\n", PacketLineIn.end()));
		assertThat(e.getCause().getMessage(),
				containsString("No commits selected for shallow request"));
	}

	@Test
	public void testV2FetchDeepenNot() throws Exception {
		RevCommit one = remote.commit().message("one").create();
		RevCommit two = remote.commit().message("two").parent(one).create();
		RevCommit three = remote.commit().message("three").parent(two).create();
		RevCommit side = remote.commit().message("side").parent(one).create();
		RevCommit merge = remote.commit().message("merge")
			.parent(three).parent(side).create();

		remote.update("branch1", merge);
		remote.update("side", side);

		// The client is a shallow clone that only has "three", and
		// wants "merge" while excluding "side".
		ByteArrayInputStream recvStream = uploadPackV2(
			"command=fetch\n",
			PacketLineIn.delimiter(),
			"shallow " + three.toObjectId().getName() + "\n",
			"deepen-not side\n",
			"want " + merge.toObjectId().getName() + "\n",
			"have " + three.toObjectId().getName() + "\n",
			"done\n",
				PacketLineIn.end());
		PacketLineIn pckIn = new PacketLineIn(recvStream);
		assertThat(pckIn.readString(), is("shallow-info"));

		// "merge" is shallow because "side" is excluded by deepen-not.
		// "two" is shallow because "one" (as parent of "side") is excluded by deepen-not.
		assertThat(
			Arrays.asList(pckIn.readString(), pckIn.readString()),
			hasItems(
				"shallow " + merge.toObjectId().getName(),
				"shallow " + two.toObjectId().getName()));

		// "three" is unshallow because its parent "two" is now available.
		assertThat(pckIn.readString(), is("unshallow " + three.toObjectId().getName()));

		assertTrue(PacketLineIn.isDelimiter(pckIn.readString()));
		assertThat(pckIn.readString(), is("packfile"));
		parsePack(recvStream);

		// The server does not send these because they are excluded by
		// deepen-not.
		assertFalse(client.getObjectDatabase().has(side.toObjectId()));
		assertFalse(client.getObjectDatabase().has(one.toObjectId()));

		// The server does not send this because the client claims to
		// have it.
		assertFalse(client.getObjectDatabase().has(three.toObjectId()));

		// The server sends both these commits.
		assertTrue(client.getObjectDatabase().has(merge.toObjectId()));
		assertTrue(client.getObjectDatabase().has(two.toObjectId()));
	}

	@Test
	public void testV2FetchDeepenNot_excludeDescendantOfWant()
			throws Exception {
		RevCommit one = remote.commit().message("one").create();
		RevCommit two = remote.commit().message("two").parent(one).create();
		RevCommit three = remote.commit().message("three").parent(two).create();
		RevCommit four = remote.commit().message("four").parent(three).create();

		remote.update("two", two);
		remote.update("four", four);

		UploadPackInternalServerErrorException e = assertThrows(
				UploadPackInternalServerErrorException.class,
				() -> uploadPackV2("command=fetch\n", PacketLineIn.delimiter(),
						"deepen-not four\n",
						"want " + two.toObjectId().getName() + "\n", "done\n",
						PacketLineIn.end()));
		assertThat(e.getCause().getMessage(),
				containsString("No commits selected for shallow request"));
	}

	@Test
	public void testV2FetchDeepenNot_supportAnnotatedTags() throws Exception {
		RevCommit one = remote.commit().message("one").create();
		RevCommit two = remote.commit().message("two").parent(one).create();
		RevCommit three = remote.commit().message("three").parent(two).create();
		RevCommit four = remote.commit().message("four").parent(three).create();
		RevTag twoTag = remote.tag("twotag", two);

		remote.update("refs/tags/twotag", twoTag);
		remote.update("four", four);

		ByteArrayInputStream recvStream = uploadPackV2(
			"command=fetch\n",
			PacketLineIn.delimiter(),
			"deepen-not twotag\n",
			"want " + four.toObjectId().getName() + "\n",
			"done\n",
				PacketLineIn.end());
		PacketLineIn pckIn = new PacketLineIn(recvStream);
		assertThat(pckIn.readString(), is("shallow-info"));
		assertThat(pckIn.readString(), is("shallow " + three.toObjectId().getName()));
		assertTrue(PacketLineIn.isDelimiter(pckIn.readString()));
		assertThat(pckIn.readString(), is("packfile"));
		parsePack(recvStream);
		assertFalse(client.getObjectDatabase().has(one.toObjectId()));
		assertFalse(client.getObjectDatabase().has(two.toObjectId()));
		assertTrue(client.getObjectDatabase().has(three.toObjectId()));
		assertTrue(client.getObjectDatabase().has(four.toObjectId()));
	}

	@Test
	public void testV2FetchDeepenNot_excludedParentWithMultipleChildren() throws Exception {
		PersonIdent person = new PersonIdent(remote.getRepository());

		RevCommit base = remote.commit()
			.committer(new PersonIdent(person, 1500000000, 0)).create();
		RevCommit child1 = remote.commit().parent(base)
			.committer(new PersonIdent(person, 1510000000, 0)).create();
		RevCommit child2 = remote.commit().parent(base)
			.committer(new PersonIdent(person, 1520000000, 0)).create();

		remote.update("base", base);
		remote.update("branch1", child1);
		remote.update("branch2", child2);

		ByteArrayInputStream recvStream = uploadPackV2(
			"command=fetch\n",
			PacketLineIn.delimiter(),
			"deepen-not base\n",
			"want " + child1.toObjectId().getName() + "\n",
			"want " + child2.toObjectId().getName() + "\n",
			"done\n",
				PacketLineIn.end());
		PacketLineIn pckIn = new PacketLineIn(recvStream);
		assertThat(pckIn.readString(), is("shallow-info"));

		// "base" is excluded, so its children are shallow.
		assertThat(
			Arrays.asList(pckIn.readString(), pckIn.readString()),
			hasItems(
				"shallow " + child1.toObjectId().getName(),
				"shallow " + child2.toObjectId().getName()));

		assertTrue(PacketLineIn.isDelimiter(pckIn.readString()));
		assertThat(pckIn.readString(), is("packfile"));
		parsePack(recvStream);

		// Only the children are sent.
		assertFalse(client.getObjectDatabase().has(base.toObjectId()));
		assertTrue(client.getObjectDatabase().has(child1.toObjectId()));
		assertTrue(client.getObjectDatabase().has(child2.toObjectId()));
	}

	@Test
	public void testV2FetchUnrecognizedArgument() throws Exception {
		UploadPackInternalServerErrorException e = assertThrows(
				UploadPackInternalServerErrorException.class,
				() -> uploadPackV2("command=fetch\n", PacketLineIn.delimiter(),
						"invalid-argument\n", PacketLineIn.end()));
		assertThat(e.getCause().getMessage(),
				containsString("unexpected invalid-argument"));
	}

	@Test
	public void testV2FetchServerOptions() throws Exception {
		String[] lines = { "command=fetch\n", "server-option=one\n",
				"server-option=two\n", PacketLineIn.delimiter(),
				PacketLineIn.end() };

		TestV2Hook testHook = new TestV2Hook();
		uploadPackSetup(TransferConfig.ProtocolVersion.V2.version(),
				(UploadPack up) -> {
					up.setProtocolV2Hook(testHook);
				}, lines);

		FetchV2Request req = testHook.fetchRequest;
		assertNotNull(req);
		assertEquals(2, req.getServerOptions().size());
		assertThat(req.getServerOptions(), hasItems("one", "two"));
	}

	@Test
	public void testV2FetchFilter() throws Exception {
		RevBlob big = remote.blob("foobar");
		RevBlob small = remote.blob("fooba");
		RevTree tree = remote.tree(remote.file("1", big),
				remote.file("2", small));
		RevCommit commit = remote.commit(tree);
		remote.update("master", commit);

		server.getConfig().setBoolean("uploadpack", null, "allowfilter", true);

		ByteArrayInputStream recvStream = uploadPackV2(
			"command=fetch\n",
			PacketLineIn.delimiter(),
			"want " + commit.toObjectId().getName() + "\n",
			"filter blob:limit=5\n",
			"done\n",
				PacketLineIn.end());
		PacketLineIn pckIn = new PacketLineIn(recvStream);
		assertThat(pckIn.readString(), is("packfile"));
		parsePack(recvStream);

		assertFalse(client.getObjectDatabase().has(big.toObjectId()));
		assertTrue(client.getObjectDatabase().has(small.toObjectId()));
	}

	abstract class TreeBuilder {
		abstract void addElements(DirCacheBuilder dcBuilder) throws Exception;

		RevTree build() throws Exception {
			DirCache dc = DirCache.newInCore();
			DirCacheBuilder dcBuilder = dc.builder();
			addElements(dcBuilder);
			dcBuilder.finish();
			ObjectId id;
			try (ObjectInserter ins =
					remote.getRepository().newObjectInserter()) {
				id = dc.writeTree(ins);
				ins.flush();
			}
			return remote.getRevWalk().parseTree(id);
		}
	}

	class DeepTreePreparator {
		RevBlob blobLowDepth = remote.blob("lo");
		RevBlob blobHighDepth = remote.blob("hi");

		RevTree subtree = remote.tree(remote.file("1", blobHighDepth));
		RevTree rootTree = (new TreeBuilder() {
				@Override
				void addElements(DirCacheBuilder dcBuilder) throws Exception {
					dcBuilder.add(remote.file("1", blobLowDepth));
					dcBuilder.addTree(new byte[] {'2'}, DirCacheEntry.STAGE_0,
							remote.getRevWalk().getObjectReader(), subtree);
				}
			}).build();
		RevCommit commit = remote.commit(rootTree);

		DeepTreePreparator() throws Exception {}
	}

	private void uploadV2WithTreeDepthFilter(
			long depth, ObjectId... wants) throws Exception {
		server.getConfig().setBoolean("uploadpack", null, "allowfilter", true);

		List<String> input = new ArrayList<>();
		input.add("command=fetch\n");
		input.add(PacketLineIn.delimiter());
		for (ObjectId want : wants) {
			input.add("want " + want.getName() + "\n");
		}
		input.add("filter tree:" + depth + "\n");
		input.add("done\n");
		input.add(PacketLineIn.end());
		ByteArrayInputStream recvStream =
				uploadPackV2(
						(UploadPack up) -> {up.setRequestPolicy(RequestPolicy.ANY);},
						input.toArray(new String[0]));
		PacketLineIn pckIn = new PacketLineIn(recvStream);
		assertThat(pckIn.readString(), is("packfile"));
		parsePack(recvStream);
	}

	@Test
	public void testV2FetchFilterTreeDepth0() throws Exception {
		DeepTreePreparator preparator = new DeepTreePreparator();
		remote.update("master", preparator.commit);

		uploadV2WithTreeDepthFilter(0, preparator.commit.toObjectId());

		assertFalse(client.getObjectDatabase()
				.has(preparator.rootTree.toObjectId()));
		assertFalse(client.getObjectDatabase()
				.has(preparator.subtree.toObjectId()));
		assertFalse(client.getObjectDatabase()
				.has(preparator.blobLowDepth.toObjectId()));
		assertFalse(client.getObjectDatabase()
				.has(preparator.blobHighDepth.toObjectId()));
		assertEquals(1, stats.getTreesTraversed());
	}

	@Test
	public void testV2FetchFilterTreeDepth1_serverHasBitmap() throws Exception {
		DeepTreePreparator preparator = new DeepTreePreparator();
		remote.update("master", preparator.commit);

		// The bitmap should be ignored since we need to track the depth while
		// traversing the trees.
		generateBitmaps(server);

		uploadV2WithTreeDepthFilter(1, preparator.commit.toObjectId());

		assertTrue(client.getObjectDatabase()
				.has(preparator.rootTree.toObjectId()));
		assertFalse(client.getObjectDatabase()
				.has(preparator.subtree.toObjectId()));
		assertFalse(client.getObjectDatabase()
				.has(preparator.blobLowDepth.toObjectId()));
		assertFalse(client.getObjectDatabase()
				.has(preparator.blobHighDepth.toObjectId()));
		assertEquals(1, stats.getTreesTraversed());
	}

	@Test
	public void testV2FetchFilterTreeDepth2() throws Exception {
		DeepTreePreparator preparator = new DeepTreePreparator();
		remote.update("master", preparator.commit);

		uploadV2WithTreeDepthFilter(2, preparator.commit.toObjectId());

		assertTrue(client.getObjectDatabase()
				.has(preparator.rootTree.toObjectId()));
		assertTrue(client.getObjectDatabase()
				.has(preparator.subtree.toObjectId()));
		assertTrue(client.getObjectDatabase()
				.has(preparator.blobLowDepth.toObjectId()));
		assertFalse(client.getObjectDatabase()
				.has(preparator.blobHighDepth.toObjectId()));
		assertEquals(2, stats.getTreesTraversed());
	}

	/**
	 * Creates a commit with the following files:
	 * <pre>
	 * a/x/b/foo
	 * x/b/foo
	 * </pre>
	 * which has an identical tree in two locations: once at / and once at /a
	 */
	class RepeatedSubtreePreparator {
		RevBlob foo = remote.blob("foo");
		RevTree subtree3 = remote.tree(remote.file("foo", foo));
		RevTree subtree2 = (new TreeBuilder() {
			@Override
			void addElements(DirCacheBuilder dcBuilder) throws Exception {
				dcBuilder.addTree(new byte[] {'b'}, DirCacheEntry.STAGE_0,
						remote.getRevWalk().getObjectReader(), subtree3);
			}
		}).build();
		RevTree subtree1 = (new TreeBuilder() {
			@Override
			void addElements(DirCacheBuilder dcBuilder) throws Exception {
				dcBuilder.addTree(new byte[] {'x'}, DirCacheEntry.STAGE_0,
						remote.getRevWalk().getObjectReader(), subtree2);
			}
		}).build();
		RevTree rootTree = (new TreeBuilder() {
			@Override
			void addElements(DirCacheBuilder dcBuilder) throws Exception {
				dcBuilder.addTree(new byte[] {'a'}, DirCacheEntry.STAGE_0,
						remote.getRevWalk().getObjectReader(), subtree1);
				dcBuilder.addTree(new byte[] {'x'}, DirCacheEntry.STAGE_0,
						remote.getRevWalk().getObjectReader(), subtree2);
			}
		}).build();
		RevCommit commit = remote.commit(rootTree);

		RepeatedSubtreePreparator() throws Exception {}
	}

	@Test
	public void testV2FetchFilterTreeDepth_iterateOverTreeAtTwoLevels()
			throws Exception {
		// Test tree:<depth> where a tree is iterated to twice - once where a
		// subentry is too deep to be included, and again where the blob inside
		// it is shallow enough to be included.
		RepeatedSubtreePreparator preparator = new RepeatedSubtreePreparator();
		remote.update("master", preparator.commit);

		uploadV2WithTreeDepthFilter(4, preparator.commit.toObjectId());

		assertTrue(client.getObjectDatabase()
				.has(preparator.foo.toObjectId()));
	}

	/**
	 * Creates a commit with the following files:
	 * <pre>
	 * a/x/b/foo
	 * b/u/c/baz
	 * y/x/b/foo
	 * z/v/c/baz
	 * </pre>
	 * which has two pairs of identical trees:
	 * <ul>
	 * <li>one at /a and /y
	 * <li>one at /b/u and /z/v
	 * </ul>
	 * Note that this class defines unique 8 trees (rootTree and subtree1-7)
	 * which means PackStatistics should report having visited 8 trees.
	 */
	class RepeatedSubtreeAtSameLevelPreparator {
		RevBlob foo = remote.blob("foo");

		/** foo */
		RevTree subtree1 = remote.tree(remote.file("foo", foo));

		/** b/foo */
		RevTree subtree2 = (new TreeBuilder() {
			@Override
			void addElements(DirCacheBuilder dcBuilder) throws Exception {
				dcBuilder.addTree(new byte[] {'b'}, DirCacheEntry.STAGE_0,
						remote.getRevWalk().getObjectReader(), subtree1);
			}
		}).build();

		/** x/b/foo */
		RevTree subtree3 = (new TreeBuilder() {
			@Override
			void addElements(DirCacheBuilder dcBuilder) throws Exception {
				dcBuilder.addTree(new byte[] {'x'}, DirCacheEntry.STAGE_0,
						remote.getRevWalk().getObjectReader(), subtree2);
			}
		}).build();

		RevBlob baz = remote.blob("baz");

		/** baz */
		RevTree subtree4 = remote.tree(remote.file("baz", baz));

		/** c/baz */
		RevTree subtree5 = (new TreeBuilder() {
			@Override
			void addElements(DirCacheBuilder dcBuilder) throws Exception {
				dcBuilder.addTree(new byte[] {'c'}, DirCacheEntry.STAGE_0,
						remote.getRevWalk().getObjectReader(), subtree4);
			}
		}).build();

		/** u/c/baz */
		RevTree subtree6 = (new TreeBuilder() {
			@Override
			void addElements(DirCacheBuilder dcBuilder) throws Exception {
				dcBuilder.addTree(new byte[] {'u'}, DirCacheEntry.STAGE_0,
						remote.getRevWalk().getObjectReader(), subtree5);
			}
		}).build();

		/** v/c/baz */
		RevTree subtree7 = (new TreeBuilder() {
			@Override
			void addElements(DirCacheBuilder dcBuilder) throws Exception {
				dcBuilder.addTree(new byte[] {'v'}, DirCacheEntry.STAGE_0,
						remote.getRevWalk().getObjectReader(), subtree5);
			}
		}).build();

		RevTree rootTree = (new TreeBuilder() {
			@Override
			void addElements(DirCacheBuilder dcBuilder) throws Exception {
				dcBuilder.addTree(new byte[] {'a'}, DirCacheEntry.STAGE_0,
						remote.getRevWalk().getObjectReader(), subtree3);
				dcBuilder.addTree(new byte[] {'b'}, DirCacheEntry.STAGE_0,
						remote.getRevWalk().getObjectReader(), subtree6);
				dcBuilder.addTree(new byte[] {'y'}, DirCacheEntry.STAGE_0,
						remote.getRevWalk().getObjectReader(), subtree3);
				dcBuilder.addTree(new byte[] {'z'}, DirCacheEntry.STAGE_0,
						remote.getRevWalk().getObjectReader(), subtree7);
			}
		}).build();
		RevCommit commit = remote.commit(rootTree);

		RepeatedSubtreeAtSameLevelPreparator() throws Exception {}
	}

	@Test
	public void testV2FetchFilterTreeDepth_repeatTreeAtSameLevelIncludeFile()
			throws Exception {
		RepeatedSubtreeAtSameLevelPreparator preparator =
				new RepeatedSubtreeAtSameLevelPreparator();
		remote.update("master", preparator.commit);

		uploadV2WithTreeDepthFilter(5, preparator.commit.toObjectId());

		assertTrue(client.getObjectDatabase()
				.has(preparator.foo.toObjectId()));
		assertTrue(client.getObjectDatabase()
				.has(preparator.baz.toObjectId()));
		assertEquals(8, stats.getTreesTraversed());
	}

	@Test
	public void testV2FetchFilterTreeDepth_repeatTreeAtSameLevelExcludeFile()
			throws Exception {
		RepeatedSubtreeAtSameLevelPreparator preparator =
				new RepeatedSubtreeAtSameLevelPreparator();
		remote.update("master", preparator.commit);

		uploadV2WithTreeDepthFilter(4, preparator.commit.toObjectId());

		assertFalse(client.getObjectDatabase()
				.has(preparator.foo.toObjectId()));
		assertFalse(client.getObjectDatabase()
				.has(preparator.baz.toObjectId()));
		assertEquals(8, stats.getTreesTraversed());
	}

	@Test
	public void testWantFilteredObject() throws Exception {
		RepeatedSubtreePreparator preparator = new RepeatedSubtreePreparator();
		remote.update("master", preparator.commit);

		// Specify wanted blob objects that are deep enough to be filtered. We
		// should still upload them.
		uploadV2WithTreeDepthFilter(
				3,
				preparator.commit.toObjectId(),
				preparator.foo.toObjectId());
		assertTrue(client.getObjectDatabase()
				.has(preparator.foo.toObjectId()));

		client = newRepo("client");
		// Specify a wanted tree object that is deep enough to be filtered. We
		// should still upload it.
		uploadV2WithTreeDepthFilter(
				2,
				preparator.commit.toObjectId(),
				preparator.subtree3.toObjectId());
		assertTrue(client.getObjectDatabase()
				.has(preparator.foo.toObjectId()));
		assertTrue(client.getObjectDatabase()
				.has(preparator.subtree3.toObjectId()));
	}

	private void checkV2FetchWhenNotAllowed(String fetchLine, String expectedMessage)
			throws Exception {
		RevCommit commit = remote.commit().message("0").create();
		remote.update("master", commit);

		UploadPackInternalServerErrorException e = assertThrows(
				UploadPackInternalServerErrorException.class,
				() -> uploadPackV2("command=fetch\n", PacketLineIn.delimiter(),
						"want " + commit.toObjectId().getName() + "\n",
						fetchLine, "done\n", PacketLineIn.end()));
		assertThat(e.getCause().getMessage(),
				containsString(expectedMessage));
	}

	@Test
	public void testV2FetchFilterWhenNotAllowed() throws Exception {
		checkV2FetchWhenNotAllowed(
			"filter blob:limit=5\n",
			"unexpected filter blob:limit=5");
	}

	@Test
	public void testV2FetchWantRefIfNotAllowed() throws Exception {
		checkV2FetchWhenNotAllowed(
			"want-ref refs/heads/one\n",
			"unexpected want-ref refs/heads/one");
	}

	@Test
	public void testV2FetchSidebandAllIfNotAllowed() throws Exception {
		checkV2FetchWhenNotAllowed(
			"sideband-all\n",
			"unexpected sideband-all");
	}

	@Test
	public void testV2FetchWantRef() throws Exception {
		RevCommit one = remote.commit().message("1").create();
		RevCommit two = remote.commit().message("2").create();
		RevCommit three = remote.commit().message("3").create();
		remote.update("one", one);
		remote.update("two", two);
		remote.update("three", three);

		server.getConfig().setBoolean("uploadpack", null, "allowrefinwant", true);

		ByteArrayInputStream recvStream = uploadPackV2(
			"command=fetch\n",
			PacketLineIn.delimiter(),
			"want-ref refs/heads/one\n",
			"want-ref refs/heads/two\n",
			"done\n",
				PacketLineIn.end());
		PacketLineIn pckIn = new PacketLineIn(recvStream);
		assertThat(pckIn.readString(), is("wanted-refs"));
		assertThat(
				Arrays.asList(pckIn.readString(), pckIn.readString()),
				hasItems(
					one.toObjectId().getName() + " refs/heads/one",
					two.toObjectId().getName() + " refs/heads/two"));
		assertTrue(PacketLineIn.isDelimiter(pckIn.readString()));
		assertThat(pckIn.readString(), is("packfile"));
		parsePack(recvStream);

		assertTrue(client.getObjectDatabase().has(one.toObjectId()));
		assertTrue(client.getObjectDatabase().has(two.toObjectId()));
		assertFalse(client.getObjectDatabase().has(three.toObjectId()));
	}

	@Test
	public void testV2FetchBadWantRef() throws Exception {
		RevCommit one = remote.commit().message("1").create();
		remote.update("one", one);

		server.getConfig().setBoolean("uploadpack", null, "allowrefinwant",
				true);

		UploadPackInternalServerErrorException e = assertThrows(
				UploadPackInternalServerErrorException.class,
				() -> uploadPackV2("command=fetch\n", PacketLineIn.delimiter(),
						"want-ref refs/heads/one\n",
						"want-ref refs/heads/nonExistentRef\n", "done\n",
						PacketLineIn.end()));
		assertThat(e.getCause().getMessage(),
				containsString("Invalid ref name: refs/heads/nonExistentRef"));
	}

	@Test
	public void testV2FetchMixedWantRef() throws Exception {
		RevCommit one = remote.commit().message("1").create();
		RevCommit two = remote.commit().message("2").create();
		RevCommit three = remote.commit().message("3").create();
		remote.update("one", one);
		remote.update("two", two);
		remote.update("three", three);

		server.getConfig().setBoolean("uploadpack", null, "allowrefinwant", true);

		ByteArrayInputStream recvStream = uploadPackV2(
			"command=fetch\n",
			PacketLineIn.delimiter(),
			"want-ref refs/heads/one\n",
			"want " + two.toObjectId().getName() + "\n",
			"done\n",
				PacketLineIn.end());
		PacketLineIn pckIn = new PacketLineIn(recvStream);
		assertThat(pckIn.readString(), is("wanted-refs"));
		assertThat(
				pckIn.readString(),
				is(one.toObjectId().getName() + " refs/heads/one"));
		assertTrue(PacketLineIn.isDelimiter(pckIn.readString()));
		assertThat(pckIn.readString(), is("packfile"));
		parsePack(recvStream);

		assertTrue(client.getObjectDatabase().has(one.toObjectId()));
		assertTrue(client.getObjectDatabase().has(two.toObjectId()));
		assertFalse(client.getObjectDatabase().has(three.toObjectId()));
	}

	@Test
	public void testV2FetchWantRefWeAlreadyHave() throws Exception {
		RevCommit one = remote.commit().message("1").create();
		remote.update("one", one);

		server.getConfig().setBoolean("uploadpack", null, "allowrefinwant", true);

		ByteArrayInputStream recvStream = uploadPackV2(
			"command=fetch\n",
			PacketLineIn.delimiter(),
			"want-ref refs/heads/one\n",
			"have " + one.toObjectId().getName(),
			"done\n",
				PacketLineIn.end());
		PacketLineIn pckIn = new PacketLineIn(recvStream);

		// The client still needs to know the hash of the object that
		// refs/heads/one points to, even though it already has the
		// object ...
		assertThat(pckIn.readString(), is("wanted-refs"));
		assertThat(
				pckIn.readString(),
				is(one.toObjectId().getName() + " refs/heads/one"));
		assertTrue(PacketLineIn.isDelimiter(pckIn.readString()));

		// ... but the client does not need the object itself.
		assertThat(pckIn.readString(), is("packfile"));
		parsePack(recvStream);
		assertFalse(client.getObjectDatabase().has(one.toObjectId()));
	}

	@Test
	public void testV2FetchWantRefAndDeepen() throws Exception {
		RevCommit parent = remote.commit().message("parent").create();
		RevCommit child = remote.commit().message("x").parent(parent).create();
		remote.update("branch1", child);

		server.getConfig().setBoolean("uploadpack", null, "allowrefinwant", true);

		ByteArrayInputStream recvStream = uploadPackV2(
			"command=fetch\n",
			PacketLineIn.delimiter(),
			"want-ref refs/heads/branch1\n",
			"deepen 1\n",
			"done\n",
				PacketLineIn.end());
		PacketLineIn pckIn = new PacketLineIn(recvStream);

		// shallow-info appears first, then wanted-refs.
		assertThat(pckIn.readString(), is("shallow-info"));
		assertThat(pckIn.readString(), is("shallow " + child.toObjectId().getName()));
		assertTrue(PacketLineIn.isDelimiter(pckIn.readString()));
		assertThat(pckIn.readString(), is("wanted-refs"));
		assertThat(pckIn.readString(), is(child.toObjectId().getName() + " refs/heads/branch1"));
		assertTrue(PacketLineIn.isDelimiter(pckIn.readString()));
		assertThat(pckIn.readString(), is("packfile"));
		parsePack(recvStream);
		assertTrue(client.getObjectDatabase().has(child.toObjectId()));
		assertFalse(client.getObjectDatabase().has(parent.toObjectId()));
	}

	@Test
	public void testV2FetchMissingShallow() throws Exception {
		RevCommit one = remote.commit().message("1").create();
		RevCommit two = remote.commit().message("2").parent(one).create();
		RevCommit three = remote.commit().message("3").parent(two).create();
		remote.update("three", three);

		server.getConfig().setBoolean("uploadpack", null, "allowrefinwant",
				true);

		ByteArrayInputStream recvStream = uploadPackV2("command=fetch\n",
				PacketLineIn.delimiter(),
				"want-ref refs/heads/three\n",
				"deepen 3",
				"shallow 0123012301230123012301230123012301230123",
				"shallow " + two.getName() + '\n',
				"done\n",
				PacketLineIn.end());
		PacketLineIn pckIn = new PacketLineIn(recvStream);

		assertThat(pckIn.readString(), is("shallow-info"));
		assertThat(pckIn.readString(),
				is("shallow " + one.toObjectId().getName()));
		assertThat(pckIn.readString(),
				is("unshallow " + two.toObjectId().getName()));
		assertTrue(PacketLineIn.isDelimiter(pckIn.readString()));
		assertThat(pckIn.readString(), is("wanted-refs"));
		assertThat(pckIn.readString(),
				is(three.toObjectId().getName() + " refs/heads/three"));
		assertTrue(PacketLineIn.isDelimiter(pckIn.readString()));
		assertThat(pckIn.readString(), is("packfile"));
		parsePack(recvStream);

		assertTrue(client.getObjectDatabase().has(one.toObjectId()));
		assertTrue(client.getObjectDatabase().has(two.toObjectId()));
		assertTrue(client.getObjectDatabase().has(three.toObjectId()));
	}

	@Test
	public void testV2FetchSidebandAllNoPackfile() throws Exception {
		RevCommit fooParent = remote.commit().message("x").create();
		RevCommit fooChild = remote.commit().message("x").parent(fooParent).create();
		RevCommit barParent = remote.commit().message("y").create();
		RevCommit barChild = remote.commit().message("y").parent(barParent).create();
		remote.update("branch1", fooChild);
		remote.update("branch2", barChild);

		server.getConfig().setBoolean("uploadpack", null, "allowsidebandall", true);

		ByteArrayInputStream recvStream = uploadPackV2(
			"command=fetch\n",
			PacketLineIn.delimiter(),
			"sideband-all\n",
			"want " + fooChild.toObjectId().getName() + "\n",
			"want " + barChild.toObjectId().getName() + "\n",
			"have " + fooParent.toObjectId().getName() + "\n",
			PacketLineIn.end());
		PacketLineIn pckIn = new PacketLineIn(recvStream);

		assertThat(pckIn.readString(), is("\001acknowledgments"));
		assertThat(pckIn.readString(), is("\001ACK " + fooParent.getName()));
		assertTrue(PacketLineIn.isEnd(pckIn.readString()));
	}

	@Test
	public void testV2FetchSidebandAllPackfile() throws Exception {
		RevCommit commit = remote.commit().message("x").create();
		remote.update("master", commit);

		server.getConfig().setBoolean("uploadpack", null, "allowsidebandall", true);

		ByteArrayInputStream recvStream = uploadPackV2("command=fetch\n",
				PacketLineIn.delimiter(),
				"want " + commit.getName() + "\n",
				"sideband-all\n",
				"done\n",
				PacketLineIn.end());
		PacketLineIn pckIn = new PacketLineIn(recvStream);

		String s;
		// When sideband-all is used, object counting happens before
		// "packfile" is written, and object counting outputs progress
		// in sideband 2. Skip all these lines.
		for (s = pckIn.readString(); s.startsWith("\002"); s = pckIn.readString()) {
			// do nothing
		}
		assertThat(s, is("\001packfile"));
		parsePack(recvStream);
	}

	@Test
	public void testV2FetchPackfileUris() throws Exception {
		// Inside the pack
		RevCommit commit = remote.commit().message("x").create();
		remote.update("master", commit);
		generateBitmaps(server);

		// Outside the pack
		RevCommit commit2 = remote.commit().message("x").parent(commit).create();
		remote.update("master", commit2);

		server.getConfig().setBoolean("uploadpack", null, "allowsidebandall", true);

		ByteArrayInputStream recvStream = uploadPackV2(
			(UploadPack up) -> {
				up.setCachedPackUriProvider(new CachedPackUriProvider() {
					@Override
					public PackInfo getInfo(CachedPack pack,
							Collection<String> protocolsSupported)
							throws IOException {
						assertThat(protocolsSupported, hasItems("https"));
						if (!protocolsSupported.contains("https"))
							return null;
						return new PackInfo("myhash", "myuri", 100);
					}

				});
			},
			"command=fetch\n",
			PacketLineIn.delimiter(),
			"want " + commit2.getName() + "\n",
			"sideband-all\n",
			"packfile-uris https\n",
			"done\n",
			PacketLineIn.end());
		PacketLineIn pckIn = new PacketLineIn(recvStream);

		String s;
		// skip all \002 strings
		for (s = pckIn.readString(); s.startsWith("\002"); s = pckIn.readString()) {
			// do nothing
		}
		assertThat(s, is("\001packfile-uris"));
		assertThat(pckIn.readString(), is("\001myhash myuri"));
		assertTrue(PacketLineIn.isDelimiter(pckIn.readString()));
		assertThat(pckIn.readString(), is("\001packfile"));
		parsePack(recvStream);

		assertFalse(client.getObjectDatabase().has(commit.toObjectId()));
		assertTrue(client.getObjectDatabase().has(commit2.toObjectId()));
	}

	@Test
	public void testGetPeerAgentProtocolV0() throws Exception {
		RevCommit one = remote.commit().message("1").create();
		remote.update("one", one);

		UploadPack up = new UploadPack(server);
		ByteArrayInputStream send = linesAsInputStream(
				"want " + one.getName() + " agent=JGit-test/1.2.3\n",
				PacketLineIn.end(),
				"have 11cedf1b796d44207da702f7d420684022fc0f09\n", "done\n");

		ByteArrayOutputStream recv = new ByteArrayOutputStream();
		up.upload(send, recv, null);

		assertEquals(up.getPeerUserAgent(), "JGit-test/1.2.3");
	}

	@Test
	public void testGetPeerAgentProtocolV2() throws Exception {
		server.getConfig().setString(ConfigConstants.CONFIG_PROTOCOL_SECTION,
				null, ConfigConstants.CONFIG_KEY_VERSION,
				TransferConfig.ProtocolVersion.V2.version());

		RevCommit one = remote.commit().message("1").create();
		remote.update("one", one);

		UploadPack up = new UploadPack(server);
		up.setExtraParameters(Sets.of("version=2"));

		ByteArrayInputStream send = linesAsInputStream(
				"command=fetch\n", "agent=JGit-test/1.2.4\n",
				PacketLineIn.delimiter(), "want " + one.getName() + "\n",
				"have 11cedf1b796d44207da702f7d420684022fc0f09\n", "done\n",
				PacketLineIn.end());

		ByteArrayOutputStream recv = new ByteArrayOutputStream();
		up.upload(send, recv, null);

		assertEquals(up.getPeerUserAgent(), "JGit-test/1.2.4");
	}

	private static class RejectAllRefFilter implements RefFilter {
		@Override
		public Map<String, Ref> filter(Map<String, Ref> refs) {
			return new HashMap<>();
		}
	}

	@Test
	public void testSingleBranchCloneTagChain() throws Exception {
		RevBlob blob0 = remote.blob("Initial content of first file");
		RevBlob blob1 = remote.blob("Second file content");
		RevCommit commit0 = remote
				.commit(remote.tree(remote.file("prvni.txt", blob0)));
		RevCommit commit1 = remote
				.commit(remote.tree(remote.file("druhy.txt", blob1)), commit0);
		remote.update("master", commit1);

		RevTag heavyTag1 = remote.tag("commitTagRing", commit0);
		remote.getRevWalk().parseHeaders(heavyTag1);
		RevTag heavyTag2 = remote.tag("middleTagRing", heavyTag1);
		remote.lightweightTag("refTagRing", heavyTag2);

		UploadPack uploadPack = new UploadPack(remote.getRepository());

		ByteArrayOutputStream cli = new ByteArrayOutputStream();
		PacketLineOut clientWant = new PacketLineOut(cli);
		clientWant.writeString("want " + commit1.name()
				+ " multi_ack_detailed include-tag thin-pack ofs-delta agent=tempo/pflaska");
		clientWant.end();
		clientWant.writeString("done\n");

		try (ByteArrayOutputStream serverResponse = new ByteArrayOutputStream()) {

			uploadPack.setPreUploadHook(new PreUploadHook() {
				@Override
				public void onBeginNegotiateRound(UploadPack up,
						Collection<? extends ObjectId> wants, int cntOffered)
						throws ServiceMayNotContinueException {
					// Do nothing.
				}

				@Override
				public void onEndNegotiateRound(UploadPack up,
						Collection<? extends ObjectId> wants, int cntCommon,
						int cntNotFound, boolean ready)
						throws ServiceMayNotContinueException {
					// Do nothing.
				}

				@Override
				public void onSendPack(UploadPack up,
						Collection<? extends ObjectId> wants,
						Collection<? extends ObjectId> haves)
						throws ServiceMayNotContinueException {
					// collect pack data
					serverResponse.reset();
				}
			});
			uploadPack.upload(new ByteArrayInputStream(cli.toByteArray()),
					serverResponse, System.err);
			InputStream packReceived = new ByteArrayInputStream(
					serverResponse.toByteArray());
			PackLock lock = null;
			try (ObjectInserter ins = client.newObjectInserter()) {
				PackParser parser = ins.newPackParser(packReceived);
				parser.setAllowThin(true);
				parser.setLockMessage("receive-tag-chain");
				ProgressMonitor mlc = NullProgressMonitor.INSTANCE;
				lock = parser.parse(mlc, mlc);
				ins.flush();
			} finally {
				if (lock != null) {
					lock.unlock();
				}
			}
			InMemoryRepository.MemObjDatabase objDb = client
					.getObjectDatabase();
			assertTrue(objDb.has(blob0.toObjectId()));
			assertTrue(objDb.has(blob1.toObjectId()));
			assertTrue(objDb.has(commit0.toObjectId()));
			assertTrue(objDb.has(commit1.toObjectId()));
			assertTrue(objDb.has(heavyTag1.toObjectId()));
			assertTrue(objDb.has(heavyTag2.toObjectId()));
		}
	}

	@Test
	public void testSafeToClearRefsInFetchV0() throws Exception {
		server =
			new RefCallsCountingRepository(
				new DfsRepositoryDescription("server"));
		remote = new TestRepository<>(server);
		RevCommit one = remote.commit().message("1").create();
		remote.update("one", one);
		testProtocol = new TestProtocol<>((Object req, Repository db) -> {
			UploadPack up = new UploadPack(db);
			return up;
		}, null);
		uri = testProtocol.register(ctx, server);
		try (Transport tn = testProtocol.open(uri, client, "server")) {
			tn.fetch(NullProgressMonitor.INSTANCE,
				Collections.singletonList(new RefSpec(one.name())));
		}
		assertTrue(client.getObjectDatabase().has(one.toObjectId()));
		assertEquals(1, ((RefCallsCountingRepository)server).numRefCalls());
	}

	@Test
	public void testSafeToClearRefsInFetchV2() throws Exception {
		server =
			new RefCallsCountingRepository(
				new DfsRepositoryDescription("server"));
		remote = new TestRepository<>(server);
		RevCommit one = remote.commit().message("1").create();
		RevCommit two = remote.commit().message("2").create();
		remote.update("one", one);
		remote.update("two", two);
		server.getConfig().setBoolean("uploadpack", null, "allowrefinwant", true);
		ByteArrayInputStream recvStream = uploadPackV2(
			"command=fetch\n",
			PacketLineIn.delimiter(),
			"want-ref refs/heads/one\n",
			"want-ref refs/heads/two\n",
			"done\n",
			PacketLineIn.end());
		PacketLineIn pckIn = new PacketLineIn(recvStream);
		assertThat(pckIn.readString(), is("wanted-refs"));
		assertThat(
			Arrays.asList(pckIn.readString(), pckIn.readString()),
			hasItems(
				one.toObjectId().getName() + " refs/heads/one",
				two.toObjectId().getName() + " refs/heads/two"));
		assertTrue(PacketLineIn.isDelimiter(pckIn.readString()));
		assertThat(pckIn.readString(), is("packfile"));
		parsePack(recvStream);
		assertTrue(client.getObjectDatabase().has(one.toObjectId()));
		assertEquals(1, ((RefCallsCountingRepository)server).numRefCalls());
	}

	@Test
	public void testNotAdvertisedWantsV1Fetch() throws Exception {
		String commonInBlob = "abcdefghijklmnopqrstuvwxyz";

		RevBlob parentBlob = remote.blob(commonInBlob + "a");
		RevCommit parent = remote
				.commit(remote.tree(remote.file("foo", parentBlob)));
		RevBlob childBlob = remote.blob(commonInBlob + "b");
		RevCommit child = remote
				.commit(remote.tree(remote.file("foo", childBlob)), parent);
		remote.update("branch1", child);

		uploadPackV1("want " + child.toObjectId().getName() + "\n",
				PacketLineIn.end(),
				"have " + parent.toObjectId().getName() + "\n",
				"done\n", PacketLineIn.end());

		assertEquals(0, stats.getNotAdvertisedWants());
	}

	@Test
	public void testNotAdvertisedWantsV1FetchRequestPolicyReachableCommit() throws Exception {
		String commonInBlob = "abcdefghijklmnopqrstuvwxyz";

		RevBlob parentBlob = remote.blob(commonInBlob + "a");
		RevCommit parent = remote
				.commit(remote.tree(remote.file("foo", parentBlob)));
		RevBlob childBlob = remote.blob(commonInBlob + "b");
		RevCommit child = remote
				.commit(remote.tree(remote.file("foo", childBlob)), parent);

		remote.update("branch1", child);

		uploadPackV1((UploadPack up) -> {up.setRequestPolicy(RequestPolicy.REACHABLE_COMMIT);},
				"want " + parent.toObjectId().getName() + "\n",
				PacketLineIn.end(),
				"done\n", PacketLineIn.end());

		assertEquals(1, stats.getNotAdvertisedWants());
	}

	@Test
	public void testNotAdvertisedWantsV2FetchThinPack() throws Exception {
		String commonInBlob = "abcdefghijklmnopqrstuvwxyz";

		RevBlob parentBlob = remote.blob(commonInBlob + "a");
		RevCommit parent = remote
				.commit(remote.tree(remote.file("foo", parentBlob)));
		RevBlob childBlob = remote.blob(commonInBlob + "b");
		RevCommit child = remote
				.commit(remote.tree(remote.file("foo", childBlob)), parent);
		remote.update("branch1", child);

		ByteArrayInputStream recvStream = uploadPackV2("command=fetch\n",
				PacketLineIn.delimiter(),
				"want " + child.toObjectId().getName() + "\n",
				"have " + parent.toObjectId().getName() + "\n", "thin-pack\n",
				"done\n", PacketLineIn.end());
		PacketLineIn pckIn = new PacketLineIn(recvStream);

		assertThat(pckIn.readString(), is("packfile"));

		assertEquals(0, stats.getNotAdvertisedWants());
	}

	@Test
	public void testNotAdvertisedWantsV2FetchRequestPolicyReachableCommit() throws Exception {
		String commonInBlob = "abcdefghijklmnopqrstuvwxyz";

		RevBlob parentBlob = remote.blob(commonInBlob + "a");
		RevCommit parent = remote
				.commit(remote.tree(remote.file("foo", parentBlob)));
		RevBlob childBlob = remote.blob(commonInBlob + "b");
		RevCommit child = remote
				.commit(remote.tree(remote.file("foo", childBlob)), parent);

		remote.update("branch1", child);

		uploadPackV2((UploadPack up) -> {up.setRequestPolicy(RequestPolicy.REACHABLE_COMMIT);},
				"command=fetch\n",
				PacketLineIn.delimiter(),
				"want " + parent.toObjectId().getName() + "\n", "thin-pack\n",
				"done\n", PacketLineIn.end());

		assertEquals(1, stats.getNotAdvertisedWants());
	}

	private class RefCallsCountingRepository extends InMemoryRepository {
		private final InMemoryRepository.MemRefDatabase refdb;
		private int numRefCalls;

		public RefCallsCountingRepository(DfsRepositoryDescription repoDesc) {
			super(repoDesc);
			refdb = new InMemoryRepository.MemRefDatabase() {
				@Override
				public List<Ref> getRefs() throws IOException {
					numRefCalls++;
					return super.getRefs();
				}
			};
		}

		public int numRefCalls() {
			return numRefCalls;
		}

		@Override
		public RefDatabase getRefDatabase() {
			return refdb;
		}
	}

	@Test
	public void testObjectInfo() throws Exception {
		server.getConfig().setBoolean("uploadpack", null, "advertiseobjectinfo",
				true);

		RevBlob blob1 = remote.blob("foobar");
		RevBlob blob2 = remote.blob("fooba");
		RevTree tree = remote.tree(remote.file("1", blob1),
				remote.file("2", blob2));
		RevCommit commit = remote.commit(tree);
		remote.update("master", commit);

		TestV2Hook hook = new TestV2Hook();
		ByteArrayInputStream recvStream = uploadPackV2((UploadPack up) -> {
			up.setProtocolV2Hook(hook);
		}, "command=object-info\n", "size",
				"oid " + ObjectId.toString(blob1.getId()),
				"oid " + ObjectId.toString(blob2.getId()), PacketLineIn.end());
		PacketLineIn pckIn = new PacketLineIn(recvStream);

		assertThat(hook.objectInfoRequest, notNullValue());
		assertThat(pckIn.readString(), is("size"));
		assertThat(pckIn.readString(),
				is(ObjectId.toString(blob1.getId()) + " 6"));
		assertThat(pckIn.readString(),
				is(ObjectId.toString(blob2.getId()) + " 5"));
		assertTrue(PacketLineIn.isEnd(pckIn.readString()));
	}

	@Test
	public void testObjectInfo_invalidOid() throws Exception {
		server.getConfig().setBoolean("uploadpack", null, "advertiseobjectinfo",
				true);

		assertThrows(UploadPackInternalServerErrorException.class,
				() -> uploadPackV2("command=object-info\n", "size",
						"oid invalid",
						PacketLineIn.end()));
	}
}
