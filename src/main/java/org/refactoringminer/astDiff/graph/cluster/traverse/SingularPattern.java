package org.refactoringminer.astDiff.graph.cluster.traverse;

import java.util.List;
import org.refactoringminer.astDiff.graph.Node;
import org.refactoringminer.astDiff.graph.NodeType;

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
    public String base() {
        StringBuilder prompt = new StringBuilder();
        prompt.append("# Subject:\n```\n").append(node.mapping(this.getGraph())).append("\n```\n");
        
        // Add immediate semantic context (surrounding code)
        List<Node> semanticContexts = node.getSemanticContexts(this.getGraph());
        if (!semanticContexts.isEmpty()) {
            prompt.append("\n# Surrounding:\n```\n").append(semanticContexts.get(0)
                .mapping(this.getGraph())).append("\n```\n");
        }
        
        return prompt.toString();
    }
}
