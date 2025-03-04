package narrator.graph.cluster.traverse;

import com.github.gumtreediff.utils.Pair;
import narrator.graph.Node;
import narrator.llm.GroqClient;
import narrator.llm.Prompts;

import java.io.IOException;
import java.util.*;

// TODO: add extensions
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
            Pair<String, String> useDecStrings = useDecRepresentation(useNode);
            String useString = useDecStrings.first;
            String decString = useDecStrings.second;

            nodeResult.put(useNode, useString + "\n\n---\n\n" + "DECLARATIONS:\n\n" + decString);
        }

        return String.join("\n\n", nodeResult.values());
    }

    private Pair<String, String> useDecRepresentation(Node useNode) {
        Set<Node> usedNodes = util.getUsedNodes(useNode);
        HashMap<Node, Set<Node>> contextUsedNodes = util.getContextNodes(usedNodes.stream().toList());

        return new Pair<>(useRepresentation(useNode), getContextString(contextUsedNodes));
    }

    public List<Pair<String, String>> useDecRepresentations() {
        List<Pair<String, String>> result = new ArrayList<>();

        for (Node useNode : useNodes) {
            result.add(useDecRepresentation(useNode));
        }

        return result;
    }

    public String useRepresentation(Node useNode) {
        List<Node> useContexts = util.getContexts(useNode);
        String useContextString = "";
        if (!useContexts.isEmpty()) {
            useContextString += "\nIN\n";
            useContextString += String.join("\nIN\n", useContexts.stream().map(Node::textualRepresentation).toList());
        }

        return useNode.textualRepresentation() + useContextString;
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
                iterationContextString.put(context,
                        String.join("\nAND\n", nodesString) + "\nIN\n" + context.textualRepresentation());
            }

            for (Node node : iterationContextString.keySet()) {
                contextNodes.remove(node);
                contextString.put(node, iterationContextString.get(node));
            }
        }

        return String.join("\nAND\n", contextString.values());
    }

    @Override
    public Node getLead() {
        List<Node> nodes = useNodes.stream().toList();

        Random random = new Random();
        int randomIndex = random.nextInt(nodes.size());

        return nodes.get(randomIndex);
    }

    @Override
    public String description() throws IOException {
        String descriptionCache = super.description();
        if (descriptionCache != null) {
            return descriptionCache;
        }

        // A UsagePattern with requirements can only get description within RequirementComponent
        if (!requirements.isEmpty()) {
            System.out.println("Something went wrong");
            System.out.println(textualRepresentation());
            return null;
        }

        String generatedDescription = GroqClient.generate(Prompts.getUsagePatternPrompt(this));

        setDescriptionCache(generatedDescription);

        return generatedDescription;
    }
}
