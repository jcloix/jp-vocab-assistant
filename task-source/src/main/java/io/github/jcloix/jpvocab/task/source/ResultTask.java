package io.github.jcloix.jpvocab.task.source;

public interface ResultTask {
    /**
     * Returns a string representation of the task for external consumption (e.g., Discord).
     */
    String toMessage();
}
