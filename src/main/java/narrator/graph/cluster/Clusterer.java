package narrator.graph.cluster;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import narrator.graph.Edge;
import narrator.graph.Node;
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

        int clusterIndex = 0;
        for (Node source : nodes) {
            for (Node target : nodes) {
                if (source.equals(target)) {
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
            if (!nodeToCluster.containsKey(node)) {
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

        List<Node> sourceClusterNodes = nodeToCluster.entrySet().stream()
                .filter(entry -> entry.getValue().equals(source)).map(Map.Entry::getKey).toList();
        for (Node node : sourceClusterNodes) {
            nodeToCluster.put(node, target);
        }
    }

    public List<Cluster> getClusters() {
        return clusters.values().stream().toList();
    }
}
