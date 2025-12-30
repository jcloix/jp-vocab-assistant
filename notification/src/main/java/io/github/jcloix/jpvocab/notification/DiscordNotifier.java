package io.github.jcloix.jpvocab.notification;

import io.github.jcloix.jpvocab.util.LambdaLogger;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

public class DiscordNotifier {

    private static final String API_URL = "https://discord.com/api/v10";
    private static final HttpClient CLIENT = HttpClient.newHttpClient();

    private static final int MAX_CHOICES = 4;

    private static final String[] BODY_EMOJI = {"1️⃣", "2️⃣", "3️⃣", "4️⃣"};
    private static final String[] BUTTON_LABELS = {"①", "②", "③", "④"};

    // ==================================================
    // Public API
    // ==================================================

    public static void sendMessage(String content) throws Exception {
        validateContent(content);

        String json = "{ \"content\": \"" + escapeJson(content) + "\" }";
        send(json);
    }

    public static void sendTaskMessage(String word, String rawWord, int rowId, List<String> choices) throws Exception {
        validateTask(word, choices);

        List<String> safeChoices = normalizeChoices(choices);

        String content = buildContentBody(rawWord, rowId, safeChoices);
        String components = buildButtons(word, rowId, safeChoices.size());

        String json = """
            {
              "content": "%s",
              "components": [%s]
            }
            """.formatted(
                escapeJson(content),
                components
        );

        logPayload(json);
        send(json);
    }

    // ==================================================
    // Validation / normalization
    // ==================================================

    private static void validateContent(String content) {
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("Discord message content is empty");
        }
    }

    private static void validateTask(String word, List<String> choices) {
        if (word == null || word.isBlank()) {
            throw new IllegalArgumentException("Word is empty");
        }
        if (choices == null || choices.isEmpty()) {
            throw new IllegalArgumentException("No choices provided");
        }
    }

    private static List<String> normalizeChoices(List<String> choices) {
        if (choices.size() > MAX_CHOICES) {
            LambdaLogger.log("⚠ Too many choices (" + choices.size() + "), truncating to " + MAX_CHOICES);
            return choices.subList(0, MAX_CHOICES);
        }
        return choices;
    }

    // ==================================================
    // Builders
    // ==================================================

    private static String buildContentBody(String word, int rowId, List<String> choices) {
        StringBuilder sb = new StringBuilder();

        sb.append("📘 **New vocabulary task**\n\n")
                .append("📝 Word: **").append(word).append("**\n")
                .append("📍 Row: ").append(rowId).append("\n\n")
                .append("Choose the correct sentence:\n\n");

        for (int i = 0; i < choices.size(); i++) {
            sb.append(BODY_EMOJI[i])
                    .append(" ")
                    .append(choices.get(i))
                    .append("\n");
        }

        return sb.toString();
    }

    private static String buildButtons(String word, int rowId, int count) {
        StringBuilder sb = new StringBuilder();

        sb.append("""
            {
              "type": 1,
              "components": [
        """);

        for (int i = 0; i < count; i++) {
            sb.append(String.format(
                    """
                    { "type": 2, "label": "%s Choice %d", "style": 1, "custom_id": "word_%s_row_%d_choice_%d" }
                    """,
                    BUTTON_LABELS[i],
                    i + 1,
                    word,
                    rowId,
                    i
            ));

            if (i < count - 1) {
                sb.append(",");
            }
        }

        sb.append("""
              ]
            }
        """);

        return sb.toString();
    }

    // ==================================================
    // HTTP / infrastructure
    // ==================================================

    private static void send(String json) throws Exception {
        String token = System.getenv("DISCORD_BOT_TOKEN");
        String channelId = System.getenv("DISCORD_CHANNEL_ID");

        if (token == null || channelId == null) {
            throw new IllegalStateException("Missing Discord environment variables");
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL + "/channels/" + channelId + "/messages"))
                .header("Authorization", "Bot " + token)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> response =
                CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() >= 300) {
            throw new RuntimeException(
                    "Discord error (" + response.statusCode() + "): " + response.body()
            );
        }
    }

    private static void logPayload(String json) {
        LambdaLogger.log("=== DISCORD JSON PAYLOAD ===");
        LambdaLogger.log(json);
        LambdaLogger.log("===========================");
    }

    // ==================================================
    // Utils
    // ==================================================

    private static String escapeJson(String s) {
        return s
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n");
    }
}
