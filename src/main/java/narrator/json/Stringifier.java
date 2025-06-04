package narrator.json;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import narrator.graph.*;
import narrator.graph.cluster.traverse.*;
import org.jgrapht.Graph;

import java.util.*;

public class Stringifier {
    public static JsonObject graph(Graph<Node, Edge> graph) {
        String aggregatorId = "";

        Set<Node> nodes = graph.vertexSet();

        Map<String, PreprocessedNode> preprocessedNodes = new HashMap<>();
        for (Node node : nodes) {
            if (!node.isActive()) {
                continue;
            }

            String id = node.getSubAggregatorId(aggregatorId);
            preprocessedNodes.put(id, new PreprocessedNode(node.stringify(aggregatorId), new HashSet<>()));
            preprocessedNodes.get(id).aggregatorIds.add(aggregatorId);
        }

        JsonArray edgesArray = new JsonArray();
        for (Node source : nodes) {
            for (Node target : nodes) {
                if (source.equals(target) || !source.isActive() || !target.isActive()) {
                    continue;
                }

                Set<Edge> edges = graph.getAllEdges(source, target);
                for (Edge edge : edges) {
                    edgesArray.add(stringifyEdge(source.getSubAggregatorId(aggregatorId),
                            target.getSubAggregatorId(aggregatorId), edge.getType().name()));
                }
            }
        }

        JsonObject graphObj = new JsonObject();
        graphObj.add("nodes", processNodes(preprocessedNodes));
        graphObj.add("edges", edgesArray);

        return graphObj;
    }

    private static void stringifyComponentGraph(TraversalPattern traversalComponent, String aggregatorId, Map<String,
            PreprocessedNode> preprocessedNodes, JsonArray edges) {
        Map<Node, UsagePattern> replacements = new HashMap<>();
        if (traversalComponent instanceof UsagePattern) {
            replacements.putAll(((UsagePattern) traversalComponent).getRequirements());
        }

        Graph<Node, Edge> graph = traversalComponent.getGraph();

        List<Node> nodes = graph.vertexSet().stream().filter(Node::isActive).toList();
        for (Node source : nodes) {
            for (Node target : nodes) {
                if (source.equals(target) || !source.isActive() || !target.isActive()) {
                    continue;
                }

                String sourceId;
                if (replacements.containsKey(source)) {
                    TraversalPattern sourceComponent = replacements.get(source);
                    sourceId = sourceComponent.getId();
                    stringifyTraversalComponent(sourceComponent, aggregatorId, preprocessedNodes, edges);
                } else {
                    sourceId = source.getSubAggregatorId(aggregatorId);
                    preprocessedNodes.put(sourceId, new PreprocessedNode(source.stringify(aggregatorId),
                            new HashSet<>()));
                    preprocessedNodes.get(sourceId).aggregatorIds.add(aggregatorId);
                }

                String targetId;
                if (replacements.containsKey(target)) {
                    TraversalPattern targetComponent = replacements.get(target);
                    targetId = targetComponent.getId();
                    stringifyTraversalComponent(targetComponent, aggregatorId, preprocessedNodes, edges);
                } else {
                    targetId = target.getSubAggregatorId(aggregatorId);
                    preprocessedNodes.put(targetId, new PreprocessedNode(target.stringify(aggregatorId),
                            new HashSet<>()));
                    preprocessedNodes.get(targetId).aggregatorIds.add(aggregatorId);
                }

                Set<Edge> sourceTargetEdges = graph.getAllEdges(source, target);
                for (Edge edge : sourceTargetEdges) {
                    edges.add(stringifyEdge(sourceId, targetId, edge.getType().name()));
                }
            }
        }
    }

    private static JsonObject stringifyTraversalComponents(List<TraversalPattern> traversalComponents,
                                                           String aggregatorId) {
        Map<String, PreprocessedNode> preprocessedNodes = new HashMap<>();
        JsonArray edges = new JsonArray();

        for (TraversalPattern traversalComponent : traversalComponents) {
            stringifyTraversalComponent(traversalComponent, aggregatorId, preprocessedNodes, edges);
        }

        JsonObject graphObj = new JsonObject();
        graphObj.add("nodes", processNodes(preprocessedNodes));
        graphObj.add("edges", edges);

        return graphObj;
    }

    private static void stringifyTraversalComponent(TraversalPattern traversalComponent, String aggregatorId,
                                                    Map<String, PreprocessedNode> preprocessedNodes, JsonArray edges) {
        if (aggregatorId.isEmpty()) {
            System.out.println("aggregatorId is empty");
        }

        String traversalComponentId = traversalComponent.getId();

        PreprocessedNode preprocessedNode = preprocessedNodes.get(traversalComponentId);
        if (preprocessedNode != null) {
            if (!preprocessedNode.aggregatorIds.contains(aggregatorId)) {
                edges.add(stringifyEdge(aggregatorId, traversalComponentId, EdgeType.EXPANSION.name()));
            }
            preprocessedNode.aggregatorIds.add(aggregatorId);
            return;
        }

        edges.add(stringifyEdge(aggregatorId, traversalComponentId, EdgeType.EXPANSION.name()));
        preprocessedNodes.put(traversalComponentId, new PreprocessedNode(traversalComponent.stringify(),
                new HashSet<>()));
        preprocessedNodes.get(traversalComponentId).aggregatorIds.add(aggregatorId);

        if (traversalComponent instanceof TraversalComponent) {
            for (TraversalPattern component : ((TraversalComponent) traversalComponent).getComponents()) {
                stringifyTraversalComponent(component, traversalComponentId, preprocessedNodes, edges);
            }
        } else {
            Node lead = traversalComponent.getLead();
            edges.add(stringifyEdge(traversalComponentId, lead.getSubAggregatorId(traversalComponentId),
                    EdgeType.EXPANSION.name()));

            stringifyComponentGraph(traversalComponent, traversalComponentId, preprocessedNodes, edges);
        }
    }

    private static JsonArray processNodes(Map<String, PreprocessedNode> preprocessedNodes) {
        JsonArray nodes = new JsonArray();

        preprocessedNodes.values().forEach((preprocessedNode) -> {
            JsonArray aggregatorIdsJson = new JsonArray();
            for (String ai : preprocessedNode.aggregatorIds) {
                aggregatorIdsJson.add(ai);
            }
            preprocessedNode.node.add("aggregatorIds", aggregatorIdsJson);

            nodes.add(preprocessedNode.node);
        });

        return nodes;
    }

    public static JsonObject hierarchy(List<List<TraversalPattern>> clustersComponents) {
        String rootId = "root";

        JsonArray nodesArray = new JsonArray();
        JsonArray edgesArray = new JsonArray();

        for (int i = 1; i < clustersComponents.size() + 1; i++) {
            String clusterId = String.format("cluster-%d", i);

            List<TraversalPattern> clusterComponents = clustersComponents.get(i - 1);
            JsonObject stringifiedCluster = stringifyTraversalComponents(clusterComponents, clusterId);
            nodesArray.addAll(stringifiedCluster.getAsJsonArray("nodes"));
            edgesArray.addAll(stringifiedCluster.getAsJsonArray("edges"));

            JsonObject clusterNode = new JsonObject();
            clusterNode.addProperty("id", clusterId);
            clusterNode.addProperty("nodeType", NodeType.CLUSTER.name());
            JsonArray aggregatorIdsJson = new JsonArray();
            aggregatorIdsJson.add(rootId);
            clusterNode.add("aggregatorIds", aggregatorIdsJson);
            nodesArray.add(clusterNode);
            edgesArray.add(stringifyEdge(rootId, clusterId, EdgeType.EXPANSION.name()));
        }

        JsonObject rootNode = new JsonObject();
        rootNode.addProperty("id", rootId);
        rootNode.addProperty("nodeType", NodeType.COMMIT.name());
        rootNode.add("aggregatorIds", new JsonArray());
        nodesArray.add(rootNode);

        JsonObject graphObj = new JsonObject();
        graphObj.add("nodes", nodesArray);
        graphObj.add("edges", edgesArray);

        return graphObj;
    }

    private static JsonObject stringifyEdge(String sourceId, String targetId, String type) {
        JsonObject edgeObj = new JsonObject();

        edgeObj.addProperty("sourceId", sourceId);
        edgeObj.addProperty("targetId", targetId);
        edgeObj.addProperty("type", type);

        return edgeObj;
    }
}

class PreprocessedNode {
    JsonObject node;
    Set<String> aggregatorIds;

    PreprocessedNode(JsonObject node, Set<String> aggregatorIds) {
        this.node = node;
        this.aggregatorIds = aggregatorIds;
    }
}
