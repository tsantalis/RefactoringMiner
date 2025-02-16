package gui.webdiff.export;

import gui.webdiff.WebDiff;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/* Created by pourya on 2025-02-12*/
public class WebExporter {
    final WebDiff webDiff;
    final String baseURL = "http://127.0.0.1";
    final String resourcePath = "src/main/resources/";
    final Set<String> viewers_path = Set.of(
            "monaco-page",
            "vanilla-diff"
    );
    final Set<String> otherPages = Set.of(
            "singleView",
            "list"
    );

    final String baseFolder = "web";
    final String resourceFolderNameInFinalExport = "resources";


    public WebExporter(WebDiff webDiff) {
        this.webDiff = webDiff;
    }

    public void export(String exportPath){
        if (!exportPath.endsWith(File.separator))
            exportPath += File.separator;
        exportPath = exportPath + baseFolder + File.separator;
        exportViewers(exportPath);
        exportOthersPages(exportPath);
        exportResources(exportPath + File.separator, resourcePath + webDiff.getResources());
    }

    private void exportOthersPages(String exportPath) {
        for (String otherPage : otherPages) {
            String url = String.format("%s:%d/%s", baseURL, webDiff.port, otherPage);
            export(exportPath, otherPage, url);
        }
    }

    private void exportViewers(String exportPath) {
        String url;
        for (int i = 0; i < webDiff.projectASTDiff.getDiffSet().size(); i++) {
            for (String viewer_path : viewers_path) {
                url = String.format("%s:%d/%s/%d", baseURL, webDiff.port, viewer_path, i);
                export(exportPath, viewer_path + File.separator + i, url);
            }
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

    private String doProperReplacement(String content, int nestingLevel) {
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
            }
            matcher.appendReplacement(result, addon + filePath);
        }
        matcher.appendTail(result);
        content = result.toString();
        return content;
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
