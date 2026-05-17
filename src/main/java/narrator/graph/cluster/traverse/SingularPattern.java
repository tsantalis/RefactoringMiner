package narrator.graph.cluster.traverse;

import narrator.graph.Node;
import narrator.graph.NodeType;

public class SingularPattern implements Leaf {

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

    public String textualRepresentation() {
        return node.getDetailedRepresentation();
    }
}
