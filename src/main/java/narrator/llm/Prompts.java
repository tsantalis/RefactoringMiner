package narrator.llm;

import narrator.graph.Node;
import narrator.graph.cluster.traverse.ReasonType;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class Prompts {
    private static final String successivePatternTemplate = """
            The following code, along with its location details, has been added to a Java project in a commit:
            ```
            %s
            ```
            
            As a review assistant, your task is to help the reviewer understand the purpose of this added code by describing all evident intentions behind it.
            """;

    private static final String declarationPatternTemplate = """
            As part of a commit, changes have been made to variable or field declarations in a Java project.
            Below are the modified declarations along with an instance of their usage for context:
            ```
            %s
            ```
            
            As a review assistant, your task is to help the reviewer understand the purpose of these declaration changes by describing all evident intentions behind them.
            Use the usage instance only to understand the motivations behind the declaration changes.
            """;

    private static final String componentTemplate = """
            A commit in a Java project includes multiple groups of changes. Each group has been described independently as follows:
            
            %s
            
            As a review assistant, analyze these descriptions to determine their collective intent. Then, construct a description that captures the overall purpose of these groups within the commit, helping the reviewer understand how these changes fit together.
            """;

    public static String getSuccessivePatternPrompt(String textualRepresentation) {
        return String.format(successivePatternTemplate, textualRepresentation);
    }

    public static String getDeclarationPatternPrompt(String textualRepresentation) {
        return String.format(declarationPatternTemplate, textualRepresentation);
    }

    public static String getComponentPatternPrompt(List<String> componentsDescription) throws IOException {
        return getComponentPatternPrompt(componentsDescription, null, null);
    }

    public static String getComponentPatternPrompt(List<String> componentsDescription, Set<Node> reasons, ReasonType reasonType) throws IOException {
        String templateInput = "";

        templateInput += getComponentsString(componentsDescription);

        String reasonString = getReasonString(reasons, reasonType);
        if (reasonString != null) {
            templateInput += "\n" + reasonString;
        }

        return String.format(componentTemplate, templateInput);
    }

    private static String getComponentsString(List<String> componentsDescription) {
        List<String> componentsStrings = new ArrayList<>();

        int componentIndex = 0;
        for (String componentDescription : componentsDescription) {
            componentsStrings.add(String.format("""
                    Component %s:
                    ```
                    %s
                    ```
                    """, ++componentIndex, componentDescription));
        }

        return String.join("\n", componentsStrings);
    }

    private static String getReasonString(Set<Node> reasons, ReasonType reasonType) {
        if (reasons == null || reasonType == null) {
            return null;
        }

        String reasonString = null;

        // TODO: support REQUIREMENT reason type
        switch (reasonType) {
            case COMMON -> reasonString = "These components overlap in the codes below:";
            case SIMILAR -> reasonString = "All of these components contain the same changes below:";
        }

        if (reasonString == null) {
            return null;
        }

        reasonString += "\n" + String.format("""
                ```
                %s
                ```""", String.join("\nAND\n", reasons.stream().map(Node::textualRepresentation).toList()));

        return reasonString;
    }
}
