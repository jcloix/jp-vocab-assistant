package io.github.jcloix.jpvocab.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.jcloix.jpvocab.notification.DiscordNotifier;
import io.github.jcloix.jpvocab.repository.PersistedTask;
import io.github.jcloix.jpvocab.repository.TaskRepository;
import io.github.jcloix.jpvocab.util.LambdaLogger;

import java.util.Map;
import java.util.Optional;

public class DiscordTaskWorker implements RequestHandler<SQSEvent, Void> {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public Void handleRequest(SQSEvent event, Context context) {
        LambdaLogger.init(context);
        TaskRepository repo = new TaskRepository();

        for (SQSEvent.SQSMessage msg : event.getRecords()) {
            try {
                Map<String, Object> payload =
                        MAPPER.readValue(msg.getBody(), Map.class);

                String word = (String) payload.get("word");
                int rowId = (Integer) payload.get("rowId");
                int choice = (Integer) payload.get("choice");

                Optional<PersistedTask> taskOpt =
                        repo.findByWord(System.getenv("GOOGLE_DOC_ID"), word, rowId);

                if (taskOpt.isEmpty()) {
                    DiscordNotifier.sendMessage("⚠ Task not found for word: " + word);
                    continue;
                }

                PersistedTask task = taskOpt.get();
                repo.markDone(
                        System.getenv("GOOGLE_DOC_ID"),
                        task.getRowId(),
                        task.getWord(),
                        choice
                );

                DiscordNotifier.sendMessage(
                        "✅ You selected choice " + (choice + 1) + " for word: " + word
                );

            } catch (Exception e) {
                LambdaLogger.log("Worker failure: " + e.getMessage());
            }
        }
        return null;
    }
}
