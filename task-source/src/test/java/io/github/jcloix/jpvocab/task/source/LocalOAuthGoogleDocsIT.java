package io.github.jcloix.jpvocab.task.source;

import com.google.api.services.docs.v1.Docs;
import io.github.jcloix.jpvocab.domain.VocabTask;
import io.github.jcloix.jpvocab.util.LambdaLogger;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;

@Tag("integration")
@EnabledIfEnvironmentVariable(named = "GOOGLE_DOC_ID", matches = ".+")
class LocalOAuthGoogleDocsIT {

    @Test
    void shouldFetchTasksFromRealGoogleDoc_usingOAuth() throws Exception {
        // Arrange
        String clientSecretPath = requireEnv("GOOGLE_CREDENTIALS_PATH");
        String documentId = requireEnv("GOOGLE_DOC_ID");
        String tokensDirectory = requireEnv("TOKEN_FOLDER");
        String myName = requireEnv("MY_NAME");

        LambdaLogger.log("Authenticating with Google (OAuth)...");
        Docs docsClient =
                LocalOAuthGenerator.getDocsServiceWithOAuth(clientSecretPath, tokensDirectory);

        TaskSource source = new GoogleDocsTaskSource(
                docsClient,
                documentId,
                new VocabTaskParser()
        );

        // Act
        List<VocabTask> allTasks = source.fetchTasks();

        // Assert (VERY LIGHT ASSERTIONS)
        assertFalse(allTasks.isEmpty(), "Document should contain at least one task");

        LambdaLogger.log("✓ Tasks fetched: " + allTasks.size());

        TaskDetectionService detector = new TaskDetectionService(myName);
        List<VocabTask> pending = detector.detectPendingTasks(allTasks);

        LambdaLogger.log("✓ Pending tasks for " + myName + ": " + pending.size());
    }

    private String requireEnv(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing required env var: " + name);
        }
        return value;
    }
}
