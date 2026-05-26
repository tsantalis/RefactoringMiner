package narrator.graph.cluster.traverse;

import java.util.*;
import java.util.stream.Collectors;
import narrator.graph.Node;
import narrator.graph.Edge;
import narrator.graph.EdgeType;
import narrator.graph.cluster.Cluster;
import org.jgrapht.Graph;
import org.refactoringminer.astDiff.utils.Constants;

public class Narrator {
    private final TraversalPattern rootPattern;
    private final Map<GrainLevel, List<TraversalPattern>> cache = new HashMap<>();
    private final Map<GrainLevel, Integer> progressMap = new HashMap<>();

    private static final Map<GrainLevel, Set<String>> GRAIN_LEVEL_TYPES = new HashMap<>();
    static {
        Constants c = new Constants("");
        GRAIN_LEVEL_TYPES.put(GrainLevel.USAGE_CHAIN_ROOT, Set.of("UsagePattern")); // Handled by type check
        GRAIN_LEVEL_TYPES.put(GrainLevel.METHOD, Set.of(c.METHOD_DECLARATION));
        GRAIN_LEVEL_TYPES.put(GrainLevel.CLASS, Set.of(c.TYPE_DECLARATION, c.INTERFACE_DECLARATION, c.ENUM_DECLARATION, c.RECORD_DECLARATION));
        GRAIN_LEVEL_TYPES.put(GrainLevel.FILE, Set.of(c.COMPILATION_UNIT));
    }

    public Narrator(TraversalPattern rootPattern) {
        this.rootPattern = rootPattern;
    }

    public List<TraversalPattern> getNarrative(GrainLevel grainLevel) {
        return cache.computeIfAbsent(grainLevel, this::narrate);
    }

    public int getProgress(GrainLevel grainLevel) {
        return progressMap.getOrDefault(grainLevel, 0);
    }

    public void incrementProgress(GrainLevel grainLevel) {
        progressMap.put(grainLevel, getProgress(grainLevel) + 1);
    }

    private List<TraversalPattern> narrate(GrainLevel grainLevel) {
        if (rootPattern == null) {
            return Collections.emptyList();
        }

        List<TraversalPattern> result = new ArrayList<>();
        Set<TraversalPattern> visited = new HashSet<>();
        
        switch (grainLevel) {
            case LEAF -> narrateLeaf(rootPattern, visited, result);
            case USAGE_CHAIN_ROOT -> narrateUsageChainRoot(rootPattern, visited, result);
            case METHOD, CLASS, FILE -> narrateComponentGrain(rootPattern, visited, result, grainLevel);
        }
        
        return result;
    }

    private void narrateLeaf(TraversalPattern p, Set<TraversalPattern> visited, List<TraversalPattern> result) {
        if (visited.contains(p)) return;
        visited.add(p);

        if (p instanceof AggregatorPattern agg) {
            List<TraversalPattern> sortedSubs = new ArrayList<>(aggregator.subs);
            sortedSubs.sort(Comparator.comparingInt(TraversalPattern::getDepth).reversed());

            for (TraversalPattern sub : sortedSubs) {
                narrateLeaf(sub, visited, result);
            }
        }

        if (p instanceof Leaf) {
            result.add(p);
        }
    }

    private void narrateUsageChainRoot(TraversalPattern p, Set<TraversalPattern> visited, List<TraversalPattern> result) {
        if (visited.contains(p)) return;
        visited.add(p);

        if (p instanceof AggregatorPattern agg && !(p instanceof UsagePattern)) {
            List<TraversalPattern> sortedSubs = new ArrayList<>(agg.subs);
            sortedSubs.sort(Comparator.comparingInt(TraversalPattern::getDepth).reversed());

            for (TraversalPattern sub : sortedSubs) {
                narrateUsageChainRoot(sub, visited, result);
            }
        }

        if (p instanceof Leaf) {
            result.add(p);
        }
    }

    private void narrateComponentGrain(TraversalPattern p, Set<TraversalPattern> visited, List<TraversalPattern> result, GrainLevel grainLevel) {
        // TODO: Implement based on updated requirements from user
        // Should handle TraversalComponent types (METHOD, CLASS, FILE)
        // and fall back to leaf behavior.
    }
}
