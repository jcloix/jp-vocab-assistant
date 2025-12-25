package io.github.jcloix.jpvocab.service;

import com.google.genai.Client;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;

public class GeminiService {

    private final Client client;
    private final String modelName;

    public GeminiService(String apiKey, String modelName) {
        // Initialize with API key
        this.client = Client.builder()
                .apiKey(apiKey)
                .build();
        this.modelName = modelName;
    }

    /**
     * Generate a Japanese example sentence for the given vocabulary word
     * @param word The Japanese word to create an example for
     * @return Generated sentence in Japanese
     */
    public String generateExampleSentence(String word) {
        try {
            String prompt = String.format(
                    "あなたは日本語の先生です。次の単語を使った自然な日本語の例文を1つ作成してください。" +
                            "レベルはJLPT N3です。例文だけを返してください。説明は不要です。\n\n" +
                            "単語: %s",
                    word
            );

            GenerateContentConfig config = GenerateContentConfig.builder()
                    .temperature(0.7f)
                    .maxOutputTokens(100)
                    .build();


            GenerateContentResponse response = client.models
                    .generateContent(modelName, prompt, config);

            return response.text().trim();

        } catch (Exception e) {
            throw new RuntimeException("Failed to generate sentence for word: " + word, e);
        }
    }

    /**
     * Close the client when done
     */
    public void close() {
        if (client != null) {
            // Client cleanup if needed
        }
    }
}