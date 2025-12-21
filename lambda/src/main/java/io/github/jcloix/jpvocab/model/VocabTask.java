package com.julien.jpvocab.model;

public class VocabTask {

    private final String word;
    private final String assignedTo;
    private final boolean sentenceMissing;

    public VocabTask(String word, String assignedTo, boolean sentenceMissing) {
        this.word = word;
        this.assignedTo = assignedTo;
        this.sentenceMissing = sentenceMissing;
    }

    public String getWord() {
        return word;
    }

    public String getAssignedTo() {
        return assignedTo;
    }

    public boolean isSentenceMissing() {
        return sentenceMissing;
    }

    @Override
    public String toString() {
        return "VocabTask{" +
                "word='" + word + '\'' +
                ", assignedTo='" + assignedTo + '\'' +
                ", sentenceMissing=" + sentenceMissing +
                '}';
    }
}
