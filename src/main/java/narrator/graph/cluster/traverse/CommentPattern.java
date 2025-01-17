package narrator.graph.cluster.traverse;

import narrator.graph.Edge;
import narrator.graph.EdgeType;
import narrator.graph.Node;
import org.jgrapht.Graph;

import java.util.*;

public class CommentPattern extends TraversalPattern {
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

    private List<Node> getSuccessiveNodes() {
        List<Node> successiveNodes = new ArrayList<>();

        Node head = getHead();
        if (head == null) {
            return successiveNodes;
        }

        successiveNodes.add(head);

        Graph<Node, Edge> graph = getGraph();
        while (true) {
            Node current = successiveNodes.get(successiveNodes.size() - 1);
            Optional<Edge> edgeToNext = graph.outgoingEdgesOf(current).stream()
                    .filter(edge -> edge.getType().equals(EdgeType.SUCCESSION)).findFirst();
            if (edgeToNext.isEmpty()) {
                break;
            }

            Node next = graph.getEdgeTarget(edgeToNext.get());
            successiveNodes.add(next);
        }

        return successiveNodes;
    }

    @Override
    public String textualRepresentation() {
        List<Node> successiveNodes = getSuccessiveNodes();

        String successiveString = String.join("\n", successiveNodes.stream().map(Node::getContent).toList());
        List<Node> contexts = util.getContexts(successiveNodes.get(0));
        return successiveString
                + "\nIN\n"
                + String.join("\nIN\n", contexts.stream().map(Node::textualRepresentation).toList());
    }

    @Override
    public Node getLead() {
        return getHead();
    }
}
