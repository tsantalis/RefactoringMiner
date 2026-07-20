package org.refactoringminer.astDiff.graph.cluster.traverse;

import org.refactoringminer.astDiff.graph.Node;

import java.util.*;
import java.util.Map.Entry;

public class AggregatorPattern extends TraversalPattern {

    Set<TraversalPattern> subs = new HashSet<>();

    @Override
    public List<Node> getMains() {
        List<TraversalPattern> leaves = this.getNarrator().getNarrative(GrainLevel.LEAF);
        Set<Node> superMainsOrdered = new LinkedHashSet<>();
        for (TraversalPattern leaf : leaves) {
            superMainsOrdered.addAll(leaf.getMains());
        }

        Set<Node> subsMains = new HashSet<>();
        for (TraversalPattern sub : subs) {
            subsMains.addAll(sub.getMains());
        }

        return superMainsOrdered.stream().filter(subsMains::contains).toList();
    }

    @Override
    public List<Node> getSides() {
        List<TraversalPattern> leaves = this.getNarrator().getNarrative(GrainLevel.LEAF);
        Set<Node> superSidesOrdered = new LinkedHashSet<>();
        for (TraversalPattern leaf : leaves) {
            superSidesOrdered.addAll(leaf.getSides());
        }

        List<Node> mains = this.getMains();
        return superSidesOrdered.stream().filter(side -> !mains.contains(side)).toList();
    }

    protected boolean containsNode(Node node, Set<TraversalPattern> visited) {
        if (!visited.add(this)) {
            return false;
        }

        boolean isRootNode = getGraph().vertexSet().stream().anyMatch(coreNode -> coreNode.equals(node));
        if (isRootNode) {
            return true;
        }

        for (TraversalPattern sub : subs) {
            if (sub instanceof AggregatorPattern) {
                if (((AggregatorPattern) sub).containsNode(node, visited)) {
                    return true;
                }
            } else {
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
                result.addAll(sub.vertexSet());
            }
        }
        return result;
    }

    protected void breakCircularDependencies(List<AggregatorPattern> path) {
        List<AggregatorPattern> acceptableSubs = subs.stream().filter(sub -> sub instanceof AggregatorPattern).map(sub -> (AggregatorPattern) sub).toList();
        if (acceptableSubs.isEmpty()) {
            return;
        }

        List<AggregatorPattern> newPath = new ArrayList<>(path);
        newPath.add(this);

        if (this instanceof UsagePattern thisUsage) {
            List<AggregatorPattern> circularSubs = acceptableSubs.stream().filter(newPath::contains).toList();
            for (AggregatorPattern circularSub : circularSubs) {
                subs.remove(circularSub);

                List<Node> requirementNodes = thisUsage.getRequirements().entrySet().stream().filter(entry -> entry.getValue().equals(circularSub)).map(Entry::getKey).toList();
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
