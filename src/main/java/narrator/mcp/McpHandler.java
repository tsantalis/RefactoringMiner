package narrator.mcp;

import com.google.gson.JsonArray;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import narrator.Driver;
import narrator.graph.Context;
import narrator.graph.Edge;
import narrator.graph.EdgeType;
import narrator.graph.Node;
import narrator.graph.NodeType;
import narrator.graph.cluster.Cluster;
import narrator.graph.cluster.Clusterer;
import narrator.graph.cluster.traverse.Leaf;
import narrator.graph.cluster.traverse.Narrator;
import narrator.graph.cluster.traverse.TraversalEngine;
import narrator.graph.cluster.traverse.TraversalPattern;
import narrator.graph.cluster.traverse.SuccessivePattern;
import narrator.graph.cluster.traverse.SingularPattern;
import narrator.graph.cluster.traverse.UsagePattern;
import org.jgrapht.Graph;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class McpHandler {
    private static final Logger logger = LoggerFactory.getLogger(McpHandler.class);
    private static final CacheManager cacheManager = new CacheManager();
    private final Map<String, Integer> clusterProgress = new ConcurrentHashMap<>();
    private final Map<String, Integer> containerLevels = new ConcurrentHashMap<>();

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
        
        tools.add(createToolDefinition("get_available_clusters",
                "Get the available cluster indices (groups of related changes) for a commit or pull request. Returns a range (e.g., 1-3).",
                "url"));
        tools.add(createToolDefinition("begin_cluster_narrative",
                "Begin the narrative for a cluster. Returns the first chapter with a progress indicator [Chapter 1 of Y]. MANDATORY: You must analyze and explain the content of the current chapter in your response, then ask the user if they would like to proceed to the next chapter using a Yes/No prompt. Do not call get_next_cluster_chapter without user confirmation.",
                "url", "clusterIndex"));
        tools.add(createToolDefinition("get_next_cluster_chapter",
                "Get the next chapter in the narrative for the cluster. Each output includes [Chapter X of Y] progress info. MANDATORY: You must analyze and explain the content of the current chapter in your response, then ask the user if they would like to proceed to the next chapter using a Yes/No prompt. Do not call this tool in a loop or batch without user confirmation.",
                "url", "clusterIndex"));
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

        if ("get_available_clusters".equals(toolName)) {
            return fetchClusterCount(url);
        } else if ("begin_cluster_narrative".equals(toolName)) {
            if (!arguments.has("clusterIndex")) {
                throw new IllegalArgumentException("Missing required argument: clusterIndex");
            }
            int clusterIndex;
            try {
                clusterIndex = Integer.parseInt(arguments.get("clusterIndex").getAsString()) - 1;
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid clusterIndex: must be a positive integer");
            }
            String stateKey = url + ":" + clusterIndex;
            clusterProgress.put(stateKey, 0);
            return narrateClusterChapter(url, clusterIndex, 0);
        } else if ("get_next_cluster_chapter".equals(toolName)) {
            if (!arguments.has("clusterIndex")) {
                throw new IllegalArgumentException("Missing required argument: clusterIndex");
            }
            int clusterIndex;
            try {
                clusterIndex = Integer.parseInt(arguments.get("clusterIndex").getAsString()) - 1;
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid clusterIndex: must be a positive integer");
            }
            String stateKey = url + ":" + clusterIndex;
            int nextIndex = clusterProgress.getOrDefault(stateKey, 0);
            clusterProgress.put(stateKey, nextIndex + 1);
            return narrateClusterChapter(url, clusterIndex, nextIndex + 1);
        } else {
            throw new UnsupportedOperationException("Unknown tool: " + toolName);
        }
    }

    private String getHierarchyCacheKey(String url) {
        return "hierarchy:" + url;
    }

    private String getNarrativeCacheKey(String url, int clusterIndex) {
        return "narrative:" + url + ":" + clusterIndex;
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

    private List<List<TraversalPattern>> getOrComputeHierarchy(String url) throws Exception {
        List<Cluster> clusters = getOrComputeClusters(url);
        String cacheKey = getHierarchyCacheKey(url);
        List<List<TraversalPattern>> cached = cacheManager.getHierarchy(cacheKey);
        if (cached != null) {
            return cached;
        }

        List<List<TraversalPattern>> hierarchy = clusters.stream()
                .map(TraversalEngine::new)
                .map(TraversalEngine::getComponents)
                .filter(components -> !components.isEmpty())
                .toList();

        cacheManager.putHierarchy(cacheKey, hierarchy);
        return hierarchy;
    }
    
    private Graph<Node, Edge> loadGraph(String url) throws Exception {
        if (url.contains("/pull/") || url.contains("/pr/")) {
            return Driver.getPullRequestGraph(url);
        } else {
            return Driver.getCommitGraph(url);
        }
    }

    private String fetchClusterCount(String url) throws Exception {
        List<Cluster> clusters = getOrComputeClusters(url);
        int count = clusters.size();
        return count == 0 ? "No clusters found." : "Available clusters: 1-" + count;
    }

    private String narrateClusterChapter(String url, int clusterIndex, int chapterIndex) throws Exception {
        List<List<TraversalPattern>> hierarchy = getOrComputeHierarchy(url);

        if (clusterIndex < 0 || clusterIndex >= hierarchy.size()) {
            throw new IllegalArgumentException(
                    String.format("Cluster index out of range: %d (clusters: 1-%d)", 
                            clusterIndex + 1, hierarchy.size()));
        }

        String cacheKey = getNarrativeCacheKey(url, clusterIndex);
        List<Leaf> leaves = cacheManager.getNarrative(cacheKey);
        if (leaves == null) {
            List<TraversalPattern> components = hierarchy.get(clusterIndex);
            leaves = new Narrator().narrate(components);
            cacheManager.putNarrative(cacheKey, leaves);
        }
        
        if (chapterIndex < 0 || chapterIndex >= leaves.size()) {
            throw new IllegalArgumentException(
                    String.format("Chapter index out of range: %d (chapters: 1-%d)", 
                            chapterIndex + 1, leaves.size()));
        }
        
        int currentChapter = chapterIndex + 1;
        int totalChapters = leaves.size();
        boolean isLastChapter = (currentChapter == totalChapters);
        
        List<Cluster> clusters = getOrComputeClusters(url);
        String chapterContent = leaves.get(chapterIndex).base(clusters.get(clusterIndex));
        
        StringBuilder output = new StringBuilder();
        output.append(chapterContent);
        output.append("\n\n");
        output.append("[Chapter ").append(currentChapter).append(" of ").append(totalChapters).append("]");
        
        if (!isLastChapter) {
            int remaining = totalChapters - currentChapter;
            output.append("\n\nContinue: ").append(remaining).append(" chapter(s) remaining. Please analyze this chapter first, then ask the user if they would like to proceed to the next chapter (Yes/No).");
        } else {
            output.append("\n\n[End of Narrative] All chapters for this cluster have been read. You may now provide your final synthesis and explanation of the cluster.");
        }
        
        return output.toString();
    }
}
