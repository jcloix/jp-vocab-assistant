package io.github.jcloix.jpvocab.lambda.completion;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.services.docs.v1.Docs;
import io.github.jcloix.jpvocab.notification.DiscordNotifier;
import io.github.jcloix.jpvocab.persistence.PersistedTask;
import io.github.jcloix.jpvocab.persistence.TaskRepository;
import io.github.jcloix.jpvocab.task.source.*;
import io.github.jcloix.jpvocab.util.LambdaLogger;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class DiscordTaskWorker implements RequestHandler<SQSEvent, Void> {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    // Lazily initialized (cold start only)
    private static Docs docsClient;
    private static TaskSource taskSource;

    @Override
    public Void handleRequest(SQSEvent event, Context context) {
        LambdaLogger.init(context);
        LambdaLogger.log("Discord task worker started");

        TaskRepository repo = new TaskRepository();
        initGoogleDocs();

        for (SQSEvent.SQSMessage msg : event.getRecords()) {
            LambdaLogger.log("Received SQS payload: " + msg.getBody());
            try {
                Map<String, Object> payload = MAPPER.readValue(msg.getBody(), Map.class);

                // Skip warmup messages
                if (payload.containsKey("warmup") && Boolean.TRUE.equals(payload.get("warmup"))) {
                    LambdaLogger.log("Skipping warmup message");
                    continue;
                }

                // Validate essential fields
                if (!payload.containsKey("word") || !payload.containsKey("rowId") || !payload.containsKey("choice")) {
                    LambdaLogger.log("Invalid SQS payload, missing fields: " + msg.getBody());
                    continue;
                }

                // Safely extract numeric fields
                String word = (String) payload.get("word");
                int rowId = ((Number) payload.get("rowId")).intValue();
                int choice = ((Number) payload.get("choice")).intValue();

                // Notify user immediately that their choice was received
                CompletableFuture<Void> firstNotification = CompletableFuture.runAsync(() -> {
                    try {
                        DiscordNotifier.sendMessage(
                                "✅ You selected choice " + (choice + 1)
                                        + " for word: " + word
                        );
                    } catch (Exception e) {
                        LambdaLogger.log("Failed to notify Discord for selection of word: " + word, e);
                    }
                });

                // Find persisted task in DB
                Optional<PersistedTask> taskOpt =
                        repo.findByWord(System.getenv("GOOGLE_DOC_ID"), word, rowId);

                if (taskOpt.isEmpty()) {
                    DiscordNotifier.sendMessage("⚠ Task not found for word: " + word);
                    continue;
                }

                PersistedTask task = taskOpt.get();

                if (choice < 0 || choice >= task.getChoices().size()) {
                    DiscordNotifier.sendMessage("⚠ Invalid choice index for word: " + word);
                    continue;
                }

                // Complete task in Google Docs
                String resolvedSentence = task.getChoices().get(choice);
                ResultTask resultTask = taskSource.completeTask(task.getWord(), task.getRowId(), resolvedSentence);

                // Mark task as done in DB
                repo.markDone(System.getenv("GOOGLE_DOC_ID"), task.getRowId(), task.getWord(), choice);

                // Notify Discord with the result
                DiscordNotifier.sendMessage(resultTask.toMessage());

                // Wait for the first notification to complete
                firstNotification.join();

            } catch (Exception e) {
                handleError(e);
            }
        }

        return null;
    }

    private void handleError(Exception e) {
        LambdaLogger.log("Worker failure: ", e);
        try {
            DiscordNotifier.sendMessage("⚠ Issue occurred while handling task");
        } catch (Exception e2) {
            LambdaLogger.log("Issue sending failure notification: ", e2);
        }
    }

    /**
     * Cold-start initialization of Google Docs dependencies.
     */
    private static synchronized void initGoogleDocs() {
        if (taskSource != null) {
            return;
        }

        try {
            docsClient = GoogleDocsAuth.getDocsServiceWithOAuth();

            taskSource = new GoogleDocsTaskSource(
                    docsClient,
                    System.getenv("GOOGLE_DOC_ID"),
                    new VocabTaskParser()
            );

        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize Google Docs client", e);
        }
    }
}
