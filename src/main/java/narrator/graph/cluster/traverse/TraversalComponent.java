package narrator.graph.cluster.traverse;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import narrator.graph.Node;
import narrator.graph.NodeType;

import java.util.*;

public class TraversalComponent extends TraversalPattern {
    private List<TraversalPattern> components;
    protected Set<Node> reasons;
    private ReasonType reasonType;

    TraversalComponent(List<TraversalPattern> components, Set<Node> reasons, ReasonType reasonType) {
        this.components = components;
        this.reasons = reasons;
        this.reasonType = reasonType;
    }

    @Override
    public boolean containsNode(Node node) {
        Node foundNode =
                getGraph().vertexSet().stream().filter(reasonNode -> reasonNode.equals(node)).findFirst().orElse(null);
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

        JsonArray reasonsArr = new JsonArray();
        for (Node reason : reasons) {
            JsonObject reasonObj = new JsonObject();
            reasonObj.addProperty("id", reason.getId());
            reasonObj.addProperty("content", reason.getContent());
            reasonsArr.add(reasonObj);
        }
        result.add("reasons", reasonsArr);

        result.addProperty("reasonType", reasonType.name());
        result.addProperty("nodeType", NodeType.COMPONENT.name());

        return result;
    }
}
