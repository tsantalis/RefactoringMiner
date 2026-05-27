package narrator.graph.cluster.traverse;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import narrator.graph.Node;
import narrator.graph.cluster.Cluster;

public class AggregatorPattern extends TraversalPattern {

    @Override
    public String extended(Cluster cluster) {
        Narrator narrator = new Narrator(this);
        List<TraversalPattern> leaves = narrator.getNarrative(GrainLevel.LEAF);
        if (leaves.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < leaves.size(); i++) {
            if (leaves.get(i) instanceof Leaf leaf) {
                sb.append(leaf.base(cluster));
            }
            if (i < leaves.size() - 1) {
                sb.append("\n---\n");
            }
        }
        return sb.toString();
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
