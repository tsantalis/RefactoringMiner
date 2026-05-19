package narrator.graph.cluster.traverse;

import narrator.graph.Node;
import narrator.graph.NodeType;
import narrator.graph.Edge;
import narrator.graph.cluster.Cluster;

public class SingularPattern extends TraversalPattern implements Leaf {

    private final Node node;

    SingularPattern(Node node) {
        nodeType = NodeType.SINGULAR;
        this.node = node;
        addNode(node);

        for (String identifier : node.getIdentifiers()) {
            this.addIdentifier(identifier);
        }
    }

    @Override
    public Node getLead() {
        return node;
    }

    @Override
    public String base(Cluster cluster) {
        StringBuilder sb = new StringBuilder();
        sb.append(node.base(cluster));

        org.jgrapht.Graph<Node, Edge> graph = cluster.getGraph();
        java.util.Set<Edge> edges = new java.util.HashSet<>(graph.edgesOf(node));

        if (!edges.isEmpty()) {
            sb.append("\n\n----------------------------------------------------------------------\n");
            sb.append("RELATED CONTEXT:\n");
            sb.append("The following nodes are connected to this change and may be required for a full explanation.\n");
            sb.append("Request them by ID to see their content:\n");
            for (Edge edge : edges) {
                Node connectedNode = graph.getEdgeSource(edge).equals(node) 
                    ? graph.getEdgeTarget(edge) 
                    : graph.getEdgeSource(edge);
                sb.append("\n- ").append(connectedNode.getPromptId())
                  .append(" (").append(connectedNode.getNodeType())
                  .append(") [Relation: ").append(edge.getType()).append("]");
            }
            sb.append("\n----------------------------------------------------------------------");
        }

        return sb.toString();
    }
}
