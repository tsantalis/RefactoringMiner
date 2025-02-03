package narrator.graph.cluster.traverse;

import narrator.graph.Node;
import narrator.llm.GroqClient;
import narrator.llm.Prompts;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TraversalComponent extends TraversalPattern {
    private List<TraversalPattern> components;
    private Set<Node> reasons;
    private ReasonType reasonType;

    TraversalComponent(List<TraversalPattern> components, Set<Node> reasons, ReasonType reasonType) {
        this.components = components;
        this.reasons = reasons;
        this.reasonType = reasonType;
    }

    @Override
    public boolean containsNode(Node node) {
        Node foundNode = getGraph().vertexSet().stream()
                .filter(reasonNode -> reasonNode.equals(node))
                .findFirst().orElse(null);
        if (foundNode != null) {
            return true;
        }

        for (TraversalPattern component : components) {
            if (component.containsNode(node)) {
                return true;
            }
        }

        return false;
    }

    public List<TraversalPattern> getComponents() {
        return components;
    }

    @Override
    public Node getLead() {
        Set<Node> nodes = getGraph().vertexSet();
        if (!nodes.isEmpty()) {
            return nodes.iterator().next();
        }

        return components.get(0).getLead();
    }

    @Override
    public Set<Node> vertexSet() {
        Set<Node> result = new HashSet<>(getGraph().vertexSet());
        for (TraversalPattern component : components) {
            result.addAll(component.vertexSet());
        }
        return result;
    }

    @Override
    public String description() throws IOException {
        String descriptionCache = super.description();
        if (descriptionCache != null) {
            return descriptionCache;
        }

        List<String> componentsDescription = new ArrayList<>();
        for (TraversalPattern component : components) {
            componentsDescription.add(component.description());
        }
        String generatedDescription = GroqClient.generate(Prompts.getComponentPatternPrompt(componentsDescription, reasons, reasonType));

        setDescriptionCache(generatedDescription);

        return generatedDescription;
    }
}
