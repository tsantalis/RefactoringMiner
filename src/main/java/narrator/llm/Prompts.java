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
            
            Strictly adhere to the following guideline:
            - Identify concrete purposes behind the changes instead of summarizing them vaguely or discussing general goals.
            """;

    private static final String componentTemplate = """
            A commit in a Java project includes multiple groups of changes. Each group has been described independently as follows:
            
            %s
            
            As a review assistant, your task is to help the reviewer understand the collective intent behind these groups by providing a summary.
            
            Strictly adhere to the following guidelines:
            - Identify concrete purposes behind the changes instead of summarizing them vaguely or discussing general goals.
            """;

    public static String getSuccessivePatternPrompt(String textualRepresentation) {
        return String.format(successivePatternTemplate, textualRepresentation);
    }

    public static String getDeclarationPatternPrompt(String declarationsRepresentation, String useRepresentation, String extensionsRepresentation) {
        StringBuilder result = new StringBuilder();

        result.append(String.format("""
                As part of a commit, the following changes, along with their location details, have been made to variable or field declarations in a Java project:
                ```
                %s
                ```
                """, declarationsRepresentation));

        if (extensionsRepresentation != null && !extensionsRepresentation.isEmpty()) {
            result.append(String.format("""
                    
                    These modified declarations involve calling the following methods:
                    ```
                    %s
                    ```
                    These methods are not part of the commit; they are provided only to help understand the changes.
                    """, extensionsRepresentation));
        }

        result.append(String.format("""
                
                The modified variables and fields were already in use before these changes. The updated versions now replace their previous counterparts in the same locations.
                For reference, here is an instance of how they are used in the code:
                ```
                %s
                ```
                This usage is not part of the commit and is included solely to assist in understanding the changes.
                """, useRepresentation));

        result.append("""
                
                As a review assistant, your task is to help the reviewer understand the specific purposes of these declaration changes by identifying and describing all evident intentions behind them.
                
                Strictly adhere to the following guideline:
                - Identify concrete purposes behind the changes instead of summarizing them vaguely or discussing general goals.
                """);

        System.out.println(result);
        return result.toString();
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
                    Group %s:
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
