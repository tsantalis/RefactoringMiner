package org.refactoringminer.astDiff.graph.cluster.traverse;

import com.github.gumtreediff.tree.Tree;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.refactoringminer.astDiff.graph.Edge;
import org.refactoringminer.astDiff.graph.Node;
import org.refactoringminer.astDiff.graph.NodeType;
import org.refactoringminer.astDiff.graph.cluster.GraphWrapper;
import org.jgrapht.Graph;

import javax.annotation.Nullable;
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

    public String extended(List<TraversalPattern> filterPatterns) {
        return String.join("\n", getElements(filterPatterns).stream().map(NarrativeElement::getContent).toList());
    }

    public List<NarrativeElement> getElements(List<TraversalPattern> filterPatterns) {
        List<Node> aggMains = getMains();

        List<MergeGroup> allGroups = new ArrayList<>();

        List<MergeGroup> mappingGroups = aggregateByMapping(aggMains);
        List<MergeGroup> contextMappingGroups = aggregateByContextMapping(aggMains);

        for (MergeGroup mappingGroup : mappingGroups) {
            boolean covered = contextMappingGroups.stream().anyMatch(contextGroup ->
                    contextGroup.nodes().size() > mappingGroup.nodes().size()
                            && contextGroup.nodes().containsAll(mappingGroup.nodes()));
            if (!covered) {
                allGroups.add(mappingGroup);
            }
        }
        for (MergeGroup contextGroup : contextMappingGroups) {
            boolean covered = mappingGroups.stream().anyMatch(mappingGroup ->
                    mappingGroup.nodes().containsAll(contextGroup.nodes()));
            if (!covered) {
                allGroups.add(contextGroup);
            }
        }

        // Nodes without any semantic context (e.g. method declaration and above)
        List<Node> remainingNodes = aggMains.stream().filter(main -> allGroups.stream().noneMatch(mg -> mg.nodes().contains(main))).toList();
        List<MergeGroup> remainingGroups = remainingNodes.stream().map(rm -> new MergeGroup(Set.of(rm), MergeType.REMAINING)).toList();
        allGroups.addAll(remainingGroups);

        allGroups.sort(Comparator.comparing(MergeGroup::mergeType)
                .thenComparing(mg -> MergeGroup.sortNodes(mg.nodes()).get(0), Node.COMPARATOR));

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
            for (Node n : mg.nodes()) {
                mainToGroup.putIfAbsent(n, new HashSet<>());
                mainToGroup.get(n).add(mg);
            };
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
        localPrintingSidesMap.replaceAll((mg, sides) -> {
            Map<Node, Integer> changeIndex = changeIndex(mg);
            Comparator<Node> byEarliestUsingChange = Comparator.comparingInt(side ->
                    printingSidesToMains.get(side).stream()
                            .mapToInt(main -> changeIndex.getOrDefault(main, Integer.MAX_VALUE))
                            .min().orElse(Integer.MAX_VALUE));
            return sides.stream()
                    .sorted(byEarliestUsingChange.thenComparing(Node.COMPARATOR))
                    .toList();
        });

        Map<MergeGroup, Integer> groupLatestIndex = new HashMap<>();
        for (MergeGroup mg : allGroups) {
            int maxIdx = -1;

            for (Node n : mg.nodes()) {
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
                    String content = "<dependency>\n    " + s.baseXml(clusterGraph).replace("\n", "\n    ") + "\n</dependency>";
                    elements.add(new NarrativeElement(content, Set.of(s), clusterGraph));
                    outputtedSides.add(s);
                }
            }
            for (MergeGroup mg : allGroups) {
                if (groupLatestIndex.get(mg) == i && !outputtedGroups.contains(mg)) {
                    String content = buildSubChapterXml(mg, leaves, localPrintingSidesMap.get(mg));
                    Set<Node> mgMains = mg.nodes();
                    elements.add(new NarrativeElement(content, mgMains, clusterGraph));
                    outputtedGroups.add(mg);
                }
            }
        }

        return elements;
    }

    private static Map<Set<Node>, Set<Node>> aggregateByContextMapping(List<Node> nodes, Graph<Node, Edge> graph) {
        Set<Node> contexts = new HashSet<>();
        for (Node node : nodes) {
            List<Node> semanticContexts = node.getSemanticContexts(graph);
            if (semanticContexts.isEmpty()) {
                continue;
            }

            contexts.add(semanticContexts.get(0));
        }

        Set<Node> parentContexts = new HashSet<>();
        for (Node subject : contexts) {
            boolean isParent = true;

            List<Node> subjectAlternatives = subject.isSrc() ? subject.getMappingTargets(graph) : subject.getMappingSources(graph);
            Node subjectAlternative = subjectAlternatives.isEmpty() ? null : subjectAlternatives.get(0);
            for (Node object : contexts) {
                if (subject.equals(object)) {
                    continue;
                }

                if (subject.isDescendantOf(object)) {
                    isParent = false;
                    break;
                }

                if (subject.getSrcDst().equals(object.getSrcDst())) {
                    continue;
                }

                if (subjectAlternative == null) {
                    continue;
                }

                if (subjectAlternative.isDescendantOf(object)) {
                    isParent = false;
                    break;
                }
            }

            if (isParent) {
                parentContexts.add(subject);
            }
        }

        Map<Set<Node>, Set<Node>> result = new HashMap<>();
        for (Node parentContext : parentContexts) {
            Set<Node> descendants = new HashSet<>();
            for (Node node : nodes) {
                if (node.isDescendantOf(parentContext)) {
                    descendants.add(node);
                }
            }

            List<Node> alternatives = parentContext.isSrc() ? parentContext.getMappingTargets(graph) : parentContext.getMappingSources(graph);
            Node alternative = alternatives.isEmpty() ? null : alternatives.get(0);
            Set<Node> alternativeDescendants = new HashSet<>();
            if (alternative != null) {
                for (Node node : nodes) {
                    if (node.isDescendantOf(alternative)) {
                        alternativeDescendants.add(node);
                    }
                }
            }

            Set<Node> parents = new HashSet<>();
            parents.add(parentContext);
            if (!alternativeDescendants.isEmpty()) {
                parents.add(alternative);
            }
            Set<Node> allDescendants = new HashSet<>();
            allDescendants.addAll(descendants);
            allDescendants.addAll(alternativeDescendants);

            result.put(parents, allDescendants);
        }

        return result;
    }

    private List<MergeGroup> aggregateByContextMapping(List<Node> nodes) {
        return aggregateByContextMapping(nodes, clusterGraph).values().stream()
                .map(aggregatedNodes -> new MergeGroup(aggregatedNodes, MergeType.CONTEXT)).toList();
    }

    protected static Map<Set<Node>, Set<Node>> aggregateByMapping(List<Node> nodes, Graph<Node, Edge> graph) {
        Set<Node> allowedNodes = new HashSet<>(nodes);
        for (Node node : nodes) {
            allowedNodes.addAll(node.getMappingSources(graph));
            allowedNodes.addAll(node.getMappingTargets(graph));
        }

        Map<Set<Node>, Set<Node>> result = new HashMap<>();

        Set<Node> visited = new HashSet<>();
        for (Node node : nodes) {
            if (visited.contains(node)) continue;

            Set<Node> component = new HashSet<>();
            Queue<Node> queue = new LinkedList<>();
            queue.add(node);
            visited.add(node);

            while (!queue.isEmpty()) {
                Node curr = queue.poll();
                component.add(curr);

                for (Node src : curr.getMappingSources(graph)) {
                    if (!visited.contains(src)) {
                        visited.add(src);
                        queue.add(src);
                    }
                }
                for (Node dst : curr.getMappingTargets(graph)) {
                    if (!visited.contains(dst)) {
                        visited.add(dst);
                        queue.add(dst);
                    }
                }
            }

            Set<Node> srcNodes = new HashSet<>();
            Set<Node> dstNodes = new HashSet<>();
            for (Node n : component) {
                if (!allowedNodes.contains(n)) continue;
                if (n.isSrc()) srcNodes.add(n);
                else dstNodes.add(n);
            }

            if (!srcNodes.isEmpty() && !dstNodes.isEmpty()) {
                result.put(srcNodes, dstNodes);
            }
        }

        return result;
    }

    protected List<MergeGroup> aggregateByMapping(List<Node> nodes) {
        return aggregateByMapping(nodes, clusterGraph).entrySet().stream().map(e -> {
            Set<Node> mergedNodes = new HashSet<>();
            mergedNodes.addAll(e.getKey());
            mergedNodes.addAll(e.getValue());
            return new MergeGroup(mergedNodes, MergeType.MAPPING);
        }).toList();
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

    private String buildSubChapterXml(MergeGroup mergeGroup, List<TraversalPattern> leaves, @Nullable List<Node> localSides) {
        StringBuilder sb = new StringBuilder();
        sb.append("<sub_chapter>");

        switch (mergeGroup.mergeType) {
            case MAPPING -> {
                sb.append("\n    ").append(buildXmlMappingHunk(mergeGroup.sources(), mergeGroup.targets()).replace("\n", "\n    "));
                List<Set<Node>> contexts = orderContexts(mergeGroup);
                if (!contexts.isEmpty()) {
                    sb.append("\n    ").append(buildXmlContext(contexts).replace("\n", "\n    "));
                }
            }
            case CONTEXT -> {
                List<Set<Node>> contexts = orderContexts(mergeGroup);
                if (!contexts.isEmpty()) {
                    sb.append("\n    ").append(buildXmlContext(contexts).replace("\n", "\n    "));
                }
                List<Node> sources = mergeGroup.sources();
                if (!sources.isEmpty()) {
                    sb.append("\n    ").append(buildXmlMappingHunk(sources, List.of()).replace("\n", "\n    "));
                }
                List<Node> targets = mergeGroup.targets();
                if (!targets.isEmpty()) {
                    sb.append("\n    ").append(buildXmlMappingHunk(List.of(), targets).replace("\n", "\n    "));
                }
            }
            case REMAINING -> {
                sb.append("\n    ").append(buildXmlMappingHunk(mergeGroup.sources(), mergeGroup.targets()).replace("\n", "\n    "));
            }
        }

        if (localSides != null && !localSides.isEmpty()) {
            sb.append("\n    <dependencies>");
            for (Node side : localSides) {
                sb.append("\n        ").append(side.baseXml(clusterGraph).replace("\n", "\n        "));
            }
            sb.append("\n    </dependencies>");
        }

        sb.append("\n</sub_chapter>");
        return sb.toString();
    }

    private String buildXmlMappingHunk(List<Node> sources, List<Node> targets) {
        StringBuilder xmlOutput = new StringBuilder();
        xmlOutput.append("<change>");
        if (!sources.isEmpty()) {
            xmlOutput.append("\n    ");
            xmlOutput.append(String.join("\n    ", sources.stream()
                    .map(n -> n.baseXml(clusterGraph).replace("\n", "\n    ")).toList()));
        }
        if (!targets.isEmpty()) {
            xmlOutput.append("\n    ");
            xmlOutput.append(String.join("\n    ", targets.stream()
                    .map(n -> n.baseXml(clusterGraph).replace("\n", "\n    ")).toList()));
        }
        xmlOutput.append("\n</change>");
        return xmlOutput.toString();
    }

    private static Map<Node, Integer> changeIndex(MergeGroup mergeGroup) {
        List<Node> printedChanges = new ArrayList<>(mergeGroup.sources());
        printedChanges.addAll(mergeGroup.targets());

        Map<Node, Integer> changeIndex = new HashMap<>();
        for (int i = 0; i < printedChanges.size(); i++) {
            changeIndex.put(printedChanges.get(i), i);
        }
        return changeIndex;
    }

    private List<Set<Node>> orderContexts(MergeGroup mergeGroup) {
        Map<Set<Node>, Set<Node>> contextAggregatedNodes =
                aggregateByContextMapping(mergeGroup.nodes().stream().toList(), clusterGraph);
        if (contextAggregatedNodes.isEmpty()) {
            return List.of();
        }

        Map<Node, Integer> changeIndex = changeIndex(mergeGroup);

        Comparator<Map.Entry<Set<Node>, Set<Node>>> byEarliestChange =
                Comparator.comparingInt(entry -> entry.getValue().stream()
                        .mapToInt(node -> changeIndex.getOrDefault(node, Integer.MAX_VALUE))
                        .min().orElse(Integer.MAX_VALUE));
        Comparator<Map.Entry<Set<Node>, Set<Node>>> byContextPosition =
                Comparator.comparing((Map.Entry<Set<Node>, Set<Node>> entry) -> MergeGroup.sortNodes(entry.getKey()).get(0),
                        Node.COMPARATOR);

        return contextAggregatedNodes.entrySet().stream()
                .sorted(byEarliestChange.thenComparing(byContextPosition))
                .map(Map.Entry::getKey)
                .toList();
    }

    private String buildXmlContext(Collection<Set<Node>> contextsSets) {
        return String.join("\n", contextsSets.stream().map(contexts -> {
            StringBuilder xmlOutput = new StringBuilder();
            xmlOutput.append("<context>");
            xmlOutput.append("\n    ").append(contexts.iterator().next().mappingXml(clusterGraph).replace("\n", "\n    "));
            xmlOutput.append("\n</context>");
            return xmlOutput.toString();
        }).toList());
    }

    protected record MergeGroup(Set<Node> nodes, MergeType mergeType) {
        public List<Node> sources() {
            return sortNodes(nodes.stream().filter(Node::isSrc).collect(Collectors.toSet()));
        }

        public List<Node> targets() {
            return sortNodes(nodes.stream().filter(Node::isDst).collect(Collectors.toSet()));
        }

        static List<Node> sortNodes(Set<Node> nodes) {
            return nodes.stream().sorted(Node.COMPARATOR).toList();
        }
    }

    protected enum MergeType {
        MAPPING,
        CONTEXT,
        REMAINING
    }
}
