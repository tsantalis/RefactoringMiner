package narrator.llm;

import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.model.chat.request.json.JsonArraySchema;
import dev.langchain4j.model.chat.request.json.JsonObjectSchema;
import dev.langchain4j.model.chat.request.json.JsonStringSchema;

public class Tools {
    public static ToolSpecification getCodeSnippet = ToolSpecification.builder()
            .name("getCodeSnippet")
            .description("Returns the code snippet of methods")
            .parameters(JsonObjectSchema.builder()
                    .addProperty("methods", JsonArraySchema.builder().items(JsonStringSchema.builder().build()).build())
                    .required("methods")
                    .build())
            .build();
}
