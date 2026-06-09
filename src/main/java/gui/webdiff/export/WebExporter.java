package gui.webdiff.export;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import gui.webdiff.WebDiff;
import org.refactoringminer.astDiff.models.ASTDiff;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

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

    public void export(String exportPath){
        if (!exportPath.endsWith(File.separator))
            exportPath += File.separator;
        exportPath = exportPath + baseFolder + File.separator;
        deletedFiles = new ArrayList<>(webDiff.getComparator().getRemovedFilesName());
        addedFiles = new ArrayList<>(webDiff.getComparator().getAddedFilesName());
        exportViewers(exportPath);
        exportOthersPages(exportPath);
        exportContentPages(exportPath);
        exportResources(exportPath + File.separator, resourcePath + webDiff.getResources());
        exportInfo(exportPath);
    }

    private void exportInfo(String exportPath) {
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
        try {
            ObjectNode jsonNode = objectMapper.createObjectNode();
            jsonNode.put("diffInfos", jsonArray);
            objectMapper.writeValue(new File(exportPath + File.separator + "info.json"), jsonNode);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void exportOthersPages(String exportPath) {
        for (String otherPage : otherPages) {
            String url = String.format("%s:%d/%s", baseURL, webDiff.port, otherPage);
            export(exportPath, otherPage, url);
        }
    }

    private void exportViewers(String exportPath) {
        String url;
        for (int i = 0; i < webDiff.getComparator().getNumOfDiffs(); i++) {
            for (String viewer_path : viewers_path) {
                url = String.format("%s:%d/%s/%d", baseURL, webDiff.port, viewer_path, i);
                export(exportPath, viewer_path + File.separator + i, url);
            }
        }
    }

    // Deleted/added files have no AST diff and so no /monaco-page/<id> page; the
    // live server serves them on demand from /content?side=&path=. Materialize
    // each one as a static page so the export works offline / on GitHub Pages.
    private void exportContentPages(String exportPath) {
        exportContentSide(exportPath, "deleted", deletedFiles);
        exportContentSide(exportPath, "added", addedFiles);
    }

    private void exportContentSide(String exportPath, String side, List<String> files) {
        for (int i = 0; i < files.size(); i++) {
            String encodedPath = URLEncoder.encode(files.get(i), StandardCharsets.UTF_8);
            String url = String.format("%s:%d/content?side=%s&path=%s", baseURL, webDiff.port, side, encodedPath);
            // Folder "content/<side>/<i>" sits three levels below the export root,
            // so export() computes nestingLevel 3 and the right "../../../" prefix.
            export(exportPath, contentFolder + File.separator + side + File.separator + i, url);
        }
    }

    private void export(String exportPath, String folderPath, String url) {
        int nestingLevel = folderPath.length() - folderPath.replace(File.separator, "").length() + 1;
        String content = getPage(url);
        String filePath = exportPath + folderPath;
        Path path = Path.of(filePath);
        try {
            Files.createDirectories(path);
        } catch (IOException e) {
            e.printStackTrace();
        }
        try (FileWriter writer = new FileWriter(filePath + "/index.html")) {
            String modifiedContent = doProperReplacement(content, nestingLevel);
            writer.write(modifiedContent);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    String doProperReplacement(String content, int nestingLevel) {
        String baseAddon = (".." + File.separator).repeat(nestingLevel);
        String baseAddonWithResources = baseAddon + resourceFolderNameInFinalExport + File.separator;
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

    private void exportResources(String destDir, String resourcesPath) {
        Path sourcePath = Paths.get(resourcesPath);
        if (!new File(String.valueOf(sourcePath)).exists()) return;
        Path destPath = Paths.get(destDir + resourceFolderNameInFinalExport);
        try {
            Files.createDirectories(destPath);

            // Copy the resources recursively
            Files.walkFileTree(sourcePath, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Path targetFile = destPath.resolve(sourcePath.relativize(file));
                    // Ensure the parent directories exist for the target file
                    Files.createDirectories(targetFile.getParent());
                    Files.copy(file, targetFile, StandardCopyOption.REPLACE_EXISTING);
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static String getPage(String url) {
        try {
            URL pageUrl = new URL(url);
            HttpURLConnection connection = (HttpURLConnection) pageUrl.openConnection();
            connection.setRequestMethod("GET");
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(connection.getInputStream()));
            int inputChar;
            StringBuilder response = new StringBuilder();
            while ((inputChar = in.read()) != -1) {
                //This must be a char, not a line, as the HTML content will break when the code contains breaking whitespaces/lines
                response.append((char)inputChar);
            }
            in.close();
            return response.toString();
        } catch (IOException e) {
            e.printStackTrace();
            return "<html><body><h1>Failed to load page</h1></body></html>";
        }
    }
}
