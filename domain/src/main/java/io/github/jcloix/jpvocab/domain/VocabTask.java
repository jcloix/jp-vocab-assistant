package io.github.jcloix.jpvocab.domain;



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
    private final Integer rowId;
    private final String rawWord;
    private final NormalizedWord normalizedWord;
    private final String assignedTo;
    private final String exampleSentence;

    public VocabTask(Integer rowId, String rawWord, NormalizedWord normalizedWord, String assignedTo, String exampleSentence) {
        this.rowId = rowId;
        this.rawWord = rawWord;
        this.normalizedWord = normalizedWord;
        this.assignedTo = assignedTo;
        this.exampleSentence = exampleSentence == null ? "" : exampleSentence;
    }

    public Integer getRowId() {
        return rowId;
    }

    public String getWord() {
        return getNormalizedWord().baseWord();
    }

    public String getRawWord() {
        return rawWord;
    }

    public NormalizedWord getNormalizedWord() {
        return normalizedWord;
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
                "rowId=" + rowId +
                ", rawWord='" + rawWord + '\'' +
                ", normalizedWord='" + (normalizedWord != null ? normalizedWord.toString() : "null") + '\'' +
                ", assignedTo='" + assignedTo + '\'' +
                ", exampleSentence='" + exampleSentence.replace("\n", "\\n") + '\'' +
                '}';
    }

}
