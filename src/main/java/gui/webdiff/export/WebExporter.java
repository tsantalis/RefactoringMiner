package gui.webdiff.export;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import gui.webdiff.WebDiff;
import org.refactoringminer.astDiff.models.ASTDiff;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.JarURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/* Created by pourya on 2025-02-12*/
public class WebExporter {
    final WebDiff webDiff;
    final String baseURL = "http://127.0.0.1";
    final String resourcePath = "src/main/resources/";
    Set<String> viewers_path = Set.of(
            "monaco-page",
            "vanilla-diff"
    );
    Set<String> otherPages = Set.of(
            "singleView",
            "list"
    );
    // Deleted/added files in their list order, used both to generate the
    // content/<side>/<i> pages and to map their /content?side=&path= links to
    // those folders. Injected from the comparator in export(); the empty
    // defaults make doProperReplacement testable without a running server.
    List<String> deletedFiles = List.of();
    List<String> addedFiles = List.of();
    final String contentFolder = "content";

    @FunctionalInterface
    private interface ExportTarget {
        void write(String relativePath, byte[] content) throws IOException;
    }

    public void setViewers_path(Set<String> viewers_path) {
        this.viewers_path = viewers_path;
    }

    public void setOtherPages(Set<String> otherPages) {
        this.otherPages = otherPages;
    }

    public void setContentFiles(List<String> deletedFiles, List<String> addedFiles) {
        this.deletedFiles = deletedFiles;
        this.addedFiles = addedFiles;
    }

    final String baseFolder = "web";
    final String resourceFolderNameInFinalExport = "resources";


    public WebExporter(WebDiff webDiff) {
        this.webDiff = webDiff;
    }

    public void export(String exportPath) {
        Objects.requireNonNull(webDiff, "webDiff must not be null");
        Path rootPath = Paths.get(exportPath, baseFolder);
        deletedFiles = new ArrayList<>(webDiff.getComparator().getRemovedFilesName());
        addedFiles = new ArrayList<>(webDiff.getComparator().getAddedFilesName());
        try {
            exportAll((relativePath, content) -> {
                Path targetFile = rootPath.resolve(relativePath);
                Path parent = targetFile.getParent();
                if (parent != null) {
                    Files.createDirectories(parent);
                }
                Files.write(targetFile, content);
            }, false);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Exports all static AST diff pages, resources, and {@code info.json} directly into an
     * in-memory ZIP archive under {@code web/} without creating any local files.
     *
     * @return the complete ZIP archive bytes
     * @throws IOException if fetching a page, reading web resources, or writing the ZIP fails
     */
    public byte[] exportToZip() throws IOException {
        Objects.requireNonNull(webDiff, "webDiff must not be null");
        deletedFiles = new ArrayList<>(webDiff.getComparator().getRemovedFilesName());
        addedFiles = new ArrayList<>(webDiff.getComparator().getAddedFilesName());
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             ZipOutputStream zos = new ZipOutputStream(baos)) {
            ExportTarget zipTarget = (relativePath, content) -> {
                String entryName = normalizeRelativePath(baseFolder + "/" + relativePath);
                ZipEntry entry = new ZipEntry(entryName);
                zos.putNextEntry(entry);
                zos.write(content);
                zos.closeEntry();
            };
            exportAll(zipTarget, true);
            zos.finish();
            return baos.toByteArray();
        }
    }

    private void exportAll(ExportTarget target, boolean failOnPageFetchError) throws IOException {
        exportViewers(target, failOnPageFetchError);
        exportOthersPages(target, failOnPageFetchError);
        exportContentPages(target, failOnPageFetchError);
        exportResources(target, resourcePath + webDiff.getResources());
        exportInfo(target);
    }

    private void exportInfo(ExportTarget target) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        ArrayNode jsonArray = objectMapper.createArrayNode();
        List<ASTDiff> diffs = webDiff.getComparator().getDiffs();
        int id = 0;
        for (ASTDiff diff : diffs) {
            ObjectNode jsonNode = objectMapper.createObjectNode();
            jsonNode.put("srcPath", diff.getSrcPath());
            jsonNode.put("dstPath", diff.getDstPath());
            jsonNode.put("id", id++);
            jsonArray.add(jsonNode);
        }
        ObjectNode jsonNode = objectMapper.createObjectNode();
        jsonNode.set("diffInfos", jsonArray);
        target.write("info.json", objectMapper.writeValueAsBytes(jsonNode));
    }

    private void exportOthersPages(ExportTarget target, boolean failOnPageFetchError) throws IOException {
        for (String otherPage : otherPages) {
            String url = String.format("%s:%d/%s", baseURL, webDiff.port, otherPage);
            exportPage(target, otherPage, url, failOnPageFetchError);
        }
    }

    private void exportViewers(ExportTarget target, boolean failOnPageFetchError) throws IOException {
        for (int i = 0; i < webDiff.getComparator().getNumOfDiffs(); i++) {
            for (String viewer_path : viewers_path) {
                String url = String.format("%s:%d/%s/%d", baseURL, webDiff.port, viewer_path, i);
                exportPage(target, viewer_path + "/" + i, url, failOnPageFetchError);
            }
        }
    }

    // Deleted/added files have no AST diff and so no /monaco-page/<id> page; the
    // live server serves them on demand from /content?side=&path=. Materialize
    // each one as a static page so the export works offline / on GitHub Pages.
    private void exportContentPages(ExportTarget target, boolean failOnPageFetchError) throws IOException {
        exportContentSide(target, "deleted", deletedFiles, failOnPageFetchError);
        exportContentSide(target, "added", addedFiles, failOnPageFetchError);
    }

    private void exportContentSide(ExportTarget target, String side, List<String> files, boolean failOnPageFetchError)
            throws IOException {
        for (int i = 0; i < files.size(); i++) {
            String encodedPath = URLEncoder.encode(files.get(i), StandardCharsets.UTF_8);
            String url = String.format("%s:%d/content?side=%s&path=%s", baseURL, webDiff.port, side, encodedPath);
            // Folder "content/<side>/<i>" sits three levels below the export root,
            // so exportPage() computes nestingLevel 3 and the right "../../../" prefix.
            exportPage(target, contentFolder + "/" + side + "/" + i, url, failOnPageFetchError);
        }
    }

    private void exportPage(ExportTarget target, String folderPath, String url, boolean failOnPageFetchError)
            throws IOException {
        String normalizedFolder = normalizeRelativePath(folderPath);
        int nestingLevel = normalizedFolder.length() - normalizedFolder.replace("/", "").length() + 1;
        String content = failOnPageFetchError ? fetchPage(url) : getPage(url);
        String modifiedContent = doProperReplacement(content, nestingLevel);
        target.write(normalizedFolder + "/index.html", modifiedContent.getBytes(StandardCharsets.UTF_8));
    }

    String doProperReplacement(String content, int nestingLevel) {
        String baseAddon = "../".repeat(nestingLevel);
        String baseAddonWithResources = baseAddon + resourceFolderNameInFinalExport + "/";
        Pattern pattern = Pattern.compile("(?<=\\b(href|src)=['\"])(/?)(?!https:)([^'\"]+)(?=['\"])");


        Matcher matcher = pattern.matcher(content);
        StringBuffer result = new StringBuffer();

        while (matcher.find()) {
            String filePath = matcher.group(3); // Extract the actual path
            String addon;
            if (filePath.endsWith(".css") || filePath.endsWith(".scss") || filePath.endsWith(".js") || filePath.endsWith(".svg")) {
                addon = baseAddonWithResources;
            } else {
                addon = baseAddon;
                // Deleted/added links are query-string shaped
                // (content?side=&path=); rewrite them to the static folder we
                // generated for that file. Other page links target a directory
                // we wrote as <page>/index.html, so point them straight at the
                // file: navigation then works offline (file://) and on hosts
                // that don't auto-resolve a directory to its index.html.
                String contentTarget = rewriteContentLink(filePath);
                if (contentTarget != null) {
                    filePath = contentTarget;
                } else if (isExportedPageLink(filePath)) {
                    filePath = filePath + "/index.html";
                }
            }
            matcher.appendReplacement(result, Matcher.quoteReplacement(addon + filePath));
        }
        matcher.appendTail(result);
        content = result.toString();
        return content;
    }

    /** True if the link targets a page we export as <page>/index.html. */
    private boolean isExportedPageLink(String filePath) {
        if (otherPages.contains(filePath)) return true;          // "list", "singleView"
        for (String viewer : viewers_path) {                     // "monaco-page/<id>", "vanilla-diff/<id>"
            if (filePath.matches(Pattern.quote(viewer) + "/\\d+")) return true;
        }
        return false;
    }

    /**
     * Map a deleted/added link ("content?side=&path=") to the static folder we
     * exported for that file ("content/<side>/<i>/index.html"), or null if the
     * link isn't a content link or names a file we didn't export.
     */
    String rewriteContentLink(String filePath) {
        int queryStart = filePath.indexOf('?');
        if (queryStart < 0 || !filePath.substring(0, queryStart).endsWith(contentFolder)) return null;

        // rendersnake escapes attribute values, so the raw HTML has "&amp;".
        String query = filePath.substring(queryStart + 1).replace("&amp;", "&");
        String side = queryParam(query, "side");
        String rawPath = queryParam(query, "path");
        if (side == null || rawPath == null) return null;

        // The /content route accepts both deleted/added and left/right.
        String normalizedSide = "left".equals(side) ? "deleted" : "right".equals(side) ? "added" : side;
        String path = URLDecoder.decode(rawPath, StandardCharsets.UTF_8);
        int index;
        if ("deleted".equals(normalizedSide)) {
            index = deletedFiles.indexOf(path);
        } else if ("added".equals(normalizedSide)) {
            index = addedFiles.indexOf(path);
        } else {
            return null;
        }
        if (index < 0) return null;
        return contentFolder + "/" + normalizedSide + "/" + index + "/index.html";
    }

    /** Read a single parameter out of an already-unescaped query string, URL-decoding nothing. */
    private static String queryParam(String query, String key) {
        for (String pair : query.split("&")) {
            int eq = pair.indexOf('=');
            if (eq > 0 && pair.substring(0, eq).equals(key)) {
                return pair.substring(eq + 1);
            }
        }
        return null;
    }

    private void exportResources(ExportTarget target, String resourcesPath) throws IOException {
        Path sourcePath = Paths.get(resourcesPath);
        if (Files.exists(sourcePath)) {
            walkAndWriteResources(sourcePath, target);
            return;
        }
        String classpathRoot = webDiff.getResources();
        if (classpathRoot == null || classpathRoot.isEmpty()) {
            return;
        }
        if (!classpathRoot.startsWith("/")) {
            classpathRoot = "/" + classpathRoot;
        }
        if (classpathRoot.length() > 1 && classpathRoot.endsWith("/")) {
            classpathRoot = classpathRoot.substring(0, classpathRoot.length() - 1);
        }
        URL resourceUrl = WebExporter.class.getResource(classpathRoot);
        if (resourceUrl == null) {
            return;
        }
        URI uri;
        try {
            uri = resourceUrl.toURI();
        } catch (URISyntaxException e) {
            throw new IOException("Invalid web resource URI: " + resourceUrl, e);
        }
        if ("file".equals(uri.getScheme())) {
            walkAndWriteResources(Paths.get(uri), target);
        } else if ("jar".equals(uri.getScheme())) {
            Path jarPath;
            try {
                jarPath = Paths.get(((JarURLConnection) resourceUrl.openConnection()).getJarFileURL().toURI());
            } catch (URISyntaxException e) {
                throw new IOException("Invalid web resource JAR: " + resourceUrl, e);
            }
            // A private (unregistered) zip file system: closing it cannot affect other
            // users of the same JAR, so concurrent exports are safe.
            try (FileSystem fs = FileSystems.newFileSystem(jarPath)) {
                walkAndWriteResources(fs.getPath(classpathRoot), target);
            }
        }
    }

    private void walkAndWriteResources(Path sourcePath, ExportTarget target) throws IOException {
        Files.walkFileTree(sourcePath, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                String relative = normalizeRelativePath(sourcePath.relativize(file).toString());
                target.write(resourceFolderNameInFinalExport + "/" + relative, Files.readAllBytes(file));
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private static String normalizeRelativePath(String path) {
        String normalized = path.replace('\\', '/');
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        return normalized;
    }

    private static String getPage(String url) {
        try {
            return fetchPage(url);
        } catch (IOException e) {
            e.printStackTrace();
            return "<html><body><h1>Failed to load page</h1></body></html>";
        }
    }

    private static String fetchPage(String url) throws IOException {
        URL pageUrl = new URL(url);
        HttpURLConnection connection = (HttpURLConnection) pageUrl.openConnection();
        connection.setRequestMethod("GET");
        try (BufferedReader in = new BufferedReader(
                new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
            int inputChar;
            StringBuilder response = new StringBuilder();
            while ((inputChar = in.read()) != -1) {
                //This must be a char, not a line, as the HTML content will break when the code contains breaking whitespaces/lines
                response.append((char) inputChar);
            }
            return response.toString();
        } finally {
            connection.disconnect();
        }
    }
}
