package org.refactoringminer.astDiff.graph.cluster.traverse;

import com.github.gumtreediff.tree.Tree;
import org.refactoringminer.astDiff.graph.Node;
import org.refactoringminer.astDiff.graph.NodeType;
import org.refactoringminer.astDiff.utils.Constants;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class Narrator {
    private final TraversalPattern rootPattern;
    private final Map<GrainLevel, List<TraversalPattern>> cache = new HashMap<>();
    private final Map<GrainLevel, List<ChapterUnit>> flatCache = new HashMap<>();
    private final Map<GrainLevel, Integer> progressMap = new HashMap<>();

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

        switch (grainLevel) {
            case SINGLE ->
                    traverse(rootPattern, visited, result, pp -> pp.clusterGraph != null, pp -> pp instanceof Leaf);
            case LEAF -> traverse(rootPattern, visited, result, pp -> false, pp -> pp instanceof Leaf);
            case USAGE_CHAIN_ROOT -> {
                Set<UsagePattern> roots = findUsageRoots(rootPattern);
                traverse(rootPattern, visited, result, pp -> pp instanceof UsagePattern u && roots.contains(u), pp -> pp instanceof Leaf && (!(pp instanceof UsagePattern) || roots.contains((UsagePattern) pp)));
            }
            case SEMANTIC_LEAF ->
                    traverse(rootPattern, visited, result, pp -> pp instanceof TraversalComponent tc && isSemanticLeaf(tc), pp -> pp instanceof Leaf);
            case SEMANTIC_ROOT ->
                    traverse(rootPattern, visited, result, pp -> pp instanceof TraversalComponent tc && isSemanticRoot(tc), pp -> pp instanceof Leaf);
            case METHOD, CLASS, FILE ->
                    traverse(rootPattern, visited, result, pp -> pp instanceof TraversalComponent tc && matchesGrain(tc, grainLevel), pp -> pp instanceof Leaf);
        }

        return sortPatterns(result);
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

    private static boolean isSemanticLeaf(TraversalComponent tc) {
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

    private static boolean isSemanticRoot(TraversalComponent tc) {
        if (tc.getMergeContexts() == null || tc.getMergeContexts().isEmpty()) return false;

        for (Node node : tc.getMergeContexts()) {
            if (node.getNodeType() != NodeType.SEMANTIC_CONTEXT) {
                return false;
            }
        }

        return true;
    }

    private static boolean matchesGrain(TraversalComponent tc, GrainLevel grainLevel) {
        if (tc.getMergeContexts() == null) {
            return false;
        }

        for (Node contextNode : tc.getMergeContexts()) {
            Constants constants = new Constants(contextNode.getPath());
            Tree tree = contextNode.getTree();

            if (matchesGrain(tree.getType().name, constants, grainLevel)) {
                return true;
            }
            for (Tree parent : tree.getParents()) {
                if (matchesGrain(parent.getType().name, constants, grainLevel)) {
                    return true;
                }
            }
        }

        return false;
    }

    private static boolean matchesGrain(String treeType, Constants constants, GrainLevel grainLevel) {
        return switch (grainLevel) {
            case METHOD -> constants.isNamedMethod(treeType);
            case CLASS -> constants.isType(treeType);
            case FILE -> constants.isRoot(treeType);
            default -> false;
        };
    }

    private static void traverse(TraversalPattern p, Set<TraversalPattern> visited, List<TraversalPattern> result, Predicate<TraversalPattern> stopPredicate, Predicate<TraversalPattern> leafPredicate) {
        if (visited.contains(p)) return;
        visited.add(p);

        if (stopPredicate.test(p)) {
            result.add(p);
            return;
        }

        if (p instanceof AggregatorPattern agg) {
            List<TraversalPattern> sortedSubs = orderSubs(new ArrayList<>(agg.subs));
            for (TraversalPattern sub : sortedSubs) {
                traverse(sub, visited, result, stopPredicate, leafPredicate);
            }
        }

        if (leafPredicate.test(p)) {
            result.add(p);
        }
    }

    private static List<TraversalPattern> orderSubs(List<TraversalPattern> subs) {
        int n = subs.size();
        if (n <= 1) return subs;

        Map<TraversalPattern, List<TraversalPattern>> dependents = new HashMap<>();
        Map<TraversalPattern, Integer> inDegree = new HashMap<>();
        for (TraversalPattern s : subs) {
            dependents.put(s, new ArrayList<>());
            inDegree.put(s, 0);
        }
        for (TraversalPattern a : subs) {
            for (TraversalPattern b : subs) {
                if (a == b) continue;
                if (a.dependsOn(b) && !b.dependsOn(a)) {
                    dependents.get(b).add(a);
                    inDegree.merge(a, 1, Integer::sum);
                }
            }
        }

        // longest-path level; cycles are broken upstream by TraversalEngine
        Map<TraversalPattern, Integer> level = new HashMap<>();
        Deque<TraversalPattern> queue = new ArrayDeque<>();
        for (TraversalPattern s : subs) {
            if (inDegree.get(s) == 0) {
                level.put(s, 0);
                queue.add(s);
            }
        }
        while (!queue.isEmpty()) {
            TraversalPattern cur = queue.poll();
            for (TraversalPattern nxt : dependents.get(cur)) {
                level.merge(nxt, level.get(cur) + 1, Math::max);
                if (inDegree.merge(nxt, -1, Integer::sum) == 0) queue.add(nxt);
            }
        }

        List<TraversalPattern> unleveled = subs.stream().filter(s -> !level.containsKey(s)).toList();
        if (!unleveled.isEmpty()) {
            throw new IllegalStateException("Dependency cycle among siblings, unbroken by TraversalEngine: "
                    + String.join(", ", unleveled.stream().map(TraversalPattern::getId).toList()));
        }

        return subs.stream().sorted(
                Comparator.comparingInt((TraversalPattern p) -> level.get(p))
                        .thenComparing(TraversalPattern::sortKey)
        ).toList();
    }

    public List<ChapterUnit> getFlatChapters(GrainLevel level) {
        if (flatCache.containsKey(level)) {
            return flatCache.get(level);
        }

        List<TraversalPattern> chapters = getNarrative(level);
        if (chapters == null) return Collections.emptyList();

        // 1. Expand original chapters into atomic units
        List<ChapterUnit> units = new ArrayList<>();
        for (int i = 0; i < chapters.size(); i++) {
            TraversalPattern chapter = chapters.get(i);

            List<TraversalPattern> filterPatterns = i > 0 ? chapters.subList(0, i) : Collections.emptyList();

            if (chapter instanceof AggregatorPattern agg) {
                List<NarrativeElement> elements = agg.getElements(filterPatterns);

                List<List<Integer>> splits = Splitter.createBalancedSplits(elements.stream().map(NarrativeElement::getContent).toList());
                for (List<Integer> split : splits) {
                    ChapterUnit chu = new ChapterUnit();
                    for (Integer index : split) {
                        NarrativeElement ne = elements.get(index);
                        chu.append(ne.getContent());
                        chu.addMains(ne.getMains());
                        chu.addSides(ne.getSides());
                    }

                    units.add(chu);
                }
            } else {
                ChapterUnit chu = new ChapterUnit();
                chu.append(chapter.extended(filterPatterns));
                chu.addMains(new HashSet<>(chapter.getMains()));
                chu.addSides(new HashSet<>(chapter.getSides()));

                units.add(chu);
            }
        }

        // 2. Merge units into flat chapters
        List<List<ChapterUnit>> mergedGroups = new ArrayList<>();
        List<ChapterUnit> currentGroup = new ArrayList<>();
        int currentSum = 0;

        for (ChapterUnit unit : units) {
            if (currentGroup.isEmpty() || (currentSum + unit.lines() <= Splitter.THRESHOLD)) {
                currentGroup.add(unit);
                currentSum += unit.lines();
            } else {
                mergedGroups.add(currentGroup);
                currentGroup = new ArrayList<>();
                currentGroup.add(unit);
                currentSum = unit.lines();
            }
        }
        if (!currentGroup.isEmpty()) {
            mergedGroups.add(currentGroup);
        }

        // 3. Final formatting
        List<ChapterUnit> flatGroups = new ArrayList<>();
        for (List<ChapterUnit> group : mergedGroups) {
            ChapterUnit chu = new ChapterUnit();
            for (ChapterUnit unit : group) {
                chu.append(unit.contents);
                chu.addMains(unit.mains);
                chu.addSides(unit.sides);
            }

            flatGroups.add(chu);
        }

        flatCache.put(level, flatGroups);
        return flatGroups;
    }

    public int getProgress(GrainLevel grainLevel) {
        return progressMap.getOrDefault(grainLevel, 0);
    }

    public void incrementProgress(GrainLevel grainLevel) {
        progressMap.put(grainLevel, getProgress(grainLevel) + 1);
    }

    private List<TraversalPattern> sortPatterns(List<TraversalPattern> patterns) {
        int n = patterns.size();
        if (n == 0) return Collections.emptyList();

        Map<TraversalPattern, Integer> priority = new HashMap<>();
        Map<TraversalPattern, List<TraversalPattern>> adj = new HashMap<>();
        Map<TraversalPattern, Integer> inDegree = new HashMap<>();

        for (int i = 0; i < n; i++) {
            TraversalPattern p = patterns.get(i);
            priority.put(p, i);
            adj.put(p, new ArrayList<>());
            inDegree.put(p, 0);
        }

        for (TraversalPattern p1 : patterns) {
            for (TraversalPattern p2 : patterns) {
                if (p1 != p2 && p1.dependsOn(p2)) {
                    adj.get(p2).add(p1);
                    inDegree.put(p1, inDegree.get(p1) + 1);
                }
            }
        }

        PriorityQueue<TraversalPattern> pq = new PriorityQueue<>(Comparator.comparingInt(priority::get));
        for (TraversalPattern p : patterns) {
            if (inDegree.get(p) == 0) {
                pq.add(p);
            }
        }

        List<TraversalPattern> result = new ArrayList<>();
        while (!pq.isEmpty()) {
            TraversalPattern p = pq.poll();
            result.add(p);
            for (TraversalPattern neighbor : adj.get(p)) {
                inDegree.put(neighbor, inDegree.get(neighbor) - 1);
                if (inDegree.get(neighbor) == 0) {
                    pq.add(neighbor);
                }
            }
        }

        if (result.size() < n) {
            Set<TraversalPattern> resultSet = new HashSet<>(result);
            for (TraversalPattern p : patterns) {
                if (!resultSet.contains(p)) {
                    result.add(p);
                }
            }
        }

        return result;
    }

    public static class ChapterUnit {
        private List<String> contents = new ArrayList<>();
        private Set<Node> mains = new HashSet<>();
        private Set<Node> sides = new HashSet<>();

        public void append(String content) {
            contents.add(content);
        }

        void append(List<String> contents) {
            this.contents.addAll(contents);
        }

        public String getContent() {
            return String.join("\n", contents);
        }

        public Set<Node> getMains() {
            return mains;
        }

        void addMains(Set<Node> mains) {
            this.mains.addAll(mains);
            this.sides = this.sides.stream().filter(side -> !this.mains.contains(side)).collect(Collectors.toSet());
        }

        public Set<Node> getSides() {
            return sides;
        }

        void addSides(Set<Node> sides) {
            this.sides.addAll(sides.stream().filter(side -> !this.mains.contains(side)).toList());
        }

        public int lines() {
            return getContent().split("\n").length;
        }
    }
}