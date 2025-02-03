package narrator.llm;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class ModelsKey {
    private static String OpenAIKey;
    private static String GroqKey;

    public static String getOpenAIKey() throws IOException {
        if (OpenAIKey != null) {
            return OpenAIKey;
        }

        populateKeys();

        return OpenAIKey;
    }

    public static String getGroqKey() throws IOException {
        if (GroqKey != null) {
            return GroqKey;
        }

        populateKeys();

        return GroqKey;
    }

    private static void populateKeys() throws IOException {
        Properties prop = new Properties();
        InputStream input = new FileInputStream("models-key.properties");
        prop.load(input);

        OpenAIKey = prop.getProperty("OpenAIKey");
        GroqKey = prop.getProperty("GroqKey");
    }
}
