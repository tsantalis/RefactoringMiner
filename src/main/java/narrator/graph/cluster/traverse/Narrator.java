package narrator.graph.cluster.traverse;

import java.util.*;
import java.util.stream.Collectors;

public class Narrator {

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
            sortedSubs.sort(Comparator.comparing((TraversalPattern sub) -> {
                return sub.getLead().isSrc();
            }).reversed()
              .thenComparing(sub -> {
                if (sub instanceof AggregatorPattern) return 1;
                if (sub instanceof SingularPattern) return 2;
                if (sub instanceof SuccessivePattern) return 3;
                return 4;
              }));

            for (TraversalPattern sub : sortedSubs) {
                postOrderTraverse(sub, visited, result);
            }
        }
        
        result.add(p);
    }

}
