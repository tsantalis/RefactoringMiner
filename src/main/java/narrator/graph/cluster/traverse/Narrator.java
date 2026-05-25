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
        } else {
            result.add(p);
        }
    }

    private boolean shouldStopAtThisLevel(AggregatorPattern aggregator, GrainLevel grainLevel) {
        if (grainLevel == GrainLevel.LEAF) return false;
        
        if (grainLevel == GrainLevel.USAGE_CHAIN_ROOT && aggregator instanceof UsagePattern) return true;
        
        if (aggregator instanceof TraversalComponent tc) {
            Set<Node> contexts = tc.getMergeContexts();
            if (contexts == null || contexts.isEmpty()) return false;
            
            Node representative = contexts.iterator().next();
            Set<String> targetTypes = GRAIN_LEVEL_TYPES.get(grainLevel);
            if (targetTypes == null) return false;
            
            com.github.gumtreediff.tree.Tree currentTree = representative.getTree();
            while (currentTree != null) {
                if (targetTypes.contains(currentTree.getType().name)) {
                    return true;
                }
                currentTree = currentTree.getParent();
            }
        }
        return false;
    }
}
