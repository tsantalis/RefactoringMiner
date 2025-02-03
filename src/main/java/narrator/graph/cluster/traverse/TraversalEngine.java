package narrator.graph.cluster.traverse;

import narrator.graph.Edge;
import narrator.graph.EdgeType;
import narrator.graph.Node;
import narrator.graph.NodeType;
import narrator.graph.cluster.Cluster;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.jgrapht.Graph;
import org.refactoringminer.astDiff.utils.Constants;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Stream;

public class TraversalEngine {
    private Graph<Node, Edge> graph;
    private List<TraversalPattern> components = new ArrayList<>();
    private final Util util;

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
        addDeclarationComponents();
        addSuccessiveComponents();
        addSingularComponents();

        // find pattern MST
        // requirements
        mergeUsagesRequirements();
        // same non-context node (e.g. requirement)
        mergeCommonNodes((node) -> !node.isContext());
        // 100% similar nodes
        mergeSimilarNodes();
        // successive nodes
        // same context
//        mergeCommonNodes(Node::isContext);
    }

    private final List<String> unacceptedSuccessiveNodes = new ArrayList<>() {{
        add(Constants.TYPE_DECLARATION);
        add(Constants.METHOD_DECLARATION);
    }};

    private void addSuccessiveComponents() {
        List<Node> acceptedNodes = graph.vertexSet().stream()
                .filter(node -> !unacceptedSuccessiveNodes.contains(node.getTree().getType().name)).toList();

        HashMap<Node, SuccessivePattern> successivePatterns = new HashMap<>();
        for (Node acceptedNode : acceptedNodes) {
            List<Edge> edges = graph.edgesOf(acceptedNode).stream()
                    .filter(edge -> edge.getType().equals(EdgeType.SUCCESSION))
                    // TODO: should we support succession for non-changes?
                    .filter(edge -> graph.getEdgeTarget(edge).isBase() && graph.getEdgeSource(edge).isBase()).toList();
            for (Edge edge : edges) {
                SuccessivePattern successivePattern = new SuccessivePattern();

                Node source = graph.getEdgeSource(edge);
                mergeCommentComponent(successivePatterns, source, successivePattern);

                Node target = graph.getEdgeTarget(edge);
                mergeCommentComponent(successivePatterns, target, successivePattern);

                successivePattern.addEdge(source, target, edge);
                addContext(source, successivePattern);
                addContext(target, successivePattern);

                successivePatterns.put(acceptedNode, successivePattern);
                components.add(successivePattern);
            }
        }
    }

    private void mergeCommentComponent(HashMap<Node, SuccessivePattern> commentPatterns, Node sourceNode, SuccessivePattern targetPattern) {
        SuccessivePattern sourceComponent = commentPatterns.get(sourceNode);

        if (sourceComponent != null) {
            targetPattern.merge(sourceComponent);

            components.remove(sourceComponent);

            List<Node> sourceComponentNodes = commentPatterns.entrySet().stream()
                    .filter(entry -> entry.getValue().equals(sourceComponent))
                    .map(Map.Entry::getKey).toList();
            for (Node node : sourceComponentNodes) {
                commentPatterns.put(node, targetPattern);
            }
        }
    }

    private void addUsageComponents() {
        List<Node> useNodes = Centrality.usedDeclarations(graph).stream()
                .filter(node -> node.getNodeType().equals(NodeType.BASE)).toList();
        HashMap<Node, UsagePattern> usagePatterns = new HashMap<>();
        for (Node useNode : useNodes) {
            addUsageComponent(useNode, new ArrayList<>(), usagePatterns);
        }
    }

    private void addUsageComponent(Node node, List<Node> usagePath, HashMap<Node, UsagePattern> usagePatterns) {
        if (usagePatterns.containsKey(node)) {
            return;
        }

        UsagePattern usageComponent = new UsagePattern(node);
        addContext(node, usageComponent);

        Set<Node> usedNodes = util.getUsedNodes(node);
        for (Node usedNode : usedNodes) {
            usageComponent.addEdge(usedNode, node, new Edge(EdgeType.DEF_USE, 1));
            addContext(usedNode, usageComponent);

            if (usagePath.contains(usedNode)) {
                usageComponent.setRingNode(usedNode);
                continue;
            }

            if (util.doesUse(usedNode)) {
                addUsageComponent(usedNode, new ArrayList<>() {{
                    addAll(usagePath);
                    add(node);
                }}, usagePatterns);

                UsagePattern usedComponent = usagePatterns.get(usedNode);
                Node usageRingNode = usedComponent.getRingNode();
                if (usageRingNode != null) {
                    if (!node.equals(usageRingNode)) {
                        usageComponent.setRingNode(usageRingNode);
                    }

                    usageComponent.merge(usedComponent);

                    List<Node> usedComponentNodes = usagePatterns.entrySet().stream()
                            .filter(entry -> entry.getValue().equals(usedComponent))
                            .map(Map.Entry::getKey)
                            .toList();
                    for (Node usedComponentNode : usedComponentNodes) {
                        usagePatterns.put(usedComponentNode, usageComponent);
                    }
                } else {
                    usageComponent.addRequirement(usedNode, usedComponent);
                }
            }
        }

        components.add(usageComponent);
        usagePatterns.put(node, usageComponent);
    }

    private void addDeclarationComponents() {
        List<Node> useNodes = Centrality.usedDeclarations(graph).stream()
                .filter(node -> node.getNodeType().equals(NodeType.EXTENSION)).toList();
        for (Node useNode : useNodes) {
            addDeclarationComponent(useNode);
        }
    }

    private void addDeclarationComponent(Node node) {
        DeclarationPattern declarationComponent = new DeclarationPattern(node);
        addContext(node, declarationComponent);

        Set<Node> usedNodes = util.getUsedNodes(node);
        for (Node usedNode : usedNodes) {
            declarationComponent.addEdge(usedNode, node, new Edge(EdgeType.DEF_USE, 1));
            addContext(usedNode, declarationComponent);
        }

        components.add(declarationComponent);
    }

    private void addContext(Node node, TraversalPattern traversalPattern) {
        List<Node> contexts = util.getContexts(node);
        Node currentNode = node;
        for (Node context : contexts) {
            traversalPattern.addEdge(currentNode, context, new Edge(EdgeType.CONTEXT, 1), (edges) -> {
                List<Edge> duplicateEdges = edges.stream()
                        .filter(edge -> edge.getType().equals(EdgeType.CONTEXT)).toList();
                return duplicateEdges.isEmpty();
            });

            currentNode = context;
        }
    }

    private List<String> singularTypes = new ArrayList<>() {{
        add(Constants.METHOD_DECLARATION);
    }};

    // for chunks of code which are using already existing declarations, or their uses are not regular
    private void addSingularComponents() {
        List<Node> singularNodes = graph.vertexSet().stream()
                .filter(node -> !node.isContext())
                .filter(node -> singularTypes.contains(node.getTree().getType().name))
                .filter(node -> {
                    boolean result = true;

                    for (TraversalPattern traversalComponent : components) {
                        boolean contains = traversalComponent.containsNode(node);
                        if (contains) {
                            return false;
                        }
                    }

                    return result;
                }).toList();

        for (Node node : singularNodes) {
            SingularPattern singularComponent = new SingularPattern(node);
            addContext(node, singularComponent);

            components.add(singularComponent);
        }
    }

    private void mergeUsagesRequirements() {
        List<UsagePattern> usageComponents = new ArrayList<>();
        for (TraversalPattern component : components) {
            if (component instanceof UsagePattern) {
                usageComponents.add((UsagePattern) component);
            }
        }

        HashMap<TraversalPattern, TraversalPattern> usageComponentParent = new HashMap<>();
        for (UsagePattern usageComponent : usageComponents) {
            mergeUsageRequirements(usageComponentParent, usageComponent);
        }
    }

    private void mergeUsageRequirements(
            HashMap<TraversalPattern, TraversalPattern> usageComponentParent,
            UsagePattern usageComponent) {
        HashMap<Node, UsagePattern> requirements = usageComponent.getRequirements();
        if (requirements.keySet().isEmpty()) {
            usageComponentParent.put(usageComponent, usageComponent);
            return;
        }

        for (UsagePattern requirement : requirements.values()) {
            if (requirement.getRequirements().isEmpty() || usageComponentParent.containsKey(requirement)) {
                continue;
            }

            mergeUsageRequirements(usageComponentParent, requirement);
        }

        List<TraversalPattern> requirementComponents = requirements.values().stream()
                .map(requirement -> usageComponentParent.getOrDefault(requirement, requirement)).toList();
        List<TraversalPattern> overallComponents = Stream.concat(requirementComponents.stream(), Stream.of(usageComponent)).toList();

        TraversalComponent parentComponent = new TraversalComponent(overallComponents, requirements.keySet(), ReasonType.REQUIREMENT);
        for (TraversalPattern component : overallComponents) {
            components.remove(component);
            usageComponentParent.put(component, parentComponent);
        }
        components.add(parentComponent);
    }

    private void mergeCommonNodes(Function<Node, Boolean> nodeCondition) {
        while (true) {
            Set<Node> mostCommonNodes = null;
            for (int i = 0; i < components.size(); i++) {
                for (int j = i + 1; j < components.size(); j++) {
                    Set<Node> subjectNodes = components.get(i).vertexSet();
                    Set<Node> objectNodes = components.get(j).vertexSet();

                    Set<Node> commonNodes = new HashSet<>();
                    for (Node subjectNode : subjectNodes) {
                        if (objectNodes.contains(subjectNode)
                                && (nodeCondition == null || nodeCondition.apply(subjectNode))) {
                            commonNodes.add(subjectNode);
                        }
                    }

                    if (commonNodes.isEmpty()
                            || (mostCommonNodes != null && mostCommonNodes.size() >= commonNodes.size())) {
                        continue;
                    }

                    mostCommonNodes = commonNodes;
                }
            }

            if (mostCommonNodes == null) {
                break;
            }

            HashMap<Node, List<TraversalPattern>> commonNodesComponents = getNodesComponents(mostCommonNodes);
            Set<TraversalPattern> commonComponents = getCommonComponents(commonNodesComponents.values());

            TraversalComponent parentComponent = new TraversalComponent(commonComponents.stream().toList(), mostCommonNodes, ReasonType.COMMON);
            for (TraversalPattern component : commonComponents) {
                components.remove(component);
            }
            components.add(parentComponent);
        }
    }

    private void mergeSimilarNodes() {
        List<Edge> similarityEdges = graph.edgeSet().stream()
                .filter(edge -> edge.getType().equals(EdgeType.SIMILARITY)).toList();
        List<ImmutablePair<Node, Node>> similarityPairs = similarityEdges.stream()
                .map(edge -> new ImmutablePair<>(graph.getEdgeSource(edge), graph.getEdgeTarget(edge))).toList();

        Set<Node> nodes = new HashSet<>();
        for (ImmutablePair<Node, Node> pair : similarityPairs) {
            nodes.add(pair.getLeft());
            nodes.add(pair.getRight());
        }

        while (true) {
            HashMap<Node, List<TraversalPattern>> nodesComponents = getNodesComponents(nodes);

            HashMap<TraversalPattern, HashMap<TraversalPattern, List<Node>>> componentsSimilaritySources = new HashMap<>();
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

                        HashMap<TraversalPattern, List<Node>> leftComponentSimilaritySources = componentsSimilaritySources.get(leftComponent);
                        if (!leftComponentSimilaritySources.containsKey(rightComponent)) {
                            leftComponentSimilaritySources.put(rightComponent, new ArrayList<>());
                        }

                        leftComponentSimilaritySources.get(rightComponent).add(pair.getLeft());
                    }
                }
            }

            List<Node> mostSimilaritySources = null;
            TraversalPattern similaritySourceComponent = null;
            for (Map.Entry<TraversalPattern, HashMap<TraversalPattern, List<Node>>> leftComponentSimilaritySources : componentsSimilaritySources.entrySet()) {
                for (Map.Entry<TraversalPattern, List<Node>> rightComponentSimilaritySources : leftComponentSimilaritySources.getValue().entrySet()) {
                    List<Node> similaritySources = rightComponentSimilaritySources.getValue();
                    if (mostSimilaritySources != null && similaritySources.size() <= mostSimilaritySources.size()) {
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
                List<Node> similarityTargets = graph.outgoingEdgesOf(similaritySource).stream()
                        .filter(edge -> edge.getType().equals(EdgeType.SIMILARITY))
                        .map(edge -> graph.getEdgeTarget(edge)).toList();
                HashMap<Node, List<TraversalPattern>> similarityTargetComponents = getNodesComponents(new HashSet<>(similarityTargets));

                Set<TraversalPattern> accumulatedTargetComponents = new HashSet<>();
                for (List<TraversalPattern> components : similarityTargetComponents.values()) {
                    accumulatedTargetComponents.addAll(components);
                }
                sourcesTargetComponents.add(accumulatedTargetComponents);
            }

            Set<TraversalPattern> commonComponents = getCommonComponents(sourcesTargetComponents.stream()
                    .map(components -> components.stream().toList()).toList());
            List<TraversalPattern> overallComponents = Stream.concat(commonComponents.stream(), Stream.of(similaritySourceComponent)).toList();

            TraversalComponent parentComponent = new TraversalComponent(overallComponents, new HashSet<>(mostSimilaritySources), ReasonType.SIMILAR);
            for (TraversalPattern component : overallComponents) {
                components.remove(component);
            }
            components.add(parentComponent);
        }
    }

    private Set<TraversalPattern> getCommonComponents(Collection<List<TraversalPattern>> componentsList) {
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
