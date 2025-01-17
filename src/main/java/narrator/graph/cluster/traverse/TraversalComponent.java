package narrator.graph.cluster.traverse;

import narrator.graph.Edge;
import narrator.graph.Node;
import org.jgrapht.Graph;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TraversalComponent extends TraversalPattern {
    private List<TraversalPattern> components;
    private Graph<Node, Edge> reason;

    TraversalComponent(List<TraversalPattern> components, Graph<Node, Edge> reason) {
        this.components = components;
        this.reason = reason;
    }

    @Override
    public boolean containsNode(Node node) {
        Node foundNode = getGraph().vertexSet().stream()
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

    public List<TraversalPattern> getComponents() {
        return components;
    }

    @Override
    public Graph<Node, Edge> getGraph() {
        return reason;
    }

    @Override
    public Node getLead() {
        Set<Node> nodes = getGraph().vertexSet();
        if (!nodes.isEmpty()) {
            return nodes.iterator().next();
        }

        return components.get(0).getLead();
    }

    @Override
    public Set<Node> vertexSet() {
        Set<Node> result = new HashSet<>(getGraph().vertexSet());
        for (TraversalPattern component : components) {
            result.addAll(component.vertexSet());
        }
        return result;
    }
}
