package io.github.jcloix.jpvocab.task.source;

import com.google.api.services.docs.v1.model.TableRow;
import com.google.api.services.docs.v1.model.TableCell;

import java.util.List;
import java.util.Objects;

import static io.github.jcloix.jpvocab.task.source.GoogleDocsHelper.readCell;

public class GoogleDocRow implements ResultTask {

    public final String date;
    public final String lesson;
    public final String page;
    public final String word;
    public final String assignedTo;
    public final String example; // includes the answer cell

    private GoogleDocRow(String date, String lesson, String page, String word, String assignedTo, String example) {
        this.date = date;
        this.lesson = lesson;
        this.page = page;
        this.word = word;
        this.assignedTo = assignedTo;
        this.example = example;
    }

    /**
     * Builds a GoogleDocRow from a TableRow and optionally the updated answer content
     */
    public static GoogleDocRow fromTableRow(TableRow row, String updatedAnswerCell) {
        List<TableCell> cells = row.getTableCells();

        String date = readCell(cells.get(GoogleDocTableSchema.DATE_COL));
        String lesson = readCell(cells.get(GoogleDocTableSchema.LESSON_COL));
        String page = readCell(cells.get(GoogleDocTableSchema.PAGE_COL));
        String word = readCell(cells.get(GoogleDocTableSchema.WORD_COL));
        String assignedTo = readCell(cells.get(GoogleDocTableSchema.ASSIGNED_TO_COL));

        // Use updated answer content if provided, otherwise read from cell
        String example = Objects.requireNonNullElseGet(updatedAnswerCell, () -> readCell(cells.get(GoogleDocTableSchema.EXAMPLE_COL)));

        return new GoogleDocRow(date, lesson, page, word, assignedTo, example);
    }

    @Override
    public String toString() {
        return "GoogleDocRow{" +
                "date='" + date + '\'' +
                ", lesson='" + lesson + '\'' +
                ", page='" + page + '\'' +
                ", word='" + word + '\'' +
                ", assignedTo='" + assignedTo + '\'' +
                ", example='" + example + '\'' +
                '}';
    }

    @Override
    public String toMessage() {
        return String.format(
                "**Word:** %s\n**Date:** %s\n**Lesson:** %s\n**Page:** %s\n**Assigned To:** %s\n**Example:** %s",
                word, date, lesson, page, assignedTo, example
        );
    }
}
