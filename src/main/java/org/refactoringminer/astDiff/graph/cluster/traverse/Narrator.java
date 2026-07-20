package org.refactoringminer.astDiff.graph.cluster.traverse;

import org.refactoringminer.astDiff.graph.Edge;
import org.refactoringminer.astDiff.graph.Node;
import org.refactoringminer.astDiff.graph.NodeType;
import org.jgrapht.Graph;
import org.refactoringminer.astDiff.utils.Constants;

import java.util.*;
import java.util.function.Predicate;

public class Narrator {
    public static final int THRESHOLD = 1000;
    private static final Map<GrainLevel, Set<String>> GRAIN_LEVEL_TYPES = new HashMap<>();

    static {
        Constants c = new Constants("");
        GRAIN_LEVEL_TYPES.put(GrainLevel.METHOD, Set.of(c.METHOD_DECLARATION));
        GRAIN_LEVEL_TYPES.put(GrainLevel.CLASS, Set.of(c.TYPE_DECLARATION, c.INTERFACE_DECLARATION, c.CLASS_DECLARATION, c.ENUM_DECLARATION, c.RECORD_DECLARATION, c.ANNOTATION_TYPE_DECLARATION));
        GRAIN_LEVEL_TYPES.put(GrainLevel.FILE, Set.of(c.COMPILATION_UNIT, c.PROGRAM, c.MODULE, c.SOURCE_FILE, c.TRANSLATION_UNIT));
    }

    private final TraversalPattern rootPattern;
    private final Map<GrainLevel, List<TraversalPattern>> cache = new HashMap<>();
    private final Map<GrainLevel, List<String>> flatCache = new HashMap<>();
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
            case SINGLE -> traverse(rootPattern, visited, result, pp -> pp.clusterGraph != null, pp -> pp instanceof Leaf);
            case LEAF -> traverse(rootPattern, visited, result, pp -> false, pp -> pp instanceof Leaf);
            case USAGE_CHAIN_ROOT -> {
                Set<UsagePattern> roots = findUsageRoots(rootPattern);
                traverse(rootPattern, visited, result,
                        pp -> pp instanceof UsagePattern u && roots.contains(u),
                        pp -> pp instanceof Leaf && (!(pp instanceof UsagePattern) || roots.contains((UsagePattern) pp)));
            }
            case SEMANTIC_LEAF -> traverse(rootPattern, visited, result,
                    pp -> pp instanceof TraversalComponent tc && isSemanticLeaf(tc),
                    pp -> pp instanceof Leaf);
            case SEMANTIC_ROOT -> traverse(rootPattern, visited, result,
                    pp -> pp instanceof TraversalComponent tc && isSemanticRoot(tc),
                    pp -> pp instanceof Leaf);
            case METHOD, CLASS, FILE -> traverse(rootPattern, visited, result,
                    pp -> pp instanceof TraversalComponent tc && matchesGrain(tc, grainLevel),
                    pp -> pp instanceof Leaf);
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

    public static boolean isSemanticLeaf(TraversalComponent tc) {
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

    public static boolean isSemanticRoot(TraversalComponent tc) {
        if (tc.getMergeContexts() == null || tc.getMergeContexts().isEmpty()) return false;

        for (Node node : tc.getMergeContexts()) {
            if (node.getNodeType() != NodeType.SEMANTIC_CONTEXT) {
                return false;
            }
        }

        return true;
    }

    public static boolean matchesGrain(TraversalComponent tc, GrainLevel grainLevel) {
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

    public static void traverse(TraversalPattern p, Set<TraversalPattern> visited, List<TraversalPattern> result, Predicate<TraversalPattern> stopPredicate, Predicate<TraversalPattern> leafPredicate) {
        if (visited.contains(p)) return;
        visited.add(p);

        if (stopPredicate.test(p)) {
            result.add(p);
            return;
        }

        if (p instanceof AggregatorPattern agg) {
            List<TraversalPattern> sortedSubs = new ArrayList<>(agg.subs);
            sortedSubs.sort((s1, s2) -> {
                boolean s1DependsOnS2 = s1.dependsOn(s2);
                boolean s2DependsOnS1 = s2.dependsOn(s1);
                if (s1DependsOnS2 && !s2DependsOnS1) {
                    return 1;
                }
                if (s2DependsOnS1 && !s1DependsOnS2) {
                    return -1;
                }

                int d1 = s1.getDepth();
                int d2 = s2.getDepth();
                if (d1 != d2) {
                    return Integer.compare(d2, d1);
                }

                List<Node> mains1 = s1.getMains();
                List<Node> mains2 = s2.getMains();

                int points1 = 0;
                int points2 = 0;

                for (Node m1 : mains1) {
                    for (Node m2 : mains2) {
                        if (m1.getPath().equals(m2.getPath())) {
                            if (m1.getTree().getPos() < m2.getTree().getPos()) {
                                points1++;
                            } else if (m2.getTree().getPos() < m1.getTree().getPos()) {
                                points2++;
                            }
                        }
                    }
                }
                return Integer.compare(points2, points1);
            });
            for (TraversalPattern sub : sortedSubs) {
                traverse(sub, visited, result, stopPredicate, leafPredicate);
            }
        }

        if (leafPredicate.test(p)) {
            result.add(p);
        }
    }


    public List<String> getFlatChapters(GrainLevel level) {
        if (flatCache.containsKey(level)) {
            return flatCache.get(level);
        }

        List<TraversalPattern> chapters = getNarrative(level);
        if (chapters == null) return Collections.emptyList();

        // 1. Expand original chapters into atomic units
        List<ChapterUnit> units = new ArrayList<>();
        for (int i = 0; i < chapters.size(); i++) {
            TraversalPattern chapter = chapters.get(i);
            Graph<Node, Edge> clusterGraph = chapter.getClusterGraph();
            if (clusterGraph == null) {
                units.add(new ChapterUnit(String.format("[Chapter %d of %d]: Error: Could not find associated cluster graph.\n\n", i + 1, chapters.size()), 0, i + 1));
                continue;
            }

            List<TraversalPattern> filterPatterns = i > 0 ? chapters.subList(0, i) : Collections.emptyList();

            if (chapter instanceof AggregatorPattern agg) {
                List<NarrativeElement> elements = agg.getElements(clusterGraph, filterPatterns);
                int totalLines = elements.stream().mapToInt(NarrativeElement::lineCount).sum();

                if (totalLines > THRESHOLD) {
                    List<List<NarrativeElement>> splits = createBalancedSplits(elements);
                    for (int s = 0; s < splits.size(); s++) {
                        String content = String.join("\n", splits.get(s).stream().map(NarrativeElement::content).toList());
                        units.add(new ChapterUnit(content, content.split("\n").length, i + 1));
                    }
                } else {
                    String content = agg.extended(clusterGraph, level, filterPatterns);
                    units.add(new ChapterUnit(content, content.split("\n").length, i + 1));
                }
            } else {
                String content = chapter.extended(clusterGraph, level, filterPatterns);
                units.add(new ChapterUnit(content, content.split("\n").length, i + 1));
            }
        }

        // 2. Merge units into flat chapters
        List<List<ChapterUnit>> mergedGroups = new ArrayList<>();
        List<ChapterUnit> currentGroup = new ArrayList<>();
        int currentSum = 0;

        for (ChapterUnit unit : units) {
            if (currentGroup.isEmpty() || (currentSum + unit.lines <= THRESHOLD)) {
                currentGroup.add(unit);
                currentSum += unit.lines;
            } else {
                mergedGroups.add(currentGroup);
                currentGroup = new ArrayList<>();
                currentGroup.add(unit);
                currentSum = unit.lines;
            }
        }
        if (!currentGroup.isEmpty()) {
            mergedGroups.add(currentGroup);
        }

        // 3. Final formatting
        List<String> flatChapters = new ArrayList<>();
        for (List<ChapterUnit> group : mergedGroups) {
            flatChapters.add(String.join("\n", group.stream().map(subGroup -> subGroup.content).toList()));
        }

        flatCache.put(level, flatChapters);
        return flatChapters;
    }

    private List<List<NarrativeElement>> createBalancedSplits(List<NarrativeElement> elements) {
        int totalLines = elements.stream().mapToInt(NarrativeElement::lineCount).sum();
        if (totalLines <= THRESHOLD) return List.of(elements);
        for (int n = 2; n <= elements.size(); n++) {
            List<List<NarrativeElement>> splits = splitIntoN(elements, n);
            boolean anyBelow = splits.stream().anyMatch(s -> s.stream().mapToInt(NarrativeElement::lineCount).sum() <= THRESHOLD);
            if (anyBelow) return splits;
        }
        return List.of(elements);
    }

    private List<List<NarrativeElement>> splitIntoN(List<NarrativeElement> elements, int n) {
        List<List<NarrativeElement>> splits = new ArrayList<>();
        int totalLines = elements.stream().mapToInt(NarrativeElement::lineCount).sum();
        double target = (double) totalLines / n;
        int currentStart = 0;
        for (int i = 0; i < n - 1; i++) {
            int bestEnd = currentStart;
            double minDiff = Double.MAX_VALUE;
            double currentSum = 0;
            for (int j = currentStart; j < elements.size(); j++) {
                currentSum += elements.get(j).lineCount();
                double diff = Math.abs(currentSum - target);
                if (diff < minDiff) {
                    minDiff = diff;
                    bestEnd = j + 1;
                } else {
                    break;
                }
            }
            splits.add(new ArrayList<>(elements.subList(currentStart, bestEnd)));
            currentStart = bestEnd;
        }
        splits.add(new ArrayList<>(elements.subList(currentStart, elements.size())));
        return splits;
    }

    public int getProgress(GrainLevel grainLevel) {
        return progressMap.getOrDefault(grainLevel, 0);
    }

    public void incrementProgress(GrainLevel grainLevel) {
        progressMap.put(grainLevel, getProgress(grainLevel) + 1);
    }

    public List<TraversalPattern> sortPatterns(List<TraversalPattern> patterns) {
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

    private record ChapterUnit(String content, int lines, int originalIdx) {
    }

}
