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

import java.util.*;
import java.util.stream.Collectors;

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

    public List<NarrativeElement> getElements() {
        List<NarrativeElement> elements = new ArrayList<>();

        List<Node> aggMains = getMains();

        List<MergeGroup> allGroups = MergeGroup.getAllMergeGroups(aggMains, clusterGraph);
        allGroups.sort(Comparator.comparing(mg -> MergeGroup.sortNodes(mg.nodes).get(0), Node.COMPARATOR));

        List<TraversalPattern> leaves = this.getNarrator().getNarrative(GrainLevel.LEAF);

        Map<Node, Set<Node>> mainsToSides = new HashMap<>();
        for (TraversalPattern leaf : leaves) {
            Set<Node> validSides = leaf.getSides().stream()
                    .filter(side -> !side.isContext() || side.getNodeType().equals(NodeType.SEMANTIC_CONTEXT))
                    .collect(Collectors.toSet());
            if (validSides.isEmpty()) {
                continue;
            }

            for (Node main : leaf.getMains()) {
                if (!aggMains.contains(main)) {
                    continue;
                }
                mainsToSides.computeIfAbsent(main, k -> new HashSet<>()).addAll(validSides);
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
        Set<MergeGroup> outputtedGroups = new HashSet<>();
        for (int i = 0; i < leaves.size(); i++) {
            for (MergeGroup mg : allGroups) {
                if (groupLatestIndex.get(mg) != i || outputtedGroups.contains(mg)) {
                    continue;
                }

                Map<Node, Set<Node>> groupMainsToSides = new HashMap<>();
                for (Node main : mg.nodes) {
                    groupMainsToSides.putIfAbsent(main, new HashSet<>());

                    Set<Node> mainToSides = mainsToSides.get(main);
                    if (mainToSides == null) {
                        continue;
                    }
                    groupMainsToSides.get(main).addAll(mainToSides);
                }

                elements.add(new NarrativeElement(groupMainsToSides, leaves, clusterGraph));

                outputtedGroups.add(mg);
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
