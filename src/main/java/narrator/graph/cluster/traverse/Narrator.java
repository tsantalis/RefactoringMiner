package narrator.graph.cluster.traverse;

import java.util.*;
import java.util.stream.Collectors;
import narrator.graph.Node;
import narrator.graph.NodeType;
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
            case USAGE_CHAIN_ROOT -> {
                Set<UsagePattern> roots = findUsageRoots(rootPattern);
                narrateUsageChainRoot(rootPattern, visited, result, roots);
            }
            case SEMANTIC_LEAF -> narrateSemanticLeaf(rootPattern, visited, result);
            case METHOD, CLASS, FILE -> narrateComponentGrain(rootPattern, visited, result, grainLevel);
        }
        
        return result;
    }

    private void narrateLeaf(TraversalPattern p, Set<TraversalPattern> visited, List<TraversalPattern> result) {
        if (visited.contains(p)) return;
        visited.add(p);

        if (p instanceof AggregatorPattern agg) {
            List<TraversalPattern> sortedSubs = new ArrayList<>(agg.subs);
            sortedSubs.sort((s1, s2) -> {
                if (s1.dependsOn(s2)) return 1;
                if (s2.dependsOn(s1)) return -1;
                return Integer.compare(s2.getDepth(), s1.getDepth());
            });

            for (TraversalPattern sub : sortedSubs) {
                narrateLeaf(sub, visited, result);
            }
        }

        if (p instanceof Leaf) {
            result.add(p);
        }
    }

    private void narrateUsageChainRoot(TraversalPattern p, Set<TraversalPattern> visited, List<TraversalPattern> result, Set<UsagePattern> roots) {
        if (visited.contains(p)) return;
        visited.add(p);

        if (p instanceof AggregatorPattern agg) {
            List<TraversalPattern> sortedSubs = new ArrayList<>(agg.subs);
            sortedSubs.sort((s1, s2) -> {
                if (s1.dependsOn(s2)) return 1;
                if (s2.dependsOn(s1)) return -1;
                return Integer.compare(s2.getDepth(), s1.getDepth());
            });

            for (TraversalPattern sub : sortedSubs) {
                narrateUsageChainRoot(sub, visited, result, roots);
            }
        }

        if (p instanceof UsagePattern usage) {
            if (roots.contains(usage)) {
                result.add(usage);
            }
        } else if (p instanceof Leaf) {
            result.add(p);
        }
    }

    
    private Set<UsagePattern> findUsageRoots(TraversalPattern root) {
        Set<UsagePattern> allUsages = new HashSet<>();
        collectUsages(root, allUsages);
        
        Set<UsagePattern> roots = new HashSet<>();
        for (UsagePattern usage : allUsages) {
            if (!isDescendantOfUsage(usage, allUsages)) {
                roots.add(usage);
            }
        }
        return roots;
    }

    private void collectUsages(TraversalPattern p, Set<UsagePattern> usages) {
        if (p instanceof UsagePattern usage) {
            usages.add(usage);
        }
        if (p instanceof AggregatorPattern agg) {
            for (TraversalPattern sub : agg.subs) {
                collectUsages(sub, usages);
            }
        }
    }

    private boolean isDescendantOfUsage(UsagePattern p, Set<UsagePattern> allUsages) {
        for (UsagePattern other : allUsages) {
            if (p == other) continue;
            if (p.dependsOn(other)) {
                return true;
            }
        }
        return false;
    }

    private void narrateSemanticLeaf(TraversalPattern p, Set<TraversalPattern> visited, List<TraversalPattern> result) {
        if (visited.contains(p)) return;
        visited.add(p);

        if (p instanceof TraversalComponent tc && isSemanticLeaf(tc)) {
            result.add(p);
            return;
        }

        if (p instanceof AggregatorPattern agg) {
            List<TraversalPattern> sortedSubs = new ArrayList<>(agg.subs);
            sortedSubs.sort((s1, s2) -> {
                if (s1.dependsOn(s2)) return 1;
                if (s2.dependsOn(s1)) return -1;
                return Integer.compare(s2.getDepth(), s1.getDepth());
            });

            for (TraversalPattern sub : sortedSubs) {
                narrateSemanticLeaf(sub, visited, result);
            }
        }

        if (p instanceof Leaf) {
            result.add(p);
        }
    }

    public boolean isSemanticLeaf(TraversalComponent tc) {
        if (tc.getMergeContexts() == null || tc.getMergeContexts().isEmpty()) return false;

        boolean hasSemanticContext = false;
        for (Node node : tc.getMergeContexts()) {
            if (node.getNodeType() == NodeType.SEMANTIC_CONTEXT) {
                hasSemanticContext = true;
                break;
            }
        }

        if (!hasSemanticContext) return false;

        for (TraversalPattern sub : tc.subs) {
            if (!(sub instanceof Leaf)) return false;
        }

        return true;
    }

    private void narrateComponentGrain(TraversalPattern p, Set<TraversalPattern> visited, List<TraversalPattern> result, GrainLevel grainLevel) {
        if (visited.contains(p)) return;
        visited.add(p);

        if (p instanceof TraversalComponent tc && matchesGrain(tc, grainLevel)) {
            result.add(p);
            return;
        }

        if (p instanceof AggregatorPattern agg) {
            List<TraversalPattern> sortedSubs = new ArrayList<>(agg.subs);
            sortedSubs.sort((s1, s2) -> {
                if (s1.dependsOn(s2)) return 1;
                if (s2.dependsOn(s1)) return -1;
                return Integer.compare(s2.getDepth(), s1.getDepth());
            });

            for (TraversalPattern sub : sortedSubs) {
                narrateComponentGrain(sub, visited, result, grainLevel);
            }
        }

        if (p instanceof Leaf) {
            result.add(p);
        }
    }

    public boolean matchesGrain(TraversalComponent tc, GrainLevel grainLevel) {
        Set<String> allowedTypes = GRAIN_LEVEL_TYPES.get(grainLevel);
        if (allowedTypes == null || tc.getMergeContexts() == null) return false;

        for (Node contextNode : tc.getMergeContexts()) {
            var tree = contextNode.getTree();
            if (allowedTypes.contains(tree.getType().name)) return true;
            for (var parent : tree.getParents()) {
                if (allowedTypes.contains(parent.getType().name)) return true;
            }
        }
        return false;
    }
}
