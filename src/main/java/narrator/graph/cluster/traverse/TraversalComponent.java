package narrator.graph.cluster.traverse;

import narrator.graph.Edge;
import narrator.graph.Node;
import org.jgrapht.Graph;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TraversalComponent extends TraversalPattern {
    private List<TraversalPattern> components = new ArrayList<>();
    private Graph<Node, Edge> reason = getGraph();

    TraversalComponent(List<TraversalPattern> components, Graph<Node, Edge> reason) {
        this.components = components;
        this.reason = reason;
    }

    @Override
    public boolean containsNode(Node node) {
        Node foundNode = reason.vertexSet().stream()
                .filter(reasonNode -> reasonNode.equals(node))
                .findFirst().orElse(null);
        if (foundNode != null) {
            return true;
        }

        for (TraversalPattern component : components) {
            if (component.containsNode(node)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public Node getLead() {
        Set<Node> nodes = reason.vertexSet();
        if (!nodes.isEmpty()) {
            return nodes.iterator().next();
        }

        return components.get(0).getLead();
    }

    @Override
    public Set<Node> vertexSet() {
        Set<Node> result = new HashSet<>(reason.vertexSet());
        for (TraversalPattern component : components) {
            result.addAll(component.vertexSet());
        }
        return result;
    }
}
