package narrator.graph.cluster.traverse;

import com.google.gson.JsonObject;
import java.util.List;
import narrator.graph.Edge;
import narrator.graph.EdgeType;
import narrator.graph.Node;
import narrator.graph.NodeType;
import org.jgrapht.Graph;

public class SuccessivePattern extends TraversalPattern {

    Node cachedHead;

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
    public JsonObject stringify() {
        JsonObject result = super.stringify();

        result.addProperty("headId", getHead().getId());
        result.addProperty("nodeType", NodeType.SUCCESSIVE.name());

        return result;
    }
}
