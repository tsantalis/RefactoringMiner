package narrator.graph.cluster;

import com.google.gson.JsonObject;
import narrator.graph.Edge;
import narrator.graph.Node;
import org.jgrapht.Graph;

import java.util.*;
import java.util.stream.Stream;

public class Clusterer {
    private Graph<Node, Edge> graph;
    private HashMap<String, Cluster> clusters;
    private HashMap<Node, String> nodeToCluster;

    public Clusterer(Graph<Node, Edge> graph) {
        this.graph = graph;
        this.clusters = new HashMap<>();
        this.nodeToCluster = new HashMap<>();

        cluster();
    }

    private void cluster() {
        Set<Node> nodes = graph.vertexSet();

        int clusterIndex = 0;
        for (Node source : nodes) {
            if (!source.isActive()) {
                continue;
            }

            for (Node target : nodes) {
                if (source.equals(target) || !target.isActive()) {
                    continue;
                }

                Set<Edge> edges = graph.getAllEdges(source, target);
                if (edges.isEmpty()) {
                    continue;
                }

                Cluster cluster = new Cluster();
                for (Edge edge : edges) {
                    cluster.addEdge(source, target, edge);
                }

                String sourceClusterId = nodeToCluster.get(source);
                String targetClusterId = nodeToCluster.get(target);

                if (sourceClusterId != null) {
                    cluster.merge(clusters.get(sourceClusterId));
                }
                if (targetClusterId != null) {
                    cluster.merge(clusters.get(targetClusterId));
                }

                String clusterIndexStr = String.valueOf(clusterIndex);

                clusters.put(clusterIndexStr, cluster);

                nodeToCluster.put(source, clusterIndexStr);
                nodeToCluster.put(target, clusterIndexStr);
                overrideClustersNodes(sourceClusterId, clusterIndexStr);
                overrideClustersNodes(targetClusterId, clusterIndexStr);

                clusters.remove(sourceClusterId);
                clusters.remove(targetClusterId);

                clusterIndex++;
            }
        }

        for (Node node : graph.vertexSet()) {
            if (node.isActive() && !nodeToCluster.containsKey(node)) {
                clusters.put(String.valueOf(clusterIndex), new Cluster(node));
                nodeToCluster.put(node, String.valueOf(clusterIndex));
                clusterIndex++;
            }
        }
    }

    private void overrideClustersNodes(String source, String target) {
        if (source == null) {
            return;
        }

        List<Node> sourceClusterNodes = nodeToCluster.entrySet().stream().filter(entry -> entry.getValue().equals(source)).map(Map.Entry::getKey).toList();
        for (Node node : sourceClusterNodes) {
            nodeToCluster.put(node, target);
        }
    }

    public List<Cluster> getClusters() {
        return clusters.values().stream().toList();
    }
}
