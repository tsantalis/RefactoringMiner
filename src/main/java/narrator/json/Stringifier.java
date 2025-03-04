package narrator.json;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import narrator.graph.*;
import narrator.graph.cluster.Cluster;
import narrator.graph.cluster.traverse.TraversalComponent;
import narrator.graph.cluster.traverse.TraversalEngine;
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

    private static JsonObject stringifyTraversalComponents(List<TraversalPattern> traversalComponents,
                                                           String aggregatorId) throws IOException {
        JsonArray nodesArray = new JsonArray();
        JsonArray edgesArray = new JsonArray();

        for (TraversalPattern traversalComponent : traversalComponents) {
            stringifyTraversalComponent(traversalComponent, aggregatorId, nodesArray, edgesArray);
        }

        JsonObject graphObj = new JsonObject();
        graphObj.add("nodes", nodesArray);
        graphObj.add("edges", edgesArray);

        return graphObj;
    }

    private static void stringifyTraversalComponent(TraversalPattern traversalComponent, String aggregatorId,
                                                    JsonArray nodes, JsonArray edges) throws IOException {
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

        nodes.add(getNode(id, aggregatorId, "", traversalComponent.textualRepresentation(),
                traversalComponent.description(), NodeType.AGGREGATOR));

        Node lead = traversalComponent.getLead();
        edges.add(getEdge(id, getNodeId(lead.getId(), id), EdgeType.EXPANSION.name(), 1));

        if (!aggregatorId.isEmpty()) {
            edges.add(getEdge(aggregatorId, id, EdgeType.EXPANSION.name(), 1));
        }
    }

    public static String stringifyCommit(String url, List<Cluster> clusters) throws IOException {
        JsonArray nodesArray = new JsonArray();
        JsonArray edgesArray = new JsonArray();

        String commitNodeId = "commit";

        List<String> clustersDescription = new ArrayList<>();
        for (int i = 0; i < clusters.size(); i++) {
            String clusterNodeId = String.format("cluster-%d", i);

            TraversalEngine traversalEngine = new TraversalEngine(clusters.get(i));
            List<TraversalPattern> components = traversalEngine.getComponents();
            JsonObject stringifiedClusterTraversal = stringifyTraversalComponents(components, clusterNodeId);
            nodesArray.addAll(stringifiedClusterTraversal.getAsJsonArray("nodes"));
            edgesArray.addAll(stringifiedClusterTraversal.getAsJsonArray("edges"));

            List<String> componentsDescription = new ArrayList<>();
            for (TraversalPattern component : components) {
                componentsDescription.add(component.description());
            }
            String clusterDescription = components.size() == 1 ? components.get(0).description() :
                    GroqClient.generate(Prompts.getComponentPrompt(componentsDescription));
            nodesArray.add(getNode(clusterNodeId, commitNodeId, "", null, clusterDescription, NodeType.AGGREGATOR));
            edgesArray.add(getEdge(commitNodeId, clusterNodeId, EdgeType.EXPANSION.name(), 1));

            clustersDescription.add(clusterDescription);
        }

        String commitDescription = clustersDescription.size() == 1 ? clustersDescription.get(0) :
                GroqClient.generate(Prompts.getComponentPrompt(clustersDescription));
        nodesArray.add(getNode(commitNodeId, "", "", null, commitDescription, NodeType.AGGREGATOR));

        JsonObject graphObj = new JsonObject();
        graphObj.add("nodes", nodesArray);
        graphObj.add("edges", edgesArray);
        graphObj.addProperty("url", url);

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
            nodesArray.add(getNode(getNodeId(node.getId(), aggregatorId), node.getId(), aggregatorId, node.getPath(),
                    node.getContent(), null, node.getStartLine(), node.getEndLine(), node.getNodeType()));
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
                    edgesArray.add(getEdge(getNodeId(source.getId(), aggregatorId), getNodeId(target.getId(),
                            aggregatorId), edge.getType().name(), edge.getWeight()));
                }
            }
        }

        return edgesArray;
    }

    private static JsonObject getNode(String id, String hunkId, String aggregatorId, String file,
                                      String textualRepresentation, String description, Integer startLine,
                                      Integer endLine, NodeType nodeType) {
        JsonObject nodeObj = new JsonObject();

        nodeObj.addProperty("id", id);
        nodeObj.addProperty("hunkId", hunkId);
        nodeObj.addProperty("file", file);
        nodeObj.addProperty("textualRepresentation", textualRepresentation);
        nodeObj.addProperty("description", description);
        nodeObj.addProperty("startLine", startLine);
        nodeObj.addProperty("endLine", endLine);
        nodeObj.addProperty("nodeType", nodeType.name());
        if (!aggregatorId.isEmpty()) {
            nodeObj.addProperty("aggregatorId", aggregatorId);
        }

        return nodeObj;
    }

    private static JsonObject getNode(String id, String aggregatorId, String file, String textualRepresentation,
                                      String description, NodeType nodeType) {
        return getNode(id, id, aggregatorId, file, textualRepresentation, description, null, null, nodeType);
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
