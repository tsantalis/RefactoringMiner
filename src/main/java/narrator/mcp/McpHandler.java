package narrator.mcp;

import com.google.gson.JsonArray;
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
import narrator.graph.cluster.traverse.TraversalEngine;
import narrator.graph.cluster.traverse.TraversalPattern;
import org.jgrapht.Graph;
import java.util.ArrayList;
import java.util.List;

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
        
        tools.add(createToolDefinition("get_available_clusters",
                "Get the available cluster indices (groups of refactoring patterns) for a commit or pull request. Returns a range (e.g., 1-3).",
                "url"));
        tools.add(createToolDefinition("get_available_chapters",
                "Get the available chapter indices (narrative elements) in a specific cluster. Returns a range (e.g., 1-12).",
                "url", "clusterIndex"));
        tools.add(createToolDefinition("narrate_cluster_chapter",
                "Get a specific chapter of narration for a cluster. Returns a single narrative element. Call get_cluster_chapters_count first to know how many chapters exist.",
                "url", "clusterIndex", "chapterIndex"));
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

    private static JsonObject makeErrorResult(JsonObject response, int code, String message) {
        JsonObject error = new JsonObject();
        error.addProperty("code", code);
        error.addProperty("message", message);
        response.add("error", error);
        return response;
    }
    
    private void sendMethodNotFound(JsonObject response) {
        JsonObject error = new JsonObject();
        error.addProperty("code", -32601);
        error.addProperty("message", "Method not found");
        response.add("error", error);
    }

    private void handleCallTool(JsonObject request, JsonObject response) {
        JsonObject params = request.getAsJsonObject("params");
        if (params == null) {
            makeErrorResult(response, -32602, "Missing params");
            return;
        }
        
        String toolName = params.get("name").getAsString();
        JsonObject arguments = params.getAsJsonObject("arguments");
        if (arguments == null) {
            makeErrorResult(response, -32602, "Missing arguments");
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
            makeErrorResult(response, -32602, e.getMessage());
        } catch (Exception e) {
            logger.error("Internal error during tool call", e);
            makeErrorResult(response, -32603, "Internal error: " + e.getMessage());
        }
    }

    private String executeTool(String toolName, JsonObject arguments) throws Exception {
        String url = arguments.get("url").getAsString();
        
        if ("get_available_clusters".equals(toolName)) {
            return fetchClusterCount(url);
        } else if ("get_available_chapters".equals(toolName)) {
            int clusterIndex;
            try {
                clusterIndex = Integer.parseInt(arguments.get("clusterIndex").getAsString()) - 1;
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid clusterIndex: must be a positive integer");
            }
            return fetchClusterChaptersCount(url, clusterIndex);
        } else if ("narrate_cluster_chapter".equals(toolName)) {
            int clusterIndex;
            int chapterIndex;
            try {
                clusterIndex = Integer.parseInt(arguments.get("clusterIndex").getAsString()) - 1;
                chapterIndex = Integer.parseInt(arguments.get("chapterIndex").getAsString()) - 1;
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid index: clusterIndex and chapterIndex must be positive integers");
            }
            return narrateClusterChapter(url, clusterIndex, chapterIndex);
        } else {
            throw new UnsupportedOperationException("Unknown tool: " + toolName);
        }
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
    
    private List<List<TraversalPattern>> getOrComputeHierarchy(List<Cluster> clusters) {
        String cacheKey = clustersToKey(clusters);
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
    
    private static String clustersToKey(List<Cluster> clusters) {
        if (clusters == null || clusters.isEmpty()) {
            return "empty";
        }
        // Use the hashCode of the first cluster and the size of the list as a stable key
        return clusters.get(0).hashCode() + "@" + clusters.size();
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

    private String fetchClusterChaptersCount(String url, int clusterIndex) throws Exception {
        List<Cluster> clusters = getOrComputeClusters(url);
        List<List<TraversalPattern>> hierarchy = getOrComputeHierarchy(clusters);
        
        if (clusterIndex < 0 || clusterIndex >= hierarchy.size()) {
            throw new IllegalArgumentException(
                    String.format("Cluster index out of range: %d (clusters: 1-%d)", 
                            clusterIndex + 1, hierarchy.size()));
        }
        
        String cacheKey = clustersToKey(clusters) + ":" + clusterIndex;
        List<Leaf> leaves = cacheManager.getNarrative(cacheKey);
        if (leaves == null) {
            List<TraversalPattern> components = hierarchy.get(clusterIndex);
            leaves = new Narrator().narrate(components);
            cacheManager.putNarrative(cacheKey, leaves);
        }
        int count = leaves.size();
        return count == 0 ? "No chapters found in this cluster." : "Available chapters: 1-" + count;
    }

    private String narrateClusterChapter(String url, int clusterIndex, int chapterIndex) throws Exception {
        List<Cluster> clusters = getOrComputeClusters(url);
        List<List<TraversalPattern>> hierarchy = getOrComputeHierarchy(clusters);
        
        if (clusterIndex < 0 || clusterIndex >= hierarchy.size()) {
            throw new IllegalArgumentException(
                    String.format("Cluster index out of range: %d (clusters: 1-%d)", 
                            clusterIndex + 1, hierarchy.size()));
        }
        
        String cacheKey = clustersToKey(clusters) + ":" + clusterIndex;
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
        
        return leaves.get(chapterIndex).textualRepresentation(null);
    }

    private String narrateCluster(String url, int clusterIndex) throws Exception {
        List<Cluster> clusters = getOrComputeClusters(url);
        List<List<TraversalPattern>> hierarchy = getOrComputeHierarchy(clusters);
        
        if (clusterIndex < 0 || clusterIndex >= hierarchy.size()) {
            throw new IllegalArgumentException(
                    String.format("Cluster index out of range: %d (clusters: 1-%d)", 
                            clusterIndex + 1, hierarchy.size()));
        }
        
        String cacheKey = clustersToKey(clusters) + ":" + clusterIndex;
        List<Leaf> leaves = cacheManager.getNarrative(cacheKey);
        if (leaves == null) {
            List<TraversalPattern> components = hierarchy.get(clusterIndex);
            leaves = new Narrator().narrate(components);
            cacheManager.putNarrative(cacheKey, leaves);
        }
        
        List<String> narrations = new ArrayList<>();
        for (Leaf leaf : leaves) {
            narrations.add(leaf.textualRepresentation(null));
        }
        
        return String.join("\n\n", narrations);
    }
}
