package narrator.json;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import narrator.graph.*;
import narrator.graph.cluster.traverse.TraversalComponent;
import narrator.graph.cluster.traverse.TraversalPattern;
import narrator.llm.GroqClient;
import narrator.llm.Prompts;
import org.jgrapht.Graph;

import java.io.IOException;
import java.util.*;

public class Stringifier {
    public static String stringifyGraph(Graph<Node, Edge> graph) {
        JsonObject graphObj = new JsonObject();
        graphObj.add("nodes", getNodesArray(graph, ""));
        graphObj.add("edges", getEdgesArray(graph, ""));

        return graphObj.toString();
    }

    public static String stringifyTraversalComponents(List<TraversalPattern> traversalComponents) throws IOException {
        JsonArray nodesArray = new JsonArray();
        JsonArray edgesArray = new JsonArray();

        for (TraversalPattern traversalComponent : traversalComponents) {
            stringifyTraversalComponent(traversalComponent, "", nodesArray, edgesArray);
        }

        JsonObject graphObj = new JsonObject();
        graphObj.add("nodes", nodesArray);
        graphObj.add("edges", edgesArray);

        return graphObj.toString();
    }

    private static void stringifyTraversalComponent(TraversalPattern traversalComponent, String aggregatorId, JsonArray nodes, JsonArray edges) throws IOException {
        String id = traversalComponent.getId();

        if (traversalComponent instanceof TraversalComponent) {
            for (TraversalPattern component : ((TraversalComponent) traversalComponent).getComponents()) {
                stringifyTraversalComponent(component, id, nodes, edges);
            }
        } else {
            Graph<Node, Edge> graph = traversalComponent.getGraph();
            nodes.addAll(getNodesArray(graph, id));
            edges.addAll(getEdgesArray(graph, id));
        }

        String textualRepresentation = traversalComponent.textualRepresentation();
        String description = traversalComponent.description();
        List<String> contents = new ArrayList<>();
        if (textualRepresentation != null) {
            contents.add(textualRepresentation);
        }
        if (description != null) {
            contents.add(description);
        }

        nodes.add(getNode(id, aggregatorId, String.join("\n\n ----- \n\n", contents), NodeType.AGGREGATOR));

        Node lead = traversalComponent.getLead();
        edges.add(getEdge(id, getNodeId(lead.getId(), id), EdgeType.EXPANSION.name(), 1));

        if (!aggregatorId.isEmpty()) {
            edges.add(getEdge(aggregatorId, id, EdgeType.EXPANSION.name(), 1));
        }
    }

    public static String stringifyCommit(List<String> clustersDescription) throws IOException {
        JsonArray nodesArray = new JsonArray();
        JsonArray edgesArray = new JsonArray();

        String rootId = "commit";


        String commitDescription = GroqClient.generate(Prompts.getComponentPatternPrompt(clustersDescription));
        System.out.println(commitDescription);
        nodesArray.add(getNode(rootId, "", commitDescription, NodeType.AGGREGATOR));

        int clusterIndex = 0;
        for (String clusterDescription : clustersDescription) {
            String nodeId = String.format("cluster-%d", clusterIndex++);
            nodesArray.add(getNode(nodeId, rootId, clusterDescription, NodeType.BASE));
            edgesArray.add(getEdge(rootId, nodeId, EdgeType.EXPANSION.name(), 1));
        }

        JsonObject graphObj = new JsonObject();
        graphObj.add("nodes", nodesArray);
        graphObj.add("edges", edgesArray);

        return graphObj.toString();
    }

    private static JsonArray getNodesArray(Graph<Node, Edge> graph, String aggregatorId) {
        Set<Node> nodes = graph.vertexSet();

        JsonArray nodesArray = new JsonArray();
        for (Node node : nodes) {
            if (!node.isActive()) {
                continue;
            }

            // nodes can be repeated between components
            nodesArray.add(getNode(getNodeId(node.getId(), aggregatorId), aggregatorId, node.getContent(), node.getNodeType()));
        }

        return nodesArray;
    }

    private static JsonArray getEdgesArray(Graph<Node, Edge> graph, String aggregatorId) {
        Set<Node> nodes = graph.vertexSet();

        JsonArray edgesArray = new JsonArray();
        for (Node source : nodes) {
            for (Node target : nodes) {
                if (source.equals(target) || !source.isActive() || !target.isActive()) {
                    continue;
                }

                Set<Edge> edges = graph.getAllEdges(source, target);
                for (Edge edge : edges) {
                    edgesArray.add(getEdge(getNodeId(source.getId(), aggregatorId), getNodeId(target.getId(), aggregatorId), edge.getType().name(), edge.getWeight()));
                }
            }
        }

        return edgesArray;
    }

    private static JsonObject getNode(String id, String aggregatorId, String content, NodeType nodeType) {
        JsonObject nodeObj = new JsonObject();

        nodeObj.addProperty("id", id);
        nodeObj.addProperty("content", content);
        nodeObj.addProperty("nodeType", nodeType.name());
        if (!aggregatorId.isEmpty()) {
            nodeObj.addProperty("aggregatorId", aggregatorId);
        }

        return nodeObj;
    }

    private static JsonObject getEdge(String sourceId, String targetId, String type, float weight) {
        JsonObject edgeObj = new JsonObject();

        edgeObj.addProperty("sourceId", sourceId);
        edgeObj.addProperty("targetId", targetId);
        edgeObj.addProperty("type", type);
        edgeObj.addProperty("weight", weight);

        return edgeObj;
    }

    private static String getNodeId(String id, String aggregatorId) {
        if (!aggregatorId.isEmpty()) {
            id += "-" + aggregatorId;
        }
        return id;
    }

    public static String stringifyContextGraph() {
        JsonArray nodesArray = new JsonArray();
        JsonArray edgesArray = new JsonArray();

        List<String> visitedNodes = new ArrayList<>();
        for (Map.Entry<String, List<String>> typeToContextType : Context.typeToContextType.entrySet()) {
            List<String> nodes = new ArrayList<>() {{
                add(typeToContextType.getKey());
                addAll(typeToContextType.getValue());
            }};
            for (String node : nodes) {
                if (visitedNodes.contains(node)) {
                    continue;
                }

                JsonObject nodeObj = new JsonObject();
                nodeObj.addProperty("id", node);

                nodesArray.add(nodeObj);

                visitedNodes.add(node);
            }

            String type = typeToContextType.getKey();
            for (String contextType : typeToContextType.getValue()) {
                JsonObject edgeObj = new JsonObject();
                edgeObj.addProperty("sourceId", type);
                edgeObj.addProperty("targetId", contextType);

                edgesArray.add(edgeObj);
            }
        }

        JsonObject graphObj = new JsonObject();
        graphObj.add("nodes", nodesArray);
        graphObj.add("edges", edgesArray);

        return graphObj.toString();
    }
}
