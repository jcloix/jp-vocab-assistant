package io.github.jcloix.jpvocab.ai;

import com.google.genai.errors.ClientException;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;
import io.github.jcloix.jpvocab.domain.NormalizedWord;
import io.github.jcloix.jpvocab.util.LambdaLogger;

import java.util.*;
import java.util.stream.Collectors;

public class GeminiTextGenerator implements TextGenerator {

    private static final String[] MODELS_BY_PRIORITY = {
            "models/gemini-2.5-flash",
            "models/gemini-2.5-flash-lite",
            "models/gemma-3-12b-it",
            "models/gemma-3-4b-it"
    };

    private static final Map<String, Integer> MODEL_DAILY_QUOTAS = Map.of(
            "models/gemini-2.5-flash", 20,
            "models/gemini-2.5-flash-lite", 20,
            "models/gemma-3-12b-it", 14400,
            "models/gemma-3-4b-it", 14400
    );

    private static final int MAX_WORDS_PER_BATCH = 3;
    private static final int MAX_FALLBACK_MODELS = 3;
    private static final int MIN_SENTENCES_PER_WORD = 2;

    private final GeminiClientWrapper clientWrapper;
    private final QuotaManager quotaManager;
    private final PromptBuilder promptBuilder;
    private final ResponseParser responseParser;

    public GeminiTextGenerator(String apiKey, QuotaManager quotaManager) {
        this.clientWrapper = new GeminiClientWrapper(apiKey);
        this.quotaManager = quotaManager;
        this.promptBuilder = new PromptBuilder();
        this.responseParser = new ResponseParser();
    }

    @Override
    public List<List<String>> generateExampleChoices(List<NormalizedWord> words) {
        if (words == null || words.isEmpty()) return List.of();

        // Initialize results with empty lists
        List<List<String>> results = new ArrayList<>();
        for (int i = 0; i < words.size(); i++) results.add(new ArrayList<>());

        // Track remaining words to retry if needed
        List<NormalizedWord> remainingWords = new ArrayList<>(words);

        // Loop over models by priority
        for (String model : MODELS_BY_PRIORITY) {
            if (remainingWords.isEmpty()) break;

            // Check quota
            if (quotaManager.getModelCountToday(model) >= MODEL_DAILY_QUOTAS.getOrDefault(model, 0)) {
                LambdaLogger.log("⚠ Model quota reached today: " + model);
                continue;
            }

            // Process in batches
            remainingWords = processBatchesWithModel(model, remainingWords, results);
        }

        if (remainingWords.size() == words.size()) {
            throw new RuntimeException("⚠ Gemini could not generate enough examples for any words.");
        } else if (!remainingWords.isEmpty()) {
            LambdaLogger.log("⚠ Some words could not get enough examples: " +
                    remainingWords.stream().map(NormalizedWord::baseWord).collect(Collectors.joining(", ")));
        }

        return results;
    }

    /** Process remaining words in batches for a given model */
    private List<NormalizedWord> processBatchesWithModel(String model, List<NormalizedWord> words, List<List<String>> results) {
        List<NormalizedWord> remainingWords = new ArrayList<>();

        for (int i = 0; i < words.size(); i += MAX_WORDS_PER_BATCH) {
            int end = Math.min(i + MAX_WORDS_PER_BATCH, words.size());
            List<NormalizedWord> batch = words.subList(i, end);

            String prompt = promptBuilder.buildBatchPrompt(batch);
            Map<NormalizedWord, List<String>> parsedResponse;

            try {
                parsedResponse = callGeminiForBatch(model, batch, prompt);
            } catch (ClientException e) {
                if (isQuotaExceeded(e)) {
                    LambdaLogger.log("⚠ Quota exceeded on model: " + model + " → fallback");
                    remainingWords.addAll(batch);
                    break; // fallback to next model
                }
                throw e;
            }

            // Validate parsed response and update results
            remainingWords.addAll(validateAndUpdateResults(batch, parsedResponse, results));
        }

        return remainingWords;
    }

    /**
     * Validates parsed response for a batch of words.
     * Updates the results list for words that meet the minimum sentence requirement.
     * Returns the list of words that do not meet the requirement.
     */
    private List<NormalizedWord> validateAndUpdateResults(List<NormalizedWord> batch,
                                                          Map<NormalizedWord, List<String>> parsedResponse,
                                                          List<List<String>> results) {
        List<NormalizedWord> remainingWords = new ArrayList<>();

        for (NormalizedWord w : batch) {
            List<String> sentences = parsedResponse.getOrDefault(w, List.of());
            int index = results.indexOf(results.stream().filter(List::isEmpty).findFirst().orElse(null));
            if (sentences.size() < MIN_SENTENCES_PER_WORD) {
                remainingWords.add(w);
                // Remove any previous placeholder in results
                if (index >= 0) results.set(index, List.of());
            } else {
                // Update results at the correct index
                if (index >= 0) results.set(index, sentences);
            }
        }

        return remainingWords;
    }


    /** Single responsibility: call Gemini for a batch, handle quota, logging, parsing */
    private Map<NormalizedWord, List<String>> callGeminiForBatch(String model, List<NormalizedWord> batch, String prompt) {
        quotaManager.incrementModelCounter(model);
        Logger.logBatchRequest(model, batch, prompt);

        GenerateContentResponse response = clientWrapper.generateContent(model, prompt, defaultConfig());
        String text = response.text();

        Logger.logBatchRawResponse(model, batch, text);

        return responseParser.parseBatchResponseMap(batch, text);
    }

    private GenerateContentConfig defaultConfig() {
        return GenerateContentConfig.builder()
                .temperature(0.8f)
                .maxOutputTokens(600)
                .build();
    }

    private boolean isQuotaExceeded(Exception e) {
        return e.getMessage() != null && e.getMessage().contains("429");
    }

    @Override
    public void close() {
        clientWrapper.close();
    }
}
