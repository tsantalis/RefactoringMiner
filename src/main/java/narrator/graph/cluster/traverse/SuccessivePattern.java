package narrator.graph.cluster.traverse;

import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import narrator.graph.Context;
import narrator.graph.Edge;
import narrator.graph.EdgeType;
import narrator.graph.Node;
import narrator.graph.NodeType;
import narrator.graph.SrcDst;
import narrator.graph.cluster.Cluster;
import org.jgrapht.Graph;

public class SuccessivePattern extends TraversalPattern implements Leaf {

    Node cachedHead;

    SuccessivePattern() {
        nodeType = NodeType.SUCCESSIVE;
    }

    private Node getHead() {
        if (cachedHead == null) {
            Graph<Node, Edge> graph = getGraph();
            for (Node node : graph.vertexSet()) {
                List<Edge> outSuccession = graph.outgoingEdgesOf(node).stream()
                        .filter(edge -> edge.getType().equals(EdgeType.SUCCESSION)).toList();
                List<Edge> inSuccession = graph.incomingEdgesOf(node).stream()
                        .filter(edge -> edge.getType().equals(EdgeType.SUCCESSION)).toList();
                if (inSuccession.isEmpty() && !outSuccession.isEmpty()) {
                    cachedHead = node;
                    break;
                }
            }
        }

        return cachedHead;
    }

    @Override
    public Node getLead() {
        return getHead();
    }

    public List<Node> getSequence(Cluster cluster) {
        List<Node> sequence = new ArrayList<>();
        Node current = getHead();
        if (current == null) {
            return sequence;
        }

        Graph<Node, Edge> graph = cluster.getGraph();
        while (current != null) {
            sequence.add(current);
            Node next = null;
            for (Edge edge : graph.outgoingEdgesOf(current)) {
                if (edge.getType().equals(EdgeType.SUCCESSION)) {
                    next = graph.getEdgeTarget(edge);
                    break;
                }
            }
            current = next;
        }
        return sequence;
    }

    @Override
    public String base(Cluster cluster) {
        StringBuilder prompt = new StringBuilder();

        prompt.append("# Subject:\n```\n");
        List<Node> sequence = getSequence(cluster);
        List<String> basePrompts = sequence.stream()
                .map(node -> node.base(cluster))
                .toList();
        prompt.append(String.join("\n---\n", basePrompts));
        prompt.append("\n```");

        Node semanticContext = sequence.get(0).getSemanticContexts(cluster).get(0);
        if (semanticContext != null) {
            prompt.append("\n# Surrounding:\n```\n");
            prompt.append(semanticContext.mapping(cluster));
            prompt.append("\n```\n");
        }

        List<String> mappingHunks = new ArrayList<>();
        Map<Set<Node>, List<Node>> aggregated = new LinkedHashMap<>();
    
        for (Node node : sequence) {
            List<Node> partners = node.getSrcDst() == SrcDst.SRC ? node.getMappingTargets(cluster) : node.getMappingSources(cluster);
            if (partners.isEmpty()) continue;
        
            Set<Node> partnerSet = new HashSet<>(partners);
            aggregated.computeIfAbsent(partnerSet, k -> new ArrayList<>()).add(node);
        }

        for (Map.Entry<Set<Node>, List<Node>> entry : aggregated.entrySet()) {
            List<Node> group = entry.getValue();
            Set<Node> partners = entry.getKey();
            
            Set<String> allOps = new HashSet<>();
            for (Node n : group) {
                allOps.addAll(n.getOperations(cluster));
            }
            String ops = String.join(" and ", allOps.stream().map(op -> op + "d").toList());
            
            StringBuilder hunk = new StringBuilder();
            Node rep = group.get(0);
            
            Collection<Node> from = (rep.getSrcDst() == SrcDst.SRC) ? group : partners;
            Collection<Node> to = (rep.getSrcDst() == SrcDst.SRC) ? partners : group;

            hunk.append(String.join("\n", from.stream().map(n -> n.base(cluster)).toList()))
                .append("\n\n").append(ops).append(" to:\n\n")
                .append(String.join("\n", to.stream().map(n -> n.base(cluster)).toList()));
            mappingHunks.add(hunk.toString());
        }

        if (!mappingHunks.isEmpty()) {
            prompt.append("\n# Context:\n```\n");
            prompt.append(String.join("\n---\n", mappingHunks));
            prompt.append("\n```");
        }

        return prompt.toString();
    }

    @Override
    public String extended(Cluster cluster, GrainLevel level) {
        return base(cluster);
    }

    @Override
    public List<Node> getMains(Cluster cluster) {
        return getSequence(cluster);
    }

    @Override
    public JsonObject stringify() {
        JsonObject result = super.stringify();

        result.addProperty("headId", getHead().getId());

        return result;
    }
}
