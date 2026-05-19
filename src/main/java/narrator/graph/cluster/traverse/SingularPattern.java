package narrator.graph.cluster.traverse;

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
        return node.base(cluster);
    }
}
