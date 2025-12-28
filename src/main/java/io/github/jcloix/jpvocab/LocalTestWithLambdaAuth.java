package io.github.jcloix.jpvocab;

import com.google.api.services.docs.v1.Docs;
import io.github.jcloix.jpvocab.config.GoogleDocsAuth;
import io.github.jcloix.jpvocab.model.VocabTask;
import io.github.jcloix.jpvocab.service.TaskDetectionService;
import io.github.jcloix.jpvocab.service.VocabTaskParser;
import io.github.jcloix.jpvocab.service.source.GoogleDocsTaskSource;
import io.github.jcloix.jpvocab.service.source.TaskSource;
import io.github.jcloix.jpvocab.util.LambdaLogger;

import java.util.List;

/**
 * Local test using Lambda-safe UserCredentials (refresh token)
 *
 * Requires environment variables:
 * - GOOGLE_CLIENT_ID
 * - GOOGLE_CLIENT_SECRET
 * - GOOGLE_REFRESH_TOKEN
 * - GOOGLE_DOC_ID
 * - MY_NAME
 */
public class LocalTestWithLambdaAuth {
    public static void main(String[] args) throws Exception {

        String documentId = System.getenv("GOOGLE_DOC_ID");
        String myName = System.getenv("MY_NAME");

        LambdaLogger.log("Authenticating with Google using refresh token...");
        Docs docsClient = GoogleDocsAuth.getDocsServiceWithOAuth();

        LambdaLogger.log("Reading document...");
        VocabTaskParser parser = new VocabTaskParser();
        TaskSource source = new GoogleDocsTaskSource(docsClient, documentId, parser);

        List<VocabTask> allTasks = source.fetchTasks();
        LambdaLogger.log("✓ Total tasks found: " + allTasks.size());

        LambdaLogger.log("\nAll tasks:");
        allTasks.forEach(task ->
                LambdaLogger.log("  - Word: " + task.getWord() +
                        ", Assigned to: " + task.getAssignedTo() +
                        ", Example: " + task.getExampleSentence())
        );

        LambdaLogger.log("\nDetecting pending tasks...");
        TaskDetectionService detector = new TaskDetectionService(myName);
        List<VocabTask> pending = detector.detectPendingTasks(allTasks);

        LambdaLogger.log("✓ Pending tasks for " + myName + ": " + pending.size());
        if (pending.isEmpty()) {
            LambdaLogger.log("  No pending tasks! 🎉");
        } else {
            pending.forEach(task ->
                    LambdaLogger.log("  ⚠ " + task.getWord() + " - needs example sentence")
            );
        }
    }
}
