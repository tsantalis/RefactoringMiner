package narrator.graph.cluster.traverse;

import java.util.*;
import java.util.stream.Collectors;
import narrator.graph.Node;
import narrator.graph.Edge;
import narrator.graph.EdgeType;
import narrator.graph.cluster.Cluster;
import org.jgrapht.Graph;

public class Narrator {

    public List<Node> getTopCandidates(Cluster cluster) {
        Graph<Node, Edge> graph = cluster.getGraph();
        Set<Node> allNodes = graph.vertexSet();
        
        Map<Node, Set<Node>> supportBases = new HashMap<>();
        
        for (Node n : allNodes) {
            if (n.isContext() || n.isExtension()) {
                continue;
            }
            
            Set<Node> ancestors = new HashSet<>();
            Queue<Node> queue = new LinkedList<>();
            queue.add(n);
            
            Set<Node> visited = new HashSet<>();
            visited.add(n);
            
            while (!queue.isEmpty()) {
                Node curr = queue.poll();
                for (Edge edge : graph.incomingEdgesOf(curr)) {
                    Node parent = graph.getEdgeSource(edge);
                    if (edge.getType() == EdgeType.DEF_USE && !visited.contains(parent)) {
                        visited.add(parent);
                        if (!parent.isContext() && !parent.isExtension()) {
                            ancestors.add(parent);
                        }
                        queue.add(parent);
                    }
                }
            }
            supportBases.put(n, ancestors);
        }
        
        return supportBases.entrySet().stream()
                .sorted((e1, e2) -> Integer.compare(e2.getValue().size(), e1.getValue().size()))
                .map(Map.Entry::getKey)
                .toList();
    }

    /**
     * Produces a list of patterns ordered from deepest leaf to highest.
     * Deepest leaves are those that are not requirements for any other pattern in the set,
     * or are the base requirements in a chain.
     * The order is determined by a topological sort of the dependency graph.
     */
    public List<Leaf> narrate(TraversalPattern pattern) {
        if (pattern == null) {
            return Collections.emptyList();
        }

        List<TraversalPattern> result = new ArrayList<>();
        Set<TraversalPattern> visited = new HashSet<>();
        
        postOrderTraverse(pattern, visited, result);
        
        return result.stream()
                .filter(p -> p instanceof Leaf)
                .map(p -> (Leaf) p)
                .toList();
    }

    private void postOrderTraverse(TraversalPattern p, Set<TraversalPattern> visited, List<TraversalPattern> result) {
        if (visited.contains(p)) {
            return;
        }
        
        visited.add(p);
        
        if (p instanceof AggregatorPattern aggregator) {
            List<TraversalPattern> sortedSubs = new ArrayList<>(aggregator.subs);
            sortedSubs.sort(Comparator.comparingInt(TraversalPattern::getDepth).reversed());


            for (TraversalPattern sub : sortedSubs) {
                postOrderTraverse(sub, visited, result);
            }
        }
        
        result.add(p);
    }

}
