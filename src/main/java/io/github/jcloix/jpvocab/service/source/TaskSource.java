package io.github.jcloix.jpvocab.service.source;

import io.github.jcloix.jpvocab.model.VocabTask;

import java.util.List;

/**
 * Abstraction representing a source of vocabulary tasks.
 *
 * <p>Implementations of this interface are responsible for retrieving
 * {@link io.github.jcloix.jpvocab.model.VocabTask} objects from a specific source,
 * such as Google Docs.
 *
 * <p>This interface enables:
 * <ul>
 *   <li>Decoupling business logic from data sources</li>
 *   <li>Easy testing via mock implementations</li>
 *   <li>Future extensibility (other sources)</li>
 * </ul>
 */
public interface TaskSource {
    List<VocabTask> fetchTasks();
}
