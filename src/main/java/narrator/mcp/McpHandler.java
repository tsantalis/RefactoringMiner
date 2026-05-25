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
import org.jgrapht.Graph;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class McpHandler {
    private static final Logger logger = LoggerFactory.getLogger(McpHandler.class);
    private static final CacheManager cacheManager = new CacheManager();
    private final Map<String, Integer> clusterProgress = new ConcurrentHashMap<>();

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
                "Prepares the narrative for a commit or pull request. Returns an overview of the narrative, including the total number of chapters. This is the required first step before calling get_next_chapter.",
                "url"));
        tools.add(createToolDefinition("get_next_chapter",
                "Retrieves the next chapter in the narrative. \n\nMANDATORY: High-quality narration requires understanding the intent and impact of a change, which is rarely visible in a small snippet. DO NOT rely on superficial inferences or pattern-matching. You MUST call 'get_surrounding_code' to verify the role and intention of the code unless the change is an obvious, triviality (e.g., a typo fix). Whenever you find yourself using words like 'likely' or 'probably', you have failed to call this tool. \n\nAfter obtaining necessary context, analyze and explain the content of the current chapter in your response, and always represent the changes as a diff block (using + and -) together with your explanation. Then, ask the user if they would like to proceed to the next chapter using a Yes/No prompt. Do not call this tool in a loop or batch without user confirmation.",
                "url"));
        tools.add(createToolDefinition("get_surrounding_code",
                "Reveals the semantic and structural context of a code snippet. Use this tool to uncover the method signatures, class hierarchies, and surrounding logic that are essential for a professional analysis. This tool is the only way to transform a superficial guess into a verified technical explanation.",
                "url", "codeId"));
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
            return getNextChapter(url);
        } else if ("get_surrounding_code".equals(toolName)) {
            if (!arguments.has("codeId")) {
                throw new IllegalArgumentException("Missing required argument: codeId");
            }
            return fetchSurroundingCode(url, arguments.get("codeId").getAsString());
        } else {
            throw new UnsupportedOperationException("Unknown tool: " + toolName);
        }
    }

    private Cluster findClusterForLeaf(Leaf leaf, List<Cluster> clusters) {
        if (clusters.isEmpty()) {
            return null;
        }

        Node lead = ((TraversalPattern) leaf).getLead();
        for (Cluster cluster : clusters) {
            if (cluster.getGraph().vertexSet().contains(lead)) {
                return cluster;
            }
        }

        return null;
    }

    private String getHierarchyCacheKey(String url) {
        return "hierarchy:" + url;
    }

    private String getNextChapter(String url) throws Exception {
        Integer progress = clusterProgress.get(url);
        if (progress == null) {
            return "No narrative initialized for this URL. Please call init_narrative first.";
        }
        
        List<Leaf> leaves = cacheManager.getNarrative(url + ":root");
        if (leaves == null) {
            return "Narrative state lost. Please call init_narrative again.";
        }
        
        if (progress >= leaves.size()) {
            return "[End of Narrative] All chapters have been read.";
        }
        
        Leaf leaf = leaves.get(progress);
        clusterProgress.put(url, progress + 1);
        
        // Need a cluster to call leaf.base(cluster).
        // We just use the first cluster from the project as a context for base().
        List<Cluster> clusters = getOrComputeClusters(url);
        if (clusters.isEmpty()) {
            return "Error: No clusters available to provide context for the chapter.";
        }
        
        Cluster cluster = findClusterForLeaf(leaf, clusters);
        String content = leaf.base(cluster);
        int currentChapter = progress + 1;
        int totalChapters = leaves.size();
        
        StringBuilder output = new StringBuilder();
        output.append("[Chapter ").append(currentChapter).append(" of ").append(totalChapters).append("]\n\n");
        output.append(content);
        output.append("\n\n");
        
        if (currentChapter < totalChapters) {
            output.append("Reminder: Evaluate context, explain changes as a diff, and ask to proceed.");
        } else {
            output.append("Reminder: Evaluate context and explain changes as a diff.");
            output.append("\n\n[End of Narrative] All chapters have been read. You may now provide your final synthesis and explanation of the commit.");
        }
        
        return output.toString();
    }

    private String initNarrative(String url) throws Exception {
        TraversalPattern root = getOrComputeHierarchy(url);
        if (root == null) {
            return "No changes found to narrate.";
        }

        List<Leaf> leaves = new Narrator().narrate(root);
        int chapterCount = leaves.size();

        cacheManager.putNarrative(url + ":root", leaves);
        clusterProgress.put(url, 0);

        return "Narrative initialized. Total chapters: " + chapterCount + ". Use get_next_chapter to start.";
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

    private String fetchSurroundingCode(String url, String codeId) throws Exception {
        List<Cluster> clusters = getOrComputeClusters(url);
        
        for (Cluster cluster : clusters) {
            Node targetNode = cluster.getGraph().vertexSet().stream()
                    .filter(node -> node.getPromptId().equals(codeId))
                    .findFirst()
                    .orElse(null);
            
            if (targetNode != null) {
                List<Node> semanticContexts = targetNode.getSemanticContexts(cluster);
                if (!semanticContexts.isEmpty()) {
                    return semanticContexts.get(0).mapping(cluster);
                }
                return "No semantic context found for " + codeId;
            }
        }
        
        return "Could not find code with ID " + codeId + " in any cluster.";
    }
}
