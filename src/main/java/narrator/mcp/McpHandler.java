package narrator.mcp;

import com.google.gson.JsonArray;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import narrator.Driver;
import narrator.graph.Edge;
import narrator.graph.Node;
import narrator.graph.cluster.Cluster;
import narrator.graph.cluster.Clusterer;
import narrator.graph.cluster.traverse.Leaf;
import narrator.graph.cluster.traverse.Narrator;
import narrator.graph.cluster.traverse.TraversalComponent;
import narrator.graph.cluster.traverse.TraversalEngine;
import narrator.graph.cluster.traverse.TraversalPattern;
import narrator.graph.cluster.traverse.ReasonType;
import narrator.graph.cluster.traverse.GrainLevel;
import narrator.mcp.html.NarrativeHtmlGenerator;
import org.jgrapht.Graph;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class McpHandler {
    private static final Logger logger = LoggerFactory.getLogger(McpHandler.class);
    private static final CacheManager cacheManager = new CacheManager();

    public JsonObject handle(JsonObject request) {
        logger.debug("Handling MCP request: {}", request);
        String method = request.get("method").getAsString();
        JsonObject response = new JsonObject();
        response.addProperty("jsonrpc", "2.0");

        if (request.has("id")) {
            response.add("id", request.get("id"));
        }

        switch (method) {
            case "initialize":
                handleInitialize(response);
                break;
            case "tools/list":
                handleListTools(response);
                break;
            case "tools/call":
                handleCallTool(request, response);
                break;
            default:
                sendMethodNotFound(response);
        }

        return response;
    }

    private void handleInitialize(JsonObject response) {
        JsonObject result = new JsonObject();
        result.addProperty("protocolVersion", "2024-11-05");
        JsonObject capabilities = new JsonObject();
        capabilities.add("tools", new JsonObject());
        result.add("capabilities", capabilities);

        JsonObject serverInfo = new JsonObject();
        serverInfo.addProperty("name", "RefactoringMiner MCP");
        serverInfo.addProperty("version", "1.0.0");
        result.add("serverInfo", serverInfo);

        response.add("result", result);
    }

    private void handleListTools(JsonObject response) {
        JsonObject result = new JsonObject();
        JsonArray tools = new JsonArray();

        tools.add(createToolDefinition("init_narrative",
                "Prepares the narrative for a commit or pull request and returns an overview of the narrative, including the total number of chapters for each grain level. Additionally, it generates a set of HTML pages, demonstrating the narrative and the chapters in each grain level, and returns the path to their entry page. \n\nReport the path to the entry page and the number of chapter for each grain level, and ask user to choose a GrainLevel for the narrative.",
                "url"));
        tools.add(createToolDefinition("get_next_chapter",
                "Retrieves the next chapter in the narrative for the specified grain level.\n\nAnalyze and explain the content of the current chapter, and ask user if they would like to proceed to the next chapter.",
                "url", "grainLevel"));
        result.add("tools", tools);
        response.add("result", result);
    }

    private JsonObject createToolDefinition(String name, String description, String... paramNames) {
        JsonObject tool = new JsonObject();
        tool.addProperty("name", name);
        tool.addProperty("description", description);

        JsonObject inputSchema = new JsonObject();
        inputSchema.addProperty("type", "object");
        JsonObject properties = new JsonObject();
        JsonArray required = new JsonArray();

        for (String paramName : paramNames) {
            JsonObject prop = new JsonObject();
            prop.addProperty("type", "string");
            properties.add(paramName, prop);
            required.add(paramName);
        }

        inputSchema.add("properties", properties);
        inputSchema.add("required", required);

        tool.add("inputSchema", inputSchema);
        return tool;
    }

    private static void sendToolError(JsonObject response, int code, String message) {
        JsonObject error = new JsonObject();
        error.addProperty("code", code);
        error.addProperty("message", message);
        response.add("error", error);
        response.add("result", JsonNull.INSTANCE);
    }

    private static void sendMethodNotFound(JsonObject response) {
        JsonObject error = new JsonObject();
        error.addProperty("code", -32601);
        error.addProperty("message", "Method not found");
        response.add("error", error);
        response.add("result", JsonNull.INSTANCE);
    }

    private void handleCallTool(JsonObject request, JsonObject response) {
        JsonObject params = request.getAsJsonObject("params");
        if (params == null) {
            sendToolError(response, -32602, "Missing params");
            return;
        }

        if (!params.has("name")) {
            sendToolError(response, -32602, "Missing tool name");
            return;
        }
        String toolName = params.get("name").getAsString();

        JsonObject arguments = params.has("arguments") ? params.getAsJsonObject("arguments") : null;
        if (arguments == null) {
            sendToolError(response, -32602, "Missing arguments");
            return;
        }

        try {
            String resultValue = executeTool(toolName, arguments);

            JsonObject result = new JsonObject();
            JsonArray content = new JsonArray();
            JsonObject textContent = new JsonObject();
            textContent.addProperty("type", "text");
            textContent.addProperty("text", resultValue);
            content.add(textContent);
            result.add("content", content);

            response.add("result", result);
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid arguments for tool call: {}", e.getMessage());
            sendToolError(response, -32602, e.getMessage());
        } catch (Exception e) {
            logger.error("Internal error during tool call", e);
            sendToolError(response, -32603, "Internal error: " + e.getMessage());
        }
    }

    private String executeTool(String toolName, JsonObject arguments) throws Exception {
        if (!arguments.has("url")) {
            throw new IllegalArgumentException("Missing required argument: url");
        }
        String url = arguments.get("url").getAsString();

        if ("init_narrative".equals(toolName)) {
            return initNarrative(url);
        } else if ("get_next_chapter".equals(toolName)) {
            if (!arguments.has("grainLevel")) {
                throw new IllegalArgumentException("Missing required argument: grainLevel");
            }
            return getNextChapter(url, arguments.get("grainLevel").getAsString());
        } else {
            throw new UnsupportedOperationException("Unknown tool: " + toolName);
        }
    }

    private Cluster findClusterForNode(Node node, List<Cluster> clusters) {
        if (clusters.isEmpty()) {
            return null;
        }

        for (Cluster cluster : clusters) {
            if (cluster.getGraph().vertexSet().contains(node)) {
                return cluster;
            }
        }

        return null;
    }

    private String getHierarchyCacheKey(String url) {
        return "hierarchy:" + url;
    }

    private String getNextChapter(String url, String grainLevelStr) throws Exception {
        GrainLevel level;
        try {
            level = GrainLevel.valueOf(grainLevelStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid grainLevel: " + grainLevelStr + ". Valid values are: " + java.util.Arrays.toString(GrainLevel.values()));
        }

        Narrator narrator = cacheManager.getNarrative(url + ":root");
        if (narrator == null) {
            return "No narrative initialized for this URL. Please call init_narrative first.";
        }

        int progress = narrator.getProgress(level);
        List<TraversalPattern> chapters = narrator.getNarrative(level);
        if (chapters == null) {
            return "Narrative state lost. Please call init_narrative again.";
        }

        if (progress >= chapters.size()) {
            return "[End of Narrative] All chapters for grain level " + level + " have been read.";
        }

        TraversalPattern chapterPattern = chapters.get(progress);
        narrator.incrementProgress(level);

        List<Cluster> clusters = getOrComputeClusters(url);
        if (clusters.isEmpty()) {
            return "Error: No clusters available to provide context for the chapter.";
        }

        Cluster cluster = findClusterForNode(chapterPattern.getLead(), clusters);

        if (cluster == null) {
            return "Error: Could not find associated cluster for the current chapter.";
        }

        String content = chapterPattern.extended(cluster, level);
        int currentChapter = progress + 1;
        int totalChapters = chapters.size();

        // Update the HTML page for this chapter with the actual content
        NarrativeHtmlGenerator generator = cacheManager.getHtmlGenerator(url);
        if (generator != null) {
            generator.updateChapterContent(level, progress, content);
        }

        StringBuilder output = new StringBuilder();
        output.append("[Chapter ").append(currentChapter).append(" of ").append(totalChapters).append(" - GrainLevel: ").append(level).append("]\n\n");
        output.append("Professional Diff Page: ").append(generator != null ? generator.getChapterPath(level, progress) : "Not available").append("\n\n");
        output.append(content);
        output.append("\n\n");

        if (currentChapter < totalChapters) {
            output.append("Reminder: Explain the changes, and ask to proceed.");
        } else {
            output.append("Reminder: Explain the changes.");
            output.append("\n\n[End of Narrative] All chapters for grain level " + level + " have been read. You may now provide your final synthesis and explanation of the commit.");
        }

        return output.toString();
    }

    private String initNarrative(String url) throws Exception {
        TraversalPattern root = getOrComputeHierarchy(url);
        if (root == null) {
            return "No changes found to narrate.";
        }

        Narrator narrator = new Narrator(root);
        cacheManager.putNarrative(url + ":root", narrator);

        NarrativeHtmlGenerator generator = new NarrativeHtmlGenerator(url, narrator);
        generator.generateAll();
        cacheManager.putHtmlGenerator(url, generator);

        StringBuilder summary = new StringBuilder();
        summary.append("Narrative initialized. A professional HTML report has been generated. \n\n");
        summary.append("Overview page: ").append(generator.getOverviewPath()).append("\n\n");
        summary.append("Available GrainLevels and their chapter counts:\n");

        for (GrainLevel level : GrainLevel.values()) {
            int count = narrator.getNarrative(level).size();
            summary.append("- ").append(level).append(" (").append(level.getDescription()).append("): ").append(count).append(" chapters\n");
        }

        summary.append("\nPlease choose a GrainLevel to start the narration.");

        return summary.toString();
    }


    private List<Cluster> getOrComputeClusters(String url) throws Exception {
        List<Cluster> cached = cacheManager.getClusters(url);
        if (cached != null) {
            return cached;
        }

        Graph<Node, Edge> graph = loadGraph(url);
        List<Cluster> clusters = new Clusterer(graph).getClusters();
        cacheManager.putClusters(url, clusters);
        return clusters;
    }

    private TraversalPattern getOrComputeHierarchy(String url) throws Exception {
        String cacheKey = getHierarchyCacheKey(url);
        TraversalPattern cached = cacheManager.getHierarchy(cacheKey);
        if (cached != null) {
            return cached;
        }

        List<Cluster> clusters = getOrComputeClusters(url);
        List<TraversalPattern> finalHierarchy = new java.util.ArrayList<>();

        for (Cluster cluster : clusters) {
            List<TraversalPattern> patterns = new TraversalEngine(cluster).getComponents();
            if (patterns.size() > 1) {
                finalHierarchy.add(new TraversalComponent(patterns, ReasonType.CONTEXT));
            } else if (patterns.size() == 1) {
                finalHierarchy.add(patterns.get(0));
            }
        }

        TraversalPattern root;
        if (finalHierarchy.size() > 1) {
            root = new TraversalComponent(finalHierarchy, ReasonType.CONTEXT);
        } else if (finalHierarchy.size() == 1) {
            root = finalHierarchy.get(0);
        } else {
            return null;
        }

        cacheManager.putHierarchy(cacheKey, root);
        return root;
    }

    private Graph<Node, Edge> loadGraph(String url) throws Exception {
        if (url.contains("/pull/") || url.contains("/pr/")) {
            return Driver.getPullRequestGraph(url);
        } else {
            return Driver.getCommitGraph(url);
        }
    }

}
