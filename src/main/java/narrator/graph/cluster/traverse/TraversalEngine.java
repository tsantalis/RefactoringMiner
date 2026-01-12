package narrator.graph.cluster.traverse;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;
import narrator.graph.Edge;
import narrator.graph.EdgeType;
import narrator.graph.Node;
import narrator.graph.cluster.Cluster;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.jgrapht.Graph;
import org.refactoringminer.astDiff.utils.Constants;

public class TraversalEngine {

    private final Util util;
    private final List<String> unacceptedSuccessiveNodes = new ArrayList<>() {{
        add(Constants.get().TYPE_DECLARATION);
        add(Constants.get().METHOD_DECLARATION);
    }};
    private final Graph<Node, Edge> graph;
    private final List<TraversalPattern> components = new ArrayList<>();

    public TraversalEngine(Cluster cluster) {
        graph = cluster.getGraph();
        util = new Util(graph);
        process();
    }

    public List<TraversalPattern> getComponents() {
        return components;
    }

    private void process() {
        // add patterns
        addUsageComponents();
        addSuccessiveComponents();
        addSingularComponents();

        // find pattern MST
        // common non-context nodes
        mergeByStrongestCommonNodes((node) -> !node.isContext());
        // 100% similar nodes (common nodes should have higher priority than similar nodes)
        mergeSimilarNodes();
        // common context nodes
        mergeByStrongestCommonNodes(null);
    }

    private void addUsageComponents() {
        List<Node> useNodes = Centrality.usedDeclarations(graph).stream().toList();
        HashMap<Node, UsagePattern> usagePatterns = new HashMap<>();
        for (Node useNode : useNodes) {
            addUsageComponent(useNode, usagePatterns);
        }

        for (UsagePattern usagePattern : usagePatterns.values()) {
            ArrayList<UsagePattern> path = new ArrayList<>();
            path.add(usagePattern);
            breakCircularDependencies(usagePattern, path);
        }

    }

    // TODO: test and validate
    private Set<UsagePattern> breakCircularDependencies(UsagePattern usagePattern,
            List<UsagePattern> path) {
        Set<UsagePattern> ringNodes = new HashSet<>();

        HashMap<Node, UsagePattern> requirements = usagePattern.getRequirements();
        if (requirements.isEmpty()) {
            return ringNodes;
        }

        ArrayList<UsagePattern> newPath = new ArrayList<>() {{
            addAll(path);
            add(usagePattern);
        }};
        for (Entry<Node, UsagePattern> nodeRequirement : requirements.entrySet()) {
            Node node = nodeRequirement.getKey();
            UsagePattern requirement = nodeRequirement.getValue();

            if (path.contains(requirement)) {
                usagePattern.breakRequirement(node);
                ringNodes.add(requirement);
                continue;
            }

            Set<UsagePattern> requirementRingNodes = breakCircularDependencies(requirement,
                    newPath);
            if (!requirementRingNodes.isEmpty()) {
                usagePattern.breakRequirement(node);
                ringNodes.addAll(requirementRingNodes);
            }
        }

        ringNodes.remove(usagePattern);
        return ringNodes;
    }

    private void addUsageComponent(Node node,
            HashMap<Node, UsagePattern> usagePatterns) {
        if (usagePatterns.containsKey(node)) {
            return;
        }

        UsagePattern usageComponent = new UsagePattern(node);
        addMapping(node, usageComponent);
        addContext(node, usageComponent);

        Set<Node> usedNodes = util.getUsedNodes(node);
        for (Node usedNode : usedNodes) {
            usageComponent.addEdge(usedNode, node, new Edge(EdgeType.DEF_USE));
            addMapping(usedNode, usageComponent);
            addContext(usedNode, usageComponent);

            if (util.doesUse(usedNode)) {
                addUsageComponent(usedNode, usagePatterns);

                UsagePattern usedComponent = usagePatterns.get(usedNode);
                usageComponent.addRequirement(usedNode, usedComponent);
            }
        }

        components.add(usageComponent);
        usagePatterns.put(node, usageComponent);
    }

    private void addSuccessiveComponents() {
        List<Node> acceptedNodes =
                graph.vertexSet().stream().filter(node -> !unacceptedSuccessiveNodes.contains(
                        node.getTree().getType().name)).toList();

        HashMap<Node, SuccessivePattern> successivePatterns = new HashMap<>();
        for (Node acceptedNode : acceptedNodes) {
            List<Edge> edges =
                    graph.edgesOf(acceptedNode).stream()
                            .filter(edge -> edge.getType().equals(EdgeType.SUCCESSION))
                            // TODO: should we support succession for non-changes?
                            .filter(edge -> graph.getEdgeTarget(edge).isBase()
                                    && graph.getEdgeSource(edge).isBase()).toList();
            for (Edge edge : edges) {
                SuccessivePattern successivePattern = new SuccessivePattern();

                Node source = graph.getEdgeSource(edge);
                mergeSuccessiveComponent(successivePatterns, source, successivePattern);

                Node target = graph.getEdgeTarget(edge);
                mergeSuccessiveComponent(successivePatterns, target, successivePattern);

                successivePattern.addEdge(source, target, edge);
                addMapping(source, successivePattern);
                addContext(source, successivePattern);
                addMapping(target, successivePattern);
                addContext(target, successivePattern);

                successivePatterns.put(acceptedNode, successivePattern);
                components.add(successivePattern);
            }
        }

        // successive pattern is for expanding to immediate neighbors of other patterns
        // if all nodes are already covered in other patterns, there is no value to have it
        for (SuccessivePattern successivePattern : successivePatterns.values()) {
            boolean hasUncoveredNode = false;
            for (Node node : successivePattern.vertexSet()) {
                boolean isNodeCovered = false;
                for (TraversalPattern component : components) {
                    if (!component.equals(successivePattern) && component.containsNode(node)) {
                        isNodeCovered = true;
                        break;
                    }
                }

                if (!isNodeCovered) {
                    hasUncoveredNode = true;
                    break;
                }
            }

            if (hasUncoveredNode) {
                continue;
            }

            components.remove(successivePattern);
        }
    }

    private void mergeSuccessiveComponent(HashMap<Node, SuccessivePattern> successivePatterns,
            Node sourceNode,
            SuccessivePattern targetPattern) {
        SuccessivePattern sourceComponent = successivePatterns.get(sourceNode);

        if (sourceComponent != null) {
            targetPattern.merge(sourceComponent);

            components.remove(sourceComponent);

            List<Node> sourceComponentNodes =
                    successivePatterns.entrySet().stream()
                            .filter(entry -> entry.getValue().equals(sourceComponent))
                            .map(Map.Entry::getKey).toList();
            for (Node node : sourceComponentNodes) {
                successivePatterns.put(node, targetPattern);
            }
        }
    }

    private void addContext(Node node, TraversalPattern traversalPattern) {
        List<Node> contexts = util.getContexts(node);
        Node currentNode = node;
        for (Node context : contexts) {
            traversalPattern.addEdge(currentNode, context, new Edge(EdgeType.CONTEXT), (edges) -> {
                List<Edge> duplicateEdges =
                        edges.stream().filter(edge -> edge.getType().equals(EdgeType.CONTEXT))
                                .toList();
                return duplicateEdges.isEmpty();
            });

            currentNode = context;
        }
    }

    private void addMapping(Node node, TraversalPattern traversalPattern) {
        List<Edge> incomingMappingEdges = graph.incomingEdgesOf(node).stream()
                .filter(edge -> edge.getType().equals(EdgeType.MAPPING)).toList();
        for (Edge mappingEdge : incomingMappingEdges) {
            Node edgeSource = graph.getEdgeSource(mappingEdge);
            traversalPattern.addEdge(edgeSource, node, new Edge(EdgeType.MAPPING), (edges) -> {
                List<Edge> duplicateEdges =
                        edges.stream().filter(edge -> edge.getType().equals(EdgeType.MAPPING))
                                .toList();
                return duplicateEdges.isEmpty();
            });
        }

        List<Edge> outgoingMappingEdges = graph.outgoingEdgesOf(node).stream()
                .filter(edge -> edge.getType().equals(EdgeType.MAPPING)).toList();
        for (Edge mappingEdge : outgoingMappingEdges) {
            Node edgeTarget = graph.getEdgeTarget(mappingEdge);
            traversalPattern.addEdge(node, edgeTarget, new Edge(EdgeType.MAPPING), (edges) -> {
                List<Edge> duplicateEdges =
                        edges.stream().filter(edge -> edge.getType().equals(EdgeType.MAPPING))
                                .toList();
                return duplicateEdges.isEmpty();
            });
        }
    }

    //    private List<String> singularTypes = new ArrayList<>() {{
    //        add(Constants.METHOD_DECLARATION);
    //        add(Constants.EXPRESSION_STATEMENT);
    //        add(Constants.METHOD_INVOCATION);
    //        add(Constants.RETURN_STATEMENT);
    //    }};

    // for chunks of code which are using already existing declarations, or their uses are not regular
    private void addSingularComponents() {
        List<Node> singularNodes = graph.vertexSet().stream().filter(node -> !node.isContext())
                //                        .filter(node -> singularTypes.contains(node.getTree().getType().name))
                .filter(node -> {
                    for (TraversalPattern traversalComponent : components) {
                        boolean contains = traversalComponent.containsNode(node);
                        if (contains) {
                            return false;
                        }
                    }
                    return true;
                }).toList();

        for (Node node : singularNodes) {
            SingularPattern singularComponent = new SingularPattern(node);
            addMapping(node, singularComponent);
            addContext(node, singularComponent);

            components.add(singularComponent);
        }
    }

    private void pruneUsageComponents() {
        List<UsagePattern> usageComponents = new ArrayList<>();
        for (TraversalPattern component : components) {
            if (component instanceof UsagePattern) {
                usageComponents.add((UsagePattern) component);
            }
        }

        for (UsagePattern subject : usageComponents) {
            boolean isSubjectRequirement =
                    usageComponents.stream()
                            .anyMatch(object -> object.getRequirements().containsValue(subject));
            if (isSubjectRequirement) {
                components.remove(subject);
            }
        }
    }

    private void mergeByStrongestCommonNodes(Function<Node, Boolean> nodeCondition) {
        while (true) {
            Set<Node> mostCommonNodes = null;
            for (int i = 0; i < components.size(); i++) {
                for (int j = i + 1; j < components.size(); j++) {
                    Set<Node> subjectNodes = components.get(i).vertexSet();
                    Set<Node> objectNodes = components.get(j).vertexSet();

                    Set<Node> commonNodes = new HashSet<>();
                    for (Node subjectNode : subjectNodes) {
                        if (objectNodes.contains(subjectNode) && (nodeCondition == null
                                || nodeCondition.apply(subjectNode))) {
                            commonNodes.add(subjectNode);
                        }
                    }

                    if (commonNodes.isEmpty() || (mostCommonNodes != null
                            && mostCommonNodes.size() >= commonNodes.size())) {
                        continue;
                    }

                    mostCommonNodes = commonNodes;
                }
            }

            if (mostCommonNodes == null) {
                break;
            }

            HashMap<Node, List<TraversalPattern>> commonNodesComponents = getNodesComponents(
                    mostCommonNodes);
            Set<TraversalPattern> commonComponents = getCommonComponents(
                    commonNodesComponents.values());

            TraversalComponent parentComponent = new TraversalComponent(
                    commonComponents.stream().toList(),
                    mostCommonNodes, ReasonType.COMMON);
            for (TraversalPattern component : commonComponents) {
                components.remove(component);
            }
            components.add(parentComponent);
        }
    }

    private void mergeSimilarNodes() {
        List<Edge> similarityEdges =
                graph.edgeSet().stream().filter(edge -> edge.getType().equals(EdgeType.SIMILARITY))
                        .toList();
        List<ImmutablePair<Node, Node>> similarityPairs =
                similarityEdges.stream().map(edge -> new ImmutablePair<>(graph.getEdgeSource(edge),
                        graph.getEdgeTarget(edge))).toList();

        Set<Node> nodes = new HashSet<>();
        for (ImmutablePair<Node, Node> pair : similarityPairs) {
            nodes.add(pair.getLeft());
            nodes.add(pair.getRight());
        }

        // Stage 1: merge components with similarity nodes
        while (true) {
            HashMap<Node, List<TraversalPattern>> nodesComponents = getNodesComponents(nodes);

            HashMap<TraversalPattern, HashMap<TraversalPattern, List<Node>>> componentsSimilaritySources =
                    new HashMap<>();
            for (ImmutablePair<Node, Node> pair : similarityPairs) {
                List<TraversalPattern> leftComponents = nodesComponents.get(pair.getLeft());
                List<TraversalPattern> rightComponents = nodesComponents.get(pair.getRight());

                for (TraversalPattern leftComponent : leftComponents) {
                    for (TraversalPattern rightComponent : rightComponents) {
                        if (leftComponent.equals(rightComponent)) {
                            continue;
                        }

                        if (!componentsSimilaritySources.containsKey(leftComponent)) {
                            componentsSimilaritySources.put(leftComponent, new HashMap<>());
                        }

                        HashMap<TraversalPattern, List<Node>> leftComponentSimilaritySources =
                                componentsSimilaritySources.get(leftComponent);
                        if (!leftComponentSimilaritySources.containsKey(rightComponent)) {
                            leftComponentSimilaritySources.put(rightComponent, new ArrayList<>());
                        }

                        leftComponentSimilaritySources.get(rightComponent).add(pair.getLeft());
                    }
                }
            }

            List<Node> mostSimilaritySources = null;
            TraversalPattern similaritySourceComponent = null;
            for (Map.Entry<TraversalPattern, HashMap<TraversalPattern, List<Node>>> leftComponentSimilaritySources :
                    componentsSimilaritySources.entrySet()) {
                for (Map.Entry<TraversalPattern, List<Node>> rightComponentSimilaritySources :
                        leftComponentSimilaritySources.getValue().entrySet()) {
                    List<Node> similaritySources = rightComponentSimilaritySources.getValue();
                    if (mostSimilaritySources != null
                            && similaritySources.size() <= mostSimilaritySources.size()) {
                        continue;
                    }

                    mostSimilaritySources = similaritySources;
                    similaritySourceComponent = leftComponentSimilaritySources.getKey();
                }
            }

            if (mostSimilaritySources == null) {
                break;
            }

            List<Set<TraversalPattern>> sourcesTargetComponents = new ArrayList<>();
            for (Node similaritySource : mostSimilaritySources) {
                List<Node> similarityTargets =
                        graph.outgoingEdgesOf(similaritySource).stream()
                                .filter(edge -> edge.getType().equals(EdgeType.SIMILARITY))
                                .map(edge -> graph.getEdgeTarget(edge)).toList();
                HashMap<Node, List<TraversalPattern>> similarityTargetComponents =
                        getNodesComponents(new HashSet<>(similarityTargets));

                Set<TraversalPattern> accumulatedTargetComponents = new HashSet<>();
                for (List<TraversalPattern> components : similarityTargetComponents.values()) {
                    accumulatedTargetComponents.addAll(components);
                }
                sourcesTargetComponents.add(accumulatedTargetComponents);
            }

            Set<TraversalPattern> commonComponents =
                    getCommonComponents(sourcesTargetComponents.stream()
                            .map(components -> components.stream().toList()).toList());
            List<TraversalPattern> overallComponents = Stream.concat(commonComponents.stream(),
                    Stream.of(similaritySourceComponent)).toList();

            TraversalComponent parentComponent = new TraversalComponent(overallComponents,
                    new HashSet<>(mostSimilaritySources), ReasonType.SIMILAR);
            for (TraversalPattern component : overallComponents) {
                components.remove(component);
            }
            components.add(parentComponent);
        }

        // Stage 2: Create Similarity patterns
        while (true) {
            HashMap<Node, List<TraversalPattern>> nodesComponents = getNodesComponents(nodes);
            List<Node> singularNodes =
                    nodesComponents.entrySet().stream().filter(entry -> entry.getValue().isEmpty())
                            .map(Map.Entry::getKey).toList();
            if (singularNodes.isEmpty()) {
                break;
            }

            Node singularNode = singularNodes.get(0);
            List<Node> similarityTargets =
                    similarityPairs.stream().filter(pair -> pair.getLeft().equals(singularNode))
                            .map(ImmutablePair::getRight).toList();
            Set<Node> similarityNodes = new HashSet<>(similarityTargets);
            similarityNodes.add(singularNode);

            SimilarityPattern similarityPattern = new SimilarityPattern(similarityNodes);
            for (Node node : similarityNodes) {
                addMapping(node, similarityPattern);
                addContext(node, similarityPattern);
            }

            components.add(similarityPattern);
        }
    }

    private Set<TraversalPattern> getCommonComponents(
            Collection<List<TraversalPattern>> componentsList) {
        Iterator<List<TraversalPattern>> componetsIterator = componentsList.iterator();
        Set<TraversalPattern> commonComponents = new HashSet<>(componetsIterator.next());
        while (componetsIterator.hasNext()) {
            commonComponents.retainAll(componetsIterator.next());
        }

        return commonComponents;
    }

    private HashMap<Node, List<TraversalPattern>> getNodesComponents(Set<Node> nodes) {
        HashMap<Node, List<TraversalPattern>> nodesComponents = new HashMap<>();

        for (Node node : nodes) {
            List<TraversalPattern> nodeComponents = new ArrayList<>();
            for (TraversalPattern component : components) {
                if (component.containsNode(node)) {
                    nodeComponents.add(component);
                }
            }
            nodesComponents.put(node, nodeComponents);
        }

        return nodesComponents;
    }
}
