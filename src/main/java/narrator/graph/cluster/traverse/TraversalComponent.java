package narrator.graph.cluster.traverse;

import com.google.gson.JsonObject;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import narrator.graph.Node;
import narrator.graph.NodeType;

public class TraversalComponent extends TraversalPattern {

    private final List<TraversalPattern> components;
    private final ReasonType reasonType;

    TraversalComponent(List<TraversalPattern> components, ReasonType reasonType) {
        this.components = components;
        this.reasonType = reasonType;
    }

    @Override
    public String getId() {
        String baseId = super.getId();
        return baseId + "-" + System.identityHashCode(this);
    }

    @Override
    public boolean containsNode(Node node) {
        Node foundNode =
                getGraph().vertexSet().stream().filter(reasonNode -> reasonNode.equals(node))
                        .findFirst().orElse(null);
        if (foundNode != null) {
            return true;
        }

        for (TraversalPattern component : components) {
            if (component.containsNode(node)) {
                return true;
            }
        }

        return false;
    }

    public List<TraversalPattern> getComponents() {
        return components;
    }

    @Override
    public Node getLead() {
        if (cachedLead == null) {
            Set<Node> nodes = getGraph().vertexSet();
            if (!nodes.isEmpty()) {
                cachedLead = nodes.iterator().next();
            } else {
                cachedLead = components.get(0).getLead();
            }
        }

        return cachedLead;
    }

    @Override
    public Set<Node> vertexSet() {
        Set<Node> result = new HashSet<>(getGraph().vertexSet());
        for (TraversalPattern component : components) {
            result.addAll(component.vertexSet());
        }
        return result;
    }

    @Override
    public JsonObject stringify() {
        JsonObject result = super.stringify();

        result.addProperty("nodeType", NodeType.COMPONENT.name());
        result.addProperty("reasonType", reasonType.name());

        return result;
    }
}
