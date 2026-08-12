package org.refactoringminer.astDiff.graph.cluster;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.refactoringminer.astDiff.graph.Edge;
import org.refactoringminer.astDiff.graph.Node;
import org.jgrapht.Graph;

public class Clusterer {

    private final Graph<Node, Edge> graph;
    private final HashMap<String, Cluster> clusters;
    private final HashMap<Node, String> nodeToCluster;

    public Clusterer(Graph<Node, Edge> graph) {
        this.graph = graph;
        this.clusters = new HashMap<>();
        this.nodeToCluster = new HashMap<>();

        cluster();
    }

    private void cluster() {
        Set<Node> nodes = graph.vertexSet();
        Map<Node, Node> parent = new HashMap<>();
        for (Node node : nodes) {
            parent.put(node, node);
        }

        for (Edge edge : graph.edgeSet()) {
            union(parent, graph.getEdgeSource(edge), graph.getEdgeTarget(edge));
        }

        Map<Node, String> rootToId = new HashMap<>();
        int[] clusterIndexWrapper = {0};
        for (Node node : nodes) {
            Node root = find(parent, node);
            String id = rootToId.computeIfAbsent(root, r -> {
                String newId = String.valueOf(clusterIndexWrapper[0]++);
                Cluster cluster = new Cluster();
                clusters.put(newId, cluster);
                return newId;
            });
            nodeToCluster.put(node, id);
            clusters.get(id).addNode(node);
        }

        for (Edge edge : graph.edgeSet()) {
            Node source = graph.getEdgeSource(edge);
            Node target = graph.getEdgeTarget(edge);
            String sourceId = nodeToCluster.get(source);
            String targetId = nodeToCluster.get(target);

            if (sourceId != null && sourceId.equals(targetId)) {
                clusters.get(sourceId).addEdge(source, target, edge);
            }
        }
    }

    private Node find(Map<Node, Node> parent, Node i) {
        if (parent.get(i).equals(i)) {
            return i;
        }
        Node root = find(parent, parent.get(i));
        parent.put(i, root);
        return root;
    }

    private void union(Map<Node, Node> parent, Node i, Node j) {
        Node rootI = find(parent, i);
        Node rootJ = find(parent, j);
        if (!rootI.equals(rootJ)) {
            parent.put(rootI, rootJ);
        }
    }

    public List<Cluster> getClusters() {
        return clusters.values().stream().toList();
    }
}
