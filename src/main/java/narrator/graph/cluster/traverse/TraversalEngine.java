package narrator.graph.cluster.traverse;

import narrator.graph.Edge;
import narrator.graph.EdgeType;
import narrator.graph.Node;
import narrator.graph.cluster.Cluster;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.jgrapht.Graph;
import org.jgrapht.graph.builder.GraphTypeBuilder;
import org.refactoringminer.astDiff.utils.Constants;

import java.util.*;
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
        addCommentComponents();
        addSingularComponents();

        // find pattern MST
        // requirements
        mergeUsagesRequirements();
        System.out.println(components.size());
        // same non-context node (e.g. requirement)
        mergeCommonNodes();
        System.out.println(components.size());
        // 100% similar nodes
        // successive nodes
        // same context
    }

    // TODO: support sub changes to javadoc
    private List<String> commentTypes = new ArrayList<>() {{
        add(Constants.LINE_COMMENT);
        add(Constants.BLOCK_COMMENT);
    }};

    private void addCommentComponents() {
        List<Node> commentNodes = graph.vertexSet().stream().filter(node -> {
            String type = node.getTree().getType().name;
            return commentTypes.contains(type);
        }).toList();

        HashMap<Node, CommentPattern> commentPatterns = new HashMap<>();
        for (Node commentNode : commentNodes) {
            List<Edge> edges = graph.edgesOf(commentNode).stream()
                    .filter(edge -> edge.getType().equals(EdgeType.SUCCESSION)).toList();
            for (Edge edge : edges) {
                CommentPattern commentPattern = new CommentPattern();

                Node source = graph.getEdgeSource(edge);
                mergeCommentComponent(commentPatterns, source, commentPattern);

                Node target = graph.getEdgeTarget(edge);
                mergeCommentComponent(commentPatterns, target, commentPattern);

                commentPattern.addEdge(source, target, edge);
                addContext(source, commentPattern);
                addContext(target, commentPattern);

                commentPatterns.put(commentNode, commentPattern);
                components.add(commentPattern);
            }
        }
    }

    private void mergeCommentComponent(HashMap<Node, CommentPattern> commentPatterns, Node sourceNode, CommentPattern targetPattern) {
        CommentPattern sourceComponent = commentPatterns.get(sourceNode);

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
        List<Node> useNodes = Centrality.usedDeclarations(graph);
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

        Graph<Node, Edge> reason = GraphTypeBuilder
                .<Node, Edge>directed()
                .allowingMultipleEdges(true)
                .allowingSelfLoops(true)
                .edgeClass(Edge.class)
                .weighted(true)
                .buildGraph();
        for (Node requirement : requirements.keySet()) {
            reason.addVertex(requirement);
        }

        TraversalComponent parentComponent = new TraversalComponent(overallComponents, reason);
        for (TraversalPattern component : overallComponents) {
            components.remove(component);
            usageComponentParent.put(component, parentComponent);
        }
        components.add(parentComponent);
    }

    private void mergeCommonNodes() {
        List<Node> nodes = graph.vertexSet().stream().filter(node -> !node.isContext()).toList();
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

        HashMap<Pair<TraversalPattern, TraversalPattern>, Set<Node>> componentsCommonNodes = new HashMap<>();
        for (int i = 0; i < components.size(); i++) {
            for (int j = i + 1; j < components.size(); j++) {
                TraversalPattern subject = components.get(i);
                TraversalPattern object = components.get(j);

                Set<Node> subjectNodes = subject.vertexSet();
                Set<Node> objectNodes = object.vertexSet();
                Set<Node> commonNodes = new HashSet<>();
                for (Node subjectNode : subjectNodes) {
                    if (objectNodes.contains(subjectNode)) {
                        commonNodes.add(subjectNode);
                    }
                }

                if (commonNodes.isEmpty()) {
                    continue;
                }

                componentsCommonNodes.put(new ImmutablePair<>(subject, object), commonNodes);
            }
        }

    }
}
