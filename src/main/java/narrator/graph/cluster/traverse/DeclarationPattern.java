package narrator.graph.cluster.traverse;

import narrator.graph.Edge;
import narrator.graph.EdgeType;
import narrator.graph.Node;
import narrator.llm.GroqClient;
import narrator.llm.Prompts;

import java.io.IOException;
import java.util.*;

public class DeclarationPattern extends TraversalPattern {
    Node useNode;
    List<Node> declarationExtensions = new ArrayList<>();

    DeclarationPattern(Node node) {
        addNode(node);
        useNode = node;
    }

    public Node getUseNode() {
        return useNode;
    }

    public void addDeclarationExtension(Node declaration, Node extension) {
        addEdge(extension, declaration, new Edge(EdgeType.DEF_USE, 1));
        declarationExtensions.add(extension);
    }

    public String declarationsRepresentation() {
        Set<Node> declarations = util.getUsedNodes(useNode);
        HashMap<Node, Set<Node>> declarationsContextNodes = util.getContextNodes(declarations.stream().toList());
        return getContextString(declarationsContextNodes);
    }

    public String useRepresentation() {
        List<Node> useContexts = util.getContexts(useNode);
        String useContextString = "";
        if (!useContexts.isEmpty()) {
            useContextString += "\nIN\n";
            useContextString += String.join("\nIN\n", useContexts.stream().map(Node::textualRepresentation).toList());
        }

        return useNode.textualRepresentation()
                + useContextString;
    }

    public String extensionsRepresentation() {
        if (declarationExtensions.isEmpty()) {
            return "";
        }

        HashMap<Node, Set<Node>> extensionContextNodes = util.getContextNodes(declarationExtensions);
        return getContextString(extensionContextNodes);
    }

    @Override
    public String textualRepresentation() {
        String result = declarationsRepresentation()
                + "\n\n---\n\n"
                + "USED IN:\n\n"
                + useRepresentation();

        String extensionRepresentation = extensionsRepresentation();
        if (!extensionRepresentation.isEmpty()) {
            result += "\n\n---\n\n" + "DECLARATIONS:\n\n" + extensionRepresentation;
        }

        return result;
    }

    private String getContextString(HashMap<Node, Set<Node>> contextNodes) {
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

        return String.join("\nAND\n", contextString.values());
    }

    @Override
    public String description() throws IOException {
        String descriptionCache = super.description();
        if (descriptionCache != null) {
            return descriptionCache;
        }

        String generatedDescription = GroqClient.generate(Prompts.getDeclarationPatternPrompt(declarationsRepresentation(),
                useRepresentation(), extensionsRepresentation()));

        setDescriptionCache(generatedDescription);

        return generatedDescription;
    }

    @Override
    public Node getLead() {
        return useNode;
    }
}
