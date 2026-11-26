package gui.webdiff.export;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import gui.webdiff.WebDiff;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.refactoringminer.astDiff.models.DiffMetaInfo;
import org.refactoringminer.astDiff.models.ProjectASTDiff;
import org.refactoringminer.rm1.GitHistoryRefactoringMinerImpl;
import spark.Spark;

class WebExporterTest {

	// Nesting depth of a monaco-page/<id>/index.html page (two path segments
	// below the export root), so relativized links get a "../../" prefix.
	private static final int MONACO_PAGE_DEPTH = 2;

	@Test
	void rewritesInterPageLinksToTheIndexHtmlFile() {
		String html = new WebExporter(null).doProperReplacement(
				"<a href=\"/monaco-page/5\">m</a>"
						+ "<a href=\"/vanilla-diff/2\">v</a>"
						+ "<a href=\"/list\">l</a>",
				MONACO_PAGE_DEPTH);

		assertTrue(html.contains("href=\"../../monaco-page/5/index.html\""), html);
		assertTrue(html.contains("href=\"../../vanilla-diff/2/index.html\""), html);
		assertTrue(html.contains("href=\"../../list/index.html\""), html);
	}

	@Test
	void sendsResourceLinksToTheResourcesFolderUntouched() {
		String html = new WebExporter(null).doProperReplacement(
				"<link href=\"/dist/monaco.css\">"
						+ "<script src=\"/monaco/min/vs/loader.js\">",
				MONACO_PAGE_DEPTH);

		assertTrue(html.contains("href=\"../../resources/dist/monaco.css\""), html);
		assertTrue(html.contains("src=\"../../resources/monaco/min/vs/loader.js\""), html);
		// Resources must not get an index.html appended.
		assertFalse(html.contains("monaco.css/index.html"), html);
	}

	@Test
	void rewritesDeletedAndAddedLinksToTheirExportedContentFolders() {
		WebExporter exporter = new WebExporter(null);
		// Order here is the order the folders are numbered, mirroring the
		// comparator's getRemovedFilesName()/getAddedFilesName() ordering.
		exporter.setContentFiles(List.of("A.java", "src/main/B.java"), List.of("C.java"));

		String html = exporter.doProperReplacement(
				// rendersnake escapes the "&" in attribute values to "&amp;",
				// and paths with slashes are URL-encoded (B.java below).
				"<a href=\"/content?side=deleted&amp;path=A.java\">a</a>"
						+ "<a href=\"/content?side=deleted&amp;path=src%2Fmain%2FB.java\">b</a>"
						+ "<a href=\"/content?side=added&amp;path=C.java\">c</a>",
				MONACO_PAGE_DEPTH);

		assertTrue(html.contains("href=\"../../content/deleted/0/index.html\""), html);
		assertTrue(html.contains("href=\"../../content/deleted/1/index.html\""), html);
		assertTrue(html.contains("href=\"../../content/added/0/index.html\""), html);
	}

	@Test
	void normalizesLeftRightSidesToDeletedAdded() {
		WebExporter exporter = new WebExporter(null);
		exporter.setContentFiles(List.of("A.java"), List.of("C.java"));

		String html = exporter.doProperReplacement(
				"<a href=\"/content?side=left&amp;path=A.java\">a</a>"
						+ "<a href=\"/content?side=right&amp;path=C.java\">c</a>",
				MONACO_PAGE_DEPTH);

		assertTrue(html.contains("href=\"../../content/deleted/0/index.html\""), html);
		assertTrue(html.contains("href=\"../../content/added/0/index.html\""), html);
	}

	@Test
	void leavesUnknownContentLinksAndServerOnlyEndpointsAlone() {
		// No content files registered, so a /content link names a file we did
		// not export: leave it as-is rather than pointing at a missing folder.
		String html = new WebExporter(null).doProperReplacement(
				"<a href=\"/content?side=deleted&path=A.java\">d</a>"
						+ "<a href=\"/quit\">q</a>",
				MONACO_PAGE_DEPTH);

		assertTrue(html.contains("content?side=deleted&path=A.java"), html);
		assertFalse(html.contains("A.java/index.html"), html);
		// quit is a server-only endpoint, not an exported page.
		assertFalse(html.contains("quit/index.html"), html);
	}

	@Test
	void exportsStaticPagesToMemoryZipWithoutWritingLocalFiles(@TempDir Path tempDir) throws Exception {
		Path beforeDir = Files.createDirectories(tempDir.resolve("before/src/main/java/example"));
		Path afterDir = Files.createDirectories(tempDir.resolve("after/src/main/java/example"));
		Files.writeString(beforeDir.resolve("Sample.java"), """
				package example;
				class Sample {
					int value() { return 1; }
				}
				""");
		Files.writeString(beforeDir.resolve("Removed.java"), """
				package example;
				class Removed {}
				""");
		Files.writeString(afterDir.resolve("Sample.java"), """
				package example;
				class Sample {
					int value() { return 2; }
				}
				""");
		Files.writeString(afterDir.resolve("Added.java"), """
				package example;
				class Added {}
				""");

		ProjectASTDiff projectASTDiff = new GitHistoryRefactoringMinerImpl()
				.diffAtDirectories(tempDir.resolve("before"), tempDir.resolve("after"));
		projectASTDiff.setMetaInfo(new DiffMetaInfo("Local diff", null));

		int port;
		try (ServerSocket socket = new ServerSocket(0)) {
			port = socket.getLocalPort();
		}

		WebDiff webDiff = new WebDiff(projectASTDiff);
		webDiff.setPort(port);
		webDiff.setStaticExport(true);
		webDiff.setExitJvmOnQuit(false);
		webDiff.setQuitEnabled(false);

		Path diskExportDir = tempDir.resolve("disk-export");
		byte[] zipBytes;
		try {
			webDiff.run();
			WebExporter exporter = new WebExporter(webDiff);
			zipBytes = exporter.exportToZip();
			assertFalse(Files.exists(diskExportDir));
			exporter.export(diskExportDir.toString());
		} finally {
			webDiff.terminate();
			Spark.awaitStop();
		}

		assertNotNull(zipBytes);
		assertTrue(zipBytes.length > 0);

		Map<String, byte[]> zipEntries = readZipEntries(zipBytes);
		for (String entryName : zipEntries.keySet()) {
			assertFalse(entryName.contains("\\"), "ZIP entry must use '/': " + entryName);
		}

		assertTrue(zipEntries.containsKey("web/list/index.html"));
		assertTrue(zipEntries.containsKey("web/singleView/index.html"));
		assertTrue(zipEntries.containsKey("web/monaco-page/0/index.html"));
		assertTrue(zipEntries.containsKey("web/vanilla-diff/0/index.html"));
		assertTrue(zipEntries.containsKey("web/content/deleted/0/index.html"));
		assertTrue(zipEntries.containsKey("web/content/added/0/index.html"));
		assertTrue(zipEntries.containsKey("web/resources/dist/monaco.css"));
		assertTrue(zipEntries.containsKey("web/info.json"));

		String listHtml = new String(zipEntries.get("web/list/index.html"), StandardCharsets.UTF_8);
		assertTrue(listHtml.contains("../monaco-page/0/index.html"), listHtml);
		assertTrue(listHtml.contains("../content/deleted/0/index.html"), listHtml);
		assertTrue(listHtml.contains("../content/added/0/index.html"), listHtml);
		assertFalse(listHtml.contains("..\\"), listHtml);

		String monacoHtml = new String(zipEntries.get("web/monaco-page/0/index.html"), StandardCharsets.UTF_8);
		assertTrue(monacoHtml.contains("../../resources/"), monacoHtml);
		assertFalse(monacoHtml.contains("..\\"), monacoHtml);

		JsonNode infoJson = new ObjectMapper().readTree(zipEntries.get("web/info.json"));
		JsonNode diffInfos = infoJson.get("diffInfos");
		assertNotNull(diffInfos);
		assertEquals(1, diffInfos.size());
		assertEquals(0, diffInfos.get(0).get("id").asInt());
		assertTrue(diffInfos.get(0).get("srcPath").asText().endsWith("Sample.java"));
		assertTrue(diffInfos.get(0).get("dstPath").asText().endsWith("Sample.java"));

		Map<String, byte[]> diskEntries = readDiskEntries(diskExportDir);
		assertEquals(diskEntries.keySet(), zipEntries.keySet());
		for (Map.Entry<String, byte[]> entry : diskEntries.entrySet()) {
			assertArrayEquals(entry.getValue(), zipEntries.get(entry.getKey()),
					"Mismatch for exported entry: " + entry.getKey());
		}
	}

	private static Map<String, byte[]> readZipEntries(byte[] zipBytes) throws IOException {
		Map<String, byte[]> entries = new LinkedHashMap<>();
		try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
			ZipEntry entry;
			while ((entry = zis.getNextEntry()) != null) {
				if (!entry.isDirectory()) {
					entries.put(entry.getName(), zis.readAllBytes());
				}
				zis.closeEntry();
			}
		}
		return entries;
	}

	private static Map<String, byte[]> readDiskEntries(Path exportRoot) throws IOException {
		Map<String, byte[]> entries = new LinkedHashMap<>();
		try (Stream<Path> walk = Files.walk(exportRoot)) {
			for (Path path : walk.filter(Files::isRegularFile).toList()) {
				String relative = exportRoot.relativize(path).toString().replace('\\', '/');
				entries.put(relative, Files.readAllBytes(path));
			}
		}
		return entries;
	}
}
