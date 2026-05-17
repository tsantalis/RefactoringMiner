package narrator.mcp;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
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
    private static final CacheManager cacheManager = new CacheManager();

    public JsonObject handle(JsonObject request) {
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
                sendMethodNotFound(response, request);
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
        
        tools.add(createToolDefinition("get_cluster_count",
                "Get the number of clusters (groups of refactoring patterns) for a commit or pull request. Call this first to know how many clusters exist, then use narrate_cluster with each cluster index (1-based) to get narration for that cluster.",
                "url"));
        tools.add(createToolDefinition("narrate_cluster",
                "Get narration for a single cluster — returns the ordered narrative of changes (usage, succession, singular patterns) in that cluster. Call get_cluster_count first to discover how many clusters exist, then call this tool once per cluster with the cluster index (1-based).",
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

    private void handleCallTool(JsonObject request, JsonObject response) {
        JsonObject params = request.getAsJsonObject("params");
        String toolName = params.get("name").getAsString();
        JsonObject arguments = params.getAsJsonObject("arguments");
        String url = arguments.get("url").getAsString();
        
        try {
            String resultValue;
            if ("get_cluster_count".equals(toolName)) {
                resultValue = fetchClusterCount(url);
            } else if ("narrate_cluster".equals(toolName)) {
                int clusterIndex = Integer.parseInt(arguments.get("clusterIndex").getAsString()) - 1;
                resultValue = narrateCluster(url, clusterIndex);
            } else {
                throw new IllegalArgumentException("Unknown tool: " + toolName);
            }
            
            JsonObject result = new JsonObject();
            JsonArray content = new JsonArray();
            JsonObject textContent = new JsonObject();
            textContent.addProperty("type", "text");
            textContent.addProperty("text", resultValue);
            content.add(textContent);
            result.add("content", content);
            
            response.add("result", result);
        } catch (Exception e) {
            JsonObject error = new JsonObject();
            error.addProperty("code", -32602);
            error.addProperty("message", "Tool execution failed: " + e.getMessage());
            response.add("error", error);
        }
    }

    private String fetchClusterCount(String url) throws Exception {
        List<Cluster> clusters = getOrComputeClusters(url);
        return String.valueOf(clusters.size());
    }

    private List<Cluster> getOrComputeClusters(String url) throws Exception {
        List<Cluster> cached = cacheManager.getClusters(url);
        if (cached != null) {
            return cached;
        }

        Graph<Node, Edge> graph;
        if (url.contains("/pull/") || url.contains("/pr/")) {
            graph = Driver.getPullRequestGraph(url);
        } else {
            graph = Driver.getCommitGraph(url);
        }
        
        Clusterer clusterer = new Clusterer(graph);
        List<Cluster> clusters = clusterer.getClusters();
        cacheManager.putClusters(url, clusters);
        return clusters;
    }

    private String narrateCluster(String url, int clusterIndex) throws Exception {
        List<Cluster> clusters = getOrComputeClusters(url);
        Graph<Node, Edge> graph;
        if (url.contains("/pull/") || url.contains("/pr/")) {
            graph = Driver.getPullRequestGraph(url);
        } else {
            graph = Driver.getCommitGraph(url);
        }
        
        Clusterer clusterer = new Clusterer(graph);
        List<Cluster> allClusters = clusterer.getClusters();
        List<List<TraversalPattern>> hierarchy =
                allClusters.stream().map(TraversalEngine::new).map(TraversalEngine::getComponents)
                        .filter(components -> !components.isEmpty()).toList();
        
        if (clusterIndex < 0 || clusterIndex >= hierarchy.size()) {
            throw new IllegalArgumentException(
                    String.format("Cluster index out of range: %d (clusters: 1-%d)", 
                            clusterIndex + 1, hierarchy.size()));
        }
        
        List<TraversalPattern> components = hierarchy.get(clusterIndex);
        Narrator narrator = new Narrator();
        List<Leaf> leaves = narrator.narrate(components);
        
        List<String> narrations = new ArrayList<>();
        for (Leaf leaf : leaves) {
            narrations.add(leaf.textualRepresentation(null));
        }
        
        return narrations.toString();
    }

    private void sendMethodNotFound(JsonObject response, JsonObject request) {
        JsonObject error = new JsonObject();
        error.addProperty("code", -32601);
        error.addProperty("message", "Method not found");
        response.add("error", error);
    }
}
