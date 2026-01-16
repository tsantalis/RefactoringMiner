package narrator.graph.cluster.traverse;

import java.util.Set;
import java.util.stream.Collectors;
import narrator.graph.Edge;
import narrator.graph.EdgeType;
import narrator.graph.Node;
import org.jgrapht.Graph;

public class Util {

    Graph<Node, Edge> graph;

    Util(Graph<Node, Edge> graph) {
        this.graph = graph;
    }

    public Set<Node> getUsedNodes(Node node) {
        return graph.incomingEdgesOf(node).stream()
                .filter(edge -> edge.getType().equals(EdgeType.DEF_USE))
                .map(edge -> graph.getEdgeSource(edge)).collect(Collectors.toSet());
    }

    public boolean doesUse(Node node) {
        return graph.incomingEdgesOf(node).stream()
                .anyMatch(edge -> edge.getType().equals(EdgeType.DEF_USE));
    }
}
