package narrator.mcp;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import narrator.Driver;
import narrator.graph.Edge;
import narrator.graph.Node;
import narrator.graph.cluster.Cluster;
import narrator.graph.cluster.Clusterer;
import narrator.graph.cluster.traverse.TraversalEngine;
import narrator.graph.cluster.traverse.TraversalPattern;
import narrator.json.Stringifier;
import org.jgrapht.Graph;
import java.util.List;

public class McpHandler {
    private static final Gson gson = new Gson();

    public JsonObject handle(JsonObject request) {
        String method = request.get("method").getAsString();
        JsonObject response = new JsonObject();
        response.addProperty("jsonrpc", "2.0");
        
        if (request.has("id")) {
            response.addProperty("id", request.get("id"));
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
        
        tools.add(createToolDefinition("get_clusters", "Get clusters for a commit or pull request", "url"));
        tools.add(createToolDefinition("get_hierarchy", "Get refactoring hierarchy for a commit or pull request", "url"));
        
        result.add("tools", tools);
        response.add("result", result);
    }

    private JsonObject createToolDefinition(String name, String description, String paramName) {
        JsonObject tool = new JsonObject();
        tool.addProperty("name", name);
        tool.addProperty("description", description);
        
        JsonObject inputSchema = new JsonObject();
        inputSchema.addProperty("type", "object");
        JsonObject properties = new JsonObject();
        JsonObject prop = new JsonObject();
        prop.addProperty("type", "string");
        properties.add(paramName, prop);
        inputSchema.add("properties", properties);
        
        JsonArray required = new JsonArray();
        required.add(paramName);
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
            if ("get_clusters".equals(toolName)) {
                resultValue = fetchClusters(url);
            } else if ("get_hierarchy".equals(toolName)) {
                resultValue = fetchHierarchy(url);
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

    private String fetchClusters(String url) throws Exception {
        Graph<Node, Edge> graph;
        if (url.contains("/pull/") || url.contains("/pr/")) {
            graph = Driver.getPullRequestGraph(url);
        } else {
            graph = Driver.getCommitGraph(url);
        }
        
        Clusterer clusterer = new Clusterer(graph);
        List<Cluster> clusters = clusterer.getClusters();
        JsonArray stringifiedClusters = new JsonArray();
        clusters.forEach(cluster -> stringifiedClusters.add(Stringifier.graph(cluster.getGraph())));
        return stringifiedClusters.toString();
    }

    private String fetchHierarchy(String url) throws Exception {
        Graph<Node, Edge> graph;
        if (url.contains("/pull/") || url.contains("/pr/")) {
            graph = Driver.getPullRequestGraph(url);
        } else {
            graph = Driver.getCommitGraph(url);
        }
        
        Clusterer clusterer = new Clusterer(graph);
        List<Cluster> clusters = clusterer.getClusters();
        List<List<TraversalPattern>> clustersComponents =
                clusters.stream().map(TraversalEngine::new).map(TraversalEngine::getComponents)
                        .filter(components -> !components.isEmpty()).toList();
        
        JsonObject stringifiedCommit = Stringifier.hierarchy(clustersComponents);
        return stringifiedCommit.toString();
    }

    private void sendMethodNotFound(JsonObject response, JsonObject request) {
        JsonObject error = new JsonObject();
        error.addProperty("code", -32601);
        error.addProperty("message", "Method not found");
        response.add("error", error);
    }
}
