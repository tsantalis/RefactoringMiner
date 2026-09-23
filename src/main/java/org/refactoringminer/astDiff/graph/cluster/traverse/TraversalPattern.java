package org.refactoringminer.astDiff.graph.cluster.traverse;

import com.github.gumtreediff.tree.Tree;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.refactoringminer.astDiff.graph.Edge;
import org.refactoringminer.astDiff.graph.Node;
import org.refactoringminer.astDiff.graph.NodeType;
import org.refactoringminer.astDiff.graph.cluster.GraphWrapper;
import org.jgrapht.Graph;
import org.refactoringminer.astDiff.graph.cluster.representation.MergeGroup;
import org.refactoringminer.astDiff.graph.cluster.representation.Representation;

import java.util.*;

public class TraversalPattern extends GraphWrapper {

    protected Graph<Node, Edge> clusterGraph;
    protected final Util util = new Util(getGraph());
    protected final Set<String> identifiers = new HashSet<>();
    private final Narrator narrator = new Narrator(this);
    protected Node cachedLead = null;
    protected NodeType nodeType;
    private List<TraversalPattern> cachedFlatten = null;
    private SortKey cachedSortKey;

    public void setClusterGraph(Graph<Node, Edge> clusterGraph) {
        this.clusterGraph = clusterGraph;
    }

    public Narrator getNarrator() {
        return narrator;
    }

    public List<NarrativeElement> getElements(List<TraversalPattern> filterPatterns, int length) {
        List<Node> aggMains = getMains();

        List<MergeGroup> allGroups = MergeGroup.getAllMergeGroups(aggMains, clusterGraph);
        allGroups.sort(Comparator.comparing(mg -> MergeGroup.sortNodes(mg.nodes).get(0), Node.COMPARATOR));

        List<TraversalPattern> leaves = this.getNarrator().getNarrative(GrainLevel.LEAF);

        Set<Node> allMains = new HashSet<>(aggMains);
        if (filterPatterns != null) {
            for (TraversalPattern fp : filterPatterns) {
                allMains.addAll(fp.getMains());
            }
        }
        Map<Node, Set<Node>> printingSidesToMains = new HashMap<>();
        for (TraversalPattern leaf : leaves) {
            List<Node> leafMains = leaf.getMains();
            List<Node> leafSides = leaf.getSides();
            for (Node side : leafSides) {
                if ((side.isContext() && !side.getNodeType().equals(NodeType.SEMANTIC_CONTEXT)) || allMains.contains(side)) {
                    continue;
                }

                printingSidesToMains.putIfAbsent(side, new HashSet<>());
                Set<Node> relyingMains = printingSidesToMains.get(side);
                for (Node m : leafMains) {
                    if (allMains.contains(m)) {
                        relyingMains.add(m);
                    }
                }
            }
        }

        Map<Node, Set<MergeGroup>> mainToGroup = new HashMap<>();
        for (MergeGroup mg : allGroups) {
            for (Node n : mg.nodes) {
                mainToGroup.putIfAbsent(n, new HashSet<>());
                mainToGroup.get(n).add(mg);
            }
        }
        Set<Node> globalPrintingSides = new HashSet<>();
        Map<MergeGroup, List<Node>> localPrintingSidesMap = new HashMap<>();
        for (Map.Entry<Node, Set<Node>> entry : printingSidesToMains.entrySet()) {
            Node printingSide = entry.getKey();
            Set<Node> mains = entry.getValue();
            Set<MergeGroup> groups = new HashSet<>();
            for (Node m : mains) {
                Set<MergeGroup> mgs = mainToGroup.get(m);
                if (mgs != null) {
                    groups.addAll(mgs);
                }
            }

            if (groups.size() > 1) {
                globalPrintingSides.add(printingSide);
            } else if (groups.size() == 1) {
                MergeGroup mg = groups.iterator().next();
                localPrintingSidesMap.putIfAbsent(mg, new ArrayList<>());
                localPrintingSidesMap.get(mg).add(printingSide);
            }
        }

        Map<MergeGroup, Integer> groupLatestIndex = new HashMap<>();
        for (MergeGroup mg : allGroups) {
            int maxIdx = -1;

            for (Node n : mg.nodes) {
                for (int i = leaves.size() - 1; i >= 0; i--) {
                    if (leaves.get(i).getMains().contains(n)) {
                        maxIdx = Math.max(maxIdx, i);
                        break;
                    }
                }
            }

            groupLatestIndex.put(mg, maxIdx);
        }

        // Produce Elements
        List<NarrativeElement> elements = new ArrayList<>();
        Set<Node> outputtedSides = new HashSet<>();
        Set<MergeGroup> outputtedGroups = new HashSet<>();
        for (int i = 0; i < leaves.size(); i++) {
            TraversalPattern leaf = leaves.get(i);
            for (Node s : leaf.getSides()) {
                if (globalPrintingSides.contains(s) && !outputtedSides.contains(s)) {
                    elements.add(Representation.DEFAULT.dependency(s, clusterGraph));
                    outputtedSides.add(s);
                }
            }
            for (MergeGroup mg : allGroups) {
                if (groupLatestIndex.get(mg) == i && !outputtedGroups.contains(mg)) {
                    elements.add(Representation.DEFAULT.mergeGroup(mg, leaves, localPrintingSidesMap.get(mg), length, clusterGraph));
                    outputtedGroups.add(mg);
                }
            }
        }

        return elements;
    }

    public Node getLead() {
        if (cachedLead == null) {
            cachedLead = getGraph().vertexSet().iterator().next();
        }

        return cachedLead;
    }

    public String getId() {
        Tree tree = getLead().getTree();
        return getClass().getSimpleName() + "-" + tree.getPos() + '-' + tree.getEndPos() + '-' + System.identityHashCode(this);
    }

    public JsonObject stringify() {
        JsonObject nodeObj = new JsonObject();

        nodeObj.addProperty("id", getId());
        nodeObj.addProperty("nodeType", nodeType.name());

        if (!identifiers.isEmpty()) {
            JsonArray identifiersArr = new JsonArray();
            for (String identifier : identifiers) {
                identifiersArr.add(identifier);
            }

            nodeObj.add("identifiers", identifiersArr);
        }

        return nodeObj;
    }

    public Set<Node> vertexSet() {
        return getGraph().vertexSet();
    }

    public void addIdentifier(String identifier) {
        this.identifiers.add(identifier);
    }

    public List<Node> getMains() {
        return List.of(getLead());
    }

    public List<Node> getSides() {
        return List.of();
    }

    public boolean dependsOn(TraversalPattern p) {
        return dependsOnRecursive(p, new HashSet<>());
    }

    private boolean dependsOnRecursive(TraversalPattern p, Set<TraversalPattern> visited) {
        if (visited.contains(this)) {
            return false;
        }
        visited.add(this);

        if (this.flatten().contains(p)) {
            return true;
        }

        for (TraversalPattern tp : this.flatten()) {
            if (tp instanceof UsagePattern usage) {
                for (TraversalPattern req : usage.subs) {
                    if (req.dependsOnRecursive(p, visited)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    private List<TraversalPattern> flatten() {
        if (cachedFlatten == null) {
            List<TraversalPattern> result = new ArrayList<>();
            Set<TraversalPattern> visited = new HashSet<>();
            flattenRecursive(this, visited, result);
            cachedFlatten = List.copyOf(result);
        }
        return cachedFlatten;
    }

    private void flattenRecursive(TraversalPattern p, Set<TraversalPattern> visited, List<TraversalPattern> result) {
        if (visited.contains(p)) return;
        visited.add(p);

        // UsagePattern subs lead to cross dependency which contradicts the idea of "dependsOn"
        if (p instanceof AggregatorPattern agg && !(p instanceof UsagePattern)) {
            for (TraversalPattern sub : agg.subs) {
                flattenRecursive(sub, visited, result);
            }
        }

        result.add(p);
    }

    public SortKey sortKey() {
        if (cachedSortKey == null) {
            List<Node> mains = flatten().stream()
                    .filter(fl -> fl instanceof Leaf).map(TraversalPattern::getMains).flatMap(List::stream).toList();
            cachedSortKey = mains.stream()
                    .map(n -> new SortKey(n.getPath(), n.getTree().getPos()))
                    .min(Comparator.naturalOrder())
                    .orElse(new SortKey("", 0));
        }
        return cachedSortKey;
    }

    public record SortKey(String path, int pos) implements Comparable<SortKey> {
        private static final Comparator<SortKey> COMPARATOR = Comparator.comparing(SortKey::path)
                .thenComparingInt(SortKey::pos);

        @Override
        public int compareTo(SortKey other) {
            return COMPARATOR.compare(this, other);
        }
    }
}
