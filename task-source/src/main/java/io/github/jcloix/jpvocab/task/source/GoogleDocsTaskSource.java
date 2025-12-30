package io.github.jcloix.jpvocab.task.source;

import com.google.api.services.docs.v1.Docs;
import com.google.api.services.docs.v1.model.*;
import io.github.jcloix.jpvocab.domain.VocabTask;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class GoogleDocsTaskSource implements TaskSource {

    private final Docs docs;
    private final String documentId;
    private final VocabTaskParser parser;

    public GoogleDocsTaskSource(Docs docs, String documentId, VocabTaskParser parser) {
        this.docs = docs;
        this.documentId = documentId;
        this.parser = parser;
    }

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

    private Table findFirstTable(List<StructuralElement> content) {
        for (StructuralElement element : content) {
            if (element.getTable() != null) {
                return element.getTable();
            }
        }
        return null;
    }

    private List<List<String>> extractRows(Table table) {
        List<List<String>> rows = new ArrayList<>();

        for (TableRow row : table.getTableRows()) {
            List<String> values = new ArrayList<>();
            for (TableCell cell : row.getTableCells()) {
                values.add(readCell(cell));
            }
            rows.add(values);
        }
        return rows;
    }

    private String readCell(TableCell cell) {
        StringBuilder text = new StringBuilder();

        for (StructuralElement element : cell.getContent()) {
            if (element.getParagraph() != null) {
                for (ParagraphElement pe : element.getParagraph().getElements()) {
                    if (pe.getTextRun() != null) {
                        text.append(pe.getTextRun().getContent());
                    }
                }
            }
        }
        return text.toString().trim();
    }
}
