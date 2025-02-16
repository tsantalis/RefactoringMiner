package gui.webdiff.export;

import gui.webdiff.WebDiff;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Set;
import java.util.stream.Collectors;

/* Created by pourya on 2025-02-12*/
public class WebExporter {
    final WebDiff webDiff;
    final String baseURL = "http://127.0.0.1";
    final Set<String> resourceFolders;
    final String resourcePath = "src/main/resources/";
    final Set<String> viewers_path = Set.of(
            "monaco-page",
            "vanilla-diff"
    );
    final Set<String> otherPages = Set.of(
            "singleView",
            "list"
    );

    final String baseFolder = "webdiff";


    public WebExporter(WebDiff webDiff) {
        Set<String> folders;
        this.webDiff = webDiff;
        try {
            folders = Files.list(Paths.get(resourcePath))
                    .filter(Files::isDirectory)
                    .map(path -> path.getFileName().toString())
                    .collect(Collectors.toSet());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        resourceFolders = folders;
    }

    public void export(String exportPath){
        if (!exportPath.endsWith(File.separator))
            exportPath += File.separator;
        exportPath = exportPath + baseFolder + File.separator;
        exportViewers(exportPath);
        exportOthersPages(exportPath);
        exportResources(exportPath, resourcePath + webDiff.getResources());
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
                export(exportPath, viewer_path + "/" + i, url);
            }
        }
    }

    private void export(String exportPath, String folderPath, String url) {
        int nestingLevel = folderPath.length() - folderPath.replace("/", "").length() + 1;
        String baseAddon = "../".repeat(nestingLevel);
        String content = getPage(url);
        String filePath = exportPath + folderPath;
        Path path = Path.of(filePath);
        try {
            Files.createDirectories(path);
        } catch (IOException e) {
            e.printStackTrace();
        }
        try (FileWriter writer = new FileWriter(filePath + "/index.html")) {
            for (String resourceFolder : resourceFolders) {
                content = content.replace("=\"/"+resourceFolder+"/", "=\""+baseAddon+resourceFolder+"/");
            }
            writer.write(content);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void exportResources(String destDir, String resourcesPath) {
        Path sourcePath = Paths.get(resourcesPath);
        Path destPath = Paths.get(destDir);
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
