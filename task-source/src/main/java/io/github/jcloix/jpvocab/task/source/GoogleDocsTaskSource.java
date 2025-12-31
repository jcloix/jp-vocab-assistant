package io.github.jcloix.jpvocab.task.source;

import com.google.api.services.docs.v1.Docs;
import com.google.api.services.docs.v1.model.*;
import io.github.jcloix.jpvocab.domain.VocabTask;
import io.github.jcloix.jpvocab.util.LambdaLogger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static io.github.jcloix.jpvocab.task.source.GoogleDocTableSchema.ANSWER_COL;
import static io.github.jcloix.jpvocab.task.source.GoogleDocsHelper.*;

public class GoogleDocsTaskSource implements TaskSource {

    private final Docs docs;
    private final String documentId;
    private final VocabTaskParser parser;

    public GoogleDocsTaskSource(Docs docs, String documentId, VocabTaskParser parser) {
        this.docs = docs;
        this.documentId = documentId;
        this.parser = parser;
    }

    // -----------------------------
    // READ
    // -----------------------------

    @Override
    public List<VocabTask> fetchTasks() {
        try {
            Document document = docs.documents().get(documentId).execute();
            Table table = findFirstTable(document.getBody().getContent());

            if (table == null) {
                throw new IllegalStateException("No table found in document");
            }

            List<List<String>> rows = extractRows(table);
            return parser.parse(rows);

        } catch (IOException e) {
            throw new RuntimeException("Failed to read Google Docs", e);
        }
    }

    // -----------------------------
    // WRITE
    // -----------------------------

    @Override
    public GoogleDocRow completeTask(String word, int rowId, String resolvedSentence) {
        try {
            String myName = System.getenv("MY_NAME");
            Document document = docs.documents().get(documentId).execute();
            Table table = findFirstTable(document.getBody().getContent());

            if (table == null) {
                throw new IllegalStateException("No table found in document");
            }

            if (rowId < 0 || rowId >= table.getTableRows().size()) {
                throw new IllegalArgumentException("RowId out of bounds: " + rowId);
            }

            TableRow row = getRowWithFallback(table, rowId, word, myName);

            TableCell answerCell = getCell(row, ANSWER_COL);
            String updatedAnswer = updateCellWithName(answerCell, resolvedSentence, myName);

            return GoogleDocRow.fromTableRow(row, updatedAnswer);
        } catch (Exception e) {
            throw new RuntimeException("Failed to complete task in Google Docs", e);
        }
    }

    // -----------------------------
    // HELPERS
    // -----------------------------

    /**
     * Tries to return the row at rowId if it contains the word in the word column.
     * Otherwise, searches all rows for a matching word in the word column.
     */
    private TableRow getRowWithFallback(Table table, int rowId, String word, String myName) {
        List<TableRow> rows = table.getTableRows();

        if (rowId >= 0 && rowId < rows.size()) {
            TableRow row = rows.get(rowId);
            String cellWord = readCell(getCell(row, GoogleDocTableSchema.WORD_COL));
            if (cellWord.contains(word)) {
                return row;
            }
            LambdaLogger.log("⚠ Word mismatch. expected=" + word + ", found=" + cellWord);

        }

        // fallback: search all rows
        for (TableRow row : rows) {
            String cellWord = readCell(getCell(row, GoogleDocTableSchema.WORD_COL));
            if (cellWord.contains(word)) {
                String assignee = readCell(getCell(row, GoogleDocTableSchema.ASSIGNED_TO_COL));
                if(!assignee.contains(myName)) {
                    throw new IllegalArgumentException("Row with word " + word + " is not assigned to " + myName + " but to " + assignee + " instead.");
                }
                return row;
            }
        }

        throw new IllegalArgumentException("No row found containing word: " + word);
    }



    private String updateCellWithName(TableCell cell, String resolvedSentence, String myName) throws IOException {
        if (myName == null || myName.isEmpty()) {
            throw new IllegalStateException("Environment variable MY_NAME is not set");
        }

        String content = readCell(cell);
        String[] lines = content.split("\n");

        String newLine = "(" + myName + ") " + resolvedSentence;
        boolean found = false;

        StringBuilder updatedContent = new StringBuilder();
        for (String line : lines) {
            if (line.contains("(" + myName + ")")) {
                updatedContent.append(newLine).append("\n");
                found = true;
            } else {
                updatedContent.append(line).append("\n");
            }
        }

        if (!found) {
            // Append new line if my name not found
            if (!content.isEmpty()) {
                updatedContent.append("\n");
            }
            updatedContent.append(newLine).append("\n");
        }

        // Replace the whole cell content
        Request replaceText = new Request()
                .setReplaceAllText(new ReplaceAllTextRequest()
                        .setContainsText(new SubstringMatchCriteria().setText(content))
                        .setReplaceText(updatedContent.toString())
                );

        BatchUpdateDocumentRequest batch =
                new BatchUpdateDocumentRequest()
                        .setRequests(List.of(replaceText));

        docs.documents().batchUpdate(documentId, batch).execute();
        return updatedContent.toString().trim(); // return the updated cell content
    }
}
