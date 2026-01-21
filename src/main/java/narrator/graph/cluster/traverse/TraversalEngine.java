package narrator.graph.cluster.traverse;

import com.github.gumtreediff.utils.Pair;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import narrator.graph.Context;
import narrator.graph.Edge;
import narrator.graph.EdgeType;
import narrator.graph.Node;
import narrator.graph.SrcDst;
import narrator.graph.cluster.Cluster;
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
    private final Set<UsagePattern> usagePatterns = new HashSet<>();

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

        mergeByContext();
        // up until this point, they are merged per file.
        mergeByUsageChain();
    }

    private void addUsageComponents() {
        List<Node> useNodes = Centrality.usedDeclarations(graph).stream().toList();
        HashMap<Node, UsagePattern> usagePatterns = new HashMap<>();
        for (Node useNode : useNodes) {
            addUsageComponent(useNode, usagePatterns);
        }

        for (UsagePattern usagePattern : usagePatterns.values()) {
            List<UsagePattern> path = new ArrayList<>();
            path.add(usagePattern);
            breakCircularDependencies(usagePattern, path);
        }

        this.usagePatterns.addAll(usagePatterns.values());
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
        addContext(node, usageComponent);
        addMapping(node, usageComponent);

        Set<Node> usedNodes = util.getUsedNodes(node);
        for (Node usedNode : usedNodes) {
            usageComponent.addEdge(usedNode, node, new Edge(EdgeType.DEF_USE));
            addContext(usedNode, usageComponent);
            addMapping(usedNode, usageComponent);

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
                addContext(source, successivePattern);
                addMapping(source, successivePattern);
                addContext(target, successivePattern);
                addMapping(target, successivePattern);

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

    /*
     * Assumption: there is no traversal component yet before calling this method
     * */
    private void mergeByContext() {
        Map<TraversalPattern, List<Node>> componentsContexts = new HashMap<>();
        for (TraversalPattern component : components) {
            List<Node> contexts = Context.get(component.getGraph(), component.getLead());
            componentsContexts.put(component, new ArrayList<>(contexts));
        }

        Set<TraversalPattern> iteratedComponents = new HashSet<>();
        while (!componentsContexts.isEmpty()) {
            Optional<TraversalPattern> iteratee = componentsContexts.keySet().stream()
                    .filter(component -> !iteratedComponents.contains(component)).findFirst();
            if (iteratee.isEmpty()) {
                iteratedComponents.clear();
                continue;
            }

            TraversalPattern subject = iteratee.get();
            List<Node> subjectContexts = componentsContexts.get(subject);
            Node subjectContextsHead = subjectContexts.get(0);
            Set<Node> headMappings = new HashSet<>();
            headMappings.add(subjectContextsHead);
            headMappings.addAll(util.getMappingSources(subjectContextsHead));
            headMappings.addAll(util.getMappingTargets(subjectContextsHead));

            List<Pair<TraversalPattern, Integer>> mergeCandidates = componentsContexts.entrySet()
                    .stream()
                    .filter(componentContexts -> !componentContexts.getKey().equals(subject))
                    .map(componentContexts -> new Pair<>(componentContexts.getKey(),
                            headMappings.stream()
                                    .map(mapping -> componentContexts.getValue().indexOf(mapping))
                                    .max(Integer::compareTo).get()))
                    .filter(componentIndex -> componentIndex.second != -1).toList();
            if (mergeCandidates.isEmpty()) {
                componentsContexts.get(subject).remove(subjectContextsHead);
                if (componentsContexts.get(subject).isEmpty()) {
                    componentsContexts.remove(subject);
                }
                continue;
            }

            List<Pair<TraversalPattern, Integer>> mergeables = mergeCandidates.stream()
                    .filter(candidate -> candidate.second == 0).toList();
            if (mergeables.isEmpty()) {
                iteratedComponents.add(subject);
                continue;
            }

            List<TraversalPattern> mergeComponents = new ArrayList<>(
                    mergeables.stream().map(mergeable -> mergeable.first).toList());
            mergeComponents.add(subject);

            for (TraversalPattern mergeComponent : mergeComponents) {
                componentsContexts.remove(mergeComponent);
                components.remove(mergeComponent);
            }

            TraversalComponent mergedComponent = new TraversalComponent(mergeComponents,
                    ReasonType.CONTEXT);
            components.add(mergedComponent);

            if (subjectContexts.size() > 1) {
                componentsContexts.put(mergedComponent,
                        subjectContexts.subList(1, subjectContexts.size()));
            }
        }
    }

    private void mergeByUsageChain() {
        Map<UsagePattern, Set<UsagePattern>> usageRequirements = new HashMap<>();
        for (UsagePattern usagePattern : usagePatterns) {
            usageRequirements.put(usagePattern,
                    new HashSet<>(usagePattern.getRequirements().values()));
        }

        while (!usageRequirements.isEmpty()) {
            Optional<UsagePattern> requirementLeaf = usageRequirements.entrySet().stream()
                    .filter(entry -> entry.getValue().isEmpty()).map(Entry::getKey).findFirst();
            if (requirementLeaf.isEmpty()) {
                break;
            }

            UsagePattern subject = requirementLeaf.get();
            Node useNode = subject.useNode;
            Set<Node> usedNodes = subject.getUsedNodes();

            HashSet<TraversalPattern> mergeComponents = new HashSet<>();
            List<TraversalPattern> useComponents = components.stream()
                    .filter(component -> component.containsNode(useNode)).toList();
            mergeComponents.addAll(useComponents);
            List<TraversalPattern> usedComponents = components.stream()
                    .filter(component -> usedNodes.stream().anyMatch(component::containsNode))
                    .toList();
            mergeComponents.addAll(usedComponents);

            if (mergeComponents.size() > 1) {
                for (TraversalPattern mergeComponent : mergeComponents) {
                    components.remove(mergeComponent);
                }

                TraversalComponent mergedComponent = new TraversalComponent(
                        mergeComponents.stream().toList(), ReasonType.USAGE);
                components.add(mergedComponent);
            }

            usageRequirements.remove(subject);
            for (UsagePattern usagePattern : usageRequirements.keySet()) {
                usageRequirements.get(usagePattern).remove(subject);
            }
        }
    }

    private void addContext(Node node, TraversalPattern traversalPattern) {
        List<Node> contexts = Context.get(graph, node);
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
        List<Node> sources = util.getMappingSources(node);
        for (Node source : sources) {
            traversalPattern.addEdge(source, node, new Edge(EdgeType.MAPPING), (edges) -> {
                List<Edge> duplicateEdges =
                        edges.stream().filter(edge -> edge.getType().equals(EdgeType.MAPPING))
                                .toList();
                return duplicateEdges.isEmpty();
            });
            addContext(source, traversalPattern);
        }

        List<Node> targets = util.getMappingTargets(node);
        for (Node target : targets) {
            traversalPattern.addEdge(node, target, new Edge(EdgeType.MAPPING), (edges) -> {
                List<Edge> duplicateEdges =
                        edges.stream().filter(edge -> edge.getType().equals(EdgeType.MAPPING))
                                .toList();
                return duplicateEdges.isEmpty();
            });
            addContext(target, traversalPattern);
        }
    }

    private void addSingularComponents() {
        while (true) {
            List<Node> singularNodes = graph.vertexSet().stream().filter(node -> !node.isContext())
                    .filter(node -> {
                        for (TraversalPattern traversalComponent : components) {
                            boolean contains = traversalComponent.containsNode(node);
                            if (contains) {
                                return false;
                            }
                        }
                        return true;
                    }).collect(Collectors.toCollection(ArrayList::new));
            if (singularNodes.isEmpty()) {
                break;
            }
            singularNodes.sort(Comparator.comparing(
                    node -> !node.getSrcDst().equals(SrcDst.DST)
            ));

            Node singularNode = singularNodes.get(0);
            SingularPattern singularComponent = new SingularPattern(singularNode);
            addContext(singularNode, singularComponent);
            addMapping(singularNode, singularComponent);
            components.add(singularComponent);
        }
    }
}
