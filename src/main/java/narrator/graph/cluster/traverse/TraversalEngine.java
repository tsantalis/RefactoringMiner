package narrator.graph.cluster.traverse;

import com.github.gumtreediff.utils.Pair;
import java.util.ArrayList;
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
import narrator.graph.cluster.Cluster;
import org.jgrapht.Graph;
import org.refactoringminer.astDiff.utils.Constants;

public class TraversalEngine {

    private final Util util;
    private final Graph<Node, Edge> graph;
    private final List<TraversalPattern> components = new ArrayList<>();
    private final Set<UsagePattern> usagePatterns = new HashSet<>();
    private final HashMap<Node, SingularPattern> singularPatternsLeads = new HashMap<>();

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

        Set<Pair<Set<Node>, TraversalPattern>> traversalComponentsTracker = mergeByContext();

        for (UsagePattern usagePattern : usagePatterns) {
            List<Node> nullRequirementsNode = usagePattern.getRequirements().entrySet().stream()
                    .filter(entry -> entry.getValue() == null).map(
                            Entry::getKey).toList();
            for (Node nullRequirementNode : nullRequirementsNode) {
                List<Node> contextNodes = Context.get(graph, nullRequirementNode);
                List<Node> sameContextNodes = contextNodes.stream().filter(contextNode -> contextNode.getTree().equals(nullRequirementNode.getTree())).toList();
                Node targetNode = sameContextNodes.isEmpty() ? nullRequirementNode : sameContextNodes.get(sameContextNodes.size() - 1);

                List<TraversalPattern> requirementComponents = traversalComponentsTracker.stream()
                        .filter(nodesTraversalComponent -> nodesTraversalComponent.first.contains(
                                targetNode))
                        .sorted((nrn1, nrn2) -> nrn2.first.size() - nrn1.first.size())
                        .map(nodesTraversalComponent -> nodesTraversalComponent.second).toList();
                if (requirementComponents.isEmpty()) {
                    continue;
                }

                TraversalPattern topRequirementComponent = requirementComponents.get(0);
                for (String identifier : nullRequirementNode.getIdentifiers()) {
                    topRequirementComponent.addIdentifier(identifier);
                }
                usagePattern.addRequirement(nullRequirementNode, topRequirementComponent);
            }

            List<Node> remainingNullRequirementNodes = usagePattern.getRequirements().entrySet()
                    .stream().filter(entry -> entry.getValue() == null).map(Entry::getKey).toList();
            for (Node remainingRequirementNode : remainingNullRequirementNodes) {
                usagePattern.breakRequirement(remainingRequirementNode);
                addMapping(remainingRequirementNode, usagePattern);
            }
        }

        for (UsagePattern usagePattern : usagePatterns) {
            usagePattern.breakCircularDependencies();
        }

        // up until this point, they are merged per file.
        mergeByUsageChain();
    }

    private void addUsageComponents() {
        List<Node> useNodes = Centrality.usedDeclarations(graph).stream().toList();
        HashMap<Node, UsagePattern> usagePatterns = new HashMap<>();
        for (Node useNode : useNodes) {
            addUsageComponent(useNode, usagePatterns);
        }

        this.usagePatterns.addAll(usagePatterns.values());
    }

    private void addUsageComponent(Node node, HashMap<Node, UsagePattern> usagePatterns) {
        if (usagePatterns.containsKey(node)) {
            return;
        }

        UsagePattern usageComponent = new UsagePattern(node);
        addContext(node, usageComponent);
        addMapping(node, usageComponent);

        components.add(usageComponent);
        usagePatterns.put(node, usageComponent);

        Set<Node> usedNodes = util.getUsedNodes(node);
        for (Node usedNode : usedNodes) {
            usageComponent.addEdge(usedNode, node, new Edge(EdgeType.DEF_USE));
            addContext(usedNode, usageComponent);

            if (util.doesUse(usedNode)) {
                addUsageComponent(usedNode, usagePatterns);

                UsagePattern usedComponent = usagePatterns.get(usedNode);
                usageComponent.addRequirement(usedNode, usedComponent);
            } else if (usedNode.isContext()) {
                // It will be populated after merging
                usageComponent.addRequirement(usedNode, null);
            } else if (!usedNode.isExtension()) {
                if (!singularPatternsLeads.containsKey(usedNode)) {
                    SingularPattern usedComponent = new SingularPattern(usedNode);
                    addContext(usedNode, usedComponent);
                    addMapping(usedNode, usedComponent);
                    components.add(usedComponent);
                    singularPatternsLeads.put(usedNode, usedComponent);
                }
                SingularPattern existingSingularComponent = singularPatternsLeads.get(usedNode);
                usageComponent.addRequirement(usedNode, existingSingularComponent);
            }
        }
    }

    private void addSuccessiveComponents() {
        List<Node> acceptedNodes =
                graph.vertexSet().stream().filter(node -> {
                    Constants constants = new Constants(node.getPath());
                    String type = node.getTree().getType().name;
                    return !type.equals(constants.TYPE_DECLARATION) && !type.equals(
                            constants.METHOD_DECLARATION);
                }).toList();

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
    private Set<Pair<Set<Node>, TraversalPattern>> mergeByContext() {
        Map<TraversalPattern, List<Node>> componentsContexts = new HashMap<>();
        for (TraversalPattern component : components) {
            List<Node> contexts = Context.get(component.getGraph(), component.getLead());
            componentsContexts.put(component, new ArrayList<>(contexts));
        }

        Set<Pair<Set<Node>, TraversalPattern>> traversalComponentsTracker = new HashSet<>();

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

            Set<Node> heads = new HashSet<>();
            heads.add(subjectContextsHead);

            List<Pair<TraversalPattern, Integer>> headDescendants = componentsContexts.entrySet()
                    .stream()
                    .map(componentContexts -> new Pair<>(componentContexts.getKey(),
                            componentContexts.getValue().indexOf(subjectContextsHead)))
                    .filter(componentIndex -> componentIndex.second != -1).toList();
            List<TraversalPattern> headMergeables = headDescendants.stream()
                    .filter(descendant -> descendant.second == 0)
                    .map(headMergeable -> headMergeable.first).toList();
            if (headDescendants.size() > 1) {
                if (headMergeables.size() < headDescendants.size()) {
                    iteratedComponents.add(subject);
                } else {
                    mergeByContext(headMergeables, subjectContexts, heads, componentsContexts,
                            traversalComponentsTracker);
                }

                continue;
            }

            Set<Node> headMappings = new HashSet<>();
            headMappings.addAll(util.getMappingSources(subjectContextsHead));
            headMappings.addAll(util.getMappingTargets(subjectContextsHead));
            Optional<Node> optionalHeadMapping = headMappings.stream().findFirst();

            if (optionalHeadMapping.isEmpty()) {
                traversalComponentsTracker.add(new Pair<>(heads, subject));

                componentsContexts.get(subject).remove(subjectContextsHead);
                if (componentsContexts.get(subject).isEmpty()) {
                    componentsContexts.remove(subject);
                }
                continue;
            }

            Node headMapping = optionalHeadMapping.get();
            List<Pair<TraversalPattern, Integer>> mappingDescendants = componentsContexts.entrySet()
                    .stream()
                    .map(componentContexts -> new Pair<>(componentContexts.getKey(),
                            componentContexts.getValue().indexOf(headMapping)))
                    .filter(componentIndex -> componentIndex.second != -1).toList();
            if (mappingDescendants.size() > 1) {
                // merge the mapping first
                iteratedComponents.add(subject);
                continue;
            }

            heads.add(headMapping);

            if (mappingDescendants.isEmpty()) {
                traversalComponentsTracker.add(new Pair<>(heads, subject));

                componentsContexts.get(subject).remove(subjectContextsHead);
                if (componentsContexts.get(subject).isEmpty()) {
                    componentsContexts.remove(subject);
                }
                continue;
            }

            List<TraversalPattern> allDescendants = new ArrayList<>();
            allDescendants.add(headMergeables.get(0));
            allDescendants.add(mappingDescendants.get(0).first);

            mergeByContext(allDescendants, subjectContexts, heads, componentsContexts,
                    traversalComponentsTracker);
        }

        return traversalComponentsTracker;
    }

    private void mergeByContext(List<TraversalPattern> mergeComponents, List<Node> subjectContexts,
            Set<Node> heads, Map<TraversalPattern, List<Node>> componentsContexts,
            Set<Pair<Set<Node>, TraversalPattern>> traversalComponentsTracker) {
        for (TraversalPattern mergeComponent : mergeComponents) {
            componentsContexts.remove(mergeComponent);
            components.remove(mergeComponent);
        }

        TraversalComponent mergedComponent = new TraversalComponent(mergeComponents,
                ReasonType.CONTEXT);
        components.add(mergedComponent);
        traversalComponentsTracker.add(new Pair<>(heads, mergedComponent));

        if (subjectContexts.size() > 1) {
            componentsContexts.put(mergedComponent,
                    subjectContexts.subList(1, subjectContexts.size()));
        }

    }

    private void mergeByUsageChain() {
        Map<UsagePattern, Set<TraversalPattern>> usageRequirements = new HashMap<>();
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

            List<TraversalPattern> useComponents = components.stream()
                    .filter(component -> component.containsNode(useNode)).toList();
            HashSet<TraversalPattern> mergeComponents = new HashSet<>(useComponents);
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
            addMapping(context, traversalPattern);

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

    // It creates duplicate singular patterns, but it is necessary for covering all hunks beneath contexts
    private void addSingularComponents() {
        List<Node> singularNodes = graph.vertexSet().stream().filter(node -> !node.isContext())
                .filter(node -> {
                    for (TraversalPattern traversalPattern : components) {
                        boolean contains = traversalPattern.containsNode(node);
                        if (contains) {
                            return false;
                        }
                    }
                    return true;
                }).collect(Collectors.toCollection(ArrayList::new));
        for (Node singularNode : singularNodes) {
            SingularPattern singularComponent = new SingularPattern(singularNode);
            addContext(singularNode, singularComponent);
            addMapping(singularNode, singularComponent);
            components.add(singularComponent);
            singularPatternsLeads.put(singularNode, singularComponent);
        }
    }
}
