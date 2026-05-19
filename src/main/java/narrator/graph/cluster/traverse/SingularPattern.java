package narrator.graph.cluster.traverse;

import java.util.List;
import java.util.stream.Collectors;
import narrator.graph.Context;
import narrator.graph.Node;
import narrator.graph.NodeType;
import narrator.graph.cluster.Cluster;

public class SingularPattern extends TraversalPattern implements Leaf {

    private final Node node;

    SingularPattern(Node node) {
        nodeType = NodeType.SINGULAR;
        this.node = node;
        addNode(node);

        for (String identifier : node.getIdentifiers()) {
            this.addIdentifier(identifier);
        }
    }

    @Override
    public Node getLead() {
        return node;
    }

    @Override
    public String base(Cluster cluster) {
        List<Node> semanticContexts = Context.get(cluster.getGraph(), node).stream()
                .filter(n -> n.getNodeType().equals(NodeType.SEMANTIC_CONTEXT))
                .toList();
        return node.mapping(cluster) + " (Containers: " + semanticContexts.size() + ")";
    }
}
