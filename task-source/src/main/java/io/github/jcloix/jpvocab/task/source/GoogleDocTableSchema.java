package io.github.jcloix.jpvocab.task.source;

public final class GoogleDocTableSchema {

    private GoogleDocTableSchema() {}

    // 0-based column indexes
    public static final int DATE_COL = 0;
    public static final int LESSON_COL = 1;
    public static final int PAGE_COL = 2;
    public static final int WORD_COL = 3;
    public static final int ASSIGNED_TO_COL = 4;
    public static final int EXAMPLE_COL = 5;

    /**
     * Column where completed answers should be written.
     * For now we append to the example column.
     */
    public static final int ANSWER_COL = EXAMPLE_COL;
}
