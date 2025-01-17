package narrator.graph.cluster;

import narrator.graph.Edge;
import narrator.graph.Node;
import org.jgrapht.Graph;
import org.jgrapht.graph.builder.GraphTypeBuilder;

import java.util.HashMap;
import java.util.Set;
import java.util.function.Function;

public class GraphWrapper {
    private Graph<Node, Edge> graph = GraphTypeBuilder
            .<Node, Edge>directed()
            .allowingMultipleEdges(true)
            .allowingSelfLoops(true)
            .edgeClass(Edge.class)
            .weighted(true)
            .buildGraph();
    protected final HashMap<String, Node> nodeMap = new HashMap<>();

    public Graph<Node, Edge> getGraph() {
        return graph;
    }

    public void addNode(Node node) {
        String nodeId = node.getId();

        if (nodeMap.containsKey(nodeId)) {
            return;
        }

        graph.addVertex(node);
        nodeMap.put(nodeId, node);
    }

    public void addEdge(Node source, Node target, Edge edge) {
        addEdge(source, target, edge, null);
    }

    public void addEdge(Node source, Node target, Edge edge, Function<Set<Edge>, Boolean> uniquenessChecker) {
        addNode(source);
        addNode(target);

        if (graph.containsEdge(edge)) {
            return;
        }

        if (uniquenessChecker != null) {
            Set<Edge> currentEdges = graph.getAllEdges(source, target);
            if (!uniquenessChecker.apply(currentEdges)) {
                return;
            }
        }

        graph.addEdge(source, target, edge);
    }

    public void merge(GraphWrapper graphWrapper) {
        Graph<Node, Edge> graph = graphWrapper.getGraph();

        Set<Node> nodes = graph.vertexSet();
        for (Node node : nodes) {
            addNode(node);
        }

        for (Node source : nodes) {
            for (Node target : nodes) {
                if (source.equals(target)) {
                    continue;
                }

                Set<Edge> edges = graph.getAllEdges(source, target);
                for (Edge edge : edges) {
                    addEdge(source, target, edge);
                }
            }
        }
    }

    public boolean containsNode(Node node) {
        return nodeMap.containsKey(node.getId());
    }
}
