package io.github.jcloix.jpvocab.task.source;

import io.github.jcloix.jpvocab.domain.VocabTask;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service responsible for detecting pending vocabulary tasks for a given user.
 *
 * <p>A task is considered pending when:
 * <ul>
 *   <li>The task is assigned to the configured user</li>
 *   <li>The example sentence does not indicate completion by that user</li>
 * </ul>
 *
 * <p>This service contains pure business logic and is fully testable
 * without AWS or Google dependencies.
 */
public class TaskDetectionService {

    private final String myName;

    public TaskDetectionService(String myName) {
        this.myName = myName;
    }

    public List<VocabTask> detectPendingTasks(List<VocabTask> allTasks) {
        return allTasks.stream()
                .filter(task -> myName.equals(task.getAssignedTo()))
                .filter(task -> !task.isSentenceDoneBy(myName))
                .collect(Collectors.toList());
    }
}
