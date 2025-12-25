package io.github.jcloix.jpvocab.service;

import io.github.jcloix.jpvocab.model.VocabTask;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class VocabTaskParserTest {

    private final VocabTaskParser parser = new VocabTaskParser();

    @Test
    void parse_validRows_createsTasks() {
        List<List<String>> rows = List.of(
                // header
                List.of("A", "B", "C", "Word", "Assigned", "Example"),

                // valid row
                List.of("1", "2", "3", "勉強", "Julien", "日本語を勉強します")
        );

        List<VocabTask> tasks = parser.parse(rows);

        assertEquals(1, tasks.size());

        VocabTask task = tasks.get(0);
        assertEquals("勉強", task.getWord());
        assertEquals("Julien", task.getAssignedTo());
        assertEquals("日本語を勉強します", task.getExampleSentence());
    }

    @Test
    void parse_emptyWord_isSkipped() {
        List<List<String>> rows = List.of(
                List.of("H1", "H2", "H3", "Word", "Assigned", "Example"),
                List.of("1", "2", "3", "   ", "Julien", "Example text")
        );

        List<VocabTask> tasks = parser.parse(rows);

        assertTrue(tasks.isEmpty());
    }

    @Test
    void parse_missingColumns_isSkipped() {
        List<List<String>> rows = List.of(
                List.of("H1", "H2", "H3", "Word", "Assigned", "Example"),
                List.of("1", "2", "3", "食べる") // too short
        );

        List<VocabTask> tasks = parser.parse(rows);

        assertTrue(tasks.isEmpty());
    }

    @Test
    void parse_onlyHeader_returnsEmptyList() {
        List<List<String>> rows = List.of(
                List.of("H1", "H2", "H3", "Word", "Assigned", "Example")
        );

        List<VocabTask> tasks = parser.parse(rows);

        assertTrue(tasks.isEmpty());
    }
}
