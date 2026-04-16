package narrator.graph.cluster.traverse;

import narrator.graph.Node;
import narrator.graph.NodeType;

public class SingularPattern extends TraversalPattern {

    private final Node node;

    SingularPattern(Node node) {
        nodeType = NodeType.SINGULAR;
        this.node = node;
        addNode(node);
    }

    @Override
    public Node getLead() {
        return node;
    }
}
