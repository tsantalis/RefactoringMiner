package narrator.llm;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;

import java.io.IOException;

public class GroqClient {
    static ChatLanguageModel model;
    static ClientMode mode = ClientMode.ONLINE;

    static {
        try {
            model = OpenAiChatModel.builder()
                    .baseUrl("https://api.groq.com/openai/v1")
                    .apiKey(ModelsKey.getGroqKey())
                    .modelName("llama-3.3-70b-versatile")
                    .build();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static String generate(String message) {
        if (mode == ClientMode.OFFLINE) {
            return "";
        }

        String response = null;

        while (response == null) {
            try {
                response = model.generate(message);
            } catch (Exception ignored) {
                System.out.println(ignored);
                try {
                    Thread.sleep(10000);
                } catch (InterruptedException e) {
                }
            }
        }

        return response;
    }

//    public static String generateWithTools(List<ChatMessage> messages, List<ToolSpecification> tools) {
//        AiMessage result = model.generate(messages, tools).content();
//        result.content().
//    }

    public static ChatLanguageModel getModel() {
        return model;
    }
}


