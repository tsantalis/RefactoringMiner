package narrator.graph.cluster.traverse;

import narrator.graph.Node;
import narrator.llm.GroqClient;
import narrator.llm.Prompts;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class RequirementComponent extends TraversalComponent {
    private UsagePattern head;
    private Map<Node, TraversalPattern> requirementsMap;

    RequirementComponent(UsagePattern head, Map<Node, TraversalPattern> requirementsMap) {
        super(Stream.concat(requirementsMap.values().stream(), Stream.of(head)).toList(), requirementsMap.keySet(),
                ReasonType.REQUIREMENT);
        this.head = head;
        this.requirementsMap = requirementsMap;
    }

    @Override
    public String textualRepresentation() {
        return String.join("\n\n", reasons.stream().map(Node::getId).toList());
    }

    @Override
    public String description() throws IOException {
        String descriptionCache = getDescriptionCache();
        if (descriptionCache != null) {
            return descriptionCache;
        }

        Map<Node, String> requirementsDescription = new HashMap<>();
        for (Map.Entry<Node, TraversalPattern> requirement : requirementsMap.entrySet()) {
            requirementsDescription.put(requirement.getKey(), requirement.getValue().description());
        }

        String generatedDescription = GroqClient.generate(Prompts.getRequirementPrompt(head, requirementsDescription));

        setDescriptionCache(generatedDescription);

        return generatedDescription;
    }
}
