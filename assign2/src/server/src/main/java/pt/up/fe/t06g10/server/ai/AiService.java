package pt.up.fe.t06g10.server.ai;

import pt.up.fe.t06g10.shared.model.Message;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

public class AiService {
    private static final String OLLAMA_URL = "http://localhost:11434/api/chat";
    private final HttpClient http = HttpClient.newHttpClient();

    public String query(String systemPrompt, List<Message> history) throws IOException, InterruptedException {
        StringBuilder messages = new StringBuilder("[");
        messages.append("{\"role\":\"system\",\"content\":").append(jsonString(systemPrompt)).append("}");
        for (Message m : history) {
            messages.append(",{\"role\":\"user\",\"content\":")
                    .append(jsonString(m.getSender() + ": " + m.getContent()))
                    .append("}");
        }
        messages.append("]");

        String body = "{\"model\":\"llama3\",\"messages\":" + messages + ",\"stream\":false}";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(OLLAMA_URL))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());

        String content = response.body();
        int start = content.indexOf("\"content\":\"") + 11;
        int end = content.indexOf("\"", start);
        return content.substring(start, end);
    }

    private String jsonString(String s) {
        return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "\\r") + "\"";
    }
}
