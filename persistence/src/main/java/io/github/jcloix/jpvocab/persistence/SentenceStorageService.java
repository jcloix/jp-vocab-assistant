package io.github.jcloix.jpvocab.persistence;

import java.util.HashMap;
import java.util.Map;

/**
 * Tracks generated sentences for vocabulary tasks.
 * In production, this will be backed by DynamoDB.
 * For now, using in-memory storage for local testing.
 */
public class SentenceStorageService {

    // word -> generated sentence
    private final Map<String, String> storage = new HashMap<>();

    public void storeSentence(String word, String sentence) {
        storage.put(word, sentence);
    }

    public String getSentence(String word) {
        return storage.get(word);
    }

    public boolean hasSentence(String word) {
        return storage.containsKey(word);
    }

    public Map<String, String> getAllSentences() {
        return new HashMap<>(storage);
    }
}