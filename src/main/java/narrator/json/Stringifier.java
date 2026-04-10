package narrator.json;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import narrator.graph.Edge;
import narrator.graph.EdgeType;
import narrator.graph.Node;
import narrator.graph.NodeType;
import narrator.graph.cluster.traverse.AggregatorPattern;
import narrator.graph.cluster.traverse.TraversalComponent;
import narrator.graph.cluster.traverse.TraversalPattern;
import narrator.graph.cluster.traverse.UsagePattern;
import org.jgrapht.Graph;

public class Stringifier {

    public static JsonObject graph(Graph<Node, Edge> graph) {
        Set<Node> nodes = graph.vertexSet();

        Map<String, PreprocessedNode> preprocessedNodes = new HashMap<>();
        for (Node node : nodes) {
            String id = node.getId();
            preprocessedNodes.put(id, new PreprocessedNode(node.stringify(), new HashSet<>()));
        }

        JsonArray edgesArray = new JsonArray();
        for (Node source : nodes) {
            for (Node target : nodes) {
                if (source.equals(target)) {
                    continue;
                }

                Set<Edge> edges = graph.getAllEdges(source, target);
                for (Edge edge : edges) {
                    edgesArray.add(
                            stringifyEdge(source.getId(), target.getId(), edge.getType().name()));
                }
            }
        }

        JsonObject graphObj = new JsonObject();
        graphObj.add("nodes", processNodes(preprocessedNodes));
        graphObj.add("edges", edgesArray);

        return graphObj;
    }

    private static void stringifyComponentGraph(TraversalPattern traversalComponent,
            String aggregatorId, Map<String,
                    PreprocessedNode> preprocessedNodes, JsonArray edges) {
        Map<Node, AggregatorPattern> requirements = new HashMap<>();
        if (traversalComponent instanceof UsagePattern) {
            requirements.putAll(((UsagePattern) traversalComponent).getRequirements());
        }

        Graph<Node, Edge> graph = traversalComponent.getGraph();
        Set<Node> nodes = graph.vertexSet();

        for (Node node : nodes) {
            if (requirements.containsKey(node)) {
                stringifyTraversalComponent(requirements.get(node), aggregatorId, preprocessedNodes,
                        edges);
                continue;
            }

            String nodeId = node.getId();
            if (!preprocessedNodes.containsKey(nodeId)) {
                preprocessedNodes.put(nodeId,
                        new PreprocessedNode(node.stringify(), new HashSet<>()));
            }
            preprocessedNodes.get(nodeId).aggregatorIds.add(aggregatorId);
        }

        for (Node source : nodes) {
            for (Node target : nodes) {
                if (source.equals(target)) {
                    continue;
                }

                Set<Edge> sourceTargetEdges = graph.getAllEdges(source, target);
                for (Edge edge : sourceTargetEdges) {
                    String sourceId = source.getId();
                    String targetId = target.getId();
                    if (edge.getType().equals(EdgeType.DEF_USE)) {
                        if (requirements.containsKey(source)) {
                            sourceId = requirements.get(source).getId();
                        }
                        if (requirements.containsKey(target)) {
                            targetId = requirements.get(target).getId();
                        }
                    }

                    if (isDuplicateEdge(edges, sourceId, targetId, edge.getType().name())) {
                        continue;
                    }

                    edges.add(stringifyEdge(sourceId, targetId, edge.getType().name()));
                }
            }
        }
    }

    private static JsonObject stringifyTraversalComponents(
            List<TraversalPattern> traversalComponents,
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

    private static void stringifyTraversalComponent(TraversalPattern traversalComponent,
            String aggregatorId,
            Map<String, PreprocessedNode> preprocessedNodes, JsonArray edges) {
        String traversalComponentId = traversalComponent.getId();

        PreprocessedNode preprocessedNode = preprocessedNodes.get(traversalComponentId);
        if (preprocessedNode != null) {
            if (!preprocessedNode.aggregatorIds.contains(aggregatorId)) {
                edges.add(stringifyEdge(aggregatorId, traversalComponentId,
                        EdgeType.EXPANSION.name()));
            }
            preprocessedNode.aggregatorIds.add(aggregatorId);
            return;
        }

        edges.add(stringifyEdge(aggregatorId, traversalComponentId, EdgeType.EXPANSION.name()));
        preprocessedNodes.put(traversalComponentId,
                new PreprocessedNode(traversalComponent.stringify(),
                        new HashSet<>()));
        preprocessedNodes.get(traversalComponentId).aggregatorIds.add(aggregatorId);

        if (traversalComponent instanceof TraversalComponent) {
            for (TraversalPattern component : ((TraversalComponent) traversalComponent).getComponents()) {
                stringifyTraversalComponent(component, traversalComponentId, preprocessedNodes,
                        edges);
            }
        } else {
            Node lead = traversalComponent.getLead();
            edges.add(stringifyEdge(traversalComponentId, lead.getId(), EdgeType.EXPANSION.name()));

            stringifyComponentGraph(traversalComponent, traversalComponentId, preprocessedNodes,
                    edges);
        }
    }

    private static JsonArray processNodes(Map<String, PreprocessedNode> preprocessedNodes) {
        JsonArray nodes = new JsonArray();

        preprocessedNodes.values().forEach((preprocessedNode) -> {
            JsonArray aggregatorIdsJson = new JsonArray();
            for (String aggregatorId : preprocessedNode.aggregatorIds) {
                aggregatorIdsJson.add(aggregatorId);
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
            JsonObject stringifiedCluster = stringifyTraversalComponents(clusterComponents,
                    clusterId);
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
        rootNode.addProperty("nodeType", NodeType.ROOT.name());
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

    private static boolean isDuplicateEdge(JsonArray edges, String sourceId, String targetId,
            String type) {
        return edges.asList().stream().anyMatch(edge -> {
            JsonObject edgeObj = edge.getAsJsonObject();
            return edgeObj.get("sourceId").getAsString().equals(sourceId) && edgeObj.get("targetId")
                    .getAsString().equals(targetId) && edgeObj.get("type").getAsString()
                    .equals(type);
        });
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
