package narrator.graph.cluster.traverse;

import narrator.graph.Edge;
import narrator.graph.EdgeType;
import narrator.graph.Node;
import org.jgrapht.Graph;

import java.util.*;
import java.util.stream.Collectors;

public class Util {
    Graph<Node, Edge> graph;

    Util(Graph<Node, Edge> graph) {
        this.graph = graph;
    }

    public List<Node> getUseNodes(Node node) {
        return graph.outgoingEdgesOf(node).stream()
                .filter(edge -> edge.getType().equals(EdgeType.DEF_USE))
                .map(edge -> graph.getEdgeTarget(edge)).toList();
//        Map<String, List<Node>> pathsNodes = useNodes.stream().collect(Collectors.groupingBy(Node::getPath));
//        pathsNodes.values().forEach(pathNodes -> pathNodes.sort((n1, n2) -> n2.getTree().getPos() - n1.getTree().getPos()));
//
//        List<Node> result = new ArrayList<>();
//        pathsNodes.values().forEach(result::addAll);
//
//        return result;
    }

    public Set<Node> getUsedNodes(Node node) {
        return graph.incomingEdgesOf(node).stream()
                .filter(edge -> edge.getType().equals(EdgeType.DEF_USE))
                .map(edge -> graph.getEdgeSource(edge)).collect(Collectors.toSet());
    }

    public boolean doesUse(Node node) {
        return graph.incomingEdgesOf(node).stream().anyMatch(edge -> edge.getType().equals(EdgeType.DEF_USE));
    }

    public Node getContext(Node node) {
        Optional<Edge> contextEdge = graph.outgoingEdgesOf(node).stream()
                .filter(edge -> edge.getType().equals(EdgeType.CONTEXT))
                .findFirst();
        return contextEdge.map(edge -> graph.getEdgeTarget(edge)).orElse(null);
    }

    public List<Node> getContexts(Node node) {
        List<Node> contexts = new ArrayList<>();

        Node currentNode = node;
        while (true) {
            Node currentContext = getContext(currentNode);
            if (currentContext == null) {
                break;
            }

            contexts.add(currentContext);
            currentNode = currentContext;
        }

        return contexts;
    }

    public HashMap<Node, List<Node>> getContextNodes(List<Node> nodes) {
        HashMap<Node, List<Node>> contextNodes = new HashMap<>();

        for (Node node : nodes) {
            List<Node> contexts = getContexts(node);
            Node currentNode = node;
            for (Node context : contexts) {
                if (!contextNodes.containsKey(context)) {
                    contextNodes.put(context, new ArrayList<>());
                }

                contextNodes.get(context).add(currentNode);

                currentNode = context;
            }
        }

        return contextNodes;
    }
}
