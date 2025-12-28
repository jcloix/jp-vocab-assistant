package io.github.jcloix.jpvocab;

import io.github.jcloix.jpvocab.domain.normalization.NormalizedWord;
import io.github.jcloix.jpvocab.domain.normalization.WordNormalizer;
import io.github.jcloix.jpvocab.service.ai.generation.*;
import io.github.jcloix.jpvocab.util.LambdaLogger;


import java.util.ArrayList;
import java.util.List;

public class TestGeminiAPI {

    public static void main(String[] args) {
        String apiKey = System.getenv("GEMINI_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            System.err.println("ERROR: GEMINI_API_KEY environment variable not set");
            System.exit(1);
        }

        LambdaLogger.log("Testing GeminiTextGenerator (BATCH MODE)\n");

        // Initialize DynamoDB and QuotaManager
        QuotaManager quotaManager = new InMemoryQuotaManager();
        // Create GeminiTextGenerator
        TextGenerator geminiGenerator = new GeminiTextGenerator(apiKey, quotaManager);

        // Optional: wrap with AIProviderManager to simulate real usage
        AIProviderManager aiManager = new AIProviderManager();
        aiManager.registerProvider("gemini", geminiGenerator);

        // Test words
        String[] testWords = {"勉強", "友達"};

        List<NormalizedWord> normalizedWords = new ArrayList<>();
        for (String w : testWords) {
            normalizedWords.add(WordNormalizer.normalize(w));
        }

        try {
            List<List<String>> results = aiManager.generateExampleChoices(normalizedWords);

            for (int i = 0; i < normalizedWords.size(); i++) {
                NormalizedWord nw = normalizedWords.get(i);
                List<String> sentences = results.get(i);

                LambdaLogger.log("Word: " + nw.baseWord());

                if (sentences == null || sentences.isEmpty()) {
                    LambdaLogger.log("⚠ No sentences generated");
                } else {
                    for (String s : sentences) {
                        LambdaLogger.log("Generated: " + s);
                    }
                }
            }

        } catch (Exception e) {
            System.err.println("❌ Error during batch generation");
            e.printStackTrace();
        } finally {
            aiManager.close(); // ensure resources are released
        }

        LambdaLogger.log("✓ Test complete!");
    }
}
