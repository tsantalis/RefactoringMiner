package narrator.mcp;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.Scanner;

public class McpServer {
    private static final Gson gson = new Gson();
    private static final McpHandler handler = new McpHandler();

    public static void main(String[] args) {
        System.out.println("Up and Running");
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
             BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(System.out))) {
            
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                
                try {
                    JsonObject request = JsonParser.parseString(line).getAsJsonObject();
                    JsonObject response = handler.handle(request);
                    writer.write(gson.toJson(response));
                    writer.newLine();
                    writer.flush();
                } catch (Exception e) {
                    sendError(writer, e);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static void sendError(BufferedWriter writer, Exception e) {
        try {
            JsonObject error = new JsonObject();
            error.addProperty("jsonrpc", "2.0");
            error.add("error", new JsonObject());
            error.getAsJsonObject("error").addProperty("code", -32603);
            error.getAsJsonObject("error").addProperty("message", "Internal error: " + e.getMessage());
            writer.write(gson.toJson(error));
            writer.newLine();
            writer.flush();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
