package narrator.json;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import narrator.graph.Context;
import narrator.graph.Edge;
import narrator.graph.EdgeType;
import narrator.graph.Node;
import narrator.graph.cluster.traverse.TraversalPattern;
import org.jgrapht.Graph;

import java.util.*;

public class Stringifier {
    public static String stringifyGraph(Graph<Node, Edge> graph) {
        JsonObject graphObj = new JsonObject();
        graphObj.add("nodes", getNodesArray(graph, ""));
        graphObj.add("edges", getEdgesArray(graph, ""));

        return graphObj.toString();
    }

    public static String stringifyTraversalGraph(List<TraversalPattern> traversalComponents) {
        JsonArray nodesArray = new JsonArray();
        JsonArray edgesArray = new JsonArray();

        for (int i = 0; i < traversalComponents.size(); i++) {
            TraversalPattern traversalPattern = traversalComponents.get(i);
            Graph<Node, Edge> graph = traversalPattern.getGraph();
            String aggregatorId = String.valueOf(i);

            JsonArray nodesBatch = getNodesArray(graph, aggregatorId);

            nodesArray.addAll(nodesBatch);
            edgesArray.addAll(getEdgesArray(graph, aggregatorId));

            JsonObject nodeObj = new JsonObject();
            nodeObj.addProperty("id", aggregatorId);
            nodeObj.addProperty("content", traversalPattern.textualRepresentation());
            nodeObj.addProperty("isContext", false);
            nodeObj.addProperty("isAggregator", true);
            nodesArray.add(nodeObj);

            Node lead = traversalPattern.getLead();
            JsonObject edgeObj = new JsonObject();
            edgeObj.addProperty("sourceId", aggregatorId);
            edgeObj.addProperty("targetId", getNodeId(lead, aggregatorId));
            edgeObj.addProperty("type", EdgeType.EXPANSION.name());
            edgeObj.addProperty("weight", 1);
            edgesArray.add(edgeObj);
        }

//        for (int i = 0; i < traversalPatterns.size(); i++) {
//            for (int j = 0; j < traversalPatterns.size(); j++) {
//                TraversalPattern source = traversalPatterns.get(i);
//                TraversalPattern target = traversalPatterns.get(j);
//                if (source.equals(target)) {
//                    continue;
//                }
//
//                Set<TraversalEdge> edges = traversalGraph.getAllEdges(source, target);
//                for (TraversalEdge edge : edges) {
//                    String sourceId = edge.getSourceNode().getId() + "-" + i;
//                    String targetId = edge.getTargetNode().getId() + "-" + j;
//
//                    JsonObject edgeObj = new JsonObject();
//                    edgeObj.addProperty("sourceId", sourceId);
//                    edgeObj.addProperty("targetId", targetId);
//                    edgeObj.addProperty("type", edge.getType().name());
//                    edgeObj.addProperty("weight", 1);
//
//                    edgesArray.add(edgeObj);
//                }
//            }
//        }

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

            JsonObject nodeObj = new JsonObject();
            String id = getNodeId(node, aggregatorId);
            nodeObj.addProperty("id", id);
            nodeObj.addProperty("content", node.getContent());
            nodeObj.addProperty("isContext", node.isContext());
            if (!aggregatorId.isEmpty()) {
                nodeObj.addProperty("aggregatorId", aggregatorId);
            }
            nodesArray.add(nodeObj);
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

                String sourceId = getNodeId(source, aggregatorId);
                String targetId = getNodeId(target, aggregatorId);

                Set<Edge> edges = graph.getAllEdges(source, target);
                for (Edge edge : edges) {
                    JsonObject edgeObj = new JsonObject();
                    edgeObj.addProperty("sourceId", sourceId);
                    edgeObj.addProperty("targetId", targetId);
                    edgeObj.addProperty("type", edge.getType().name());
                    edgeObj.addProperty("weight", edge.getWeight());

                    edgesArray.add(edgeObj);
                }
            }
        }

        return edgesArray;
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

    private static String getNodeId(Node node, String aggregatorId) {
        String id = node.getId();
        if (!aggregatorId.isEmpty()) {
            id += "-" + aggregatorId;
        }
        return id;
    }
}
