package io.github.jcloix.jpvocab.model;


/**
 * Domain model representing a vocabulary task extracted from the Google Docs table.
 *
 * <p>Each {@code VocabTask} corresponds to one row in the vocabulary assignment table
 * and contains:
 * <ul>
 *   <li>The vocabulary word</li>
 *   <li>The assigned person</li>
 *   <li>The example sentence column content</li>
 * </ul>
 *
 * <p>This class is a pure domain object:
 * <ul>
 *   <li>No AWS dependencies</li>
 *   <li>No Google API dependencies</li>
 *   <li>No infrastructure concerns</li>
 * </ul>
 *
 * <p>It should remain stable and not contain orchestration or parsing logic.
 */
public class VocabTask {

    private final String word;
    private final String assignedTo;
    private final String exampleSentence;

    public VocabTask(String word, String assignedTo, String exampleSentence) {
        this.word = word;
        this.assignedTo = assignedTo;
        this.exampleSentence = exampleSentence == null ? "" : exampleSentence;
    }

    public String getWord() {
        return word;
    }

    public String getAssignedTo() {
        return assignedTo;
    }

    public String getExampleSentence() {
        return exampleSentence;
    }

    public boolean isSentenceDoneBy(String name) {
        return exampleSentence.contains(name);
    }

    @Override
    public String toString() {
        return "VocabTask{" +
                "word='" + word + '\'' +
                ", assignedTo='" + assignedTo + '\'' +
                ", exampleSentence='" + exampleSentence + '\'' +
                '}';
    }
}
