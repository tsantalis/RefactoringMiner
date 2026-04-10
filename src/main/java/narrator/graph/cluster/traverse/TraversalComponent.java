package narrator.graph.cluster.traverse;

import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import narrator.graph.Node;
import narrator.graph.NodeType;

public class TraversalComponent extends AggregatorPattern {

    private final List<TraversalPattern> components;
    private final ReasonType reasonType;

    TraversalComponent(List<TraversalPattern> components, ReasonType reasonType) {
        this.components = components;
        subs = new HashSet<>(components);
        this.reasonType = reasonType;
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
    public JsonObject stringify() {
        JsonObject result = super.stringify();

        result.addProperty("nodeType", NodeType.COMPONENT.name());
        result.addProperty("reasonType", reasonType.name());

        return result;
    }

    @Override
    public boolean containsNode(Node node) {
        return this.containsNode(node, new HashSet<>());
    }

    @Override
    public Set<Node> vertexSet() {
        return this.vertexSet(new HashSet<>());
    }

    public void breakCircularDependencies() {
        breakCircularDependencies(new ArrayList<>());
    }
}
