package narrator.graph.cluster.traverse;

import com.google.gson.JsonObject;
import java.util.List;
import java.util.stream.Collectors;
import narrator.graph.Context;
import narrator.graph.Edge;
import narrator.graph.EdgeType;
import narrator.graph.Node;
import narrator.graph.NodeType;
import narrator.graph.cluster.Cluster;
import org.jgrapht.Graph;

public class SuccessivePattern extends TraversalPattern implements Leaf {

    Node cachedHead;

    SuccessivePattern() {
        nodeType = NodeType.SUCCESSIVE;
    }

    private Node getHead() {
        if (cachedHead == null) {
            Graph<Node, Edge> graph = getGraph();
            for (Node node : graph.vertexSet()) {
                List<Edge> outSuccession = graph.outgoingEdgesOf(node).stream()
                        .filter(edge -> edge.getType().equals(EdgeType.SUCCESSION)).toList();
                List<Edge> inSuccession = graph.incomingEdgesOf(node).stream()
                        .filter(edge -> edge.getType().equals(EdgeType.SUCCESSION)).toList();
                if (inSuccession.isEmpty() && !outSuccession.isEmpty()) {
                    cachedHead = node;
                    break;
                }
            }
        }

        return cachedHead;
    }

    @Override
    public Node getLead() {
        return getHead();
    }

    @Override
    public String base(Cluster cluster) {
        Node head = getHead();
        List<Node> semanticContexts = Context.get(cluster.getGraph(), head).stream()
                .filter(n -> n.getNodeType().equals(NodeType.SEMANTIC_CONTEXT))
                .toList();
        return "Succession starting at " + head.mapping(cluster) + " (Containers: " + semanticContexts.size() + ")";
    }

    @Override
    public JsonObject stringify() {
        JsonObject result = super.stringify();

        result.addProperty("headId", getHead().getId());

        return result;
    }
}
