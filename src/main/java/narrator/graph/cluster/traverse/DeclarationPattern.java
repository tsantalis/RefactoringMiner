package narrator.graph.cluster.traverse;

import narrator.graph.Node;
import narrator.llm.GroqClient;
import narrator.llm.Prompts;

import java.io.IOException;
import java.util.*;

public class DeclarationPattern extends TraversalPattern {
    Node useNode;

    DeclarationPattern(Node node) {
        addNode(node);
        useNode = node;
    }

    @Override
    public String textualRepresentation() {
        Set<Node> usedNodes = util.getUsedNodes(useNode);
        HashMap<Node, Set<Node>> contextNodes = util.getContextNodes(usedNodes.stream().toList());

        HashMap<Node, String> contextString = new HashMap<>();
        while (!contextNodes.isEmpty()) {
            HashMap<Node, String> iterationContextString = new HashMap<>();

            for (Node context : contextNodes.keySet()) {
                Set<Node> nodes = contextNodes.get(context);

                List<Node> unprocessedNodes = nodes.stream().filter(contextNodes::containsKey).toList();
                if (!unprocessedNodes.isEmpty()) {
                    continue;
                }

                List<String> nodesString = nodes.stream().map(node -> {
                    if (contextString.containsKey(node)) {
                        // TODO: group sub-context nodes
                        return contextString.remove(node);
                    }

                    return node.textualRepresentation();
                }).toList();
                iterationContextString.put(context, String.join("\nAND\n", nodesString) + "\nIN\n" + context.textualRepresentation());
            }

            for (Node node : iterationContextString.keySet()) {
                contextNodes.remove(node);
                contextString.put(node, iterationContextString.get(node));
            }
        }

        List<Node> useContexts = util.getContexts(useNode);
        String useContextString = "";
        if (!useContexts.isEmpty()) {
            useContextString += "\nIN\n";
            useContextString += String.join("\nIN\n", useContexts.stream().map(Node::textualRepresentation).toList());
        }

        return String.join("\nAND\n", contextString.values())
                + "\n\n---\n\n"
                + "USED IN:\n\n"
                + useNode.textualRepresentation()
                + useContextString;
    }

    @Override
    public String description() throws IOException {
        String descriptionCache = super.description();
        if (descriptionCache != null) {
            return descriptionCache;
        }


        String generatedDescription = GroqClient.generate(Prompts.getDeclarationPatternPrompt(textualRepresentation()));

        setDescriptionCache(generatedDescription);

        return generatedDescription;
    }

    @Override
    public Node getLead() {
        return useNode;
    }
}
