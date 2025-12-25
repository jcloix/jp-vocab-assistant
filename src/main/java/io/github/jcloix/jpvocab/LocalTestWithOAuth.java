package io.github.jcloix.jpvocab;

import com.google.api.services.docs.v1.Docs;
import io.github.jcloix.jpvocab.config.GoogleDocsAuth;
import io.github.jcloix.jpvocab.model.VocabTask;
import io.github.jcloix.jpvocab.service.TaskDetectionService;
import io.github.jcloix.jpvocab.service.VocabTaskParser;
import io.github.jcloix.jpvocab.service.source.GoogleDocsTaskSource;
import io.github.jcloix.jpvocab.service.source.TaskSource;

import java.util.List;

/**
 * Local test using OAuth 2.0 authentication with your personal Google account.
 *
 * First run: Will open browser for you to authorize the app
 * Subsequent runs: Uses stored tokens automatically
 *
 */
public class LocalTestWithOAuth {
    public static void main(String[] args) throws Exception {
        // Replace with your actual values
        // Get configuration from environment variables
        String clientSecretPath = System.getenv("GOOGLE_CREDENTIALS_PATH");
        String documentId = System.getenv("GOOGLE_DOC_ID");
        String tokensDirectory = System.getenv("TOKEN_FOLDER");
        String myName = System.getenv("MY_NAME");

        System.out.println("Authenticating with Google...");
        Docs docsClient = GoogleDocsAuth.getDocsServiceWithOAuth(clientSecretPath, tokensDirectory);

        System.out.println("Reading document...");
        VocabTaskParser parser = new VocabTaskParser();
        TaskSource source = new GoogleDocsTaskSource(docsClient, documentId, parser);

        List<VocabTask> allTasks = source.fetchTasks();
        System.out.println("✓ Total tasks found: " + allTasks.size());

        System.out.println("\nAll tasks:");
        allTasks.forEach(task ->
                System.out.println("  - Word: " + task.getWord() +
                        ", Assigned to: " + task.getAssignedTo() +
                        ", Example: " + task.getExampleSentence())
        );

        System.out.println("\nDetecting pending tasks...");
        TaskDetectionService detector = new TaskDetectionService(myName);
        List<VocabTask> pending = detector.detectPendingTasks(allTasks);

        System.out.println("✓ Pending tasks for " + myName + ": " + pending.size());
        if (pending.isEmpty()) {
            System.out.println("  No pending tasks! 🎉");
        } else {
            pending.forEach(task ->
                    System.out.println("  ⚠ " + task.getWord() + " - needs example sentence")
            );
        }
    }
}