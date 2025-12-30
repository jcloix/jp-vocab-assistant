package io.github.jcloix.jpvocab.ai;

import io.github.jcloix.jpvocab.domain.NormalizedWord;

import java.util.List;

public interface TextGenerator {

    /**
     * Generate example sentences for a list of words.
     * @param words list of words
     * @return list of sentence lists, one per word
     */
    List<List<String>> generateExampleChoices(List<NormalizedWord> words);

    /**
     * Optional cleanup (close client, free resources)
     */
    void close();
}