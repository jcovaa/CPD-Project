package pt.up.fe.t06g10.server.ai;

import pt.up.fe.t06g10.shared.model.Message;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;

public class AiService {
    private static final String OLLAMA_URL = "http://localhost:11434/api/chat";
    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    public String query(String systemPrompt, List<Message> history, String botName) throws IOException, InterruptedException {
        StringBuilder messages = new StringBuilder("[");
        messages.append("{\"role\":\"system\",\"content\":").append(jsonString(systemPrompt == null ? "" : systemPrompt)).append("}");
        for (Message m : history) {
            boolean isAssistant = botName != null && m.getSender().equalsIgnoreCase(botName);
            String role = isAssistant ? "assistant" : "user";
            String content = isAssistant ? m.getContent() : (m.getSender() + ": " + m.getContent());
            messages.append(",{\"role\":\"")
                    .append(role)
                    .append("\",\"content\":")
                    .append(jsonString(content))
                    .append("}");
        }
        messages.append("]");

        String body = "{\"model\":\"llama3\",\"messages\":" + messages + ",\"stream\":false}";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(OLLAMA_URL))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(30))
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IOException("Ollama error: HTTP " + response.statusCode() + " - " + response.body());
        }

        return extractContent(response.body());
    }

    private String jsonString(String s) {
        return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "\\r") + "\"";
    }

    private String extractContent(String json) throws IOException {
        int messageIndex = json.indexOf("\"message\"");
        int searchFrom = messageIndex >= 0 ? messageIndex : 0;
        int contentIndex = json.indexOf("\"content\"", searchFrom);
        if (contentIndex < 0) {
            throw new IOException("Invalid AI response: missing content");
        }
        int colon = json.indexOf(':', contentIndex);
        if (colon < 0) {
            throw new IOException("Invalid AI response: missing content value");
        }
        int startQuote = json.indexOf('"', colon + 1);
        if (startQuote < 0) {
            throw new IOException("Invalid AI response: content not quoted");
        }
        return parseJsonString(json, startQuote);
    }

    private String parseJsonString(String json, int startQuote) throws IOException {
        StringBuilder sb = new StringBuilder();
        boolean escape = false;
        for (int i = startQuote + 1; i < json.length(); i++) {
            char c = json.charAt(i);
            if (escape) {
                switch (c) {
                    case '"' -> sb.append('"');
                    case '\\' -> sb.append('\\');
                    case '/' -> sb.append('/');
                    case 'b' -> sb.append('\b');
                    case 'f' -> sb.append('\f');
                    case 'n' -> sb.append('\n');
                    case 'r' -> sb.append('\r');
                    case 't' -> sb.append('\t');
                    case 'u' -> {
                        if (i + 4 >= json.length()) {
                            throw new IOException("Invalid AI response: bad unicode escape");
                        }
                        String hex = json.substring(i + 1, i + 5);
                        try {
                            sb.append((char) Integer.parseInt(hex, 16));
                        } catch (NumberFormatException e) {
                            throw new IOException("Invalid AI response: bad unicode escape");
                        }
                        i += 4;
                    }
                    default -> sb.append(c);
                }
                escape = false;
            } else if (c == '\\') {
                escape = true;
            } else if (c == '"') {
                return sb.toString();
            } else {
                sb.append(c);
            }
        }
        throw new IOException("Invalid AI response: unterminated string");
    }
}
