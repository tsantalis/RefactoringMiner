package org.refactoringminer.astDiff.graph.cluster.traverse;

import com.google.gson.JsonObject;
import org.refactoringminer.astDiff.graph.*;
import org.jgrapht.Graph;

import java.util.*;

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

    public List<Node> getSequence() {
        List<Node> sequence = new ArrayList<>();
        Node current = getHead();
        if (current == null) {
            return sequence;
        }

        while (current != null) {
            sequence.add(current);
            Node next = null;
            for (Edge edge : this.getGraph().outgoingEdgesOf(current)) {
                if (edge.getType().equals(EdgeType.SUCCESSION)) {
                    next = this.getGraph().getEdgeTarget(edge);
                    break;
                }
            }
            current = next;
        }
        return sequence;
    }

    @Override
    public String base() {
        StringBuilder prompt = new StringBuilder();

        prompt.append("# Subject:\n```\n");
        List<Node> sequence = getSequence();
        List<String> basePrompts = sequence.stream().map(node -> node.base(this.getGraph()))
                .toList();
        prompt.append(String.join("\n---\n", basePrompts));
        prompt.append("\n```");

        List<Node> semanticContexts = sequence.get(0).getSemanticContexts(this.getGraph());
        if (!semanticContexts.isEmpty()) {
            prompt.append("\n# Surrounding:\n```\n");
            prompt.append(semanticContexts.get(0).mapping(this.getGraph()));
            prompt.append("\n```\n");
        }

        List<String> mappingHunks = new ArrayList<>();
        List<TraversalPattern.MappingGroup> aggregated = TraversalPattern.aggregateByMapping(this.getGraph(), sequence).stream()
                .filter(mg -> !mg.sources().isEmpty() && !mg.targets().isEmpty()).toList();
        for (TraversalPattern.MappingGroup mg : aggregated) {
            List<Node> sources = mg.sources();
            List<Node> targets = mg.targets();

            Set<String> allOps = new HashSet<>();
            for (Node n : sources) {
                allOps.addAll(n.getOperations(this.getGraph()));
            }
            for (Node n : targets) {
                allOps.addAll(n.getOperations(this.getGraph()));
            }
            String ops = String.join(" and ", allOps.stream().map(op -> op + "d").toList());

            String hunk =
                String.join("\n", sources.stream().map(n -> n.base(this.getGraph())).toList())
                    + "\n\n" + ops + " to:\n\n"
                    + String.join("\n", targets.stream().map(n -> n.base(this.getGraph())).toList());
            mappingHunks.add(hunk);
        }

        if (!mappingHunks.isEmpty()) {
            prompt.append("\n# Context:\n```\n");
            prompt.append(String.join("\n---\n", mappingHunks));
            prompt.append("\n```");
        }

        return prompt.toString();
    }

    @Override
    public List<Node> getMains() {
        return getSequence();
    }

    @Override
    public JsonObject stringify() {
        JsonObject result = super.stringify();

        result.addProperty("headId", getHead().getId());

        return result;
    }
}
