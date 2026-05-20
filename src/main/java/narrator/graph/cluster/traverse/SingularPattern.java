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
        String basePrompt = node.base(cluster);
        String mappingPrompt = node.mapping(cluster);

        StringBuilder prompt = new StringBuilder();
        prompt.append("# Subject:\n```\n").append(basePrompt).append("\n```\n");
        if (!basePrompt.equals(mappingPrompt)) {
            prompt.append("\nContext:\n```\n").append(mappingPrompt).append("\n```\n");
        }
        return prompt.toString();
    }
}
