package narrator.graph.cluster.traverse;

import java.util.List;
import java.util.Set;
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
        StringBuilder prompt = new StringBuilder();
        prompt.append("# Subject:\n```\n").append(node.mapping(cluster)).append("\n```\n");
        
        // Add immediate semantic context (surrounding code)
        List<Node> semanticContexts = node.getSemanticContexts(cluster);
        if (!semanticContexts.isEmpty()) {
            prompt.append("\nSurrounding:\n```\n").append(semanticContexts.get(0).mapping(cluster)).append("\n```\n");
        }
        
        return prompt.toString();
    }

    @Override
    public String extended(Cluster cluster) {
        return base(cluster);
    }
}
