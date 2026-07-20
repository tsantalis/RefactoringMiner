package org.refactoringminer.astDiff.graph.cluster.traverse;

import com.github.gumtreediff.tree.Tree;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.refactoringminer.astDiff.graph.Edge;
import org.refactoringminer.astDiff.graph.Node;
import org.refactoringminer.astDiff.graph.NodeType;
import org.refactoringminer.astDiff.graph.cluster.GraphWrapper;
import org.jgrapht.Graph;

import java.util.*;

public class TraversalPattern extends GraphWrapper {
    protected Graph<Node, Edge> clusterGraph;
    protected final Util util = new Util(getGraph());
    protected final Set<String> identifiers = new HashSet<>();
    private final Narrator narrator = new Narrator(this);
    private final Map<TraversalPattern, Boolean> dependsOnCache = new HashMap<>();
    protected Node cachedLead = null;
    protected NodeType nodeType;
    private List<TraversalPattern> cachedFlatten = null;

    public Graph<Node, Edge> getClusterGraph() {
        return clusterGraph;
    }

    public void setClusterGraph(Graph<Node, Edge> clusterGraph) {
        this.clusterGraph = clusterGraph;
    }

    public static List<MappingGroup> aggregateByMapping(Graph<Node, Edge> graph, Collection<Node> nodes) {
        Set<Node> allowedNodes = new HashSet<>(nodes);
        for (Node node : nodes) {
            allowedNodes.addAll(node.getMappingSources(graph));
            allowedNodes.addAll(node.getMappingTargets(graph));
        }

        List<MappingGroup> result = new ArrayList<>();

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

            List<Node> srcNodes = new ArrayList<>();
            List<Node> dstNodes = new ArrayList<>();
            for (Node n : component) {
                if (!allowedNodes.contains(n)) continue;
                if (n.isSrc()) srcNodes.add(n);
                else dstNodes.add(n);
            }

            // TODO: Sort by Dependency
            Comparator<Node> nodeComparator = Comparator.comparing(Node::getPath)
                    .thenComparingInt(n -> n.getTree().getPos());
            srcNodes.sort(nodeComparator);
            dstNodes.sort(nodeComparator);

            result.add(new MappingGroup(srcNodes, dstNodes));
        }

        return result;
    }

    public Narrator getNarrator() {
        return narrator;
    }

     public String extended(Graph<Node, Edge> graph, GrainLevel level, List<TraversalPattern> filterPatterns) {
        return String.join("\n", getElements(graph, filterPatterns).stream().map(NarrativeElement::content).toList());
    }

    public List<NarrativeElement> getElements(Graph<Node, Edge> graph, List<TraversalPattern> filterPatterns) {
        List<Node> aggMains = getMains();

        // Merge Mains by Mapping
        List<MappingGroup> mappingGroups = TraversalPattern.aggregateByMapping(graph, aggMains);

        // Get Group Contexts
        Map<MappingGroup, Set<Node>> mappingGroupContexts = new HashMap<>();
        for (MappingGroup mg : mappingGroups) {
            Set<Node> contexts = new HashSet<>();
            for (Node target : mg.targets()) {
                List<Node> semanticContexts = target.getSemanticContexts(graph);
                if (!semanticContexts.isEmpty()) {
                    Node semanticContext = semanticContexts.get(0);
                    contexts.add(semanticContext);
                    contexts.addAll(semanticContext.getMappingSources(graph));
                }
            }
            for (Node source : mg.sources()) {
                List<Node> semanticContexts = source.getSemanticContexts(graph);
                if (!semanticContexts.isEmpty()) {
                    Node semanticContext = semanticContexts.get(0);
                    contexts.add(semanticContext);
                    contexts.addAll(semanticContext.getMappingTargets(graph));
                }
            }
            mappingGroupContexts.put(mg, filterLargest(contexts, graph));
        }

        // Merge Groups by Context
        List<MergedGroup> mergedGroups = new ArrayList<>();
        for (Map.Entry<MappingGroup, Set<Node>> entry : mappingGroupContexts.entrySet()) {
            MappingGroup mg = entry.getKey();
            Set<Node> context = entry.getValue();
            boolean merged = false;
            for (MergedGroup mergedGroup : mergedGroups) {
                if (isCompatible(context, mergedGroup.context, graph)) {
                    mergedGroup.groups.add(mg);
                    Set<Node> union = new HashSet<>(mergedGroup.context);
                    union.addAll(context);
                    mergedGroup.context = filterLargest(union, graph);
                    merged = true;
                    break;
                }
            }
            if (!merged) {
                MergedGroup newGroup = new MergedGroup(context);
                newGroup.groups.add(mg);
                mergedGroups.add(newGroup);
            }
        }

        List<TraversalPattern> leaves = this.getNarrator().getNarrative(GrainLevel.LEAF);

        // Side to Mains Mappings
        Set<Node> allMains = new HashSet<>(aggMains);
        if (filterPatterns != null) {
            for (TraversalPattern fp : filterPatterns) {
                allMains.addAll(fp.getMains());
            }
        }
        Map<Node, Set<Node>> sideToMains = new HashMap<>();
        for (TraversalPattern leaf : leaves) {
            List<Node> leafMains = leaf.getMains();
            List<Node> leafSides = leaf.getSides();
            for (Node side : leafSides) {
                if ((side.isContext() && !side.getNodeType().equals(NodeType.SEMANTIC_CONTEXT)) || allMains.contains(side)) {
                    continue;
                }

                Set<Node> relyingMains = sideToMains.computeIfAbsent(side, k -> new HashSet<>());
                for (Node m : leafMains) {
                    if (allMains.contains(m)) {
                        relyingMains.add(m);
                    }
                }
            }
        }

        // Main to Group Mappings (Each Main Maps to Only 1 Group)
        Map<Node, MergedGroup> mainToGroup = new HashMap<>();
        for (MergedGroup mg : mergedGroups) {
            for (MappingGroup group : mg.groups) {
                for (Node n : group.sources()) mainToGroup.put(n, mg);
                for (Node n : group.targets()) mainToGroup.put(n, mg);
            }
        }

        // TODO: Sort Must be Done By Dependency, Not Latest Index
        Map<MergedGroup, Integer> groupLatestIndex = new HashMap<>();
        for (MergedGroup mg : mergedGroups) {
            int maxIdx = -1;
            for (MappingGroup group : mg.groups) {
                for (Node n : group.sources()) {
                    for (int i = leaves.size() - 1; i >= 0; i--) {
                        if (leaves.get(i).getMains().contains(n)) {
                            maxIdx = Math.max(maxIdx, i);
                        }
                    }
                }
                for (Node n : group.targets()) {
                    for (int i = leaves.size() - 1; i >= 0; i--) {
                        if (leaves.get(i).getMains().contains(n)) {
                            maxIdx = Math.max(maxIdx, i);
                        }
                    }
                }
            }
            groupLatestIndex.put(mg, maxIdx);
        }

        // Identify Group Dependencies
        Set<Node> globalSides = new HashSet<>();
        Map<MergedGroup, List<Node>> localSidesMap = new HashMap<>();
        for (Map.Entry<Node, Set<Node>> entry : sideToMains.entrySet()) {
            Node side = entry.getKey();
            Set<Node> mains = entry.getValue();
            Set<MergedGroup> chapters = new HashSet<>();
            for (Node m : mains) {
                MergedGroup mg = mainToGroup.get(m);
                if (mg != null) chapters.add(mg);
            }

            if (chapters.size() > 1) {
                globalSides.add(side);
            } else if (chapters.size() == 1) {
                MergedGroup mg = chapters.iterator().next();
                localSidesMap.computeIfAbsent(mg, k -> new ArrayList<>()).add(side);
            }
        }

        // Produce Elements
        List<NarrativeElement> elements = new ArrayList<>();
        Set<Node> outputtedSides = new HashSet<>();
        Set<MergedGroup> outputtedGroups = new HashSet<>();
        for (int i = 0; i < leaves.size(); i++) {
            TraversalPattern leaf = leaves.get(i);
            for (Node s : leaf.getSides()) {
                if (globalSides.contains(s) && !outputtedSides.contains(s)) {
                    String content = "<dependency>\n    " + s.baseXml(graph).replace("\n", "\n    ") + "\n</dependency>";
                    elements.add(new NarrativeElement(content, content.split("\n").length, NarrativeElement.ElementType.DEPENDENCY));
                    outputtedSides.add(s);
                }
            }
            for (MergedGroup mg : mergedGroups) {
                if (groupLatestIndex.get(mg) == i && !outputtedGroups.contains(mg)) {
                    String content = buildSubChapterXml(mg, graph, leaves, localSidesMap);
                    elements.add(new NarrativeElement(content, content.split("\n").length, NarrativeElement.ElementType.SUB_CHAPTER));
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

    public int getDepth() {
        if (this instanceof AggregatorPattern aggregator) {
            return aggregator.subs.stream().mapToInt(TraversalPattern::getDepth).max().orElse(0) + 1;
        }
        return 0;
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
        if (dependsOnCache.containsKey(p)) {
            return dependsOnCache.get(p);
        }

        if (visited.contains(this)) {
            return false;
        }
        visited.add(this);

        if (this.flatten().contains(p)) {
            dependsOnCache.put(p, true);
            return true;
        }

        for (TraversalPattern tp : this.flatten()) {
            if (tp instanceof UsagePattern usage) {
                for (TraversalPattern req : usage.subs) {
                    if (req.dependsOnRecursive(p, visited)) {
                        dependsOnCache.put(p, true);
                        return true;
                    }
                }
            }
        }

        dependsOnCache.put(p, false);
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

    private String buildSubChapterXml(MergedGroup mergedGroup, Graph<Node, Edge> graph, List<TraversalPattern> leaves, Map<MergedGroup, List<Node>> localSidesMap) {
        StringBuilder sb = new StringBuilder();
        sb.append("<sub_chapter>");

        for (MappingGroup mg : mergedGroup.groups) {
            sb.append("\n    ").append(buildXmlMappingHunk(mg.sources(), mg.targets(), graph).replace("\n", "\n    "));
        }

        if (!mergedGroup.context.isEmpty()) {
            sb.append("\n    <context>");
            Set<String> contextsText = new HashSet<>();
            for (Node context : mergedGroup.context) {
                contextsText.add(context.mappingXml(graph));
            }
            sb.append("\n        ").append(String.join("\n        ", contextsText.stream().map(text -> text.replace("\n", "\n        ")).toList()));
            sb.append("\n    </context>");
        }

        List<Node> localSides = localSidesMap.getOrDefault(mergedGroup, List.of());
        if (!localSides.isEmpty()) {
            sb.append("\n    <dependencies>");
            for (Node side : localSides) {
                sb.append("\n        ").append(side.baseXml(graph).replace("\n", "\n        "));
            }
            sb.append("\n    </dependencies>");
        }

        sb.append("\n</sub_chapter>");
        return sb.toString();
    }

    private String buildXmlMappingHunk(List<Node> sources, List<Node> targets, Graph<Node, Edge> graph) {
        StringBuilder xmlOutput = new StringBuilder();
        xmlOutput.append("<change>");
        if (!sources.isEmpty()) {
            xmlOutput.append("\n    ");
            xmlOutput.append(String.join("\n    ", sources.stream().map(n -> n.baseXml(graph).replace("\n", "\n    ")).toList()));
        }
        if (!targets.isEmpty()) {
            xmlOutput.append("\n    ");
            xmlOutput.append(String.join("\n    ", targets.stream().map(n -> n.baseXml(graph).replace("\n", "\n    ")).toList()));
        }
        xmlOutput.append("\n</change>");
        return xmlOutput.toString();
    }

    private boolean isCompatible(Set<Node> setA, Set<Node> setB, Graph<Node, Edge> graph) {
        if (setA.isEmpty() || setB.isEmpty()) return false;

        boolean aCoveredByB = true;
        for (Node a : setA) {
            boolean matched = false;
            for (Node b : setB) {
                if (a.equals(b) || a.getSemanticContexts(graph).contains(b) || b.getSemanticContexts(graph).contains(a)) {
                    matched = true;
                    break;
                }
            }
            if (!matched) {
                aCoveredByB = false;
                break;
            }
        }

        boolean bCoveredByA = true;
        for (Node b : setB) {
            boolean matched = false;
            for (Node a : setA) {
                if (a.equals(b) || a.getSemanticContexts(graph).contains(b) || b.getSemanticContexts(graph).contains(a)) {
                    matched = true;
                    break;
                }
            }
            if (!matched) {
                bCoveredByA = false;
                break;
            }
        }

        return aCoveredByB || bCoveredByA;
    }

    private Set<Node> filterLargest(Set<Node> contexts, Graph<Node, Edge> graph) {
        return contexts.stream().filter(ctx -> ctx.getSemanticContexts(graph).stream().noneMatch(sc -> sc != ctx && contexts.contains(sc))).collect(java.util.stream.Collectors.toSet());
    }

    public record MappingGroup(List<Node> sources, List<Node> targets) {
    }

    private static class MergedGroup {
        Set<Node> context;
        List<MappingGroup> groups = new ArrayList<>();

        MergedGroup(Set<Node> context) {
            this.context = context;
        }
    }
}
