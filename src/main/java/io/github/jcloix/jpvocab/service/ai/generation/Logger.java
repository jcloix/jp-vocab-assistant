package io.github.jcloix.jpvocab.service.ai.generation;

import io.github.jcloix.jpvocab.domain.normalization.NormalizedWord;
import io.github.jcloix.jpvocab.util.LambdaLogger;

import java.util.List;

public class Logger {

    public static void logBatchRequest(String model, List<NormalizedWord> batch, String prompt) {
        LambdaLogger.log("===== AI BATCH REQUEST =====");
        LambdaLogger.log("Model: " + model);
        LambdaLogger.log("Words: " + batch.stream()
                .map(w -> w.fixedPhrase() != null ? w.fixedPhrase() : w.baseWord())
                .toList());
        LambdaLogger.log("----- PROMPT -----");
        LambdaLogger.log(prompt);
        LambdaLogger.log("==============================");
    }

    public static void logBatchRawResponse(String model, List<NormalizedWord> batch, String text) {
        LambdaLogger.log("===== AI BATCH RESPONSE =====");
        LambdaLogger.log("Model: " + model);
        LambdaLogger.log("Words: " + batch.stream()
                .map(w -> w.fixedPhrase() != null ? w.fixedPhrase() : w.baseWord())
                .toList());
        LambdaLogger.log("----- RAW TEXT -----");
        LambdaLogger.log(text);
        LambdaLogger.log("==============================");
    }
}
