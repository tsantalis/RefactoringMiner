package narrator.graph.cluster.traverse;

import narrator.graph.Node;

import java.util.List;

public class SingularPattern extends TraversalPattern {
    private Node node;

    SingularPattern(Node node) {
        this.node = node;
        addNode(node);
    }

    @Override
    public String textualRepresentation() {
        List<Node> contexts = util.getContexts(node);
        
        return node.getContent()
                + "\nIN\n"
                + String.join("\nIN\n", contexts.stream().map(Node::textualRepresentation).toList());
    }

    @Override
    public Node getLead() {
        return node;
    }
}
