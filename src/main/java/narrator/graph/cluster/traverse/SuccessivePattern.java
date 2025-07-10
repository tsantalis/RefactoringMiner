package narrator.graph.cluster.traverse;

import com.google.gson.JsonObject;
import narrator.graph.Edge;
import narrator.graph.Node;
import narrator.graph.NodeType;
import org.jgrapht.Graph;

import java.util.*;

public class SuccessivePattern extends TraversalPattern {
    private Node getHead() {
        Graph<Node, Edge> graph = getGraph();

        Set<Node> nodes = graph.vertexSet();
        for (Node node : nodes) {
            if (graph.incomingEdgesOf(node).isEmpty()) {
                return node;
            }
        }

        return null;
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
