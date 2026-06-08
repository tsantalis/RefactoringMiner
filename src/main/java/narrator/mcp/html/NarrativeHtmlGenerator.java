package narrator.mcp.html;

import narrator.graph.cluster.Cluster;
import narrator.graph.cluster.traverse.GrainLevel;
import narrator.graph.cluster.traverse.Narrator;
import narrator.graph.cluster.traverse.TraversalPattern;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

public class NarrativeHtmlGenerator {
    private final String url;
    private final Narrator narrator;
    private final List<Cluster> clusters;
    private final Path baseDir;

    public NarrativeHtmlGenerator(String url, Narrator narrator, List<Cluster> clusters) throws IOException {
        this.url = url;
        this.narrator = narrator;
        this.clusters = clusters;
        // Create a unique directory in /tmp based on the URL hash
        String urlHash = Integer.toHexString(url.hashCode());
        this.baseDir = Paths.get("/tmp", "narratives", urlHash);
        Files.createDirectories(baseDir);
    }

    public String generateAll() throws IOException {
        // 1. Generate Chapter pages
        for (GrainLevel level : GrainLevel.values()) {
            List<TraversalPattern> chapters = narrator.getNarrative(level);
            if (chapters == null) continue;

            for (int i = 0; i < chapters.size(); i++) {
                TraversalPattern pattern = chapters.get(i);
                Cluster cluster = findClusterForPattern(pattern);
                String content = (cluster != null) ? pattern.extended(cluster, level) : "[No cluster available for this chapter]";
                generateChapterPage(level, i, content);
            }
        }

        // 2. Generate Grain Level pages
        for (GrainLevel level : GrainLevel.values()) {
            generateGrainLevelPage(level);
        }

        // 3. Generate Overview page
        generateOverviewPage();
        return getOverviewPath();
    }

    private Cluster findClusterForPattern(TraversalPattern pattern) {
        if (clusters == null || clusters.isEmpty()) return null;
        for (Cluster cluster : clusters) {
            if (cluster.getGraph().vertexSet().contains(pattern.getLead())) {
                return cluster;
            }
        }
        return null;
    }

    private void generateOverviewPage() throws IOException {
        StringBuilder html = new StringBuilder();
        html.append("<html><head><title>Narrative Overview</title>");
        html.append("<style>body { font-family: sans-serif; margin: 40px; line-height: 1.6; }");
        html.append("h1 { color: #333; } .grain-item { margin-bottom: 10px; } a { color: #0066cc; text-decoration: none; }");
        html.append("a:hover { text-decoration: underline; }</style></head><body>");
        html.append("<h1>Narrative Overview</h1>");
        html.append("<p>Select a grain level to explore the changes:</p>");

        for (GrainLevel level : GrainLevel.values()) {
            int count = narrator.getNarrative(level).size();
            String filename = "grain_" + level.name().toLowerCase() + ".html";
            html.append("<div class='grain-item'>");
            html.append("<a href='").append(filename).append("'><strong>").append(level).append("</strong></a>");
            html.append(" - ").append(level.getDescription()).append(" (").append(count).append(" chapters)</div>");
        }

        html.append("</body></html>");
        writeFile("index.html", html.toString());
    }

    private void generateGrainLevelPage(GrainLevel level) throws IOException {
        List<TraversalPattern> chapters = narrator.getNarrative(level);
        StringBuilder html = new StringBuilder();
        html.append("<html><head><title>").append(level).append(" Overview</title>");
        html.append("<style>body { font-family: sans-serif; margin: 40px; line-height: 1.6; }");
        html.append("h1 { color: #333; } .chapter-item { margin-bottom: 10px; } a { color: #0066cc; text-decoration: none; }");
        html.append("a:hover { text-decoration: underline; }</style></head><body>");
        html.append("<a href='index.html'>&larr; Back to Overview</a>");
        html.append("<h1>Grain Level: ").append(level).append("</h1>");
        html.append("<p>Chapters in this level:</p>");

        for (int i = 0; i < chapters.size(); i++) {
            String filename = "chapter_" + level.name().toLowerCase() + "_" + (i + 1) + ".html";
            html.append("<div class='chapter-item'>");
            html.append("<a href='").append(filename).append("'>Chapter ").append(i + 1).append("</a></div>");
        }

        html.append("</body></html>");
        writeFile("grain_" + level.name().toLowerCase() + ".html", html.toString());
    }

    private void generateChapterPage(GrainLevel level, int index, String content) throws IOException {
        StringBuilder html = new StringBuilder();
        html.append("<html><head><title>Chapter ").append(index + 1).append("</title>");
        html.append("<style>body { font-family: sans-serif; margin: 40px; line-height: 1.6; }");
        html.append(".diff-container { background: #f6f8fa; border: 1px solid #ddd; padding: 15px; border-radius: 6px; font-family: monospace; white-space: pre-wrap; }");
        html.append("h1 { color: #333; }</style></head><body>");
        html.append("<a href='grain_" + level.name().toLowerCase() + ".html'>&larr; Back to Grain Level Overview</a>");
        html.append("<h1>Chapter ").append(index + 1).append(" (").append(level).append(")</h1>");
        html.append("<div class='diff-container'>");
        html.append(content);
        html.append("</div>");
        html.append("</body></html>");

        writeFile("chapter_" + level.name().toLowerCase() + "_" + (index + 1) + ".html", html.toString());
    }

    private void writeFile(String filename, String content) throws IOException {
        Files.write(baseDir.resolve(filename), content.getBytes(StandardCharsets.UTF_8));
    }

    public String getOverviewPath() {
        return baseDir.resolve("index.html").toAbsolutePath().toString();
    }

    public String getChapterPath(GrainLevel level, int index) {
        return baseDir.resolve("chapter_" + level.name().toLowerCase() + "_" + (index + 1) + ".html").toAbsolutePath().toString();
    }

    // We need a way to update the content of a chapter page when the actual diff is ready
    public void updateChapterContent(GrainLevel level, int index, String content) throws IOException {
        String filename = "chapter_" + level.name().toLowerCase() + "_" + (index + 1) + ".html";
        Path path = baseDir.resolve(filename);

        StringBuilder html = new StringBuilder();
        html.append("<html><head><title>Chapter ").append(index + 1).append("</title>");
        html.append("<style>body { font-family: sans-serif; margin: 40px; line-height: 1.6; }");
        html.append(".diff-container { background: #f6f8fa; border: 1px solid #ddd; padding: 15px; border-radius: 6px; font-family: monospace; white-space: pre-wrap; }");
        html.append("h1 { color: #333; }</style></head><body>");
        html.append("<a href='grain_" + level.name().toLowerCase() + ".html'>&larr; Back to Grain Level Overview</a>");
        html.append("<h1>Chapter ").append(index + 1).append(" (").append(level).append(")</h1>");
        html.append("<div class='diff-container'>");
        html.append(content);
        html.append("</div>");
        html.append("</body></html>");

        writeFile(filename, html.toString());
    }
}
