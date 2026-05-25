package narrator.graph.cluster.traverse;

import com.google.gson.JsonObject;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import narrator.graph.Node;
import narrator.graph.NodeType;
import narrator.graph.cluster.Cluster;
import narrator.graph.cluster.GraphWrapper;

public class TraversalComponent extends AggregatorPattern {

    private final List<TraversalPattern> components;
    private final ReasonType reasonType;
    private String mergeContextType;

    public TraversalComponent(List<TraversalPattern> components, ReasonType reasonType) {
        nodeType = NodeType.COMPONENT;
        this.components = components;
        subs = new HashSet<>(components);
        this.reasonType = reasonType;
    }

    public List<TraversalPattern> getComponents() {
        return components;
    }

    public String getMergeContextType() {
        return mergeContextType;
    }

    public void setMergeContextType(String mergeContextType) {
        this.mergeContextType = mergeContextType;
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

        result.addProperty("reasonType", reasonType.name());
        result.addProperty("mergeContextType", mergeContextType);

        return result;
    }

    public String extended(Cluster cluster) {
        return super.extended(cluster);
    }

    @Override
    public boolean containsNode(Node node) {
        return this.containsNode(node, new HashSet<>());
    }

    @Override
    public Set<Node> vertexSet() {
        return this.vertexSet(new HashSet<>());
    }
}
