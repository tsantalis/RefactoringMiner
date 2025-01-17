package narrator.openai;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Properties;

public class OpenAIClient {
    private static final String API_ENDPOINT = "https://api.openai.com/v1/chat/completions";

    private static final String API_KEY;

    static {
        try {
            Properties prop = new Properties();
            InputStream input = new FileInputStream("openai-key.properties");
            prop.load(input);
            API_KEY = prop.getProperty("OpenAIKey");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static final String MODEL = "gpt-4o";

    private static String chat(JSONArray messages) throws Exception {
        URL obj = new URL(API_ENDPOINT);
        HttpURLConnection con = (HttpURLConnection) obj.openConnection();
        con.setRequestMethod("POST");
        con.setRequestProperty("Authorization", "Bearer " + API_KEY);
        con.setRequestProperty("Content-Type", "application/json");

        JSONObject data = new JSONObject();
        data.put("model", MODEL);
        data.put("messages", messages);

        con.setDoOutput(true);
        con.getOutputStream().write(data.toString().getBytes());

        String output =
                new BufferedReader(new InputStreamReader(con.getInputStream())).lines().reduce((a, b) -> a + b).get();

        return new JSONObject(output).getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString(
                "content");
    }

    public static String getClusterDescription(String cluster) throws Exception {
        JSONArray messages = new JSONArray() {{
            put(new JSONObject().put("role", "system").put("content", """
                    You will be provided with a segment of a commit that contains Java code additions which are related to each other:
                    ---
                    PATH/FILE:
                    ...
                    ADDITION
                    ...
                    ADDITION
                    IN
                    CONTEXT
                    ...
                    
                    PATH/FILE:
                    ...
                    ADDITION
                    IN
                    CONTEXT
                    ...
                    ---
                    
                    The context is provided for some of the additions to clarify where the addition has occurred. It may include the addition itself or simply indicate the location of the addition. For example, the context of a return statement addition is the signature of the method it returns from.
                    
                    Your task is to analyze these additions and identify all underlying goals and intentions.
                    
                    When performing this task, adhere to the following guidelines:
                    - Clearly identify and articulate the individual goals and intentions behind each code addition, as well as their collective objectives.
                    - Consider both explicit goals (e.g., bug fixes, new features) and implicit intentions (e.g., optimization, readability, maintainability), including those that might be subtle or less apparent.
                    - Leverage any provided context to enhance your analysis and integrate it where relevant.
                    """));
            put(new JSONObject().put("role", "user").put("content", cluster));
        }};

        return chat(messages);
    }

    public static String getCommitDescription(String clustersDescription) throws Exception {
        JSONArray messages = new JSONArray() {{
            put(new JSONObject().put("role", "system").put("content", """
                    
                    """));
            put(new JSONObject().put("role", "user").put("content", clustersDescription));
        }};

        return chat(messages);
    }
}
