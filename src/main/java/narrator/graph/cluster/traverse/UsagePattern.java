package narrator.graph.cluster.traverse;

import com.google.gson.JsonObject;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import narrator.graph.Node;
import narrator.graph.NodeType;

/*
 * EXTENSION:
 * - variable declaration changes without any usage change: EXTENSION is using
 * - method invocations within a variable declaration change: EXTENSION is being used
 * */

public class UsagePattern extends TraversalPattern {

    private final HashMap<Node, UsagePattern> requirements = new HashMap<>();
    Node useNode;

    UsagePattern(Node node) {
        addNode(node);
        useNode = node;
    }

    public void addRequirement(Node node, UsagePattern requirement) {
        requirements.put(node, requirement);
    }

    public void breakRequirement(Node node) {
        requirements.remove(node);
    }

    public HashMap<Node, UsagePattern> getRequirements() {
        return requirements;
    }

    @Override
    public Node getLead() {
        if (cachedLead == null) {
            cachedLead = useNode;
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
        return this.containsNode(node, new HashSet<>());
    }

    private boolean containsNode(Node node, Set<UsagePattern> visited) {
        if (!visited.add(this)) {
            return false;
        }

        boolean isRootNode = getGraph().vertexSet().stream()
                .anyMatch(rootNode -> rootNode.equals(node));
        if (isRootNode) {
            return true;
        }

        for (UsagePattern requirement : requirements.values()) {
            if (requirement.containsNode(node, visited)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public Set<Node> vertexSet() {
        return this.vertexSet(new HashSet<>());
    }

    private Set<Node> vertexSet(Set<UsagePattern> visited) {
        if (!visited.add(this)) {
            return new HashSet<>();
        }

        Set<Node> result = new HashSet<>(getGraph().vertexSet());
        for (UsagePattern requirement : requirements.values()) {
            result.addAll(requirement.vertexSet(visited));
        }
        return result;
    }
}
