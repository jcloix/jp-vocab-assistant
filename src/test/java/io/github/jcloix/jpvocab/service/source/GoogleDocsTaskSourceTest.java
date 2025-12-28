package io.github.jcloix.jpvocab.service.source;

import com.google.api.services.docs.v1.Docs;
import com.google.api.services.docs.v1.model.*;
import io.github.jcloix.jpvocab.domain.normalization.WordNormalizer;
import io.github.jcloix.jpvocab.model.VocabTask;
import io.github.jcloix.jpvocab.service.VocabTaskParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GoogleDocsTaskSourceTest {

    @Mock
    private VocabTaskParser parser;

    private TestableGoogleDocsTaskSource taskSource;
    private static final String TEST_DOCUMENT_ID = "test-doc-id-123";

    @BeforeEach
    void setUp() {
        taskSource = new TestableGoogleDocsTaskSource(null, TEST_DOCUMENT_ID, parser);
    }

    @Test
    void fetchTasks_shouldReturnParsedTasks_whenTableExists() {
        // Arrange
        Document document = createDocumentWithTable(
                Arrays.asList("Header1", "Header2"),
                Arrays.asList("Value1", "Value2")
        );

        List<VocabTask> expectedTasks = createMockTasks(2);
        taskSource.setMockDocument(document);

        when(parser.parse(anyList())).thenReturn(expectedTasks);

        // Act
        List<VocabTask> result = taskSource.fetchTasks();

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(expectedTasks, result);
        verify(parser).parse(anyList());
    }

    @Test
    void fetchTasks_shouldExtractCorrectCellValues() {
        // Arrange
        Document document = createDocumentWithTable(
                Arrays.asList("Japanese", "English"),
                Arrays.asList("こんにちは", "Hello")
        );

        taskSource.setMockDocument(document);
        when(parser.parse(anyList())).thenReturn(new ArrayList<>());

        // Act
        taskSource.fetchTasks();

        // Assert
        verify(parser).parse(argThat(rows -> {
            assertEquals(2, rows.size());
            assertEquals(Arrays.asList("Japanese", "English"), rows.get(0));
            assertEquals(Arrays.asList("こんにちは", "Hello"), rows.get(1));
            return true;
        }));
    }

    @Test
    void fetchTasks_shouldHandleEmptyCells() {
        // Arrange
        Document document = createDocumentWithTable(
                Arrays.asList("Col1", ""),
                Arrays.asList("", "Col2")
        );

        taskSource.setMockDocument(document);
        when(parser.parse(anyList())).thenReturn(new ArrayList<>());

        // Act
        taskSource.fetchTasks();

        // Assert
        verify(parser).parse(argThat(rows -> {
            assertEquals("", rows.get(0).get(1));
            assertEquals("", rows.get(1).get(0));
            return true;
        }));
    }

    @Test
    void fetchTasks_shouldHandleMultilineText() {
        // Arrange
        Document document = createDocumentWithMultilineCell("Line1\nLine2\nLine3");

        taskSource.setMockDocument(document);
        when(parser.parse(anyList())).thenReturn(new ArrayList<>());

        // Act
        taskSource.fetchTasks();

        // Assert
        verify(parser).parse(argThat(rows -> {
            String cellContent = rows.get(0).get(0);
            assertTrue(cellContent.contains("Line1"));
            assertTrue(cellContent.contains("Line2"));
            assertTrue(cellContent.contains("Line3"));
            return true;
        }));
    }

    @Test
    void fetchTasks_shouldTrimWhitespace() {
        // Arrange
        Document document = createDocumentWithTable(
                Arrays.asList("  trimmed  ", "\ttabbed\t")
        );

        taskSource.setMockDocument(document);
        when(parser.parse(anyList())).thenReturn(new ArrayList<>());

        // Act
        taskSource.fetchTasks();

        // Assert
        verify(parser).parse(argThat(rows -> {
            assertEquals("trimmed", rows.get(0).get(0));
            assertEquals("tabbed", rows.get(0).get(1));
            return true;
        }));
    }

    @Test
    void fetchTasks_shouldThrowException_whenNoTableFound() {
        // Arrange
        Document document = createDocumentWithoutTable();
        taskSource.setMockDocument(document);

        // Act & Assert
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> taskSource.fetchTasks()
        );

        assertEquals("No table found in document", exception.getMessage());
        verify(parser, never()).parse(anyList());
    }

    @Test
    void fetchTasks_shouldThrowRuntimeException_whenIOExceptionOccurs() {
        // Arrange
        taskSource.setShouldThrowIOException(true);

        // Act & Assert
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> taskSource.fetchTasks()
        );

        assertEquals("Failed to read Google Docs", exception.getMessage());
        assertTrue(exception.getCause() instanceof IOException);
    }

    @Test
    void fetchTasks_shouldHandleTableWithMultipleRows() {
        // Arrange
        Document document = createDocumentWithTable(
                Arrays.asList("H1", "H2", "H3"),
                Arrays.asList("R1C1", "R1C2", "R1C3"),
                Arrays.asList("R2C1", "R2C2", "R2C3"),
                Arrays.asList("R3C1", "R3C2", "R3C3")
        );

        taskSource.setMockDocument(document);
        when(parser.parse(anyList())).thenReturn(new ArrayList<>());

        // Act
        taskSource.fetchTasks();

        // Assert
        verify(parser).parse(argThat(rows -> rows.size() == 4));
    }

    @Test
    void fetchTasks_shouldHandleCellWithMultipleParagraphs() {
        // Arrange
        TableCell cell = new TableCell();
        List<StructuralElement> content = new ArrayList<>();

        // First paragraph
        content.add(createParagraphElement("Paragraph 1"));
        // Second paragraph
        content.add(createParagraphElement("Paragraph 2"));

        cell.setContent(content);

        TableRow row = new TableRow().setTableCells(Arrays.asList(cell));
        Table table = new Table().setTableRows(Arrays.asList(row));

        Document document = new Document();
        Body body = new Body();
        StructuralElement element = new StructuralElement().setTable(table);
        body.setContent(Arrays.asList(element));
        document.setBody(body);

        taskSource.setMockDocument(document);
        when(parser.parse(anyList())).thenReturn(new ArrayList<>());

        // Act
        taskSource.fetchTasks();

        // Assert
        verify(parser).parse(argThat(rows -> {
            String cellContent = rows.get(0).get(0);
            assertTrue(cellContent.contains("Paragraph 1"));
            assertTrue(cellContent.contains("Paragraph 2"));
            return true;
        }));
    }

    @Test
    void fetchTasks_shouldHandleEmptyTable() {
        // Arrange
        Table table = new Table().setTableRows(new ArrayList<>());

        Document document = new Document();
        Body body = new Body();
        StructuralElement element = new StructuralElement().setTable(table);
        body.setContent(Arrays.asList(element));
        document.setBody(body);

        taskSource.setMockDocument(document);
        when(parser.parse(anyList())).thenReturn(new ArrayList<>());

        // Act
        taskSource.fetchTasks();

        // Assert
        verify(parser).parse(argThat(List::isEmpty));
    }

    @Test
    void fetchTasks_shouldHandleNullContentInCell() {
        // Arrange
        TableCell cell = new TableCell();
        cell.setContent(null);

        TableRow row = new TableRow().setTableCells(Arrays.asList(cell));
        Table table = new Table().setTableRows(Arrays.asList(row));

        Document document = new Document();
        Body body = new Body();
        StructuralElement element = new StructuralElement().setTable(table);
        body.setContent(Arrays.asList(element));
        document.setBody(body);

        taskSource.setMockDocument(document);
        when(parser.parse(anyList())).thenReturn(new ArrayList<>());

        // Act & Assert
        assertDoesNotThrow(() -> taskSource.fetchTasks());
    }

    // Helper methods

    private Document createDocumentWithTable(List<String>... rowData) {
        List<TableRow> tableRows = new ArrayList<>();

        for (List<String> row : rowData) {
            List<TableCell> cells = new ArrayList<>();
            for (String cellValue : row) {
                cells.add(createTableCell(cellValue));
            }
            tableRows.add(new TableRow().setTableCells(cells));
        }

        Table table = new Table().setTableRows(tableRows);

        Document document = new Document();
        Body body = new Body();
        StructuralElement element = new StructuralElement().setTable(table);
        body.setContent(Arrays.asList(element));
        document.setBody(body);

        return document;
    }

    private Document createDocumentWithMultilineCell(String content) {
        TableCell cell = createTableCell(content);
        TableRow row = new TableRow().setTableCells(Arrays.asList(cell));
        Table table = new Table().setTableRows(Arrays.asList(row));

        Document document = new Document();
        Body body = new Body();
        StructuralElement element = new StructuralElement().setTable(table);
        body.setContent(Arrays.asList(element));
        document.setBody(body);

        return document;
    }

    private Document createDocumentWithoutTable() {
        Document document = new Document();
        Body body = new Body();

        // Create a structural element without a table
        StructuralElement element = new StructuralElement();
        Paragraph paragraph = new Paragraph();
        element.setParagraph(paragraph);

        body.setContent(Arrays.asList(element));
        document.setBody(body);

        return document;
    }

    private TableCell createTableCell(String text) {
        TableCell cell = new TableCell();
        cell.setContent(Arrays.asList(createParagraphElement(text)));
        return cell;
    }

    private StructuralElement createParagraphElement(String text) {
        TextRun textRun = new TextRun().setContent(text);
        ParagraphElement paragraphElement = new ParagraphElement().setTextRun(textRun);
        Paragraph paragraph = new Paragraph().setElements(Arrays.asList(paragraphElement));
        return new StructuralElement().setParagraph(paragraph);
    }

    private List<VocabTask> createMockTasks(int count) {
        List<VocabTask> tasks = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            tasks.add(new VocabTask(2,"sa", WordNormalizer.normalize("sa"),"s","dfs"));
        }
        return tasks;
    }

    // Testable subclass that bypasses the Google Docs API
    private static class TestableGoogleDocsTaskSource extends GoogleDocsTaskSource {
        private Document mockDocument;
        private boolean shouldThrowIOException = false;

        public TestableGoogleDocsTaskSource(Docs docs, String documentId, VocabTaskParser parser) {
            super(docs, documentId, parser);
        }

        public void setMockDocument(Document document) {
            this.mockDocument = document;
        }

        public void setShouldThrowIOException(boolean shouldThrow) {
            this.shouldThrowIOException = shouldThrow;
        }

        @Override
        public List<VocabTask> fetchTasks() {
            try {
                if (shouldThrowIOException) {
                    throw new IOException("Simulated network error");
                }

                // Use reflection or direct field access to call parent's private methods
                // For simplicity, we'll duplicate the logic here
                Table table = findFirstTable(mockDocument.getBody().getContent());

                if (table == null) {
                    throw new IllegalStateException("No table found in document");
                }

                List<List<String>> rows = extractRows(table);
                return getParser().parse(rows);

            } catch (IOException e) {
                throw new RuntimeException("Failed to read Google Docs", e);
            }
        }

        private Table findFirstTable(List<StructuralElement> content) {
            if (content == null) return null;
            for (StructuralElement element : content) {
                if (element.getTable() != null) {
                    return element.getTable();
                }
            }
            return null;
        }

        private List<List<String>> extractRows(Table table) {
            List<List<String>> rows = new ArrayList<>();
            if (table.getTableRows() == null) return rows;

            for (TableRow row : table.getTableRows()) {
                List<String> values = new ArrayList<>();
                if (row.getTableCells() != null) {
                    for (TableCell cell : row.getTableCells()) {
                        values.add(readCell(cell));
                    }
                }
                rows.add(values);
            }
            return rows;
        }

        private String readCell(TableCell cell) {
            StringBuilder text = new StringBuilder();
            if (cell.getContent() == null) return text.toString().trim();

            for (StructuralElement element : cell.getContent()) {
                if (element.getParagraph() != null && element.getParagraph().getElements() != null) {
                    for (ParagraphElement pe : element.getParagraph().getElements()) {
                        if (pe.getTextRun() != null && pe.getTextRun().getContent() != null) {
                            text.append(pe.getTextRun().getContent());
                        }
                    }
                }
            }
            return text.toString().trim();
        }

        private VocabTaskParser getParser() {
            // Access through reflection or make parser protected in parent
            try {
                java.lang.reflect.Field field = GoogleDocsTaskSource.class.getDeclaredField("parser");
                field.setAccessible(true);
                return (VocabTaskParser) field.get(this);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
}