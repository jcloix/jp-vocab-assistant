package io.github.jcloix.jpvocab.service;

import io.github.jcloix.jpvocab.domain.normalization.NormalizedWord;
import io.github.jcloix.jpvocab.domain.normalization.WordNormalizer;
import io.github.jcloix.jpvocab.model.VocabTask;
import org.junit.jupiter.api.Test;

import java.util.List;

import static io.github.jcloix.jpvocab.domain.normalization.WordNormalizer.normalize;
import static org.junit.jupiter.api.Assertions.assertEquals;

class TaskDetectionServiceTest {

    @Test
    void shouldDetectOnlyTasksAssignedToMeWithMissingSentence() {
        // given
        TaskDetectionService service = new TaskDetectionService("Julien");
        List<VocabTask> tasks = List.of(
                new VocabTask(1,"勉強", normalize("勉強"),"Julien", "Example test not done"),
                new VocabTask(3,"旅行", normalize("旅行"), "Julien", "Example 旅行 done by Julien"),
                new VocabTask(8,"日本語", normalize("日本語"), "SomeoneElse", "日本語 is difficult")
        );

        // when
        List<VocabTask> result = service.detectPendingTasks(tasks);

        // then
        assertEquals(1, result.size());
        assertEquals("勉強", result.get(0).getWord());
    }

    @Test
    void shouldReturnEmptyListWhenNoPendingTasks() {
        // given
        TaskDetectionService service = new TaskDetectionService("Julien");

        List<VocabTask> tasks = List.of(
                new VocabTask(1,"勉強", normalize("勉強"),"Julien", "I 勉強 a lot (Julien)"),
                new VocabTask(4,"旅行", normalize("旅行"),"SomeoneElse", "")
        );

        // when
        List<VocabTask> result = service.detectPendingTasks(tasks);

        // then
        assertEquals(0, result.size());
    }
}
