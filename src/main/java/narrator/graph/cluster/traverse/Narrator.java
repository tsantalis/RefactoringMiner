package narrator.graph.cluster.traverse;

import java.util.*;
import java.util.stream.Collectors;
import narrator.graph.Node;
import narrator.graph.Edge;
import narrator.graph.EdgeType;
import narrator.graph.cluster.Cluster;
import org.jgrapht.Graph;

public class Narrator {
    private final TraversalPattern rootPattern;
    private final Map<GrainLevel, List<TraversalPattern>> cache = new HashMap<>();

    public Narrator(TraversalPattern rootPattern) {
        this.rootPattern = rootPattern;
    }

    public List<TraversalPattern> getNarrative(GrainLevel grainLevel) {
        return cache.computeIfAbsent(grainLevel, this::narrate);
    }

    private List<TraversalPattern> narrate(GrainLevel grainLevel) {
        if (rootPattern == null) {
            return Collections.emptyList();
        }

        List<TraversalPattern> result = new ArrayList<>();
        Set<TraversalPattern> visited = new HashSet<>();
        
        postOrderTraverse(rootPattern, visited, result, grainLevel);
        
        return result;
    }

    private void postOrderTraverse(TraversalPattern p, Set<TraversalPattern> visited, List<TraversalPattern> result, GrainLevel grainLevel) {
        if (visited.contains(p)) {
            return;
        }
        
        visited.add(p);
        
        if (p instanceof AggregatorPattern aggregator) {
            if (shouldStopAtThisLevel(aggregator, grainLevel)) {
                result.add(p);
                return;
            }
            List<TraversalPattern> sortedSubs = new ArrayList<>(aggregator.subs);
            sortedSubs.sort(Comparator.comparingInt(TraversalPattern::getDepth).reversed());

            for (TraversalPattern sub : sortedSubs) {
                postOrderTraverse(sub, visited, result, grainLevel);
            }
        }
        
        if (!(p instanceof AggregatorPattern && shouldStopAtThisLevel((AggregatorPattern) p, grainLevel))) {
             result.add(p);
        }
    }

    private boolean shouldStopAtThisLevel(AggregatorPattern aggregator, GrainLevel grainLevel) {
        if (grainLevel == GrainLevel.LEAF) return false;
        if (grainLevel == GrainLevel.USAGE_CHAIN_ROOT && aggregator instanceof UsagePattern) return true;
        if (aggregator instanceof TraversalComponent tc) {
            String type = tc.getMergeContextType();
            if (type == null) return false;
            if (grainLevel == GrainLevel.METHOD && type.contains("Method")) return true;
            if (grainLevel == GrainLevel.CLASS && (type.contains("Class") || type.contains("Interface"))) return true;
            if (grainLevel == GrainLevel.FILE && type.contains("File")) return true;
        }
        return false;
    }
}
