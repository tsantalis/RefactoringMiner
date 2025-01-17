package narrator.graph.cluster.traverse;

import narrator.graph.Node;

import java.util.*;

public class UsagePattern extends TraversalPattern {
    Set<Node> useNodes = new HashSet<>();
    private HashMap<Node, UsagePattern> requirements = new HashMap<>();
    private Node ringNode;

    UsagePattern(Node node) {
        addNode(node);
        useNodes.add(node);
    }

    public Node getRingNode() {
        return ringNode;
    }

    public void setRingNode(Node ringNode) {
        this.ringNode = ringNode;
    }

    private Set<Node> getUseNodes() {
        return useNodes;
    }

    public void addRequirement(Node node, UsagePattern requirement) {
        requirements.put(node, requirement);
    }

    public HashMap<Node, UsagePattern> getRequirements() {
        return requirements;
    }

    public void merge(UsagePattern usagePattern) {
        super.merge(usagePattern);
        useNodes.addAll(usagePattern.getUseNodes());
    }

    @Override
    public String textualRepresentation() {
        HashMap<Node, String> nodeResult = new HashMap<>();

        for (Node useNode : useNodes) {
            Set<Node> usedNodes = util.getUsedNodes(useNode);
            HashMap<Node, List<Node>> contextNodes = util.getContextNodes(usedNodes.stream().toList());

            HashMap<Node, String> contextString = new HashMap<>();
            while (!contextNodes.isEmpty()) {
                HashMap<Node, String> iterationContextString = new HashMap<>();

                for (Node context : contextNodes.keySet()) {
                    List<Node> nodes = contextNodes.get(context);
                    List<Node> unprocessedNodes = nodes.stream().filter(contextNodes::containsKey).toList();
                    if (!unprocessedNodes.isEmpty()) {
                        continue;
                    }

                    List<String> nodesString = nodes.stream().map(node -> {
                        if (requirements.containsKey(node)) {
                            return requirements.get(node).textualRepresentation();
                        }

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

            nodeResult.put(useNode, useNode.textualRepresentation()
                    + useContextString
                    + "\n\n---\n\n"
                    + "DECLARATIONS:\n\n"
                    + String.join("\nAND\n", contextString.values()));
        }

        return String.join("\n\n", nodeResult.values());
    }

    @Override
    public Node getLead() {
        List<Node> nodes = useNodes.stream().toList();

        Random random = new Random();
        int randomIndex = random.nextInt(nodes.size());

        return nodes.get(randomIndex);
    }
}
