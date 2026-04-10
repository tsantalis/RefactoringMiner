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

        for (UsagePattern usagePattern : usagePatterns) {
            usagePattern.breakCircularDependencies();

            List<Node> emptyRequirementNodes = usagePattern.getRequirements().entrySet().stream()
                    .filter(entry -> entry.getValue() == null).map(Entry::getKey).toList();
            emptyRequirementNodes.forEach(node -> {
                usagePattern.breakRequirement(node);
                addMapping(node, usagePattern);
            });
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
                usageComponent.addRequirement(usedNode, null);
            } else {
                addMapping(usedNode, usageComponent);
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

            List<Pair<TraversalPattern, Integer>> headDescendants = componentsContexts.entrySet()
                    .stream()
                    .map(componentContexts -> new Pair<>(componentContexts.getKey(),
                            componentContexts.getValue().indexOf(subjectContextsHead)))
                    .filter(componentIndex -> componentIndex.second != -1).toList();
            List<Pair<TraversalPattern, Integer>> headMergeables = headDescendants.stream()
                    .filter(descendant -> descendant.second == 0).toList();
            if (headDescendants.size() > 1) {
                if (headMergeables.size() > 1) {
                    mergeByContext(headMergeables.stream().map(headMergeable -> headMergeable.first)
                            .toList(), componentsContexts);
                } else {
                    iteratedComponents.add(subject);
                }

                continue;
            }

            Set<Node> headMappings = new HashSet<>();
            headMappings.addAll(util.getMappingSources(subjectContextsHead));
            headMappings.addAll(util.getMappingTargets(subjectContextsHead));
            Optional<Node> optionalHeadMapping = headMappings.stream().findFirst();

            if (optionalHeadMapping.isEmpty()) {
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
            if (mappingDescendants.isEmpty()) {
                componentsContexts.get(subject).remove(subjectContextsHead);
                if (componentsContexts.get(subject).isEmpty()) {
                    componentsContexts.remove(subject);
                }
                continue;
            }

            List<TraversalPattern> allDescendants = new ArrayList<>();
            allDescendants.add(headMergeables.get(0).first);
            allDescendants.add(mappingDescendants.get(0).first);

            mergeByContext(allDescendants, componentsContexts);
        }
    }

    private void mergeByContext(List<TraversalPattern> mergeComponents,
            Map<TraversalPattern, List<Node>> componentsContexts) {
        List<List<Node>> subjectsContexts = componentsContexts.entrySet().stream()
                .filter(entry -> mergeComponents.contains(entry.getKey()))
                .map(Entry::getValue).toList();
        Set<Node> heads = subjectsContexts.stream().map(subjectContexts -> subjectContexts.get(0))
                .collect(Collectors.toSet());
        List<Node> subjectContexts = subjectsContexts.get(0);

        for (TraversalPattern mergeComponent : mergeComponents) {
            componentsContexts.remove(mergeComponent);
            components.remove(mergeComponent);
        }

        TraversalComponent mergedComponent = new TraversalComponent(mergeComponents,
                ReasonType.CONTEXT);
        components.add(mergedComponent);

        for (UsagePattern usagePattern : usagePatterns) {
            List<Node> existingHeads = heads.stream()
                    .filter(head -> usagePattern.getRequirements().containsKey(head)).toList();
            for (Node head : existingHeads) {
                usagePattern.getRequirements().put(head, mergedComponent);
            }
        }

        if (subjectContexts.size() > 1) {
            componentsContexts.put(mergedComponent,
                    subjectContexts.subList(1, subjectContexts.size()));
        }

    }

    private void mergeByUsageChain() {
        Map<UsagePattern, Set<AggregatorPattern>> usageRequirements = new HashMap<>();
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
