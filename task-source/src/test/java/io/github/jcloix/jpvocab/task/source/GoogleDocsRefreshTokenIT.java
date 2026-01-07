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
@EnabledIfEnvironmentVariable(named = "GOOGLE_REFRESH_TOKEN", matches = ".+")
class GoogleDocsRefreshTokenIT {

    @Test
    void shouldFetchTasksFromRealGoogleDoc_usingRefreshTokenOAuth() throws Exception {

        // Arrange
        String documentId = requireEnv("GOOGLE_DOC_ID");
        String myName = "ジュリアン";

        LambdaLogger.log("Authenticating with Google (refresh token OAuth)...");
        Docs docsClient = GoogleDocsAuth.getDocsServiceWithOAuth();

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
        for(VocabTask task : pending) {
            LambdaLogger.log(task.getWord());
        }
    }

    private String requireEnv(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing required env var: " + name);
        }
        return value;
    }
}
