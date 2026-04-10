package narrator.graph.cluster.traverse;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import narrator.graph.Node;

public class AggregatorPattern extends TraversalPattern {

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
