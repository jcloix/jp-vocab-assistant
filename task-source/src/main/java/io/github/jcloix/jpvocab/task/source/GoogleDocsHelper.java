package io.github.jcloix.jpvocab.task.source;

import com.google.api.services.docs.v1.model.*;

import java.util.ArrayList;
import java.util.List;

public final class GoogleDocsHelper {

    private GoogleDocsHelper() {
        // utility class
    }

    public static Table findFirstTable(List<StructuralElement> content) {
        for (StructuralElement element : content) {
            if (element.getTable() != null) {
                return element.getTable();
            }
        }
        return null;
    }

    public static List<List<String>> extractRows(Table table) {
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

    public static String readCell(TableCell cell) {
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

    /**
     * Returns the TableCell at the specified column.
     */
    public static TableCell getCell(TableRow row, int col) {
        if (col < 0 || col >= row.getTableCells().size()) {
            throw new IllegalArgumentException("Column index out of bounds: " + col);
        }
        return row.getTableCells().get(col);
    }

    /**
     * Finds the index where new text should be inserted in a cell.
     */
    public static int findCellEndIndex(TableCell cell) {
        List<StructuralElement> content = cell.getContent();
        StructuralElement last = content.get(content.size() - 1);

        if (last.getParagraph() == null) {
            throw new IllegalStateException("Cell has no paragraph");
        }

        List<ParagraphElement> elements = last.getParagraph().getElements();
        ParagraphElement lastElement = elements.get(elements.size() - 1);

        return lastElement.getEndIndex() - 1;
    }
}
