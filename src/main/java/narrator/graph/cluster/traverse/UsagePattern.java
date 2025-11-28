package narrator.graph.cluster.traverse;

import com.google.gson.JsonObject;
import narrator.graph.Node;
import narrator.graph.NodeType;

import java.util.*;

/*
 * EXTENSION:
 * - variable declaration changes without any usage change: EXTENSION is using
 * - method invocations within a variable declaration change: EXTENSION is being used
 * */

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
    public Node getLead() {
        if (cachedLead == null) {
            List<Node> nodes = useNodes.stream().toList();
            int randomIndex = new Random().nextInt(nodes.size());
            cachedLead = nodes.get(randomIndex);
        }

        return cachedLead;
    }

    @Override
    public JsonObject stringify() {
        JsonObject result = super.stringify();

        result.addProperty("nodeType", NodeType.USAGE.name());

        return result;
    }

    @Override
    public boolean containsNode(Node node) {
        boolean isRootNode = getGraph().vertexSet().stream().anyMatch(rootNode -> rootNode.equals(node));
        if (isRootNode) {
            return true;
        }

        for (UsagePattern requirement : requirements.values()) {
            if (requirement.containsNode(node)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public Set<Node> vertexSet() {
        Set<Node> result = new HashSet<>(getGraph().vertexSet());
        for (UsagePattern requirement : requirements.values()) {
            result.addAll(requirement.vertexSet());
        }
        return result;
    }
}
