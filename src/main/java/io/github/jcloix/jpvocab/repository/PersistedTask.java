package io.github.jcloix.jpvocab.repository;

import java.time.Instant;
import java.util.List;

public class PersistedTask {

    private final String docId;
    private final int rowId;
    private final String word;
    private final List<String> choices;
    private final String status;
    private final Instant createdAt;
    private final Instant updatedAt;

    public PersistedTask(
            String docId,
            int rowId,
            String word,
            List<String> choices,
            String status,
            Instant createdAt,
            Instant updatedAt
    ) {
        this.docId = docId;
        this.rowId = rowId;
        this.word = word;
        this.choices = choices;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public String getDocId() {
        return docId;
    }

    public int getRowId() {
        return rowId;
    }

    public String getWord() {
        return word;
    }

    public List<String> getChoices() {
        return choices;
    }

    public String getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    // getters only (immutable)
}
