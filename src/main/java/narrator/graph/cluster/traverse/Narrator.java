package narrator.graph.cluster.traverse;

import java.util.*;
import java.util.stream.Collectors;

public class Narrator {

    /**
     * Produces a list of patterns ordered from deepest leaf to highest.
     * Deepest leaves are those that are not requirements for any other pattern in the set,
     * or are the base requirements in a chain.
     * 
     * The order is determined by a topological sort of the dependency graph.
     */
    public List<TraversalPattern> narrate(List<TraversalPattern> patterns) {
        if (patterns == null || patterns.isEmpty()) {
            return Collections.emptyList();
        }

        // Build dependency graph
        // A depends on B if A requires B to be understood first.
        // In our case, if A is a UsagePattern and B is one of its requirements.
        // Or if A is a TraversalComponent and B is one of its child components.
        Map<TraversalPattern, Set<TraversalPattern>> adj = new HashMap<>();
        Map<TraversalPattern, Integer> inDegree = new HashMap<>();

        for (TraversalPattern p : patterns) {
            adj.putIfAbsent(p, new HashSet<>());
            inDegree.putIfAbsent(p, 0);

            Set<TraversalPattern> deps = getDependencies(p, patterns);
            for (TraversalPattern dep : deps) {
                // dep -> p (dep must be understood before p)
                adj.computeIfAbsent(dep, k -> new HashSet<>()).add(p);
                inDegree.put(p, inDegree.getOrDefault(p, 0) + 1);
                inDegree.putIfAbsent(dep, 0);
            }
        }

        // Topological Sort (Kahn's Algorithm)
        Queue<TraversalPattern> queue = new LinkedList<>();
        for (Map.Entry<TraversalPattern, Integer> entry : inDegree.entrySet()) {
            if (entry.getValue() == 0) {
                queue.add(entry.getKey());
            }
        }

        List<TraversalPattern> result = new ArrayList<>();
        while (!queue.isEmpty()) {
            TraversalPattern u = queue.poll();
            result.add(u);

            for (TraversalPattern v : adj.getOrDefault(u, Collections.emptySet())) {
                inDegree.put(v, inDegree.get(v) - 1);
                if (inDegree.get(v) == 0) {
                    queue.add(v);
                }
            }
        }

        // Handle cycles: if result size < patterns size, there's a cycle.
        // In the case of cycles, we just add the remaining ones to avoid data loss.
        if (result.size() < patterns.size()) {
            Set<TraversalPattern> visited = new HashSet<>(result);
            for (TraversalPattern p : patterns) {
                if (!visited.contains(p)) {
                    result.add(p);
                }
            }
        }

        return result;
    }

    private Set<TraversalPattern> getDependencies(TraversalPattern p, List<TraversalPattern> allPatterns) {
        Set<TraversalPattern> deps = new HashSet<>();
        
        if (p instanceof AggregatorPattern aggregator) {
            for (TraversalPattern sub : aggregator.subs) {
                if (allPatterns.contains(sub)) {
                    deps.add(sub);
                }
            }
        }
        
        return deps;
    }
}
