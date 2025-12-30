package io.github.jcloix.jpvocab.ai;

import io.github.jcloix.jpvocab.domain.NormalizedWord;
import io.github.jcloix.jpvocab.util.LambdaLogger;

import java.util.*;

public class ResponseParser {

    private static final int SENTENCES_PER_WORD = 3;

    /**
     * Parse Gemini batch response into a map from word -> list of sentences
     */
    public Map<NormalizedWord, List<String>> parseBatchResponseMap(List<NormalizedWord> words, String text) {
        Map<NormalizedWord, List<String>> result = new LinkedHashMap<>();
        for (NormalizedWord w : words) result.put(w, List.of());

        if (text == null || text.isBlank()) {
            LambdaLogger.log("⚠ Empty AI response");
            return result;
        }

        String[] blocks = text.split("### WORD:");
        for (String block : blocks) {
            String trimmed = block.trim();
            if (trimmed.isEmpty()) continue;

            // First line is the word label
            int nl = trimmed.indexOf('\n');
            if (nl == -1) continue;
            String label = trimmed.substring(0, nl).trim();

            NormalizedWord word = findWordByLabel(words, label);
            if (word == null) {
                LambdaLogger.log("⚠ Unknown WORD block: " + label);
                continue;
            }

            // Extract numbered sentences
            List<String> sentences = trimmed.lines()
                    .map(String::trim)
                    .filter(l -> l.matches("^\\d+\\.\\s+.*"))
                    .map(l -> l.replaceFirst("^\\d+\\.\\s*", ""))
                    .limit(SENTENCES_PER_WORD)
                    .toList();

            result.put(word, sentences);
        }

        return result;
    }

    private NormalizedWord findWordByLabel(List<NormalizedWord> words, String label) {
        return words.stream()
                .filter(w -> label.equals(w.baseWord()) || label.equals(w.fixedPhrase()))
                .findFirst()
                .orElse(null);
    }
}
