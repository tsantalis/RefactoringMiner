package narrator.graph.cluster.traverse;

import com.google.gson.JsonObject;
import java.util.ArrayList;
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

    public List<Node> getSequence(Cluster cluster) {
        List<Node> sequence = new ArrayList<>();
        Node current = getHead();
        if (current == null) {
            return sequence;
        }

        Graph<Node, Edge> graph = cluster.getGraph();
        while (current != null) {
            sequence.add(current);
            Node next = null;
            for (Edge edge : graph.outgoingEdgesOf(current)) {
                if (edge.getType().equals(EdgeType.SUCCESSION)) {
                    next = graph.getEdgeTarget(edge);
                    break;
                }
            }
            current = next;
        }
        return sequence;
    }

    @Override
    public String base(Cluster cluster) {
        List<Node> sequence = getSequence(cluster);

        StringBuilder prompt = new StringBuilder();
        prompt.append("# Subject:\n```\n");

        List<String> basePrompts = sequence.stream()
                .map(node -> node.base(cluster))
                .toList();
        prompt.append(String.join("\n---\n", basePrompts));
        prompt.append("\n```");

        List<String> mappingHunks = new ArrayList<>();
        for (Node node : sequence) {
            String base = node.base(cluster);
            String mapping = node.mapping(cluster);
            if (!base.equals(mapping)) {
                mappingHunks.add(mapping);
            }
        }

        if (!mappingHunks.isEmpty()) {
            prompt.append("\n\nContext:\n```\n");
            prompt.append(String.join("\n---\n", mappingHunks));
            prompt.append("\n```");
        }

        return prompt.toString();
    }

    @Override
    public String extended(Cluster cluster) {
        return base(cluster);
    }

    @Override
    public JsonObject stringify() {
        JsonObject result = super.stringify();

        result.addProperty("headId", getHead().getId());

        return result;
    }
}
