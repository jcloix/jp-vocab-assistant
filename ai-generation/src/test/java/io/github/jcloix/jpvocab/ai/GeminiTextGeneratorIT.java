package io.github.jcloix.jpvocab.ai;

import io.github.jcloix.jpvocab.domain.NormalizedWord;
import io.github.jcloix.jpvocab.domain.WordNormalizer;
import io.github.jcloix.jpvocab.util.LambdaLogger;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;

@Tag("integration")
@EnabledIfEnvironmentVariable(named = "GEMINI_API_KEY", matches = ".+")
class GeminiTextGeneratorIT {

    @Test
    void shouldGenerateExampleChoicesForBatchOfWords() throws Exception {
        String apiKey = requireEnv("GEMINI_API_KEY");

        LambdaLogger.log("Testing GeminiTextGenerator (BATCH MODE)\n");

        // Initialize quota manager
        QuotaManager quotaManager = new InMemoryQuotaManager();

        // Create GeminiTextGenerator
        TextGenerator geminiGenerator = new GeminiTextGenerator(apiKey, quotaManager);

        // Wrap with AIProviderManager to simulate real usage
        AIProviderManager aiManager = new AIProviderManager();
        aiManager.registerProvider("gemini", geminiGenerator);

        // Test words
        String[] testWords = {"勉強", "友達"};

        List<NormalizedWord> normalizedWords = new ArrayList<>();
        for (String w : testWords) {
            normalizedWords.add(WordNormalizer.normalize(w));
        }

        try {
            // Act: generate example choices
            List<List<String>> results = aiManager.generateExampleChoices(normalizedWords);

            // Assert: ensure each word has at least one generated sentence
            for (int i = 0; i < normalizedWords.size(); i++) {
                NormalizedWord nw = normalizedWords.get(i);
                List<String> sentences = results.get(i);

                LambdaLogger.log("\nWord: " + nw.baseWord());

                if (sentences == null || sentences.isEmpty()) {
                    LambdaLogger.log("⚠ No sentences generated for " + nw.baseWord());
                } else {
                    for (String s : sentences) {
                        LambdaLogger.log("Generated: " + s);
                    }
                }

                assertFalse(sentences == null || sentences.isEmpty(),
                        "Expected at least one sentence for word: " + nw.baseWord());
            }

        } finally {
            aiManager.close(); // ensure resources are released
        }

        LambdaLogger.log("✓ GeminiTextGenerator IT complete!");
    }

    private String requireEnv(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing required env var: " + name);
        }
        return value;
    }
}
