package org.refactoringminer.astDiff.graph.cluster.traverse;

import com.github.gumtreediff.utils.Pair;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.refactoringminer.astDiff.graph.Context;
import org.refactoringminer.astDiff.graph.Edge;
import org.refactoringminer.astDiff.graph.EdgeType;
import org.refactoringminer.astDiff.graph.Node;
import org.refactoringminer.astDiff.graph.cluster.Cluster;
import org.jgrapht.Graph;

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

    public TraversalPattern get() {
        return components.get(0);
    }

    private void process() {
        // add patterns
        addUsageComponents();
        addSuccessiveComponents();
        addSingularComponents();

        Set<Pair<Set<Node>, TraversalPattern>> traversalComponentsTracker = mergeByContext();

        finalizeUsagePatterns(traversalComponentsTracker);

        // up until this point, they are merged per file.
        mergeByUsageChain();

        if (components.size() > 1) {
            TraversalComponent finalComponent = new TraversalComponent(new ArrayList<>(components),
                ReasonType.CONTEXT);
            components.clear();
            components.add(finalComponent);
        }

        for (TraversalPattern component : components) {
            setClusterGraphRecursive(component);
        }
    }

    private void setClusterGraphRecursive(TraversalPattern pattern) {
        pattern.setClusterGraph(graph);
        if (pattern instanceof AggregatorPattern aggregator) {
            for (TraversalPattern sub : aggregator.subs) {
                setClusterGraphRecursive(sub);
            }
        }
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

        // TODO: Implement a more robust check: if the node is src which has some mappings
        // and its usedNodes are extensions AND all of those extension used nodes are 
        // a mapping of an extension used node for the mapping of the src node.
        if (node.isSrc() && !util.getMappingTargets(node).isEmpty()) {
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
            } else if (!usedNode.isExtension() &&
                    (usedNode.isDst() || util.getMappingTargets(usedNode).isEmpty())) {
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
                    String type = node.getTree().getType().name;
                    return !Context.isLocationContext(node.getPath(), type);
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

    private Set<Pair<Set<Node>, TraversalPattern>> mergeByContext() {
        Set<Pair<Set<Node>, TraversalPattern>> traversalComponentsTracker = new HashSet<>();

        Map<TraversalPattern, ComponentContexts> componentsContexts = new HashMap<>();
        for (TraversalPattern component : components) {
            componentsContexts.put(component,
                    new ComponentContexts(component,
                            new Pair<>(Context.get(component.getGraph(), component.getLead()),
                                    null)));
        }
        Set<TraversalPattern> iteratedComponents = new HashSet<>();
        while (!componentsContexts.isEmpty()) {
            Optional<TraversalPattern> iteratee = componentsContexts.keySet().stream()
                    .filter(component -> !iteratedComponents.contains(component)).findFirst();
            if (iteratee.isEmpty()) {
                iteratedComponents.clear();
                continue;
            }
            ComponentContexts subject = componentsContexts.get(iteratee.get());

            mergeByContext(subject, componentsContexts, iteratedComponents,
                    traversalComponentsTracker);
        }

        return traversalComponentsTracker;
    }

    private void mergeByContext(ComponentContexts subject,
            Map<TraversalPattern, ComponentContexts> componentsContexts,
            Set<TraversalPattern> iteratedComponents,
            Set<Pair<Set<Node>, TraversalPattern>> traversalComponentsTracker) {
        List<ComponentContexts> descendants = componentsContexts.values().stream()
                .filter(componentContexts -> isDescendant(subject.contextsPair, componentContexts.contextsPair)).toList();
        List<ComponentContexts> mergeables = descendants.stream()
                .filter(descendant -> isMergeable(subject.contextsPair, descendant.contextsPair)).toList();
        if (descendants.size() > 1) {
            if (mergeables.size() < descendants.size() ||
                    (subject.contextsPair.second == null && descendants.stream().anyMatch(descendant -> descendant.contextsPair.second != null))) {
                iteratedComponents.add(subject.component);
            } else {
                mergeByContext(mergeables, componentsContexts, traversalComponentsTracker);
            }
            return;
        }

        if (subject.contextsPair.second != null) {
            nextHop(subject, componentsContexts, traversalComponentsTracker);
            return;
        }

        Node head = getContextsHeads(subject.contextsPair).iterator().next();
        Set<Node> headMappings = new HashSet<>();
        headMappings.addAll(util.getMappingSources(head));
        headMappings.addAll(util.getMappingTargets(head));
        Optional<Node> optionalHeadMapping = headMappings.stream().findFirst();

        if (optionalHeadMapping.isEmpty()) {
            nextHop(subject, componentsContexts, traversalComponentsTracker);
            return;
        }
        Node headMapping = optionalHeadMapping.get();

        List<Node> mappingContexts = new ArrayList<>();
        mappingContexts.add(headMapping);
        mappingContexts.addAll(Context.get(graph, headMapping));
        List<ComponentContexts> mappingDescendants = componentsContexts.values().stream()
                .filter(componentContexts -> isDescendant(new Pair<>(mappingContexts, null), componentContexts.contextsPair)).toList();
        List<ComponentContexts> mappingMergeables = componentsContexts.values().stream()
                .filter(componentContexts -> isMergeable(new Pair<>(mappingContexts, null), componentContexts.contextsPair)).toList();

        if (mappingDescendants.size() > 1) {
            // merge the mapping first
            iteratedComponents.add(subject.component);
            return;
        }

        if (mappingDescendants.isEmpty()) {
            nextHop(subject, componentsContexts, traversalComponentsTracker);
            return;
        }

        if (mappingMergeables.isEmpty()) {
            // pull the mapping up first
            iteratedComponents.add(subject.component);
            return;
        }

        List<ComponentContexts> allDescendants = new ArrayList<>();
        allDescendants.add(descendants.get(0));
        allDescendants.add(mappingDescendants.get(0));

        mergeByContext(allDescendants, componentsContexts, traversalComponentsTracker);
    }

    private void mergeByContext(List<ComponentContexts> mergeComponents,
            Map<TraversalPattern, ComponentContexts> componentsContexts,
            Set<Pair<Set<Node>, TraversalPattern>> traversalComponentsTracker) {
        for (ComponentContexts mergeComponent : mergeComponents) {
            componentsContexts.remove(mergeComponent.component);
            components.remove(mergeComponent.component);
        }

        Pair<List<Node>, List<Node>> contextsPair = produceContextPair(mergeComponents);
        Set<Node> heads = getContextsHeads(contextsPair);

        TraversalComponent mergedComponent = new TraversalComponent(mergeComponents.stream()
                .map(CC -> CC.component).toList(), ReasonType.CONTEXT);
        mergedComponent.setMergeContexts(heads);
        components.add(mergedComponent);
        traversalComponentsTracker.add(new Pair<>(heads, mergedComponent));

        Pair<List<Node>, List<Node>> nextContextsPair = getNextContextsPair(contextsPair);
        if (nextContextsPair.first != null) {
            componentsContexts.put(mergedComponent,
                    new ComponentContexts(mergedComponent, nextContextsPair));
        }
    }

    private Pair<List<Node>, List<Node>> produceContextPair(List<ComponentContexts> from) {
        Optional<Pair<List<Node>, List<Node>>> optionalMappingContextsPair = from.stream()
                .map(componentContexts -> componentContexts.contextsPair)
                .filter(contextsPair -> contextsPair.second != null).findFirst();
        if (optionalMappingContextsPair.isPresent()) {
            return optionalMappingContextsPair.get();
        }

        List<List<Node>> contextsLists = from.stream()
                .map(componentContexts -> componentContexts.contextsPair.first).toList();
        Set<Node> contextsListsHeads = contextsLists.stream()
                .map(contextsList -> contextsList.get(0))
                .collect(Collectors.toSet());
        List<List<Node>> headsList = contextsListsHeads.stream()
                .map(contextsListsHead -> contextsLists.stream()
                        .filter(contextsList -> contextsList.get(0).equals(contextsListsHead))
                        .findFirst().get()).toList();

        List<Node> c1 = headsList.get(0);
        List<Node> c2 = null;
        if (headsList.size() > 1) {
            c2 = headsList.get(1);
        }

        return new Pair<>(c1, c2);
    }

    private Pair<List<Node>, List<Node>> getNextContextsPair(
            Pair<List<Node>, List<Node>> contextsPair) {
        Set<Node> contextsHeads = getContextsHeads(contextsPair);
        List<Node> c1 = contextsPair.first.stream()
                .filter(context -> !contextsHeads.contains(context)).toList();
        if (c1.isEmpty()) {
            c1 = null;
        }

        if (contextsPair.second == null) {
            return new Pair<>(c1, null);
        }

        List<Node> c2 = contextsPair.second.stream()
                .filter(context -> !contextsHeads.contains(context)).toList();
        if (c2.isEmpty()) {
            c2 = null;
        }

        return c1 != null ? new Pair<>(c1, c2) : new Pair<>(c2, c1);
    }

    private void nextHop(ComponentContexts subject,
            Map<TraversalPattern, ComponentContexts> componentsContexts,
            Set<Pair<Set<Node>, TraversalPattern>> traversalComponentsTracker) {
        traversalComponentsTracker.add(
                new Pair<>(getContextsHeads(subject.contextsPair), subject.component));

        Pair<List<Node>, List<Node>> nextContextsPair = getNextContextsPair(subject.contextsPair);
        if (nextContextsPair.first == null) {
            componentsContexts.remove(subject.component);
        } else {
            componentsContexts.get(subject.component).contextsPair = nextContextsPair;
        }
    }

    private Set<Node> getContextsHeads(Pair<List<Node>, List<Node>> contextsPair) {
        List<Node> c1 = contextsPair.first;
        List<Node> c2 = contextsPair.second;
        if (c2 == null) {
            return Set.of(c1.get(0));
        }

        int c1Idx = -1;
        int c2Idx = -1;
        for (int i = 0; i < c1.size(); i++) {
            Node c1Node = c1.get(i);

            Set<Node> mappings = new HashSet<>();
            mappings.addAll(util.getMappingSources(c1Node));
            mappings.addAll(util.getMappingTargets(c1Node));
            Optional<Node> optionalMapping = mappings.stream().findFirst();
            if (optionalMapping.isEmpty()) {
                continue;
            }

            Node mapping = optionalMapping.get();
            c2Idx = c2.indexOf(mapping);
            if (c2Idx != -1) {
                c1Idx = i;
                break;
            }
        }

        if (c1Idx == -1) {
            System.out.println("Could not find mapping in contexts");
            return Set.of();
        }

        if (c1Idx > 0 || c2Idx > 0) {
            Set<Node> result = new HashSet<>();
            result.addAll(c1.subList(0, c1Idx));
            result.addAll(c2.subList(0, c2Idx));
            return result;
        }

        return Set.of(c1.get(0), c2.get(0));
    }

    private boolean isDescendant(Pair<List<Node>, List<Node>> subjectContextsPair,
            Pair<List<Node>, List<Node>> examineeContextsPair) {
        Set<Node> subjectHeads = getContextsHeads(subjectContextsPair);

        List<Node> c1 = examineeContextsPair.first;
        if (c1.stream().anyMatch(subjectHeads::contains)) {
            return true;
        }

        List<Node> c2 = examineeContextsPair.second;
        if (c2 == null) {
            return false;
        }

        return c2.stream().anyMatch(subjectHeads::contains);
    }

    private boolean isMergeable(Pair<List<Node>, List<Node>> subjectContextsPair,
            Pair<List<Node>, List<Node>> examineeContextsPair) {
        Set<Node> subjectHeads = getContextsHeads(subjectContextsPair);
        Set<Node> examineeHeads = getContextsHeads(examineeContextsPair);
        return examineeHeads.stream().anyMatch(subjectHeads::contains);
    }

    private void finalizeUsagePatterns(
            Set<Pair<Set<Node>, TraversalPattern>> traversalComponentsTracker) {
        for (UsagePattern usagePattern : usagePatterns) {
            List<Node> nullRequirementsNode = usagePattern.getRequirements().entrySet().stream()
                    .filter(entry -> entry.getValue() == null).map(
                            Entry::getKey).toList();
            for (Node nullRequirementNode : nullRequirementsNode) {
                List<Node> requirementTargets = new ArrayList<>();
                requirementTargets.add(nullRequirementNode);
                requirementTargets.addAll(Context.get(graph, nullRequirementNode).stream()
                        .filter(contextNode -> contextNode.getTree()
                                .equals(nullRequirementNode.getTree())).toList());

                List<TraversalPattern> targetComponents = new ArrayList<>(
                        requirementTargets.stream()
                                .map(requirementTarget -> traversalComponentsTracker.stream()
                                        .filter(nodesTraversalComponent -> nodesTraversalComponent.first.contains(
                                                requirementTarget))
                                        .sorted((ntc1, ntc2) -> ntc2.first.size()
                                                - ntc1.first.size())
                                        .map(nodesTraversalComponent -> nodesTraversalComponent.second)
                                        .findFirst())
                                .filter(Optional::isPresent).map(Optional::get).toList());
                Collections.reverse(targetComponents);

                TraversalPattern targetComponent =
                        targetComponents.isEmpty() ? null : targetComponents.get(0);
//                for (String identifier : nullRequirementNode.getIdentifiers()) {
//                    topRequirementComponent.addIdentifier(identifier);
//                }
                if (targetComponent != null) {
                    usagePattern.addRequirement(nullRequirementNode, targetComponent);
                }
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

    private void addSingularComponents() {
        List<Node> nodes = graph.vertexSet().stream()
                .filter(node -> !node.isExtension() && !node.isContext()).toList();
        for (Node node : nodes) {
            if (node.isDst()) {
                tryCreatingSingularPattern(node);
            } else {
                List<Node> mappings = util.getMappingTargets(node);
                for (Node dst : mappings) {
                    tryCreatingSingularPattern(dst);
                }
                tryCreatingSingularPattern(node);
            }
        }
    }

    private void tryCreatingSingularPattern(Node node) {
        if (isCovered(node)) {
            return;
        }

        SingularPattern singularComponent = new SingularPattern(node);
        addContext(node, singularComponent);
        addMapping(node, singularComponent);
        components.add(singularComponent);
        singularPatternsLeads.put(node, singularComponent);
    }

    private boolean isCovered(Node node) {
        for (TraversalPattern component : components) {
            if (component.containsNode(node)) {
                return true;
            }
        }
        return false;
    }

    private class ComponentContexts {

        final TraversalPattern component;
        Pair<List<Node>, List<Node>> contextsPair;

        ComponentContexts(TraversalPattern component, Pair<List<Node>, List<Node>> contextsPair) {
            this.component = component;
            this.contextsPair = contextsPair;
        }
    }
}
