package io.github.jcloix.jpvocab.service.ai.generation;

import com.google.genai.Client;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;

public class GeminiClientWrapper {

    private final Client client;

    public GeminiClientWrapper(String apiKey) {
        this.client = Client.builder()
                .apiKey(apiKey)
                .build();
    }

    public GenerateContentResponse generateContent(String model, String prompt, GenerateContentConfig config) {
        return client.models.generateContent(model, prompt, config);
    }

    public void close() {
        if (client != null) {
            // Optional cleanup
        }
    }
}
