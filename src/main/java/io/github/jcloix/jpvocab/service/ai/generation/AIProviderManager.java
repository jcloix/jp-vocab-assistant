package io.github.jcloix.jpvocab.service.ai.generation;

import io.github.jcloix.jpvocab.domain.normalization.NormalizedWord;
import io.github.jcloix.jpvocab.util.LambdaLogger;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class AIProviderManager {

    /**
     * Ordered map: provider name → TextGenerator instance
     * LinkedHashMap preserves insertion order → allows priority fallback
     */
    private final Map<String, TextGenerator> providers = new LinkedHashMap<>();

    public AIProviderManager() {}

    /**
     * Register a provider (e.g., Gemini, GPT)
     */
    public void registerProvider(String name, TextGenerator generator) {
        providers.put(name, generator);
    }

    /**
     * Generate example choices using the first available provider
     * Can implement more complex selection/fallback logic if needed
     */
    public List<List<String>> generateExampleChoices(List<NormalizedWord> words) {
        for (TextGenerator generator : providers.values()) {
            try {
                return generator.generateExampleChoices(words);
            } catch (Exception e) {
                LambdaLogger.log("⚠ Provider failed, trying next: " + e.getMessage());
            }
        }
        throw new RuntimeException("All AI providers failed");
    }

    /**
     * Close all providers
     */
    public void close() {
        for (TextGenerator generator : providers.values()) {
            generator.close();
        }
    }
}
