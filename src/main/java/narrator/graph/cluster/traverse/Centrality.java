package narrator.graph.cluster.traverse;

import narrator.graph.Edge;
import narrator.graph.EdgeType;
import narrator.graph.Node;
import org.jgrapht.Graph;

import java.util.*;

public class Centrality {
    //    Nodes with the most connections (highest degree) are considered central.
    public static List<Node> degree(Graph<Node, Edge> graph) {
        List<Node> hunkNodes = graph.vertexSet().stream().filter(node -> !node.isContext()).toList();

        HashMap<Node, Integer> nodesDegree = new HashMap<>();
        for (Node node : hunkNodes) {
            nodesDegree.put(node, graph.degreeOf(node));
        }

        return nodesDegree.entrySet().stream()
                .sorted((nd1, nd2) -> nd2.getValue() - nd1.getValue())
                .map(Map.Entry::getKey).toList();
    }

    //      Nodes with most used declarations
    public static List<Node> usedDeclarations(Graph<Node, Edge> graph) {
        List<Node> hunkNodes = graph.vertexSet().stream().filter(node -> !node.isContext()).toList();

        HashMap<Node, Integer> nodesUsedDeclarations = new HashMap<>();
        for (Node node : hunkNodes) {
            List<Edge> usedDeclarations = graph.incomingEdgesOf(node).stream()
                    .filter(edge -> edge.getType().equals(EdgeType.DEF_USE))
                    .filter(edge -> graph.getEdgeSource(edge).isBase()).toList();
            if (usedDeclarations.isEmpty()) {
                continue;
            }
            
            nodesUsedDeclarations.put(node, usedDeclarations.size());
        }

        return nodesUsedDeclarations.entrySet().stream()
                .sorted((nd1, nd2) -> nd2.getValue() - nd1.getValue())
                .map(Map.Entry::getKey).toList();
    }


    /*
    Identifies nodes that are closest (in terms of shortest paths) to all other nodes.
    Emphasizes nodes with efficient access to the entire graph.
     * */
//    public static List<Node> closeness(Graph<Node, Edge> graph) {
//
//    }

    //    Evaluates a node's importance based on the importance of its neighbors.
//    public static List<Node> pageRank(Graph<Node, Edge> graph) {
//
//    }

    /*
    Algorithms like Girvan–Newman or Louvain can detect clusters or communities in the graph.
    Backbone sections can be identified as the core parts of these communities or as bridges between them.
    * */
//    public static List<List<Node>> communities(Graph<Node, Edge> graph) {
//
//    }
}
