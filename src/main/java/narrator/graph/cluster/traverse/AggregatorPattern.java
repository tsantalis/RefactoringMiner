package narrator.graph.cluster.traverse;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;
import narrator.graph.Node;
import narrator.graph.SrcDst;
import narrator.graph.cluster.Cluster;

public class AggregatorPattern extends TraversalPattern {

    @Override
    public String extended(Cluster cluster) {
        List<Node> mains = getMains(cluster);
        List<Node> sides = getSides(cluster);

        StringBuilder prompt = new StringBuilder();
        prompt.append("# Subject:\n```\n");

        List<List<Node>> mainGroups = new ArrayList<>();
        Map<Set<Node>, List<Node>> mainPartnerMap = new HashMap<>();
        for (Node n : mains) {
            List<Node> partners = (n.getSrcDst() == SrcDst.SRC) ? n.getMappingTargets(cluster) : n.getMappingSources(cluster);
            if (partners.isEmpty()) {
                mainGroups.add(new ArrayList<>(List.of(n)));
            } else {
                Set<Node> set = new HashSet<>(partners);
                List<Node> group = mainPartnerMap.computeIfAbsent(set, k -> {
                    List<Node> g = new ArrayList<>();
                    mainGroups.add(g);
                    return g;
                });
                group.add(n);
            }
        }

        List<String> subjectHunks = new ArrayList<>();
        for (List<Node> group : mainGroups) {
            Node rep = group.get(0);
            List<Node> partners = (rep.getSrcDst() == SrcDst.SRC) ? rep.getMappingTargets(cluster) : rep.getMappingSources(cluster);
            if (partners.isEmpty()) {
                subjectHunks.add(String.join("\n", group.stream().map(n -> n.base(cluster)).toList()));
            } else {
                subjectHunks.add(buildMappingHunk(group, partners, cluster));
            }
        }
        prompt.append(String.join("\n---\n", subjectHunks));
        prompt.append("\n```\n");

        // Add 'Surrounding' section for TraversalComponent with mergeContexts
        if (this instanceof TraversalComponent tc && tc.getMergeContexts() != null && !tc.getMergeContexts().isEmpty()) {
            Set<Node> mergeContexts = tc.getMergeContexts();
            Node firstMergeContext = mergeContexts.iterator().next();
            prompt.append("\n# Surrounding:\n```\n").append(firstMergeContext.mapping(cluster)).append("\n```\n");
        }

        List<List<Node>> sideGroups = new ArrayList<>();
        Map<Set<Node>, List<Node>> sidePartnerMap = new HashMap<>();
        for (Node n : sides) {
            List<Node> partners = (n.getSrcDst() == SrcDst.SRC) ? n.getMappingTargets(cluster) : n.getMappingSources(cluster);
            if (!partners.isEmpty()) {
                Set<Node> set = new HashSet<>(partners);
                List<Node> group = sidePartnerMap.computeIfAbsent(set, k -> {
                    List<Node> g = new ArrayList<>();
                    sideGroups.add(g);
                    return g;
                });
                group.add(n);
            }
        }

        if (!sideGroups.isEmpty()) {
            prompt.append("\n# Context:\n```\n");
            List<String> contextHunks = new ArrayList<>();
            for (List<Node> group : sideGroups) {
                Node rep = group.get(0);
                List<Node> partners = (rep.getSrcDst() == SrcDst.SRC) ? rep.getMappingTargets(cluster) : rep.getMappingSources(cluster);
                contextHunks.add(buildMappingHunk(group, partners, cluster));
            }
            prompt.append(String.join("\n---\n", contextHunks));
            prompt.append("\n```");
        }

        return prompt.toString();
    }

    private String buildMappingHunk(List<Node> group, List<Node> partners, Cluster cluster) {
        Set<String> allOps = new HashSet<>();
        for (Node n : group) {
            allOps.addAll(n.getOperations(cluster));
        }
        String ops = String.join(" and ", allOps.stream().map(op -> op + "d").toList());

        Node rep = group.get(0);
        Collection<Node> from = (rep.getSrcDst() == SrcDst.SRC) ? group : partners;
        Collection<Node> to = (rep.getSrcDst() == SrcDst.SRC) ? partners : group;

        StringBuilder hunk = new StringBuilder();
        hunk.append(String.join("\n", from.stream().map(n -> n.base(cluster)).toList()))
            .append("\n\n").append(ops).append(" to:\n\n")
            .append(String.join("\n", to.stream().map(n -> n.base(cluster)).toList()));
        return hunk.toString();
    }

    public List<Node> getMains(Cluster cluster) {
        Narrator narrator = new Narrator(this);
        List<TraversalPattern> leaves = narrator.getNarrative(GrainLevel.LEAF);
        if (leaves.isEmpty()) {
            return List.of();
        }

        Set<Node> mainsOrdered = new LinkedHashSet<>();
        Set<Node> sidesSet = new LinkedHashSet<>();

        for (TraversalPattern leaf : leaves) {
            for (Node main : leaf.getMains(cluster)) {
                if (!mainsOrdered.contains(main)) {
                    sidesSet.remove(main);
                    mainsOrdered.add(main);
                }
            }
            for (Node side : leaf.getSides(cluster)) {
                sidesSet.add(side);
            }
        }

        return new ArrayList<>(mainsOrdered);
    }

    public List<Node> getSides(Cluster cluster) {
        Set<Node> mainsSet = new HashSet<>(getMains(cluster));
        Narrator narrator = new Narrator(this);
        List<TraversalPattern> leaves = narrator.getNarrative(GrainLevel.LEAF);
        if (leaves.isEmpty()) {
            return List.of();
        }

        Set<Node> sidesOrdered = new LinkedHashSet<>();

        for (TraversalPattern leaf : leaves) {
            for (Node side : leaf.getSides(cluster)) {
                if (!mainsSet.contains(side)) {
                    sidesOrdered.add(side);
                }
            }
        }

        return new ArrayList<>(sidesOrdered);
    }

    Set<TraversalPattern> subs = new HashSet<>();

    protected boolean containsNode(Node node, Set<TraversalPattern> visited) {
        if (!visited.add(this)) {
            return false;
        }

        boolean isRootNode = getGraph().vertexSet().stream()
                .anyMatch(coreNode -> coreNode.equals(node));
        if (isRootNode) {
            return true;
        }

        for (TraversalPattern sub : subs) {
            if (sub instanceof AggregatorPattern) {
                if (((AggregatorPattern) sub).containsNode(node, visited)) {
                    return true;
                }
            } else {
                if (!visited.add(this)) {
                    continue;
                }
                if (sub.containsNode(node)) {
                    return true;
                }
            }
        }

        return false;
    }

    protected Set<Node> vertexSet(Set<AggregatorPattern> visited) {
        if (!visited.add(this)) {
            return new HashSet<>();
        }

        Set<Node> result = new HashSet<>(getGraph().vertexSet());
        for (TraversalPattern sub : subs) {
            if (sub instanceof AggregatorPattern) {
                result.addAll(((AggregatorPattern) sub).vertexSet(visited));
            } else {
                if (!visited.add(this)) {
                    continue;
                }
                result.addAll(sub.vertexSet());
            }
        }
        return result;
    }

    // TODO: test and validate
    protected void breakCircularDependencies(List<AggregatorPattern> path) {
        List<AggregatorPattern> acceptableSubs = subs.stream()
                .filter(sub -> sub instanceof AggregatorPattern).map(sub -> (AggregatorPattern) sub)
                .toList();
        if (acceptableSubs.isEmpty()) {
            return;
        }

        List<AggregatorPattern> newPath = new ArrayList<>(path);
        newPath.add(this);

        if (this instanceof UsagePattern thisUsage) {
            List<AggregatorPattern> circularSubs = acceptableSubs.stream().filter(newPath::contains)
                    .toList();
            for (AggregatorPattern circularSub : circularSubs) {
                subs.remove(circularSub);

                List<Node> requirementNodes = thisUsage.getRequirements().entrySet().stream()
                        .filter(entry -> entry.getValue().equals(circularSub)).map(
                                Entry::getKey).toList();
                if (requirementNodes.size() > 1) {
                    System.out.println("Requirement Breaking Failure");
                    continue;
                }
                for (Node requirementNode : requirementNodes) {
                    thisUsage.breakRequirement(requirementNode);
                }
            }
        }

        for (AggregatorPattern sub : acceptableSubs) {
            sub.breakCircularDependencies(newPath);
        }
    }
}
