package gui.webdiff.export;

import gui.webdiff.WebDiff;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Set;

/* Created by pourya on 2025-02-12*/
public class WebExporter {
    final String localhost = "http://127.0.0.1";
    final WebDiff webDiff;
    final Set<String> viewers_path = Set.of(
            "monaco-page",
            "vanilla-diff"
    );


    public WebExporter(WebDiff webDiff) {
        this.webDiff = webDiff;
    }

    public void export(String exportPath){
        String url;
        for (int i = 0; i < webDiff.projectASTDiff.getDiffSet().size(); i++) {
            for (String viewer_path : viewers_path) {
                url = String.format("%s:%d/%s/%d", localhost, webDiff.port, viewer_path, i);
                export(exportPath, viewer_path + "/" + i, url);
            }
        }
        url = String.format("%s:%d/%s", localhost, webDiff.port, "list");
        export(exportPath, "list", url);
        url = String.format("%s:%d/%s", localhost, webDiff.port, "singleView");
        export(exportPath, "singleView", url);
        exportResources(exportPath, "src/main/resources/" + webDiff.getResources());
    }

    private static void export(String exportPath, String folderPath, String url) {
        int nestingLevel = folderPath.length() - folderPath.replace("/", "").length() + 1;
        String baseAddon = "../".repeat(nestingLevel);
        Set<String> resourceFolders = Set.of("dist" , "monaco");
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
//                content = content.replace("=\"/"+resourceFolder+"/", "=\""+baseAddon+resourceFolder+"/");
                content = content.replace("=\"/"+resourceFolder+"/", "=\""+baseAddon+resourceFolder+"/");
//                content = content.replace("href=\"", "href=\""+baseAddon+"");
            }
            writer.write(content);
            System.out.println("Exported: " + filePath);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    void exportResources(String destDir, String resourcesPath) {
        Path sourcePath = Paths.get(resourcesPath);
        Path destPath = Paths.get(destDir);
        try {
            // Ensure the destination directory exists
            Files.createDirectories(destPath);

            // Copy the resources recursively
            Files.walkFileTree(sourcePath, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    // Calculate the relative path to the source directory
                    Path targetFile = destPath.resolve(sourcePath.relativize(file));

                    // Ensure the parent directories exist for the target file
                    Files.createDirectories(targetFile.getParent());

                    // Copy the file to the destination directory
                    Files.copy(file, targetFile, StandardCopyOption.REPLACE_EXISTING);
                    return FileVisitResult.CONTINUE;
                }
            });
            System.out.println("✅ Resources exported to: " + destDir);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static String getPage(String url) {
        try {
            // Open connection to the URL
            URL pageUrl = new URL(url);
            HttpURLConnection connection = (HttpURLConnection) pageUrl.openConnection();
            connection.setRequestMethod("GET");

            // Read the response
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(connection.getInputStream()));
            int inputChar;
            StringBuilder response = new StringBuilder();
            while ((inputChar = in.read()) != -1) {
                response.append((char)inputChar);
            }
            in.close();
            // Return the content
            return response.toString();
        } catch (IOException e) {
            e.printStackTrace();
            return "<html><body><h1>Failed to load page</h1></body></html>";
        }
    }



}
