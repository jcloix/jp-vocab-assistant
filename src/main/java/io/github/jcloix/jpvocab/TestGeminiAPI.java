package io.github.jcloix.jpvocab;

import io.github.jcloix.jpvocab.service.GeminiService;

public class TestGeminiAPI {
    public static void main(String[] args) {
        // Get API key from environment variable
        String apiKey = System.getenv("GEMINI_API_KEY");
        if (apiKey == null || apiKey.isEmpty()) {
            System.err.println("ERROR: GEMINI_API_KEY environment variable not set");
            System.err.println("Get your API key from: https://aistudio.google.com/app/apikey");
            System.exit(1);
        }

        // Model options: "gemini-flash-latest"
        String modelName = "gemini-flash-latest";

        System.out.println("Testing Gemini API...");
        System.out.println("Model: " + modelName);
        System.out.println();

        GeminiService generator = new GeminiService(apiKey, modelName);

        // Test with various Japanese words
        String[] testWords = {"勉強", "友達"};

        for (String word : testWords) {
            try {
                System.out.println("Word: " + word);
                String sentence = generator.generateExampleSentence(word);
                System.out.println("Generated: " + sentence);
                System.out.println();
            } catch (Exception e) {
                System.err.println("Error generating sentence for '" + word + "': " + e.getMessage());
                e.printStackTrace();
            }
        }

        generator.close();
        System.out.println("✓ Test complete!");
    }
}