package narrator.graph.cluster.traverse;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import narrator.graph.Edge;
import narrator.graph.EdgeType;
import narrator.graph.Node;
import narrator.graph.NodeType;
import narrator.graph.cluster.Cluster;
import org.jgrapht.Graph;

/*
 * EXTENSION:
 * - variable declaration changes without any usage change: EXTENSION is using
 * - method invocations within a variable declaration change: EXTENSION is being used
 * */

public class UsagePattern extends AggregatorPattern implements Leaf {

    private final HashMap<Node, TraversalPattern> requirements = new HashMap<>();
    Node useNode;

    UsagePattern(Node node) {
        nodeType = NodeType.USAGE;
        addNode(node);
        useNode = node;

        for (String identifier : node.getIdentifiers()) {
            this.addIdentifier(identifier);
        }
    }

    public void addRequirement(Node node, TraversalPattern requirement) {
        if (requirement != null) {
            subs.add(requirement);
        }
        requirements.put(node, requirement);
    }

    public void breakRequirement(Node node) {
        subs.remove(requirements.get(node));
        requirements.remove(node);
    }

    public HashMap<Node, TraversalPattern> getRequirements() {
        return requirements;
    }

    @Override
    public Node getLead() {
        if (cachedLead == null) {
            if (!useNode.isExtension()) {
                cachedLead = useNode;
            } else {
                Graph<Node, Edge> graph = getGraph();
                cachedLead = graph.incomingEdgesOf(useNode).stream()
                        .filter(edge -> edge.getType().equals(EdgeType.DEF_USE))
                        .map(graph::getEdgeSource).findFirst().get();
            }
        }

        return cachedLead;
    }

    public Set<Node> getUsedNodes() {
        return util.getUsedNodes(useNode);
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

    @Override
    public String base(Cluster cluster) {
        return "Usage of " + useNode.base(cluster);
    }
}
